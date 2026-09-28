package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.*
import org.junit.Test

class CustomerIntelligenceEngineTest {

    private val referenceTime = 1700000000000L // arbitrary fixed epoch
    private val oneDayMs = 24 * 60 * 60 * 1000L

    private fun createCustomer(
        id: String,
        name: String = "Test Customer $id",
        phone: String = "9876543210",
        totalPurchases: Double = 0.0,
        totalPaid: Double = 0.0,
        outstandingAmount: Double = 0.0,
        createdAt: Long = referenceTime - (30 * oneDayMs)
    ) = Customer(
        id = id,
        businessId = "biz1",
        name = name,
        phone = phone,
        email = "test@example.com",
        address = "123 Street",
        totalPurchases = totalPurchases,
        totalPaid = totalPaid,
        outstandingAmount = outstandingAmount,
        createdAt = createdAt,
        updatedAt = referenceTime
    )

    private fun createSale(
        id: String,
        customerId: String?,
        grandTotal: Double,
        createdAt: Long,
        itemsCount: Int = 1
    ) = Sale(
        id = id,
        businessId = "biz1",
        invoiceNumber = "INV-$id",
        customerId = customerId,
        items = listOf(
            SaleItem(
                productId = "prod1",
                productName = "Product 1",
                quantity = itemsCount,
                unitPrice = grandTotal / itemsCount,
                totalPrice = grandTotal
            )
        ),
        subtotal = grandTotal,
        discount = 0.0,
        tax = 0.0,
        grandTotal = grandTotal,
        amountPaid = grandTotal,
        outstanding = 0.0,
        paymentStatus = "PAID",
        paymentMethod = "CASH",
        createdAt = createdAt,
        updatedAt = createdAt
    )

    @Test
    fun test1_emptyCustomers() {
        val result = CustomerIntelligenceEngine.analyze(
            customers = emptyList(),
            sales = emptyList(),
            currentTime = referenceTime
        )

        assertEquals(0, result.totalCustomersAnalyzed)
        assertEquals(0, result.customers.size)
        assertEquals(0.0, result.totalCustomerSalesLifetime, 0.001)
        assertEquals(0.0, result.totalCustomerSalesPeriod, 0.001)
        assertEquals(0.0, result.totalOutstandingAmount, 0.001)
    }

    @Test
    fun test2_customerWithOnePurchase() {
        val customer = createCustomer("c1")
        val sale = createSale("s1", "c1", 1200.0, referenceTime - (5 * oneDayMs), itemsCount = 2)

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(customer),
            sales = listOf(sale),
            currentTime = referenceTime
        )

        assertEquals(1, result.totalCustomersAnalyzed)
        val metric = result.customers.first()
        assertEquals(1, metric.lifetimeOrders)
        assertEquals(1200.0, metric.lifetimePurchases, 0.001)
        assertEquals(1200.0, metric.periodPurchases, 0.001)
        assertEquals(2, metric.lifetimeUnitsPurchased)
        assertEquals(1200.0, metric.averageOrderValue, 0.001)
        assertEquals(5L, metric.daysSinceLastPurchase)
        assertTrue(metric.hasSufficientData)
    }

    @Test
    fun test3_customerWithMultiplePurchases() {
        val customer = createCustomer("c1")
        val s1 = createSale("s1", "c1", 500.0, referenceTime - (20 * oneDayMs), itemsCount = 1)
        val s2 = createSale("s2", "c1", 1500.0, referenceTime - (10 * oneDayMs), itemsCount = 3)
        val s3 = createSale("s3", "c1", 1000.0, referenceTime - (2 * oneDayMs), itemsCount = 2)

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(customer),
            sales = listOf(s1, s2, s3),
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertEquals(3, metric.lifetimeOrders)
        assertEquals(3000.0, metric.lifetimePurchases, 0.001)
        assertEquals(6, metric.lifetimeUnitsPurchased)
        assertEquals(1000.0, metric.averageOrderValue, 0.001)
        assertEquals(2L, metric.daysSinceLastPurchase)
    }

    @Test
    fun test4_multipleCustomers() {
        val c1 = createCustomer("c1", "Alice")
        val c2 = createCustomer("c2", "Bob")
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (5 * oneDayMs))
        val s2 = createSale("s2", "c2", 2000.0, referenceTime - (15 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1, c2),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(2, result.totalCustomersAnalyzed)
        assertEquals(3000.0, result.totalCustomerSalesLifetime, 0.001)
        assertEquals(3000.0, result.totalCustomerSalesPeriod, 0.001)
    }

    @Test
    fun test5_purchaseTotalsPeriodVsLifetime() {
        val c1 = createCustomer("c1")
        // s1 is 40 days ago (outside default 30-day period)
        val s1 = createSale("s1", "c1", 3000.0, referenceTime - (40 * oneDayMs))
        // s2 is 10 days ago (inside 30-day period)
        val s2 = createSale("s2", "c1", 2000.0, referenceTime - (10 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1, s2),
            analysisDays = 30,
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertEquals(5000.0, metric.lifetimePurchases, 0.001)
        assertEquals(2, metric.lifetimeOrders)
        assertEquals(2000.0, metric.periodPurchases, 0.001)
        assertEquals(1, metric.periodOrders)
    }

    @Test
    fun test6_orderCount() {
        val c1 = createCustomer("c1")
        val sales = (1..5).map { i ->
            createSale("s$i", "c1", 100.0, referenceTime - (i * oneDayMs))
        }

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = sales,
            currentTime = referenceTime
        )

        assertEquals(5, result.customers.first().lifetimeOrders)
        assertEquals(5, result.customers.first().periodOrders)
    }

    @Test
    fun test7_averageOrderValue() {
        val c1 = createCustomer("c1")
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (5 * oneDayMs))
        val s2 = createSale("s2", "c1", 3000.0, referenceTime - (2 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(2000.0, result.customers.first().averageOrderValue, 0.001)
    }

    @Test
    fun test8_lastPurchaseDate() {
        val c1 = createCustomer("c1")
        val t1 = referenceTime - (10 * oneDayMs)
        val t2 = referenceTime - (3 * oneDayMs)
        val s1 = createSale("s1", "c1", 500.0, t1)
        val s2 = createSale("s2", "c1", 500.0, t2)

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(t2, result.customers.first().lastPurchaseDate)
        assertEquals(t1, result.customers.first().firstPurchaseDate)
    }

    @Test
    fun test9_daysSinceLastPurchase() {
        val c1 = createCustomer("c1")
        val s1 = createSale("s1", "c1", 500.0, referenceTime - (12 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertEquals(12L, result.customers.first().daysSinceLastPurchase)
    }

    @Test
    fun test10_activeCustomerClassification() {
        val c1 = createCustomer("c1", createdAt = referenceTime - (60 * oneDayMs))
        val s1 = createSale("s1", "c1", 500.0, referenceTime - (45 * oneDayMs))
        val s2 = createSale("s2", "c1", 500.0, referenceTime - (5 * oneDayMs)) // 5 days ago <= 30

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(CustomerActivityStatus.ACTIVE, result.customers.first().activityStatus)
    }

    @Test
    fun test11_inactiveCustomerClassification() {
        val c1 = createCustomer("c1", createdAt = referenceTime - (100 * oneDayMs))
        // Most recent purchase was 50 days ago (>= 45 days threshold)
        val s1 = createSale("s1", "c1", 500.0, referenceTime - (50 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertEquals(CustomerActivityStatus.INACTIVE, result.customers.first().activityStatus)
        assertEquals(CustomerSegment.INACTIVE, result.customers.first().segment)
    }

    @Test
    fun test12_newCustomerClassification() {
        // Customer whose first purchase was 5 days ago (<= 14 days)
        val c1 = createCustomer("c1", createdAt = referenceTime - (5 * oneDayMs))
        val s1 = createSale("s1", "c1", 500.0, referenceTime - (5 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertEquals(CustomerActivityStatus.NEW, result.customers.first().activityStatus)
        assertEquals(CustomerSegment.NEW, result.customers.first().segment)
    }

    @Test
    fun test13_frequentCustomerClassification() {
        val c1 = createCustomer("c1", createdAt = referenceTime - (40 * oneDayMs))
        // 4 orders within 20 days span -> frequency > 1.5 orders/month
        val s1 = createSale("s1", "c1", 500.0, referenceTime - (20 * oneDayMs))
        val s2 = createSale("s2", "c1", 500.0, referenceTime - (15 * oneDayMs))
        val s3 = createSale("s3", "c1", 500.0, referenceTime - (10 * oneDayMs))
        val s4 = createSale("s4", "c1", 500.0, referenceTime - (5 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1, s2, s3, s4),
            currentTime = referenceTime
        )

        assertEquals(CustomerSegment.FREQUENT, result.customers.first().segment)
    }

    @Test
    fun test14_highValueClassification() {
        val c1 = createCustomer("c1")
        // Single order with grandTotal = ₹12,000 (>= 10,000 lifetime threshold)
        val s1 = createSale("s1", "c1", 12000.0, referenceTime - (10 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertEquals(CustomerSegment.HIGH_VALUE, result.customers.first().segment)
        assertEquals(1, result.highValueCustomerCount)
    }

    @Test
    fun test15_outstandingAmountHandling() {
        val c1 = createCustomer("c1", outstandingAmount = 4500.0, totalPaid = 1500.0)
        val s1 = createSale("s1", "c1", 6000.0, referenceTime - (5 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertEquals(4500.0, metric.outstandingAmount, 0.001)
        assertTrue(metric.hasOutstanding)
        assertEquals(1500.0, metric.totalPaid, 0.001)
        assertEquals(4500.0, result.totalOutstandingAmount, 0.001)
        assertEquals(1, result.customersWithOutstandingCount)
    }

    @Test
    fun test16_missingCustomerReference() {
        val c1 = createCustomer("c1")
        // s1 belongs to c1, s2 belongs to an unknown customer "c_unknown", s3 has null customerId (walk-in)
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (5 * oneDayMs))
        val s2 = createSale("s2", "c_unknown", 5000.0, referenceTime - (5 * oneDayMs))
        val s3 = createSale("s3", null, 2000.0, referenceTime - (5 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1, s2, s3),
            currentTime = referenceTime
        )

        assertEquals(1, result.totalCustomersAnalyzed)
        val metric = result.customers.first()
        assertEquals(1, metric.lifetimeOrders)
        assertEquals(1000.0, metric.lifetimePurchases, 0.001)
    }

    @Test
    fun test17_insufficientDataHandling() {
        // Customer with no sales and created a long time ago
        val c1 = createCustomer("c1", createdAt = referenceTime - (120 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = emptyList(),
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertEquals(CustomerSegment.INSUFFICIENT_DATA, metric.segment)
        assertEquals(CustomerActivityStatus.INSUFFICIENT_DATA, metric.activityStatus)
        assertFalse(metric.hasSufficientData)
        assertNull(metric.daysSinceLastPurchase)
        assertEquals(0.0, metric.averageOrderValue, 0.001)
    }

    @Test
    fun test18_zeroValues() {
        val c1 = createCustomer("c1", totalPurchases = 0.0, totalPaid = 0.0, outstandingAmount = 0.0)
        val s1 = createSale("s1", "c1", 0.0, referenceTime - (5 * oneDayMs), itemsCount = 0)

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertEquals(0.0, metric.lifetimePurchases, 0.001)
        assertEquals(0.0, metric.averageOrderValue, 0.001)
        assertEquals(0.0, metric.outstandingAmount, 0.001)
        assertFalse(metric.hasOutstanding)
    }

    @Test
    fun test19_invalidMissingDates() {
        val c1 = createCustomer("c1")
        // Future sales or sales with 0 timestamp
        val sInvalid1 = createSale("s1", "c1", 1000.0, 0L)
        val sInvalid2 = createSale("s2", "c1", 1000.0, referenceTime + (10 * oneDayMs)) // future

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(sInvalid1, sInvalid2),
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertEquals(0, metric.lifetimeOrders)
        assertFalse(metric.hasSufficientData)
    }

    @Test
    fun test20_nanInfinitySafety() {
        val c1 = createCustomer("c1", outstandingAmount = Double.NaN, totalPurchases = Double.POSITIVE_INFINITY)
        val s1 = createSale("s1", "c1", Double.NaN, referenceTime - (5 * oneDayMs))

        val result = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val metric = result.customers.first()
        assertTrue(metric.lifetimePurchases.isFinite())
        assertTrue(metric.averageOrderValue.isFinite())
        assertTrue(metric.outstandingAmount.isFinite())
        assertTrue(result.totalCustomerSalesLifetime.isFinite())
        assertTrue(result.totalOutstandingAmount.isFinite())
    }
}
