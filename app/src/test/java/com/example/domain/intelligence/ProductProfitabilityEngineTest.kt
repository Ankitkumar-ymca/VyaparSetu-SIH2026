package com.example.domain.intelligence

import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductProfitabilityEngineTest {

    private val now = 1700000000000L
    private val oneDayMillis = 24L * 60L * 60L * 1000L

    private fun createProduct(
        id: String,
        name: String = "Product $id",
        purchasePrice: Double = 100.0,
        sellingPrice: Double = 150.0,
        currentStock: Int = 10
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
            minStock = 2,
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

    // 1. Empty dataset
    @Test
    fun test1_emptyDataset() {
        val summary = ProductProfitabilityEngine.analyze(
            products = emptyList(),
            sales = emptyList(),
            referenceTimeMs = now
        )

        assertEquals(0.0, summary.totalRevenue, 0.001)
        assertEquals(0.0, summary.totalCostOfGoodsSold, 0.001)
        assertEquals(0.0, summary.totalGrossProfit, 0.001)
        assertEquals(0.0, summary.overallGrossMarginPercentage, 0.001)
        assertEquals(0, summary.totalUnitsSold)
        assertEquals(0, summary.productCount)
        assertNull(summary.highestGrossProfitProduct)
        assertNull(summary.highestMarginProduct)
        assertTrue(summary.products.isEmpty())
    }

    // 2. Single profitable product
    @Test
    fun test2_singleProfitableProduct() {
        val product = createProduct("p1", purchasePrice = 60.0, sellingPrice = 100.0)
        val sale = createSale(
            id = "s1",
            timestamp = now - 5 * oneDayMillis,
            items = listOf(
                SaleItem(
                    productId = "p1",
                    productName = product.name,
                    quantity = 10,
                    unitPrice = 100.0,
                    totalPrice = 1000.0,
                    purchasePriceAtSale = 60.0
                )
            )
        )

        val summary = ProductProfitabilityEngine.analyze(
            products = listOf(product),
            sales = listOf(sale),
            referenceTimeMs = now
        )

        val metrics = summary.products.first()
        assertEquals(10, metrics.unitsSold)
        assertEquals(1000.0, metrics.revenue, 0.001)
        assertEquals(600.0, metrics.costOfGoodsSold, 0.001)
        assertEquals(400.0, metrics.grossProfit, 0.001)
        assertEquals(40.0, metrics.grossMarginPercentage, 0.001)
        assertEquals(100.0, metrics.averageSellingPrice, 0.001)
        assertEquals(60.0, metrics.averageCostPerUnit, 0.001)
        assertEquals(40.0, metrics.profitPerUnit, 0.001)
        assertEquals(MarginCategory.HIGH_MARGIN, metrics.marginCategory)

        assertEquals(1000.0, summary.totalRevenue, 0.001)
        assertEquals(600.0, summary.totalCostOfGoodsSold, 0.001)
        assertEquals(400.0, summary.totalGrossProfit, 0.001)
        assertEquals(40.0, summary.overallGrossMarginPercentage, 0.001)
    }

    // 3. Multiple products
    @Test
    fun test3_multipleProducts() {
        val p1 = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)
        val p2 = createProduct("p2", purchasePrice = 80.0, sellingPrice = 100.0)
        val sale = createSale(
            id = "s1",
            timestamp = now - 2 * oneDayMillis,
            items = listOf(
                SaleItem("p1", p1.name, 2, 100.0, 200.0, 50.0),
                SaleItem("p2", p2.name, 3, 100.0, 300.0, 80.0)
            )
        )

        val summary = ProductProfitabilityEngine.analyze(
            products = listOf(p1, p2),
            sales = listOf(sale),
            referenceTimeMs = now
        )

        assertEquals(2, summary.productCount)
        assertEquals(500.0, summary.totalRevenue, 0.001)
        // p1 COGS: 2 * 50 = 100; p2 COGS: 3 * 80 = 240; Total COGS = 340
        assertEquals(340.0, summary.totalCostOfGoodsSold, 0.001)
        assertEquals(160.0, summary.totalGrossProfit, 0.001)
        assertEquals(32.0, summary.overallGrossMarginPercentage, 0.001)
    }

    // 4. High-margin classification (>= 40%)
    @Test
    fun test4_highMarginClassification() {
        val p = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 50.0)) // margin = 50%
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        assertEquals(MarginCategory.HIGH_MARGIN, summary.products.first().marginCategory)
        assertEquals(1, summary.highMarginCount)
    }

    // 5. Medium-margin classification (>= 20% and < 40%)
    @Test
    fun test5_mediumMarginClassification() {
        val p = createProduct("p1", purchasePrice = 75.0, sellingPrice = 100.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 75.0)) // margin = 25%
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        assertEquals(MarginCategory.MEDIUM_MARGIN, summary.products.first().marginCategory)
        assertEquals(1, summary.mediumMarginCount)
    }

    // 6. Low-margin classification (> 0% and < 20%)
    @Test
    fun test6_lowMarginClassification() {
        val p = createProduct("p1", purchasePrice = 90.0, sellingPrice = 100.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 90.0)) // margin = 10%
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        assertEquals(MarginCategory.LOW_MARGIN, summary.products.first().marginCategory)
        assertEquals(1, summary.lowMarginCount)
    }

    // 7. Zero-profit product (revenue == COGS)
    @Test
    fun test7_zeroProfitProduct() {
        val p = createProduct("p1", purchasePrice = 100.0, sellingPrice = 100.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 100.0)) // profit = 0
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        val metrics = summary.products.first()
        assertEquals(0.0, metrics.grossProfit, 0.001)
        assertEquals(0.0, metrics.grossMarginPercentage, 0.001)
        assertEquals(MarginCategory.NO_PROFIT, metrics.marginCategory)
        assertEquals(1, summary.noProfitCount)
    }

    // 8. Negative-profit product (sold at loss)
    @Test
    fun test8_negativeProfitProduct() {
        val p = createProduct("p1", purchasePrice = 120.0, sellingPrice = 100.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 120.0)) // loss = -20
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        val metrics = summary.products.first()
        assertEquals(-20.0, metrics.grossProfit, 0.001)
        assertEquals(MarginCategory.NO_PROFIT, metrics.marginCategory)
        assertEquals(1, summary.noProfitCount)
    }

    // 9. Zero revenue product (no sales recorded in period)
    @Test
    fun test9_zeroRevenueProduct() {
        val p = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)

        val summary = ProductProfitabilityEngine.analyze(listOf(p), emptyList(), referenceTimeMs = now)
        val metrics = summary.products.first()
        assertEquals(0.0, metrics.revenue, 0.001)
        assertEquals(0.0, metrics.grossMarginPercentage, 0.001)
        assertEquals(0, metrics.unitsSold)
        assertEquals(MarginCategory.INSUFFICIENT_DATA, metrics.marginCategory)
        assertEquals(1, summary.insufficientDataCount)
    }

    // 10. Zero purchase cost (100% margin)
    @Test
    fun test10_zeroPurchaseCost() {
        val p = createProduct("p1", purchasePrice = 0.0, sellingPrice = 50.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(SaleItem("p1", p.name, 2, 50.0, 100.0, 0.0))
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        val metrics = summary.products.first()
        assertEquals(100.0, metrics.revenue, 0.001)
        assertEquals(0.0, metrics.costOfGoodsSold, 0.001)
        assertEquals(100.0, metrics.grossProfit, 0.001)
        assertEquals(100.0, metrics.grossMarginPercentage, 0.001)
        assertEquals(MarginCategory.HIGH_MARGIN, metrics.marginCategory)
    }

    // 11. Multiple SaleItems for same product across different sales
    @Test
    fun test11_multipleSaleItemsForSameProduct() {
        val p = createProduct("p1", purchasePrice = 40.0, sellingPrice = 100.0)
        val s1 = createSale("s1", now - 5 * oneDayMillis, listOf(SaleItem("p1", p.name, 2, 100.0, 200.0, 40.0)))
        val s2 = createSale("s2", now - 2 * oneDayMillis, listOf(SaleItem("p1", p.name, 3, 100.0, 300.0, 40.0)))

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(s1, s2), referenceTimeMs = now)
        val metrics = summary.products.first()
        assertEquals(5, metrics.unitsSold)
        assertEquals(500.0, metrics.revenue, 0.001)
        assertEquals(200.0, metrics.costOfGoodsSold, 0.001)
        assertEquals(300.0, metrics.grossProfit, 0.001)
        assertEquals(60.0, metrics.grossMarginPercentage, 0.001)
    }

    // 12. Product with no sales among active sales of others
    @Test
    fun test12_productWithNoSales() {
        val pActive = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)
        val pInactive = createProduct("p2", purchasePrice = 30.0, sellingPrice = 60.0)
        val sale = createSale("s1", now - oneDayMillis, listOf(SaleItem("p1", pActive.name, 1, 100.0, 100.0, 50.0)))

        val summary = ProductProfitabilityEngine.analyze(listOf(pActive, pInactive), listOf(sale), referenceTimeMs = now)
        val mInactive = summary.products.find { it.productId == "p2" }!!
        assertEquals(0, mInactive.unitsSold)
        assertEquals(0.0, mInactive.revenue, 0.001)
        assertEquals(0.0, mInactive.grossMarginPercentage, 0.001)
        assertEquals(MarginCategory.INSUFFICIENT_DATA, mInactive.marginCategory)
    }

    // 13. Missing product reference handled safely without crashing
    @Test
    fun test13_missingProductReference() {
        val p1 = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)
        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(
                SaleItem("p1", p1.name, 1, 100.0, 100.0, 50.0),
                SaleItem("ghost_product_999", "Unknown", 5, 200.0, 1000.0, 100.0)
            )
        )

        // Must not crash and should not create fake product
        val summary = ProductProfitabilityEngine.analyze(listOf(p1), listOf(sale), referenceTimeMs = now)
        assertEquals(1, summary.products.size)
        assertEquals(1, summary.products.first().unitsSold)
        assertEquals(100.0, summary.totalRevenue, 0.001)
    }

    // 14. Division-by-zero safety
    @Test
    fun test14_divisionByZeroSafety() {
        val p = createProduct("p1", purchasePrice = 0.0, sellingPrice = 0.0)
        val sale = createSale("s1", now - oneDayMillis, listOf(SaleItem("p1", p.name, 0, 0.0, 0.0, 0.0)))

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(sale), referenceTimeMs = now)
        val metrics = summary.products.first()
        assertEquals(0.0, metrics.grossMarginPercentage, 0.0001)
        assertEquals(0.0, metrics.averageSellingPrice, 0.0001)
        assertEquals(0.0, metrics.averageCostPerUnit, 0.0001)
        assertEquals(0.0, metrics.profitPerUnit, 0.0001)
    }

    // 15. NaN and Infinity safety
    @Test
    fun test15_nanInfinitySafety() {
        val p = createProduct("p1", purchasePrice = 10.0, sellingPrice = 20.0)
        val summary = ProductProfitabilityEngine.analyze(listOf(p), emptyList(), referenceTimeMs = now)

        val m = summary.products.first()
        org.junit.Assert.assertFalse(m.grossMarginPercentage.isNaN())
        org.junit.Assert.assertFalse(m.grossMarginPercentage.isInfinite())
        org.junit.Assert.assertFalse(summary.overallGrossMarginPercentage.isNaN())
        org.junit.Assert.assertFalse(summary.overallGrossMarginPercentage.isInfinite())
    }

    // 16. Summary totals consistency
    @Test
    fun test16_summaryTotals() {
        val p1 = createProduct("p1", purchasePrice = 40.0, sellingPrice = 100.0) // profit = 60
        val p2 = createProduct("p2", purchasePrice = 80.0, sellingPrice = 100.0) // profit = 20
        val p3 = createProduct("p3", purchasePrice = 10.0, sellingPrice = 20.0) // no sale

        val sale = createSale(
            "s1",
            now - 2 * oneDayMillis,
            listOf(
                SaleItem("p1", p1.name, 1, 100.0, 100.0, 40.0),
                SaleItem("p2", p2.name, 1, 100.0, 100.0, 80.0)
            )
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p1, p2, p3), listOf(sale), referenceTimeMs = now)
        assertEquals(200.0, summary.totalRevenue, 0.001)
        assertEquals(120.0, summary.totalCostOfGoodsSold, 0.001)
        assertEquals(80.0, summary.totalGrossProfit, 0.001)
        assertEquals(40.0, summary.overallGrossMarginPercentage, 0.001)
        assertEquals(2, summary.totalUnitsSold)
        assertEquals(3, summary.productCount)
        assertEquals(1, summary.highMarginCount)
        assertEquals(1, summary.mediumMarginCount)
        assertEquals(0, summary.lowMarginCount)
        assertEquals(0, summary.noProfitCount)
        assertEquals(1, summary.insufficientDataCount)
    }

    // 17. Highest gross-profit product
    @Test
    fun test17_highestGrossProfitProduct() {
        val p1 = createProduct("p1", purchasePrice = 20.0, sellingPrice = 50.0) // 10 units => profit = 300
        val p2 = createProduct("p2", purchasePrice = 50.0, sellingPrice = 100.0) // 2 units => profit = 100

        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(
                SaleItem("p1", p1.name, 10, 50.0, 500.0, 20.0),
                SaleItem("p2", p2.name, 2, 100.0, 200.0, 50.0)
            )
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p1, p2), listOf(sale), referenceTimeMs = now)
        assertNotNull(summary.highestGrossProfitProduct)
        assertEquals("p1", summary.highestGrossProfitProduct?.productId)
        assertEquals(300.0, summary.highestGrossProfitProduct?.grossProfit ?: 0.0, 0.001)
    }

    // 18. Highest-margin product
    @Test
    fun test18_highestMarginProduct() {
        val p1 = createProduct("p1", purchasePrice = 80.0, sellingPrice = 100.0) // margin = 20%
        val p2 = createProduct("p2", purchasePrice = 10.0, sellingPrice = 100.0) // margin = 90%

        val sale = createSale(
            "s1",
            now - oneDayMillis,
            listOf(
                SaleItem("p1", p1.name, 1, 100.0, 100.0, 80.0),
                SaleItem("p2", p2.name, 1, 100.0, 100.0, 10.0)
            )
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p1, p2), listOf(sale), referenceTimeMs = now)
        assertNotNull(summary.highestMarginProduct)
        assertEquals("p2", summary.highestMarginProduct?.productId)
        assertEquals(90.0, summary.highestMarginProduct?.grossMarginPercentage ?: 0.0, 0.001)
    }

    // 19. 30-day filtering
    @Test
    fun test19_thirtyDayFiltering() {
        val p = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)
        val oldSale = createSale(
            "sOld",
            now - 35 * oneDayMillis, // 35 days ago (outside 30-day window)
            listOf(SaleItem("p1", p.name, 5, 100.0, 500.0, 50.0))
        )
        val recentSale = createSale(
            "sRecent",
            now - 5 * oneDayMillis, // 5 days ago (inside window)
            listOf(SaleItem("p1", p.name, 2, 100.0, 200.0, 50.0))
        )

        val summary = ProductProfitabilityEngine.analyze(listOf(p), listOf(oldSale, recentSale), referenceTimeMs = now)
        val m = summary.products.first()
        assertEquals(2, m.unitsSold)
        assertEquals(200.0, m.revenue, 0.001)
        assertEquals(100.0, m.costOfGoodsSold, 0.001)
    }

    // 20. Analysis period boundary (exact boundary tests and custom period)
    @Test
    fun test20_analysisPeriodBoundary() {
        val p = createProduct("p1", purchasePrice = 50.0, sellingPrice = 100.0)
        val saleAtStartBoundary = createSale(
            "sBoundary",
            now - 30 * oneDayMillis, // exact start boundary
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 50.0))
        )
        val saleAtEndBoundary = createSale(
            "sNow",
            now, // exact end boundary
            listOf(SaleItem("p1", p.name, 1, 100.0, 100.0, 50.0))
        )

        val summary = ProductProfitabilityEngine.analyze(
            products = listOf(p),
            sales = listOf(saleAtStartBoundary, saleAtEndBoundary),
            analysisPeriodDays = 30,
            referenceTimeMs = now
        )

        val m = summary.products.first()
        assertEquals(2, m.unitsSold)
        assertEquals(200.0, m.revenue, 0.001)
    }
}
