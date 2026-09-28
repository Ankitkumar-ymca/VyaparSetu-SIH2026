package com.example.domain.intelligence

import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryIntelligenceEngineTest {

    private val now = 1700000000000L // arbitrary fixed timestamp for deterministic tests
    private val oneDayMillis = 24L * 60L * 60L * 1000L

    private fun createProduct(
        id: String,
        name: String = "Test Product $id",
        currentStock: Int = 10,
        minStock: Int = 2,
        purchasePrice: Double = 100.0,
        sellingPrice: Double = 150.0
    ): Product {
        return Product(
            id = id,
            businessId = "biz1",
            name = name,
            sku = "SKU-$id",
            barcode = "BAR-$id",
            category = "General",
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            currentStock = currentStock,
            minStock = minStock,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = now - 100 * oneDayMillis,
            updatedAt = now - 10 * oneDayMillis,
            isActive = true
        )
    }

    private fun createSale(
        id: String,
        timestamp: Long,
        items: List<SaleItem>
    ): Sale {
        val total = items.sumOf { it.totalPrice }
        return Sale(
            id = id,
            businessId = "biz1",
            invoiceNumber = "INV-$id",
            customerId = "cust1",
            items = items,
            subtotal = total,
            discount = 0.0,
            tax = 0.0,
            grandTotal = total,
            amountPaid = total,
            outstanding = 0.0,
            paymentStatus = "PAID",
            paymentMethod = "CASH",
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    @Test
    fun test1_productWithZeroStock() {
        val p = createProduct("p1", currentStock = 0, purchasePrice = 200.0)
        val sales = listOf(
            createSale("s1", now - 2 * oneDayMillis, listOf(SaleItem("p1", "Item", 10, 250.0, 2500.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(0, metric.currentStock)
        assertEquals(0.0, metric.inventoryValue, 0.001)
        assertNull(metric.daysOfStockRemaining)
        assertFalse(metric.isDeadStock) // sold recently even though stock is 0
    }

    @Test
    fun test2_productWithZeroSales() {
        val p = createProduct("p2", currentStock = 20, purchasePrice = 50.0)
        val summary = InventoryIntelligenceEngine.analyze(listOf(p), emptyList(), referenceTime = now)
        val metric = summary.products.first()

        assertEquals(0, metric.unitsSold)
        assertEquals(0.0, metric.salesVelocityPerDay, 0.001)
        assertNull(metric.daysOfStockRemaining)
        assertTrue(metric.isDeadStock) // currentStock > 0 and no sales in 45 days
        assertEquals(VelocityCategory.DEAD_STOCK, metric.velocityCategory)
        assertEquals(1000.0, metric.inventoryValue, 0.001)
        assertEquals(1000.0, metric.capitalLocked, 0.001)
        assertEquals(DemandTrend.INSUFFICIENT_DATA, metric.demandTrend)
    }

    @Test
    fun test3_productWithRecentSales() {
        val p = createProduct("p3", currentStock = 30, purchasePrice = 100.0)
        val sales = listOf(
            createSale("s1", now - 5 * oneDayMillis, listOf(SaleItem("p3", "Item", 15, 150.0, 2250.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(15, metric.unitsSold)
        assertEquals(0.5, metric.salesVelocityPerDay, 0.001) // 15 / 30 = 0.5 units/day
        assertNotNull(metric.daysOfStockRemaining)
        assertEquals(60.0, metric.daysOfStockRemaining!!, 0.001) // 30 / 0.5 = 60 days
        assertFalse(metric.isDeadStock)
    }

    @Test
    fun test4_fastMovingProduct() {
        // Fast moving: > 1.5 units/day -> > 45 units in 30 days
        val p = createProduct("p4", currentStock = 50, purchasePrice = 10.0)
        val sales = listOf(
            createSale("s1", now - 10 * oneDayMillis, listOf(SaleItem("p4", "Item", 60, 20.0, 1200.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(60, metric.unitsSold)
        assertEquals(2.0, metric.salesVelocityPerDay, 0.001) // 60 / 30 = 2.0
        assertEquals(VelocityCategory.FAST_MOVING, metric.velocityCategory)
        assertEquals(25.0, metric.daysOfStockRemaining!!, 0.001) // 50 / 2.0 = 25 days
        assertEquals(0.0, metric.capitalLocked, 0.001) // fast moving has 0 capital locked
    }

    @Test
    fun test5_slowMovingProduct() {
        // High stock relative to velocity (> 45 days coverage)
        val p = createProduct("p5", currentStock = 100, purchasePrice = 20.0)
        val sales = listOf(
            createSale("s1", now - 5 * oneDayMillis, listOf(SaleItem("p5", "Item", 15, 30.0, 450.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(0.5, metric.salesVelocityPerDay, 0.001)
        assertEquals(200.0, metric.daysOfStockRemaining!!, 0.001) // 100 / 0.5 = 200 days
        assertEquals(VelocityCategory.SLOW_MOVING, metric.velocityCategory)
        assertEquals(2000.0, metric.capitalLocked, 0.001)
    }

    @Test
    fun test6_deadStockProduct() {
        // Sale occurred 50 days ago (outside 45-day lookback)
        val p = createProduct("p6", currentStock = 15, purchasePrice = 80.0)
        val sales = listOf(
            createSale("s1", now - 50 * oneDayMillis, listOf(SaleItem("p6", "Item", 5, 120.0, 600.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertTrue(metric.isDeadStock)
        assertEquals(VelocityCategory.DEAD_STOCK, metric.velocityCategory)
        assertEquals(1200.0, metric.inventoryValue, 0.001)
        assertEquals(1200.0, metric.capitalLocked, 0.001)
    }

    @Test
    fun test7_productWithInsufficientHistoricalData() {
        val p = createProduct("p7", currentStock = 0)
        val summary = InventoryIntelligenceEngine.analyze(listOf(p), emptyList(), referenceTime = now)
        val metric = summary.products.first()

        assertEquals(0, metric.unitsSold)
        assertEquals(DemandTrend.INSUFFICIENT_DATA, metric.demandTrend)
        assertFalse(metric.isDeadStock) // currentStock is 0, so not dead stock
    }

    @Test
    fun test8_zeroPurchaseCost() {
        val p = createProduct("p8", currentStock = 25, purchasePrice = 0.0)
        val summary = InventoryIntelligenceEngine.analyze(listOf(p), emptyList(), referenceTime = now)
        val metric = summary.products.first()

        assertEquals(0.0, metric.inventoryValue, 0.001)
        assertEquals(0.0, metric.capitalLocked, 0.001)
    }

    @Test
    fun test9_multipleProductsAndSummary() {
        val pFast = createProduct("pFast", currentStock = 20, purchasePrice = 10.0)
        val pSlow = createProduct("pSlow", currentStock = 100, purchasePrice = 20.0)
        val pDead = createProduct("pDead", currentStock = 10, purchasePrice = 50.0)

        val sales = listOf(
            createSale("s1", now - 5 * oneDayMillis, listOf(SaleItem("pFast", "Fast", 60, 15.0, 900.0))),
            createSale("s2", now - 5 * oneDayMillis, listOf(SaleItem("pSlow", "Slow", 15, 30.0, 450.0)))
            // pDead has no sales
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(pFast, pSlow, pDead), sales, referenceTime = now)

        assertEquals(3, summary.products.size)
        assertEquals(1, summary.fastMovingCount)
        assertEquals(1, summary.slowMovingCount)
        assertEquals(1, summary.deadStockCount)
        assertEquals(0, summary.normalCount)

        // Values: Fast (20*10 = 200), Slow (100*20 = 2000), Dead (10*50 = 500) -> Total = 2700
        assertEquals(2700.0, summary.totalInventoryValue, 0.001)
        // Capital locked in slow (2000) + dead (500) = 2500
        assertEquals(2500.0, summary.capitalLockedInSlowAndDeadStock, 0.001)
    }

    @Test
    fun test10_demandIncreasing() {
        val p = createProduct("p10", currentStock = 50)
        // First half (20 days ago): 5 units
        // Second half (5 days ago): 25 units
        val sales = listOf(
            createSale("s1", now - 20 * oneDayMillis, listOf(SaleItem("p10", "Item", 5, 10.0, 50.0))),
            createSale("s2", now - 5 * oneDayMillis, listOf(SaleItem("p10", "Item", 25, 10.0, 250.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(DemandTrend.RISING, metric.demandTrend)
    }

    @Test
    fun test11_demandDecreasing() {
        val p = createProduct("p11", currentStock = 50)
        // First half (22 days ago): 30 units
        // Second half (4 days ago): 4 units
        val sales = listOf(
            createSale("s1", now - 22 * oneDayMillis, listOf(SaleItem("p11", "Item", 30, 10.0, 300.0))),
            createSale("s2", now - 4 * oneDayMillis, listOf(SaleItem("p11", "Item", 4, 10.0, 40.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(DemandTrend.FALLING, metric.demandTrend)
    }

    @Test
    fun test12_demandStable() {
        val p = createProduct("p12", currentStock = 50)
        // First half: 10 units, Second half: 10 units
        val sales = listOf(
            createSale("s1", now - 20 * oneDayMillis, listOf(SaleItem("p12", "Item", 10, 10.0, 100.0))),
            createSale("s2", now - 5 * oneDayMillis, listOf(SaleItem("p12", "Item", 10, 10.0, 100.0)))
        )

        val summary = InventoryIntelligenceEngine.analyze(listOf(p), sales, referenceTime = now)
        val metric = summary.products.first()

        assertEquals(DemandTrend.STABLE, metric.demandTrend)
    }

    @Test
    fun test13_boundaryAndZeroDivisionSafety() {
        val pZero = createProduct("pZero", currentStock = 0, purchasePrice = 0.0)
        // Negative / 0 period passed in
        val summary = InventoryIntelligenceEngine.analyze(
            products = listOf(pZero),
            sales = emptyList(),
            referenceTime = now,
            analysisPeriodDays = 0,
            deadStockLookbackDays = -5
        )

        val metric = summary.products.first()
        assertFalse(metric.salesVelocityPerDay.isInfinite())
        assertFalse(metric.salesVelocityPerDay.isNaN())
        assertNull(metric.daysOfStockRemaining)
        assertEquals(0.0, metric.inventoryValue, 0.001)
        assertEquals(0.0, summary.totalInventoryValue, 0.001)
    }
}
