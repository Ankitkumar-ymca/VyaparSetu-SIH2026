package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossBusinessInsightsEngineTest {

    private val referenceTime = 1700000000000L
    private val oneDayMs = 24 * 60 * 60 * 1000L

    // ==========================================
    // HELPERS FOR TEST DATA CREATION
    // ==========================================

    private fun createProduct(
        id: String,
        name: String,
        price: Double = 100.0,
        cost: Double = 50.0,
        stock: Int = 20,
        minStock: Int = 5
    ): Product {
        return Product(
            id = id,
            businessId = "biz1",
            name = name,
            sku = "SKU-$id",
            barcode = "BAR-$id",
            category = "General",
            purchasePrice = cost,
            sellingPrice = price,
            currentStock = stock,
            minStock = minStock,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )
    }

    private fun createCustomer(
        id: String,
        name: String,
        outstanding: Double = 0.0,
        totalPaid: Double = 0.0
    ): Customer {
        return Customer(
            id = id,
            businessId = "biz1",
            name = name,
            phone = "9876543210",
            email = "$id@example.com",
            address = "Market Street",
            totalPurchases = 1000.0,
            totalPaid = totalPaid,
            outstandingAmount = outstanding,
            createdAt = referenceTime - (60 * oneDayMs),
            updatedAt = referenceTime
        )
    }

    private fun createSale(
        id: String,
        customerId: String,
        productId: String,
        quantity: Int = 5,
        price: Double = 100.0,
        cost: Double = 50.0,
        timestamp: Long = referenceTime - (5 * oneDayMs)
    ): Sale {
        return Sale(
            id = id,
            businessId = "biz1",
            invoiceNumber = "INV-$id",
            customerId = customerId,
            items = listOf(
                SaleItem(
                    productId = productId,
                    productName = "Product $productId",
                    quantity = quantity,
                    unitPrice = price,
                    totalPrice = quantity * price
                )
            ),
            subtotal = quantity * price,
            discount = 0.0,
            tax = 0.0,
            grandTotal = quantity * price,
            amountPaid = quantity * price,
            outstanding = 0.0,
            paymentStatus = "PAID",
            paymentMethod = "CASH",
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    private fun createInvMetric(
        productId: String,
        productName: String = "Product $productId",
        stock: Int = 20,
        minStock: Int = 5,
        unitsSold: Int = 20,
        velocity: Double = 1.0,
        daysRemaining: Double? = 20.0,
        velocityCategory: VelocityCategory = VelocityCategory.NORMAL,
        isDeadStock: Boolean = false,
        capitalLocked: Double = 0.0,
        inventoryValue: Double = 1000.0
    ): InventoryProductMetrics {
        return InventoryProductMetrics(
            productId = productId,
            productName = productName,
            currentStock = stock,
            minimumStock = minStock,
            unitsSold = unitsSold,
            salesVelocityPerDay = velocity,
            daysOfStockRemaining = daysRemaining,
            inventoryValue = inventoryValue,
            velocityCategory = velocityCategory,
            isDeadStock = isDeadStock,
            capitalLocked = capitalLocked,
            demandTrend = DemandTrend.STABLE,
            hasSufficientData = true
        )
    }

    private fun createProfMetric(
        productId: String,
        productName: String = "Product $productId",
        marginPct: Double = 25.0,
        grossProfit: Double = 1000.0,
        revenue: Double = 4000.0,
        marginCategory: MarginCategory = MarginCategory.MEDIUM_MARGIN
    ): ProductProfitabilityMetrics {
        return ProductProfitabilityMetrics(
            productId = productId,
            productName = productName,
            unitsSold = 20,
            revenue = revenue,
            costOfGoodsSold = revenue - grossProfit,
            grossProfit = grossProfit,
            grossMarginPercentage = marginPct,
            averageSellingPrice = if (revenue > 0) revenue / 20 else 0.0,
            averageCostPerUnit = if (revenue > 0) (revenue - grossProfit) / 20 else 0.0,
            profitPerUnit = if (revenue > 0) grossProfit / 20 else 0.0,
            marginCategory = marginCategory
        )
    }

    private fun createCustMetric(
        customerId: String,
        customerName: String = "Customer $customerId",
        segment: CustomerSegment = CustomerSegment.REGULAR,
        activityStatus: CustomerActivityStatus = CustomerActivityStatus.ACTIVE,
        daysSinceLastPurchase: Long? = 10L,
        outstandingAmount: Double = 0.0,
        lifetimePurchases: Double = 5000.0
    ): CustomerMetrics {
        return CustomerMetrics(
            customerId = customerId,
            customerName = customerName,
            phone = "9876543210",
            periodPurchases = 1000.0,
            periodOrders = 2,
            periodUnitsPurchased = 10,
            lifetimePurchases = lifetimePurchases,
            lifetimeOrders = 10,
            lifetimeUnitsPurchased = 50,
            averageOrderValue = 500.0,
            firstPurchaseDate = referenceTime - (100 * oneDayMs),
            lastPurchaseDate = if (daysSinceLastPurchase != null) referenceTime - (daysSinceLastPurchase * oneDayMs) else 0L,
            daysSinceLastPurchase = daysSinceLastPurchase,
            purchaseFrequencyPerMonth = 2.0,
            outstandingAmount = outstandingAmount,
            hasOutstanding = outstandingAmount > 0.0,
            totalPaid = lifetimePurchases - outstandingAmount,
            segment = segment,
            activityStatus = activityStatus,
            hasSufficientData = true
        )
    }

    // ==========================================
    // 1. HIGH MARGIN + STOCKOUT
    // ==========================================

    @Test
    fun test01_highMarginWithStockoutProducesCriticalRisk() {
        val inv = listOf(createInvMetric("p1", stock = 0, unitsSold = 25, velocity = 1.5, daysRemaining = 0.0))
        val prof = listOf(createProfMetric("p1", marginPct = 48.0, marginCategory = MarginCategory.HIGH_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        assertEquals(1, summary.totalInsights)
        val insight = summary.insights.first()
        assertEquals(CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK, insight.category)
        assertEquals(CrossInsightSeverity.CRITICAL, insight.severity)
        assertEquals("p1", insight.entityId)
        assertTrue(insight.title.contains("High-Margin Stockout"))
    }

    // ==========================================
    // 2. HIGH MARGIN + CRITICALLY LOW STOCK COVERAGE
    // ==========================================

    @Test
    fun test02_highMarginWithCriticallyLowStockCoverageProducesHighRisk() {
        val inv = listOf(createInvMetric("p1", stock = 2, minStock = 5, velocity = 1.0, daysRemaining = 2.0))
        val prof = listOf(createProfMetric("p1", marginPct = 45.0, marginCategory = MarginCategory.HIGH_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        assertEquals(1, summary.totalInsights)
        val insight = summary.insights.first()
        assertEquals(CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK, insight.category)
        assertEquals(CrossInsightSeverity.HIGH, insight.severity)
        assertTrue(insight.title.contains("Running Out"))
    }

    // ==========================================
    // 3. HIGH MARGIN WITHOUT STOCK RISK
    // ==========================================

    @Test
    fun test03_highMarginWithoutStockRiskDoesNotTriggerStockoutInsight() {
        val inv = listOf(createInvMetric("p1", stock = 100, minStock = 10, velocity = 1.0, daysRemaining = 45.0))
        val prof = listOf(createProfMetric("p1", marginPct = 45.0, marginCategory = MarginCategory.HIGH_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        // Stock is healthy, so no HIGH_MARGIN_STOCKOUT_RISK insight
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK })
    }

    // ==========================================
    // 4. HIGH VELOCITY + HIGH MARGIN
    // ==========================================

    @Test
    fun test04_highVelocityWithHighMarginProducesStarPerformer() {
        val inv = listOf(createInvMetric("p1", velocityCategory = VelocityCategory.FAST_MOVING, velocity = 4.0))
        val prof = listOf(createProfMetric("p1", marginPct = 40.0, marginCategory = MarginCategory.HIGH_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        val star = summary.insights.find { it.category == CrossInsightCategory.STAR_PERFORMER }
        assertNotNull(star)
        assertEquals(CrossInsightSeverity.POSITIVE, star!!.severity)
        assertEquals("p1", star.entityId)
    }

    // ==========================================
    // 5. HIGH VELOCITY WITHOUT HIGH MARGIN
    // ==========================================

    @Test
    fun test05_highVelocityWithoutHighMarginDoesNotProduceStarPerformer() {
        // Fast moving but medium margin (25% < 35% threshold)
        val inv = listOf(createInvMetric("p1", velocityCategory = VelocityCategory.FAST_MOVING, velocity = 4.0))
        val prof = listOf(createProfMetric("p1", marginPct = 25.0, marginCategory = MarginCategory.MEDIUM_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        assertFalse(summary.insights.any { it.category == CrossInsightCategory.STAR_PERFORMER })
    }

    // ==========================================
    // 6. DEAD STOCK + LOW/ZERO MARGIN
    // ==========================================

    @Test
    fun test06_deadStockWithZeroMarginProducesCriticalCapitalTrap() {
        val inv = listOf(createInvMetric("p1", stock = 10, isDeadStock = true, capitalLocked = 5000.0))
        val prof = listOf(createProfMetric("p1", marginPct = 0.0, marginCategory = MarginCategory.NO_PROFIT))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        val trap = summary.insights.find { it.category == CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP }
        assertNotNull(trap)
        assertEquals(CrossInsightSeverity.CRITICAL, trap!!.severity)
        assertEquals(5000.0, trap.metricValue ?: 0.0, 0.001)
    }

    // ==========================================
    // 7. SLOW STOCK + LOW MARGIN
    // ==========================================

    @Test
    fun test07_slowStockWithLowMarginProducesHighCapitalTrap() {
        val inv = listOf(createInvMetric("p1", stock = 15, velocityCategory = VelocityCategory.SLOW_MOVING, capitalLocked = 3000.0))
        val prof = listOf(createProfMetric("p1", marginPct = 10.0, marginCategory = MarginCategory.LOW_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        val trap = summary.insights.find { it.category == CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP }
        assertNotNull(trap)
        assertEquals(CrossInsightSeverity.HIGH, trap!!.severity)
        assertEquals(3000.0, trap.metricValue ?: 0.0, 0.001)
    }

    // ==========================================
    // 8. HIGH VELOCITY + THIN MARGIN
    // ==========================================

    @Test
    fun test08_highVelocityWithThinMarginProducesVolumeMarginVulnerability() {
        val inv = listOf(createInvMetric("p1", velocityCategory = VelocityCategory.FAST_MOVING, velocity = 8.0))
        val prof = listOf(createProfMetric("p1", marginPct = 6.0, marginCategory = MarginCategory.LOW_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        val vul = summary.insights.find { it.category == CrossInsightCategory.VOLUME_MARGIN_VULNERABILITY }
        assertNotNull(vul)
        assertEquals(CrossInsightSeverity.MEDIUM, vul!!.severity)
        assertTrue(vul.title.contains("High Volume, Thin Margin"))
    }

    // ==========================================
    // 9. HIGH-VALUE CUSTOMER + OUTSTANDING
    // ==========================================

    @Test
    fun test09_highValueCustomerWithOutstandingProducesKeyAccountExposure() {
        val cust = listOf(createCustMetric("c1", segment = CustomerSegment.HIGH_VALUE, outstandingAmount = 7500.0))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(customerMetrics = cust)

        val exp = summary.insights.find { it.category == CrossInsightCategory.KEY_ACCOUNT_EXPOSURE }
        assertNotNull(exp)
        assertEquals(CrossInsightSeverity.HIGH, exp!!.severity)
        assertEquals("c1", exp.entityId)
        assertEquals(7500.0, exp.metricValue ?: 0.0, 0.001)
    }

    // ==========================================
    // 10. FREQUENT CUSTOMER + OUTSTANDING
    // ==========================================

    @Test
    fun test10_frequentCustomerWithOutstandingProducesKeyAccountExposure() {
        val cust = listOf(createCustMetric("c2", segment = CustomerSegment.FREQUENT, outstandingAmount = 6000.0))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(customerMetrics = cust)

        val exp = summary.insights.find { it.category == CrossInsightCategory.KEY_ACCOUNT_EXPOSURE }
        assertNotNull(exp)
        assertEquals(CrossInsightSeverity.HIGH, exp!!.severity)
        assertEquals("c2", exp.entityId)
    }

    // ==========================================
    // 11. INACTIVE CUSTOMER + OUTSTANDING
    // ==========================================

    @Test
    fun test11_inactiveCustomerWithOutstandingProducesDormantDebtRisk() {
        val custCritical = listOf(
            createCustMetric("c_dormant_crit", activityStatus = CustomerActivityStatus.INACTIVE, daysSinceLastPurchase = 60L, outstandingAmount = 6000.0),
            createCustMetric("c_dormant_high", activityStatus = CustomerActivityStatus.INACTIVE, daysSinceLastPurchase = 50L, outstandingAmount = 2000.0)
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(customerMetrics = custCritical)

        val critInsight = summary.insights.find { it.entityId == "c_dormant_crit" }
        assertNotNull(critInsight)
        assertEquals(CrossInsightCategory.DORMANT_DEBT_RISK, critInsight!!.category)
        assertEquals(CrossInsightSeverity.CRITICAL, critInsight.severity)

        val highInsight = summary.insights.find { it.entityId == "c_dormant_high" }
        assertNotNull(highInsight)
        assertEquals(CrossInsightCategory.DORMANT_DEBT_RISK, highInsight!!.category)
        assertEquals(CrossInsightSeverity.HIGH, highInsight.severity)
    }

    // ==========================================
    // 12. INACTIVE CUSTOMER WITHOUT OUTSTANDING
    // ==========================================

    @Test
    fun test12_inactiveCustomerWithoutOutstandingDoesNotProduceDormantDebt() {
        val cust = listOf(
            createCustMetric("c_clean", activityStatus = CustomerActivityStatus.INACTIVE, daysSinceLastPurchase = 90L, outstandingAmount = 0.0)
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(customerMetrics = cust)

        assertFalse(summary.insights.any { it.category == CrossInsightCategory.DORMANT_DEBT_RISK })
    }

    // ==========================================
    // 13. WORKING-CAPITAL STRAIN WITH BOTH SIGNALS
    // ==========================================

    @Test
    fun test13_workingCapitalStrainWithBothInventoryAndReceivablesSignals() {
        val invSummary = InventoryIntelligenceSummary(
            totalInventoryValue = 50000.0,
            capitalLockedInSlowAndDeadStock = 7000.0,
            fastMovingCount = 1,
            normalCount = 1,
            slowMovingCount = 1,
            deadStockCount = 1,
            products = emptyList()
        )
        val collSummary = CollectionPrioritySummary(
            customersAnalyzed = 10,
            customersWithOutstanding = 3,
            highPriorityCount = 1,
            mediumPriorityCount = 1,
            lowPriorityCount = 1,
            noneCount = 7,
            insufficientDataCount = 0,
            totalOutstanding = 5000.0,
            results = emptyList()
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = invSummary,
            collectionSummary = collSummary
        )

        val strain = summary.insights.find { it.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN }
        assertNotNull(strain)
        assertEquals(CrossInsightSeverity.CRITICAL, strain!!.severity) // 7000 + 5000 = 12000 >= 10000 (CRITICAL)
        assertEquals(12000.0, summary.workingCapitalLockedTotal, 0.001)
    }

    // ==========================================
    // 14. WORKING-CAPITAL STRAIN WHEN ONLY ONE SIDE EXISTS
    // ==========================================

    @Test
    fun test14_workingCapitalStrainWhenOnlyOneSideExistsDoesNotTrigger() {
        // Only inventory side has capital locked; zero receivables
        val invSummaryOnly = InventoryIntelligenceSummary(
            totalInventoryValue = 50000.0,
            capitalLockedInSlowAndDeadStock = 12000.0,
            fastMovingCount = 0,
            normalCount = 1,
            slowMovingCount = 1,
            deadStockCount = 1,
            products = emptyList()
        )
        val collSummaryZero = CollectionPrioritySummary(
            customersAnalyzed = 5,
            customersWithOutstanding = 0,
            highPriorityCount = 0,
            mediumPriorityCount = 0,
            lowPriorityCount = 0,
            noneCount = 5,
            insufficientDataCount = 0,
            totalOutstanding = 0.0,
            results = emptyList()
        )

        val summaryA = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = invSummaryOnly,
            collectionSummary = collSummaryZero
        )
        assertFalse(summaryA.insights.any { it.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN })

        // Only receivables side has outstanding; zero inventory locked
        val invSummaryZero = InventoryIntelligenceSummary(
            totalInventoryValue = 50000.0,
            capitalLockedInSlowAndDeadStock = 0.0,
            fastMovingCount = 2,
            normalCount = 2,
            slowMovingCount = 0,
            deadStockCount = 0,
            products = emptyList()
        )
        val collSummaryOnly = CollectionPrioritySummary(
            customersAnalyzed = 5,
            customersWithOutstanding = 2,
            highPriorityCount = 1,
            mediumPriorityCount = 1,
            lowPriorityCount = 0,
            noneCount = 3,
            insufficientDataCount = 0,
            totalOutstanding = 15000.0,
            results = emptyList()
        )

        val summaryB = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = invSummaryZero,
            collectionSummary = collSummaryOnly
        )
        assertFalse(summaryB.insights.any { it.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN })
    }

    // ==========================================
    // 15. MULTIPLE ALERTS ON SAME FOCAL ENTITY
    // ==========================================

    @Test
    fun test15_multipleAlertsOnSameFocalEntityTriggersConvergence() {
        val alerts = listOf(
            BusinessAlert(
                alertId = "a1",
                severity = AlertSeverity.CRITICAL,
                category = AlertCategory.INVENTORY,
                productId = "prod_focus",
                productName = "Focal Product",
                title = "Critical Stockout Alert",
                description = "Stock is zero",
                supportingMetric = null,
                metricValue = null,
                unit = null
            ),
            BusinessAlert(
                alertId = "a2",
                severity = AlertSeverity.WARNING,
                category = AlertCategory.PROFITABILITY,
                productId = "prod_focus",
                productName = "Focal Product",
                title = "Margin Erosion Warning",
                description = "Margin dropped 15%",
                supportingMetric = null,
                metricValue = null,
                unit = null
            )
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(businessAlerts = alerts)

        val conv = summary.insights.find { it.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE }
        assertNotNull(conv)
        assertEquals("prod_focus", conv!!.entityId)
        assertEquals(2.0, conv.metricValue ?: 0.0, 0.001)
    }

    // ==========================================
    // 16. MULTIPLE ACTIONS ON SAME FOCAL ENTITY
    // ==========================================

    @Test
    fun test16_actionsAndAlertReinforcementOnSameEntity() {
        // An alert and an action reinforcing the same product
        val alerts = listOf(
            BusinessAlert(
                alertId = "a1",
                severity = AlertSeverity.WARNING,
                category = AlertCategory.INVENTORY,
                productId = "prod_reinforced",
                productName = "Reinforced Item",
                title = "Slow Moving Alert",
                description = "Slow turnover",
                supportingMetric = null,
                metricValue = null,
                unit = null
            )
        )
        val actions = listOf(
            BusinessAction(
                actionId = "act1",
                priority = BusinessActionPriority.HIGH,
                category = BusinessActionCategory.CLEAR_STOCK,
                title = "Discount Item",
                description = "Discount to clear",
                targetId = "prod_reinforced",
                targetName = "Reinforced Item",
                actionLabel = "Discount"
            )
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            businessAlerts = alerts,
            businessActions = actions
        )

        val conv = summary.insights.find { it.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE }
        assertNotNull(conv)
        assertEquals("prod_reinforced", conv!!.entityId)
        assertEquals(2.0, conv.metricValue ?: 0.0, 0.001)
    }

    // ==========================================
    // 17. MULTI-SIGNAL CONVERGENCE DETAILS
    // ==========================================

    @Test
    fun test17_multiSignalConvergenceContributingSignalsAndDetails() {
        val alerts = listOf(
            BusinessAlert("a1", AlertSeverity.CRITICAL, AlertCategory.INVENTORY, "p_conv", "Item Conv", "Alert 1", "Desc 1", null, null, null),
            BusinessAlert("a2", AlertSeverity.WARNING, AlertCategory.PROFITABILITY, "p_conv", "Item Conv", "Alert 2", "Desc 2", null, null, null)
        )
        val actions = listOf(
            BusinessAction(
                actionId = "act1",
                priority = BusinessActionPriority.HIGH,
                category = BusinessActionCategory.RESTOCK,
                title = "Action 1",
                description = "Restock",
                targetId = "p_conv",
                targetName = "Item Conv",
                actionLabel = "Restock"
            )
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            businessAlerts = alerts,
            businessActions = actions
        )

        val conv = summary.insights.find { it.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE }
        assertNotNull(conv)
        assertEquals(3.0, conv!!.metricValue ?: 0.0, 0.001)
        assertEquals("signals", conv.metricUnit)
        assertTrue(conv.contributingSignals.any { it.startsWith("Alert:") })
        assertTrue(conv.contributingSignals.any { it.startsWith("Action:") })
    }

    // ==========================================
    // 18. DUPLICATE INPUT RECORDS
    // ==========================================

    @Test
    fun test18_duplicateInputRecordsHandledSafely() {
        val invItem = createInvMetric("p_dup", velocityCategory = VelocityCategory.FAST_MOVING)
        val profItem = createProfMetric("p_dup", marginPct = 45.0, marginCategory = MarginCategory.HIGH_MARGIN)

        // Passing duplicate items in the metrics lists
        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = listOf(invItem, invItem),
            profitabilityMetrics = listOf(profItem, profItem)
        )

        // Deduplication by ID ensures only 1 star performer insight is produced
        val starList = summary.insights.filter { it.category == CrossInsightCategory.STAR_PERFORMER && it.entityId == "p_dup" }
        assertEquals(1, starList.size)
    }

    // ==========================================
    // 19. EMPTY INPUT
    // ==========================================

    @Test
    fun test19_emptyInputReturnsZeroInsights() {
        val summary = CrossBusinessInsightsEngine.generateFromIntelligence()
        assertEquals(0, summary.totalInsights)
        assertEquals(0, summary.criticalInsightsCount)
        assertEquals(0, summary.highInsightsCount)
        assertEquals(0, summary.mediumInsightsCount)
        assertEquals(0, summary.positiveInsightsCount)
        assertEquals(0.0, summary.workingCapitalLockedTotal, 0.001)
        assertTrue(summary.insights.isEmpty())
    }

    // ==========================================
    // 20. MISSING / NULL TARGET IDENTIFIERS
    // ==========================================

    @Test
    fun test20_missingOrNullTargetIdentifiersHandledSafely() {
        val alertsWithNulls = listOf(
            BusinessAlert("a_null", AlertSeverity.WARNING, AlertCategory.ATTENTION, null, null, "General Warning", "No product", null, null, null)
        )
        val actionsWithNulls = listOf(
            BusinessAction(
                actionId = "act_null",
                priority = BusinessActionPriority.LOW,
                category = BusinessActionCategory.COLLECT_PAYMENT,
                title = "General Review",
                description = "Review khata",
                actionLabel = "Review"
            )
        )
        val invWithNulls = listOf(
            createInvMetric("p_nulls", daysRemaining = null)
        )
        val custWithNulls = listOf(
            createCustMetric("c_nulls", daysSinceLastPurchase = null)
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = invWithNulls,
            customerMetrics = custWithNulls,
            businessAlerts = alertsWithNulls,
            businessActions = actionsWithNulls
        )

        assertNotNull(summary)
        // No crash occurs and alerts/actions with null product/target ID do not cause invalid convergence
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE })
    }

    // ==========================================
    // 21. DETERMINISTIC ORDERING WITH EQUAL PRIORITIES
    // ==========================================

    @Test
    fun test21_deterministicOrderingWithEqualPriorities() {
        // Two insights with the same HIGH severity:
        // Item A: High Margin low stock -> category HIGH_MARGIN_STOCKOUT_RISK
        // Customer B: High value outstanding -> category KEY_ACCOUNT_EXPOSURE
        val inv = listOf(createInvMetric("pA", stock = 2, minStock = 5, daysRemaining = 2.0))
        val prof = listOf(createProfMetric("pA", marginPct = 50.0, marginCategory = MarginCategory.HIGH_MARGIN))
        val cust = listOf(createCustMetric("cB", segment = CustomerSegment.HIGH_VALUE, outstandingAmount = 6000.0))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof,
            customerMetrics = cust
        )

        val highInsights = summary.insights.filter { it.severity == CrossInsightSeverity.HIGH }
        assertTrue(highInsights.size >= 2)

        // When severity is equal (both HIGH), sorted by category name:
        // "HIGH_MARGIN_STOCKOUT_RISK" ("H") precedes "KEY_ACCOUNT_EXPOSURE" ("K")
        assertEquals(CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK, highInsights[0].category)
        assertEquals(CrossInsightCategory.KEY_ACCOUNT_EXPOSURE, highInsights[1].category)
    }

    // ==========================================
    // 22. DETERMINISTIC REPEATED EXECUTION
    // ==========================================

    @Test
    fun test22_deterministicRepeatedExecution() {
        val inv = listOf(
            createInvMetric("p1", stock = 0, unitsSold = 10),
            createInvMetric("p2", velocityCategory = VelocityCategory.FAST_MOVING)
        )
        val prof = listOf(
            createProfMetric("p1", marginPct = 40.0, marginCategory = MarginCategory.HIGH_MARGIN),
            createProfMetric("p2", marginPct = 50.0, marginCategory = MarginCategory.HIGH_MARGIN)
        )

        val run1 = CrossBusinessInsightsEngine.generateFromIntelligence(inventoryMetrics = inv, profitabilityMetrics = prof)
        val run2 = CrossBusinessInsightsEngine.generateFromIntelligence(inventoryMetrics = inv, profitabilityMetrics = prof)

        assertEquals(run1.totalInsights, run2.totalInsights)
        assertEquals(run1.insights.map { it.id }, run2.insights.map { it.id })
        assertEquals(run1.insights.map { it.severity }, run2.insights.map { it.severity })
        assertEquals(run1.insights.map { it.category }, run2.insights.map { it.category })
    }

    // ==========================================
    // 23. NaN INPUT SAFETY
    // ==========================================

    @Test
    fun test23_nanInputSafety() {
        val inv = listOf(
            createInvMetric("p_nan", velocity = Double.NaN, daysRemaining = Double.NaN, capitalLocked = Double.NaN)
        )
        val prof = listOf(
            createProfMetric("p_nan", marginPct = Double.NaN, grossProfit = Double.NaN, revenue = Double.NaN)
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        assertNotNull(summary)
        // NaN margin or NaN velocity does NOT create invalid insights or crash
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.STAR_PERFORMER })
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK })
    }

    // ==========================================
    // 24. INFINITY INPUT SAFETY
    // ==========================================

    @Test
    fun test24_infinityInputSafety() {
        val inv = listOf(
            createInvMetric("p_inf", velocity = Double.POSITIVE_INFINITY, daysRemaining = Double.POSITIVE_INFINITY, capitalLocked = Double.POSITIVE_INFINITY)
        )
        val prof = listOf(
            createProfMetric("p_inf", marginPct = Double.POSITIVE_INFINITY, grossProfit = Double.POSITIVE_INFINITY)
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        assertNotNull(summary)
        // Infinite days remaining does NOT trigger stockout risk
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK })
    }

    // ==========================================
    // 25. UNRELATED SIGNALS DO NOT CREATE AN INSIGHT
    // ==========================================

    @Test
    fun test25_unrelatedSignalsDoNotCreateAnInsight() {
        // Normal inventory, normal profit, normal active customer, single isolated alert on unrelated product
        val inv = listOf(createInvMetric("p_norm", stock = 50, velocity = 1.0, daysRemaining = 50.0))
        val prof = listOf(createProfMetric("p_norm", marginPct = 25.0, marginCategory = MarginCategory.MEDIUM_MARGIN))
        val cust = listOf(createCustMetric("c_norm", segment = CustomerSegment.REGULAR, outstandingAmount = 0.0))
        val alerts = listOf(
            BusinessAlert("a_iso", AlertSeverity.INFO, AlertCategory.ATTENTION, "other_prod", "Other", "Isolated", "No peer", null, null, null)
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof,
            customerMetrics = cust,
            businessAlerts = alerts
        )

        assertEquals(0, summary.totalInsights)
    }

    // ==========================================
    // 26. NO FALSE CORRELATION BETWEEN DIFFERENT PRODUCTS
    // ==========================================

    @Test
    fun test26_noFalseCorrelationBetweenDifferentProducts() {
        // Product 1 has stockout, but LOW margin (10%)
        val invP1 = createInvMetric("p1", stock = 0, unitsSold = 10)
        val profP1 = createProfMetric("p1", marginPct = 10.0, marginCategory = MarginCategory.LOW_MARGIN)

        // Product 2 has ample stock (100), but HIGH margin (50%)
        val invP2 = createInvMetric("p2", stock = 100, daysRemaining = 50.0)
        val profP2 = createProfMetric("p2", marginPct = 50.0, marginCategory = MarginCategory.HIGH_MARGIN)

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = listOf(invP1, invP2),
            profitabilityMetrics = listOf(profP1, profP2)
        )

        // Neither product should trigger HIGH_MARGIN_STOCKOUT_RISK
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK })
    }

    // ==========================================
    // 27. NO FALSE CORRELATION BETWEEN DIFFERENT CUSTOMERS
    // ==========================================

    @Test
    fun test27_noFalseCorrelationBetweenDifferentCustomers() {
        // Customer 1 is HIGH_VALUE but has 0 outstanding
        val cust1 = createCustMetric("c1", segment = CustomerSegment.HIGH_VALUE, outstandingAmount = 0.0)

        // Customer 2 has outstanding (6000), but is REGULAR and ACTIVE (purchased yesterday)
        val cust2 = createCustMetric("c2", segment = CustomerSegment.REGULAR, activityStatus = CustomerActivityStatus.ACTIVE, daysSinceLastPurchase = 1L, outstandingAmount = 6000.0)

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            customerMetrics = listOf(cust1, cust2)
        )

        // Customer 1 has no dues; Customer 2 is not high-value/frequent and is active.
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.KEY_ACCOUNT_EXPOSURE })
        assertFalse(summary.insights.any { it.category == CrossInsightCategory.DORMANT_DEBT_RISK })
    }

    // ==========================================
    // 28. IMMUTABLE INPUT COLLECTIONS REMAIN UNCHANGED
    // ==========================================

    @Test
    fun test28_immutableInputCollectionsRemainUnchanged() {
        val invList = listOf(createInvMetric("p1", stock = 0, unitsSold = 5))
        val profList = listOf(createProfMetric("p1", marginPct = 40.0, marginCategory = MarginCategory.HIGH_MARGIN))
        val custList = listOf(createCustMetric("c1", outstandingAmount = 6000.0, segment = CustomerSegment.HIGH_VALUE))

        CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = invList,
            profitabilityMetrics = profList,
            customerMetrics = custList
        )

        assertEquals(1, invList.size)
        assertEquals("p1", invList.first().productId)
        assertEquals(1, profList.size)
        assertEquals("p1", profList.first().productId)
        assertEquals(1, custList.size)
        assertEquals("c1", custList.first().customerId)
    }

    // ==========================================
    // 29. STABLE INSIGHT IDS / DEDUPLICATION
    // ==========================================

    @Test
    fun test29_stableInsightIdsAndDeduplication() {
        val inv = listOf(createInvMetric("prod_abc", velocityCategory = VelocityCategory.FAST_MOVING))
        val prof = listOf(createProfMetric("prod_abc", marginPct = 45.0, marginCategory = MarginCategory.HIGH_MARGIN))

        val summary1 = CrossBusinessInsightsEngine.generateFromIntelligence(inventoryMetrics = inv, profitabilityMetrics = prof)
        val summary2 = CrossBusinessInsightsEngine.generateFromIntelligence(inventoryMetrics = inv, profitabilityMetrics = prof)

        val id1 = summary1.insights.first().id
        val id2 = summary2.insights.first().id

        assertEquals("insight_star_performer_prod_abc", id1)
        assertEquals(id1, id2)
    }

    // ==========================================
    // 30. OUTPUT REMAINS DETERMINISTIC ACROSS REPEATED CALLS
    // ==========================================

    @Test
    fun test30_outputRemainsDeterministicAcrossRepeatedCalls() {
        val p1 = createProduct("p1", "Atta 10kg", price = 400.0, cost = 200.0, stock = 0, minStock = 5)
        val c1 = createCustomer("c1", "Mohan Lal", outstanding = 6000.0, totalPaid = 1000.0)
        val s1 = createSale("s1", "c1", "p1", quantity = 10, price = 400.0, cost = 200.0)

        val baseline = CrossBusinessInsightsEngine.generateInsights(
            products = listOf(p1),
            sales = listOf(s1),
            customers = listOf(c1)
        )

        for (i in 1..10) {
            val repeat = CrossBusinessInsightsEngine.generateInsights(
                products = listOf(p1),
                sales = listOf(s1),
                customers = listOf(c1)
            )
            assertEquals(baseline.totalInsights, repeat.totalInsights)
            assertEquals(baseline.criticalInsightsCount, repeat.criticalInsightsCount)
            assertEquals(baseline.highInsightsCount, repeat.highInsightsCount)
            assertEquals(baseline.insights.map { it.id }, repeat.insights.map { it.id })
        }
    }

    // ==========================================
    // 31. KEY ACCOUNT EXPOSURE TRIGGERED BY HIGH COLLECTION PRIORITY
    // ==========================================

    @Test
    fun test31_keyAccountExposureTriggeredByHighCollectionPriority() {
        // Outstanding amount is ₹3000 (< ₹5000 threshold), but CollectionPriorityResult is HIGH
        val cust = listOf(createCustMetric("c_vip", segment = CustomerSegment.HIGH_VALUE, outstandingAmount = 3000.0))
        val coll = listOf(
            CollectionPriorityResult(
                customerId = "c_vip",
                customerName = "VIP Customer",
                phone = "9876543210",
                outstandingAmount = 3000.0,
                priority = CollectionPriorityLevel.HIGH,
                reasons = listOf("Critical payment overdue"),
                daysSinceLastPurchase = 5L,
                activityStatus = CustomerActivityStatus.ACTIVE,
                segment = CustomerSegment.HIGH_VALUE,
                lifetimePurchases = 15000.0,
                lifetimeOrders = 5,
                totalPaid = 12000.0
            )
        )

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            customerMetrics = cust,
            collectionResults = coll
        )

        val exp = summary.insights.find { it.category == CrossInsightCategory.KEY_ACCOUNT_EXPOSURE }
        assertNotNull(exp)
        assertEquals(CrossInsightSeverity.HIGH, exp!!.severity)
        assertEquals("c_vip", exp.entityId)
    }

    // ==========================================
    // 32. WORKING CAPITAL STRAIN SEVERITY THRESHOLDS
    // ==========================================

    @Test
    fun test32_workingCapitalStrainSeverityThresholds() {
        // Below 10000.0 -> HIGH severity
        val invSub = InventoryIntelligenceSummary(
            totalInventoryValue = 20000.0,
            capitalLockedInSlowAndDeadStock = 3000.0,
            fastMovingCount = 1, normalCount = 1, slowMovingCount = 1, deadStockCount = 0, products = emptyList()
        )
        val collSub = CollectionPrioritySummary(
            customersAnalyzed = 5, customersWithOutstanding = 1, highPriorityCount = 1, mediumPriorityCount = 0,
            lowPriorityCount = 0, noneCount = 4, insufficientDataCount = 0, totalOutstanding = 4000.0, results = emptyList()
        )

        val summaryHigh = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = invSub,
            collectionSummary = collSub
        )
        val strainHigh = summaryHigh.insights.find { it.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN }
        assertNotNull(strainHigh)
        assertEquals(CrossInsightSeverity.HIGH, strainHigh!!.severity)
        assertEquals(7000.0, summaryHigh.workingCapitalLockedTotal, 0.001)

        // At or Above 10000.0 -> CRITICAL severity
        val invCrit = InventoryIntelligenceSummary(
            totalInventoryValue = 30000.0,
            capitalLockedInSlowAndDeadStock = 6000.0,
            fastMovingCount = 1, normalCount = 1, slowMovingCount = 1, deadStockCount = 1, products = emptyList()
        )
        val collCrit = CollectionPrioritySummary(
            customersAnalyzed = 5, customersWithOutstanding = 2, highPriorityCount = 2, mediumPriorityCount = 0,
            lowPriorityCount = 0, noneCount = 3, insufficientDataCount = 0, totalOutstanding = 6000.0, results = emptyList()
        )

        val summaryCrit = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = invCrit,
            collectionSummary = collCrit
        )
        val strainCrit = summaryCrit.insights.find { it.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN }
        assertNotNull(strainCrit)
        assertEquals(CrossInsightSeverity.CRITICAL, strainCrit!!.severity)
        assertEquals(12000.0, summaryCrit.workingCapitalLockedTotal, 0.001)
    }

    // ==========================================
    // 33. STAR PERFORMER RECORDS GROSS PROFIT METRIC
    // ==========================================

    @Test
    fun test33_starPerformerRecordsGrossProfitMetric() {
        val inv = listOf(createInvMetric("p_star", velocityCategory = VelocityCategory.FAST_MOVING, velocity = 5.0))
        val prof = listOf(createProfMetric("p_star", marginPct = 42.0, grossProfit = 8500.0, marginCategory = MarginCategory.HIGH_MARGIN))

        val summary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventoryMetrics = inv,
            profitabilityMetrics = prof
        )

        val star = summary.insights.find { it.category == CrossInsightCategory.STAR_PERFORMER }
        assertNotNull(star)
        assertEquals(8500.0, star!!.metricValue ?: 0.0, 0.001)
        assertEquals("₹", star.metricUnit)
    }

    // ==========================================
    // 34. RAW MODEL DELEGATION END-TO-END
    // ==========================================

    @Test
    fun test34_rawModelDelegationWithMultipleInteractions() {
        // Product 1: Out of stock, high margin (selling price 200, cost 100 -> 50% margin)
        val p1 = createProduct("p1", "Almonds 500g", price = 400.0, cost = 200.0, stock = 0, minStock = 5)
        // Product 2: In stock, normal
        val p2 = createProduct("p2", "Sugar 1kg", price = 40.0, cost = 30.0, stock = 50, minStock = 10)
        // Customer 1: High balance
        val c1 = createCustomer("c1", "Gupta Traders", outstanding = 8000.0, totalPaid = 2000.0)
        // Sales
        val s1 = createSale("s1", "c1", "p1", quantity = 10, price = 400.0, cost = 200.0)

        val summary = CrossBusinessInsightsEngine.generateInsights(
            products = listOf(p1, p2),
            sales = listOf(s1),
            customers = listOf(c1)
        )

        assertNotNull(summary)
        assertTrue(summary.totalInsights > 0)
        assertTrue(summary.insights.any { it.entityId == "p1" || it.entityId == "c1" })
    }
}
