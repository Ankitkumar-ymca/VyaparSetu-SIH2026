package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessActionEngineTest {

    private val referenceTime = 1700000000000L
    private val oneDayMs = 24 * 60 * 60 * 1000L

    private fun createInventoryMetric(
        productId: String,
        productName: String = "Product $productId",
        currentStock: Int = 10,
        minimumStock: Int = 2,
        unitsSold: Int = 5,
        salesVelocityPerDay: Double = 0.5,
        daysOfStockRemaining: Double? = 20.0,
        inventoryValue: Double = 1000.0,
        velocityCategory: VelocityCategory = VelocityCategory.NORMAL,
        isDeadStock: Boolean = false,
        capitalLocked: Double = 0.0,
        demandTrend: DemandTrend = DemandTrend.STABLE,
        hasSufficientData: Boolean = true
    ) = InventoryProductMetrics(
        productId = productId,
        productName = productName,
        currentStock = currentStock,
        minimumStock = minimumStock,
        unitsSold = unitsSold,
        salesVelocityPerDay = salesVelocityPerDay,
        daysOfStockRemaining = daysOfStockRemaining,
        inventoryValue = inventoryValue,
        velocityCategory = velocityCategory,
        isDeadStock = isDeadStock,
        capitalLocked = capitalLocked,
        demandTrend = demandTrend,
        hasSufficientData = hasSufficientData
    )

    private fun createProfitabilityMetric(
        productId: String,
        productName: String = "Product $productId",
        unitsSold: Int = 10,
        revenue: Double = 1000.0,
        costOfGoodsSold: Double = 600.0,
        grossProfit: Double = 400.0,
        grossMarginPercentage: Double = 40.0,
        marginCategory: MarginCategory = MarginCategory.HIGH_MARGIN
    ) = ProductProfitabilityMetrics(
        productId = productId,
        productName = productName,
        unitsSold = unitsSold,
        revenue = revenue,
        costOfGoodsSold = costOfGoodsSold,
        grossProfit = grossProfit,
        grossMarginPercentage = grossMarginPercentage,
        averageSellingPrice = if (unitsSold > 0) revenue / unitsSold else 0.0,
        averageCostPerUnit = if (unitsSold > 0) costOfGoodsSold / unitsSold else 0.0,
        profitPerUnit = if (unitsSold > 0) grossProfit / unitsSold else 0.0,
        marginCategory = marginCategory
    )

    private fun createCollectionResult(
        customerId: String,
        customerName: String = "Customer $customerId",
        outstandingAmount: Double = 0.0,
        priority: CollectionPriorityLevel = CollectionPriorityLevel.NONE,
        reasons: List<String> = emptyList(),
        daysSinceLastPurchase: Long? = 10L,
        activityStatus: CustomerActivityStatus = CustomerActivityStatus.ACTIVE,
        segment: CustomerSegment = CustomerSegment.REGULAR
    ) = CollectionPriorityResult(
        customerId = customerId,
        customerName = customerName,
        phone = "9876543210",
        outstandingAmount = outstandingAmount,
        priority = priority,
        reasons = reasons,
        daysSinceLastPurchase = daysSinceLastPurchase,
        activityStatus = activityStatus,
        segment = segment,
        lifetimePurchases = 5000.0,
        lifetimeOrders = 5,
        totalPaid = 4000.0
    )

    private fun createCustomerMetric(
        customerId: String,
        customerName: String = "Customer $customerId",
        segment: CustomerSegment = CustomerSegment.REGULAR,
        activityStatus: CustomerActivityStatus = CustomerActivityStatus.ACTIVE,
        daysSinceLastPurchase: Long? = 10L,
        outstandingAmount: Double = 0.0
    ) = CustomerMetrics(
        customerId = customerId,
        customerName = customerName,
        phone = "9876543210",
        periodPurchases = 1000.0,
        periodOrders = 2,
        periodUnitsPurchased = 4,
        lifetimePurchases = 5000.0,
        lifetimeOrders = 10,
        lifetimeUnitsPurchased = 20,
        averageOrderValue = 500.0,
        firstPurchaseDate = referenceTime - (100 * oneDayMs),
        lastPurchaseDate = daysSinceLastPurchase?.let { referenceTime - (it * oneDayMs) },
        daysSinceLastPurchase = daysSinceLastPurchase,
        purchaseFrequencyPerMonth = 2.0,
        outstandingAmount = outstandingAmount,
        hasOutstanding = outstandingAmount > 0.0,
        totalPaid = 4000.0,
        segment = segment,
        activityStatus = activityStatus,
        hasSufficientData = true
    )

    // 1. Empty intelligence results returns zero counts and empty lists
    @Test
    fun test1_emptyIntelligenceResults() {
        val summary = BusinessActionEngine.generateFromIntelligence()
        assertEquals(0, summary.totalActions)
        assertEquals(0, summary.criticalActionsCount)
        assertEquals(0, summary.highActionsCount)
        assertEquals(0, summary.mediumActionsCount)
        assertEquals(0, summary.lowActionsCount)
        assertTrue(summary.topActions.isEmpty())
        assertTrue(summary.allActions.isEmpty())
    }

    // 2. Out of stock product with sales velocity generates CRITICAL RESTOCK action
    @Test
    fun test2_outOfStockWithDemandGeneratesCriticalRestock() {
        val oosItem = createInventoryMetric(
            productId = "p1",
            productName = "Basmati Rice",
            currentStock = 0,
            unitsSold = 20,
            salesVelocityPerDay = 1.5,
            daysOfStockRemaining = 0.0
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(oosItem)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.criticalActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.CRITICAL, action.priority)
        assertEquals(BusinessActionCategory.RESTOCK, action.category)
        assertEquals("p1", action.targetId)
        assertTrue(action.title.contains("Restock Basmati Rice"))
    }

    // 3. Out of stock product with 0 units sold and 0 velocity produces no action
    @Test
    fun test3_outOfStockZeroVelocityProducesNoAction() {
        val inactiveOos = createInventoryMetric(
            productId = "p2",
            productName = "Obsolete Item",
            currentStock = 0,
            unitsSold = 0,
            salesVelocityPerDay = 0.0,
            daysOfStockRemaining = null,
            hasSufficientData = false
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(inactiveOos)
        )

        assertEquals(0, summary.totalActions)
    }

    // 4. Low stock with <= 3 days remaining generates HIGH RESTOCK action
    @Test
    fun test4_lowStockCoverageGeneratesHighRestock() {
        val lowStockItem = createInventoryMetric(
            productId = "p3",
            productName = "Mustard Oil",
            currentStock = 2,
            minimumStock = 5,
            unitsSold = 10,
            salesVelocityPerDay = 1.0,
            daysOfStockRemaining = 2.0
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(lowStockItem)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.highActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.HIGH, action.priority)
        assertEquals(BusinessActionCategory.RESTOCK, action.category)
        assertTrue(action.title.contains("Reorder Mustard Oil"))
    }

    // 5. High collection priority with prolonged inactivity generates CRITICAL action
    @Test
    fun test5_highCollectionPriorityProlongedInactivityGeneratesCritical() {
        val col = createCollectionResult(
            customerId = "c1",
            customerName = "Ramesh Kumar",
            outstandingAmount = 6000.0,
            priority = CollectionPriorityLevel.HIGH,
            reasons = listOf("Substantial outstanding due with prolonged inactivity"),
            daysSinceLastPurchase = 50L
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            collectionResults = listOf(col)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.criticalActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.CRITICAL, action.priority)
        assertEquals(BusinessActionCategory.COLLECT_PAYMENT, action.category)
        assertEquals("c1", action.targetId)
        assertTrue(action.title.contains("Collect ₹6000 from Ramesh Kumar"))
    }

    // 6. High collection priority under churn threshold but substantial due generates CRITICAL or HIGH
    @Test
    fun test6_highCollectionPrioritySubstantialAmount() {
        val col = createCollectionResult(
            customerId = "c2",
            customerName = "Suresh Traders",
            outstandingAmount = 5500.0,
            priority = CollectionPriorityLevel.HIGH,
            reasons = listOf("Substantial outstanding due requires credit limit review"),
            daysSinceLastPurchase = 15L
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            collectionResults = listOf(col)
        )

        assertEquals(1, summary.totalActions)
        val action = summary.allActions.first()
        assertEquals(BusinessActionCategory.COLLECT_PAYMENT, action.category)
        assertTrue(action.priority == BusinessActionPriority.CRITICAL || action.priority == BusinessActionPriority.HIGH)
    }

    // 7. Medium collection priority generates MEDIUM COLLECT_PAYMENT action
    @Test
    fun test7_mediumCollectionPriorityGeneratesMedium() {
        val col = createCollectionResult(
            customerId = "c3",
            customerName = "Amit Verma",
            outstandingAmount = 1500.0,
            priority = CollectionPriorityLevel.MEDIUM,
            reasons = listOf("Outstanding balance with no recent purchases"),
            daysSinceLastPurchase = 25L
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            collectionResults = listOf(col)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.mediumActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.MEDIUM, action.priority)
        assertEquals(BusinessActionCategory.COLLECT_PAYMENT, action.category)
    }

    // 8. Customer with no outstanding balance does not generate collection action
    @Test
    fun test8_customerNoOutstandingNoAction() {
        val col = createCollectionResult(
            customerId = "c4",
            customerName = "Zero Due Customer",
            outstandingAmount = 0.0,
            priority = CollectionPriorityLevel.NONE,
            reasons = listOf("No outstanding due on account")
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            collectionResults = listOf(col)
        )

        assertEquals(0, summary.totalActions)
    }

    // 9. Dead stock with high capital locked (>= ₹2,000) generates HIGH CLEAR_STOCK action
    @Test
    fun test9_deadStockHighCapitalLockedGeneratesHigh() {
        val dead = createInventoryMetric(
            productId = "p4",
            productName = "Slow Mixer Grinder",
            currentStock = 5,
            isDeadStock = true,
            capitalLocked = 3500.0
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(dead)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.highActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.HIGH, action.priority)
        assertEquals(BusinessActionCategory.CLEAR_STOCK, action.category)
        assertTrue(action.title.contains("Clear Dead Stock: Slow Mixer Grinder"))
    }

    // 10. Dead stock with moderate capital locked (< ₹2,000) generates MEDIUM CLEAR_STOCK action
    @Test
    fun test10_deadStockModerateCapitalLockedGeneratesMedium() {
        val dead = createInventoryMetric(
            productId = "p5",
            productName = "Old Spices Pack",
            currentStock = 10,
            isDeadStock = true,
            capitalLocked = 800.0
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(dead)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.mediumActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.MEDIUM, action.priority)
        assertEquals(BusinessActionCategory.CLEAR_STOCK, action.category)
    }

    // 11. Dead stock with 0 stock or 0 capital locked generates no action
    @Test
    fun test11_deadStockZeroStockOrCapitalNoAction() {
        val zeroStockDead = createInventoryMetric(
            productId = "p6",
            productName = "Empty Dead Item",
            currentStock = 0,
            isDeadStock = true,
            capitalLocked = 0.0
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(zeroStockDead)
        )

        assertEquals(0, summary.totalActions)
    }

    // 12. Loss-making product generates CRITICAL REVIEW_PRICING action
    @Test
    fun test12_lossMakingProductGeneratesCriticalPricingReview() {
        val lossProduct = createProfitabilityMetric(
            productId = "p7",
            productName = "Discounted Sugar",
            unitsSold = 20,
            revenue = 800.0,
            costOfGoodsSold = 900.0,
            grossProfit = -100.0,
            grossMarginPercentage = -12.5,
            marginCategory = MarginCategory.NO_PROFIT
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            profitabilityMetrics = listOf(lossProduct)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.criticalActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.CRITICAL, action.priority)
        assertEquals(BusinessActionCategory.REVIEW_PRICING, action.category)
        assertTrue(action.title.contains("Fix Loss-Making Price: Discounted Sugar"))
    }

    // 13. Low margin product with volume generates MEDIUM REVIEW_PRICING action
    @Test
    fun test13_lowMarginProductWithVolumeGeneratesMediumPricingReview() {
        val lowMarginProduct = createProfitabilityMetric(
            productId = "p8",
            productName = "Packaged Ghee",
            unitsSold = 15,
            revenue = 3000.0,
            costOfGoodsSold = 2700.0,
            grossProfit = 300.0,
            grossMarginPercentage = 10.0,
            marginCategory = MarginCategory.LOW_MARGIN
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            profitabilityMetrics = listOf(lowMarginProduct)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.mediumActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.MEDIUM, action.priority)
        assertEquals(BusinessActionCategory.REVIEW_PRICING, action.category)
        assertTrue(action.title.contains("Review Margins: Packaged Ghee"))
    }

    // 14. Inactive high-value customer generates MEDIUM REENGAGE_CUSTOMER action
    @Test
    fun test14_inactiveHighValueCustomerGeneratesReengageAction() {
        val cust = createCustomerMetric(
            customerId = "c5",
            customerName = "Priya Sharma",
            segment = CustomerSegment.HIGH_VALUE,
            activityStatus = CustomerActivityStatus.INACTIVE,
            daysSinceLastPurchase = 60L
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            customerMetrics = listOf(cust)
        )

        assertEquals(1, summary.totalActions)
        assertEquals(1, summary.mediumActionsCount)
        val action = summary.allActions.first()
        assertEquals(BusinessActionPriority.MEDIUM, action.priority)
        assertEquals(BusinessActionCategory.REENGAGE_CUSTOMER, action.category)
        assertEquals("c5", action.targetId)
        assertTrue(action.title.contains("Re-engage Priya Sharma"))
    }

    // 15. Customer flagged for critical collection is not given duplicate/conflicting re-engage action
    @Test
    fun test15_collectionCustomerNotConflictedWithReengage() {
        val cust = createCustomerMetric(
            customerId = "c6",
            customerName = "Conflict Customer",
            segment = CustomerSegment.HIGH_VALUE,
            activityStatus = CustomerActivityStatus.INACTIVE,
            daysSinceLastPurchase = 60L,
            outstandingAmount = 6000.0
        )
        val col = createCollectionResult(
            customerId = "c6",
            customerName = "Conflict Customer",
            outstandingAmount = 6000.0,
            priority = CollectionPriorityLevel.HIGH,
            reasons = listOf("Substantial outstanding due with prolonged inactivity"),
            daysSinceLastPurchase = 60L
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            collectionResults = listOf(col),
            customerMetrics = listOf(cust)
        )

        // Only 1 action (COLLECT_PAYMENT), not 2
        assertEquals(1, summary.totalActions)
        assertEquals(BusinessActionCategory.COLLECT_PAYMENT, summary.allActions.first().category)
    }

    // 16. Deduplication by actionId prevents duplicate entries
    @Test
    fun test16_deduplicationPreservesSingleActionPerId() {
        val oosItem = createInventoryMetric(
            productId = "p9",
            productName = "Tea Leaves",
            currentStock = 0,
            unitsSold = 10,
            salesVelocityPerDay = 1.0
        )

        // Passing duplicate inventory metric
        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(oosItem, oosItem)
        )

        assertEquals(1, summary.totalActions)
    }

    // 17. Deterministic sorting: CRITICAL before HIGH before MEDIUM, ties broken by metric magnitude
    @Test
    fun test17_deterministicSortingOrder() {
        val oos = createInventoryMetric(
            productId = "p10",
            productName = "Out of Stock Item",
            currentStock = 0,
            unitsSold = 10,
            salesVelocityPerDay = 2.0
        )
        val lowStock = createInventoryMetric(
            productId = "p11",
            productName = "Low Stock Item",
            currentStock = 1,
            minimumStock = 5,
            unitsSold = 5,
            salesVelocityPerDay = 0.5,
            daysOfStockRemaining = 2.0
        )
        val colMedium = createCollectionResult(
            customerId = "c7",
            customerName = "Medium Due",
            outstandingAmount = 1200.0,
            priority = CollectionPriorityLevel.MEDIUM,
            reasons = listOf("Outstanding balance with no recent purchases")
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(lowStock, oos),
            collectionResults = listOf(colMedium)
        )

        assertEquals(3, summary.totalActions)
        assertEquals(BusinessActionPriority.CRITICAL, summary.allActions[0].priority)
        assertEquals(BusinessActionPriority.HIGH, summary.allActions[1].priority)
        assertEquals(BusinessActionPriority.MEDIUM, summary.allActions[2].priority)
    }

    // 18. Max actions limit caps topActions but preserves allActions
    @Test
    fun test18_maxActionsCapPreservesAllActions() {
        val items = (1..5).map { index ->
            createInventoryMetric(
                productId = "prod_$index",
                productName = "Product $index",
                currentStock = 0,
                unitsSold = index * 5,
                salesVelocityPerDay = index.toDouble()
            )
        }

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = items,
            maxActions = 2
        )

        assertEquals(5, summary.totalActions)
        assertEquals(2, summary.topActions.size)
        assertEquals(5, summary.allActions.size)
    }

    // 19. Robustness: handles NaN, Infinity, and null metrics gracefully
    @Test
    fun test19_nanAndInfinitySafety() {
        val nanItem = createInventoryMetric(
            productId = "p12",
            productName = "Edge Item",
            currentStock = 0,
            unitsSold = 1,
            salesVelocityPerDay = Double.NaN,
            daysOfStockRemaining = Double.POSITIVE_INFINITY,
            capitalLocked = Double.NEGATIVE_INFINITY
        )

        val summary = BusinessActionEngine.generateFromIntelligence(
            inventoryMetrics = listOf(nanItem)
        )

        assertNotNull(summary)
        assertFalse(summary.allActions.any { it.metricValue?.isNaN() == true || it.metricValue?.isInfinite() == true })
    }

    // 20. End-to-end integration: generateActions from raw domain models
    @Test
    fun test20_endToEndGenerateActionsFromEntities() {
        val products = listOf(
            Product(
                id = "p_raw_1",
                businessId = "biz1",
                name = "Wheat 10kg",
                sku = "SKU-W1",
                barcode = "BAR-W1",
                category = "Groceries",
                purchasePrice = 250.0,
                sellingPrice = 300.0,
                currentStock = 0,
                minStock = 5,
                supplierId = "sup1",
                expiryDate = null,
                createdAt = referenceTime - (30 * oneDayMs),
                updatedAt = referenceTime,
                isActive = true
            )
        )

        val sales = listOf(
            Sale(
                id = "s1",
                businessId = "biz1",
                invoiceNumber = "INV-001",
                customerId = "c_raw_1",
                items = listOf(
                    SaleItem(
                        productId = "p_raw_1",
                        productName = "Wheat 10kg",
                        quantity = 5,
                        unitPrice = 300.0,
                        totalPrice = 1500.0
                    )
                ),
                subtotal = 1500.0,
                discount = 0.0,
                tax = 0.0,
                grandTotal = 1500.0,
                amountPaid = 500.0,
                outstanding = 1000.0,
                paymentStatus = "PARTIAL",
                paymentMethod = "CREDIT",
                createdAt = referenceTime - (5 * oneDayMs),
                updatedAt = referenceTime - (5 * oneDayMs)
            )
        )

        val customers = listOf(
            Customer(
                id = "c_raw_1",
                businessId = "biz1",
                name = "Harish Patel",
                phone = "9876543210",
                email = "harish@example.com",
                address = "Ahmedabad",
                totalPurchases = 1500.0,
                totalPaid = 500.0,
                outstandingAmount = 6000.0,
                createdAt = referenceTime - (60 * oneDayMs),
                updatedAt = referenceTime
            )
        )

        val summary = BusinessActionEngine.generateActions(
            products = products,
            sales = sales,
            customers = customers,
            currentTime = referenceTime
        )

        assertTrue(summary.totalActions > 0)
        assertTrue(summary.topActions.isNotEmpty())
        // Checks that restock and collection actions are found
        val categories = summary.allActions.map { it.category }
        assertTrue(categories.contains(BusinessActionCategory.RESTOCK))
    }
}
