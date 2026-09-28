package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.*
import org.junit.Test

class CollectionPriorityEngineTest {

    private val referenceTime = 1700000000000L
    private val oneDayMs = 24 * 60 * 60 * 1000L

    private fun createCustomer(
        id: String,
        name: String = "Customer $id",
        phone: String = "9876543210",
        outstandingAmount: Double = 0.0,
        totalPurchases: Double = 0.0,
        totalPaid: Double = 0.0,
        createdAt: Long = referenceTime - (60 * oneDayMs)
    ) = Customer(
        id = id,
        businessId = "biz1",
        name = name,
        phone = phone,
        email = "cust$id@example.com",
        address = "Street 1",
        totalPurchases = totalPurchases,
        totalPaid = totalPaid,
        outstandingAmount = outstandingAmount,
        createdAt = createdAt,
        updatedAt = referenceTime
    )

    private fun createSale(
        id: String,
        customerId: String,
        grandTotal: Double,
        createdAt: Long
    ) = Sale(
        id = id,
        businessId = "biz1",
        invoiceNumber = "INV-$id",
        customerId = customerId,
        items = listOf(
            SaleItem(
                productId = "p1",
                productName = "Product 1",
                quantity = 1,
                unitPrice = grandTotal,
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
    fun test1_emptyCustomerList() {
        val summary = CollectionPriorityEngine.evaluate(
            customers = emptyList(),
            sales = emptyList(),
            currentTime = referenceTime
        )

        assertEquals(0, summary.customersAnalyzed)
        assertEquals(0, summary.customersWithOutstanding)
        assertEquals(0.0, summary.totalOutstanding, 0.001)
        assertTrue(summary.results.isEmpty())
    }

    @Test
    fun test2_customerWithZeroOutstanding() {
        val c1 = createCustomer("c1", outstandingAmount = 0.0)
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (5 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertEquals(1, summary.customersAnalyzed)
        assertEquals(0, summary.customersWithOutstanding)
        val res = summary.results.first()
        assertEquals(CollectionPriorityLevel.NONE, res.priority)
        assertEquals(0.0, res.outstandingAmount, 0.001)
        assertTrue(res.reasons.contains(CollectionPriorityEngine.Reasons.NO_OUTSTANDING))
    }

    @Test
    fun test3_customerWithOutstanding() {
        val c1 = createCustomer("c1", outstandingAmount = 500.0)
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (5 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertEquals(1, summary.customersWithOutstanding)
        assertEquals(500.0, summary.totalOutstanding, 0.001)
        val res = summary.results.first()
        assertEquals(500.0, res.outstandingAmount, 0.001)
        assertNotEquals(CollectionPriorityLevel.NONE, res.priority)
    }

    @Test
    fun test4_multipleCustomers() {
        val c1 = createCustomer("c1", outstandingAmount = 2000.0)
        val c2 = createCustomer("c2", outstandingAmount = 6000.0)
        val s1 = createSale("s1", "c1", 3000.0, referenceTime - (5 * oneDayMs))
        val s2 = createSale("s2", "c2", 8000.0, referenceTime - (50 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1, c2),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(2, summary.customersAnalyzed)
        assertEquals(2, summary.customersWithOutstanding)
        assertEquals(8000.0, summary.totalOutstanding, 0.001)
    }

    @Test
    fun test5_highPriorityRule() {
        // High outstanding (>= 5000) and inactive (last purchase 50 days ago >= 45)
        val c1 = createCustomer("c1", outstandingAmount = 7500.0)
        val s1 = createSale("s1", "c1", 10000.0, referenceTime - (50 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertEquals(CollectionPriorityLevel.HIGH, res.priority)
        assertTrue(res.reasons.contains(CollectionPriorityEngine.Reasons.HIGH_DUE_INACTIVE))
    }

    @Test
    fun test6_mediumPriorityRule() {
        // Moderate outstanding (>= 1000 and < 5000) and inactive (>= 45 days)
        val c1 = createCustomer("c1", outstandingAmount = 2500.0)
        val s1 = createSale("s1", "c1", 5000.0, referenceTime - (48 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertEquals(CollectionPriorityLevel.MEDIUM, res.priority)
        assertTrue(res.reasons.contains(CollectionPriorityEngine.Reasons.MODERATE_DUE_INACTIVE))
    }

    @Test
    fun test7_lowPriorityRule() {
        // Nominal outstanding (< 1000)
        val c1 = createCustomer("c1", outstandingAmount = 450.0)
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (10 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertEquals(CollectionPriorityLevel.LOW, res.priority)
        assertTrue(res.reasons.contains(CollectionPriorityEngine.Reasons.LOW_DUE))
    }

    @Test
    fun test8_noOutstandingClassification() {
        val c1 = createCustomer("c1", outstandingAmount = 0.0)

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = emptyList(),
            currentTime = referenceTime
        )

        assertEquals(CollectionPriorityLevel.NONE, summary.results.first().priority)
        assertEquals(1, summary.noneCount)
    }

    @Test
    fun test9_insufficientDataHandling() {
        // Customer with small outstanding but no sales recorded
        val c1 = createCustomer("c1", outstandingAmount = 200.0, createdAt = referenceTime - (100 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = emptyList(),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertEquals(CollectionPriorityLevel.INSUFFICIENT_DATA, res.priority)
        assertTrue(res.reasons.contains(CollectionPriorityEngine.Reasons.INSUFFICIENT_HISTORY))
    }

    @Test
    fun test10_outstandingAmountOrdering() {
        // Multiple customers in same priority group should be sorted by outstanding amount descending
        val c1 = createCustomer("c1", outstandingAmount = 300.0)
        val c2 = createCustomer("c2", outstandingAmount = 700.0)
        val s1 = createSale("s1", "c1", 1000.0, referenceTime - (5 * oneDayMs))
        val s2 = createSale("s2", "c2", 1000.0, referenceTime - (5 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1, c2),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(2, summary.results.size)
        // c2 (700) should appear before c1 (300)
        assertEquals("c2", summary.results[0].customerId)
        assertEquals("c1", summary.results[1].customerId)
    }

    @Test
    fun test11_priorityOrdering() {
        // HIGH priority must appear before MEDIUM, LOW, and NONE
        val cLow = createCustomer("cLow", outstandingAmount = 200.0)
        val cHigh = createCustomer("cHigh", outstandingAmount = 8000.0)
        val cMed = createCustomer("cMed", outstandingAmount = 3000.0)
        val cNone = createCustomer("cNone", outstandingAmount = 0.0)

        val sLow = createSale("sLow", "cLow", 1000.0, referenceTime - (5 * oneDayMs))
        val sHigh = createSale("sHigh", "cHigh", 10000.0, referenceTime - (50 * oneDayMs))
        val sMed = createSale("sMed", "cMed", 5000.0, referenceTime - (50 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(cLow, cHigh, cNone, cMed),
            sales = listOf(sLow, sHigh, sMed),
            currentTime = referenceTime
        )

        assertEquals(CollectionPriorityLevel.HIGH, summary.results[0].priority)
        assertEquals(CollectionPriorityLevel.MEDIUM, summary.results[1].priority)
        assertEquals(CollectionPriorityLevel.LOW, summary.results[2].priority)
        assertEquals(CollectionPriorityLevel.NONE, summary.results[3].priority)
    }

    @Test
    fun test12_deterministicReasons() {
        val c1 = createCustomer("c1", outstandingAmount = 6000.0)
        val s1 = createSale("s1", "c1", 6000.0, referenceTime - (5 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertFalse(res.reasons.isEmpty())
        assertEquals(CollectionPriorityEngine.Reasons.HIGH_DUE_ACTIVE, res.reasons.first())
    }

    @Test
    fun test13_missingCustomerIntelligence() {
        // CustomerMetrics constructed with empty/null fields
        val metric = CustomerMetrics(
            customerId = "c1",
            customerName = "Name",
            phone = "123",
            periodPurchases = 0.0,
            periodOrders = 0,
            periodUnitsPurchased = 0,
            lifetimePurchases = 0.0,
            lifetimeOrders = 0,
            lifetimeUnitsPurchased = 0,
            averageOrderValue = 0.0,
            firstPurchaseDate = null,
            lastPurchaseDate = null,
            daysSinceLastPurchase = null,
            purchaseFrequencyPerMonth = 0.0,
            outstandingAmount = 0.0,
            hasOutstanding = false,
            totalPaid = 0.0,
            segment = CustomerSegment.INSUFFICIENT_DATA,
            activityStatus = CustomerActivityStatus.INSUFFICIENT_DATA,
            hasSufficientData = false
        )

        val summary = CollectionPriorityEngine.evaluateFromMetrics(listOf(metric))
        assertEquals(CollectionPriorityLevel.NONE, summary.results.first().priority)
    }

    @Test
    fun test14_missingDates() {
        val c1 = createCustomer("c1", outstandingAmount = 2000.0)
        val s1 = createSale("s1", "c1", 2000.0, 0L) // invalid date filtered out by intelligence engine

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertNotNull(res)
        assertTrue(res.outstandingAmount.isFinite())
    }

    @Test
    fun test15_invalidDates() {
        val c1 = createCustomer("c1", outstandingAmount = 1500.0)
        val sFuture = createSale("sFuture", "c1", 1500.0, referenceTime + (100 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(sFuture),
            currentTime = referenceTime
        )

        assertNotNull(summary.results.first())
        assertEquals(1500.0, summary.results.first().outstandingAmount, 0.001)
    }

    @Test
    fun test16_zeroValues() {
        val c1 = createCustomer("c1", outstandingAmount = 0.0, totalPurchases = 0.0, totalPaid = 0.0)
        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = emptyList(),
            currentTime = referenceTime
        )

        assertEquals(0.0, summary.results.first().outstandingAmount, 0.001)
        assertEquals(CollectionPriorityLevel.NONE, summary.results.first().priority)
    }

    @Test
    fun test17_veryLargeOutstandingValues() {
        val c1 = createCustomer("c1", outstandingAmount = 100_000_000.0)
        val s1 = createSale("s1", "c1", 200_000_000.0, referenceTime - (50 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertEquals(CollectionPriorityLevel.HIGH, res.priority)
        assertEquals(100_000_000.0, res.outstandingAmount, 0.001)
    }

    @Test
    fun test18_nanInfinitySafety() {
        val c1 = createCustomer("c1", outstandingAmount = Double.NaN, totalPaid = Double.POSITIVE_INFINITY)

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1),
            sales = emptyList(),
            currentTime = referenceTime
        )

        val res = summary.results.first()
        assertTrue(res.outstandingAmount.isFinite())
        assertEquals(0.0, res.outstandingAmount, 0.001)
        assertTrue(summary.totalOutstanding.isFinite())
    }

    @Test
    fun test19_multipleCustomersWithSamePriority() {
        val c1 = createCustomer("c1", outstandingAmount = 6000.0)
        val c2 = createCustomer("c2", outstandingAmount = 9000.0)
        val s1 = createSale("s1", "c1", 10000.0, referenceTime - (50 * oneDayMs))
        val s2 = createSale("s2", "c2", 15000.0, referenceTime - (50 * oneDayMs))

        val summary = CollectionPriorityEngine.evaluate(
            customers = listOf(c1, c2),
            sales = listOf(s1, s2),
            currentTime = referenceTime
        )

        assertEquals(CollectionPriorityLevel.HIGH, summary.results[0].priority)
        assertEquals(CollectionPriorityLevel.HIGH, summary.results[1].priority)
        assertEquals("c2", summary.results[0].customerId)
        assertEquals("c1", summary.results[1].customerId)
    }

    @Test
    fun test20_existingCustomerIntelligenceEngineCompatibility() {
        val c1 = createCustomer("c1", outstandingAmount = 3000.0)
        val s1 = createSale("s1", "c1", 5000.0, referenceTime - (10 * oneDayMs))

        val intel = CustomerIntelligenceEngine.analyze(
            customers = listOf(c1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        val summary = CollectionPriorityEngine.evaluateFromMetrics(intel.customers)

        assertEquals(1, summary.customersAnalyzed)
        val res = summary.results.first()
        assertEquals(intel.customers.first().customerId, res.customerId)
        assertEquals(intel.customers.first().segment, res.segment)
        assertEquals(intel.customers.first().activityStatus, res.activityStatus)
        assertEquals(intel.customers.first().outstandingAmount, res.outstandingAmount, 0.001)
    }
}
