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

class UnifiedBusinessCommandCenterEngineTest {

    private val referenceTime = 1700000000000L
    private val oneDayMs = 24 * 60 * 60 * 1000L

    // ==========================================
    // HELPERS FOR TEST DATA
    // ==========================================

    private fun createAlert(
        id: String,
        severity: AlertSeverity = AlertSeverity.CRITICAL,
        category: AlertCategory = AlertCategory.INVENTORY,
        productId: String? = "p1",
        productName: String? = "Product 1",
        title: String = "Low Stock Alert: $productName",
        desc: String = "Stock is below minimum threshold"
    ): BusinessAlert {
        return BusinessAlert(
            alertId = id,
            severity = severity,
            category = category,
            productId = productId,
            productName = productName,
            title = title,
            description = desc,
            supportingMetric = "Units",
            metricValue = 0.0,
            unit = "units"
        )
    }

    private fun createAction(
        id: String,
        priority: BusinessActionPriority = BusinessActionPriority.HIGH,
        category: BusinessActionCategory = BusinessActionCategory.RESTOCK,
        targetId: String? = "p1",
        targetName: String? = "Product 1",
        title: String = "Reorder Stock: $targetName",
        desc: String = "Restock units immediately",
        actionLabel: String = "Reorder",
        route: String = "product_detail/$targetId"
    ): BusinessAction {
        return BusinessAction(
            actionId = id,
            priority = priority,
            category = category,
            title = title,
            description = desc,
            targetId = targetId,
            targetName = targetName,
            supportingMetric = "Days remaining",
            metricValue = 2.0,
            metricUnit = "days",
            actionLabel = actionLabel,
            destinationRoute = route
        )
    }

    private fun createInsight(
        id: String,
        category: CrossInsightCategory = CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK,
        severity: CrossInsightSeverity = CrossInsightSeverity.CRITICAL,
        entityId: String? = "p1",
        entityName: String? = "Product 1",
        entityType: CrossInsightEntityType = CrossInsightEntityType.PRODUCT,
        title: String = "High-Margin Stockout Risk: $entityName",
        summary: String = "Product yields high margin but zero units are available",
        metricValue: Double = 45.0,
        metricUnit: String = "%"
    ): CrossBusinessInsight {
        return CrossBusinessInsight(
            id = id,
            category = category,
            severity = severity,
            title = title,
            summary = summary,
            detailedExplanation = "Detailed explanation for $title",
            contributingSignals = listOf("Inventory: Stock 0", "Profitability: High margin ($metricValue%)"),
            entityId = entityId,
            entityName = entityName,
            entityType = entityType,
            metricValue = metricValue,
            metricUnit = metricUnit,
            recommendedStrategy = "Restock immediately"
        )
    }

    private fun createCollection(
        customerId: String,
        customerName: String = "Customer $customerId",
        outstanding: Double = 6000.0,
        priority: CollectionPriorityLevel = CollectionPriorityLevel.HIGH
    ): CollectionPriorityResult {
        return CollectionPriorityResult(
            customerId = customerId,
            customerName = customerName,
            phone = "9876543210",
            outstandingAmount = outstanding,
            priority = priority,
            reasons = listOf("Overdue balance exceeding ₹5,000"),
            daysSinceLastPurchase = 20L,
            activityStatus = CustomerActivityStatus.ACTIVE,
            segment = CustomerSegment.HIGH_VALUE,
            lifetimePurchases = 30000.0,
            lifetimeOrders = 10,
            totalPaid = 24000.0
        )
    }

    // ==========================================
    // 1. EMPTY INPUT
    // ==========================================

    @Test
    fun test01_emptyInputProducesZeroItems() {
        val center = UnifiedBusinessCommandCenterEngine.generate()
        assertEquals(0, center.summary.totalItems)
        assertEquals(0, center.summary.attentionCount)
        assertEquals(0, center.summary.actionCount)
        assertEquals(0, center.summary.insightCount)
        assertEquals(0, center.summary.opportunityCount)
        assertEquals(0.0, center.summary.workingCapitalLocked, 0.001)
        assertTrue(center.attentionItems.isEmpty())
        assertTrue(center.actions.isEmpty())
        assertTrue(center.insights.isEmpty())
        assertTrue(center.opportunities.isEmpty())
        assertTrue(center.allItems.isEmpty())
        assertNull(center.workingCapitalLockedTotal)
        assertNull(center.profitExplanationSummary)
    }

    // ==========================================
    // 2. ONLY ALERTS
    // ==========================================

    @Test
    fun test02_onlyAlertsProducesAttentionItems() {
        val alerts = listOf(
            createAlert("a1", severity = AlertSeverity.CRITICAL, title = "Stockout Alert"),
            createAlert("a2", severity = AlertSeverity.WARNING, title = "Low Margin Warning")
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = alerts)

        assertEquals(2, center.summary.totalItems)
        assertEquals(2, center.summary.attentionCount)
        assertEquals(0, center.summary.actionCount)
        assertEquals(0, center.summary.insightCount)
        assertTrue(center.attentionItems.all { it.sourceType == CommandCenterSourceType.BUSINESS_ALERT })
    }

    // ==========================================
    // 3. ONLY ACTIONS
    // ==========================================

    @Test
    fun test03_onlyActionsProducesActionItems() {
        val actions = listOf(
            createAction("act1", priority = BusinessActionPriority.HIGH),
            createAction("act2", priority = BusinessActionPriority.MEDIUM)
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = actions)

        assertEquals(2, center.actions.size)
        assertEquals(2, center.summary.actionCount)
        assertEquals("act1", center.actions[0].sourceId)
        assertEquals("act2", center.actions[1].sourceId)
    }

    // ==========================================
    // 4. ONLY CROSS-BUSINESS INSIGHTS
    // ==========================================

    @Test
    fun test04_onlyCrossBusinessInsightsProducesInsightsItems() {
        val insights = listOf(
            createInsight("ins1", category = CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP),
            createInsight("ins2", category = CrossInsightCategory.KEY_ACCOUNT_EXPOSURE)
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = insights)

        assertEquals(2, center.insights.size)
        assertEquals(2, center.summary.insightCount)
        assertTrue(center.insights.any { it.sourceId == "ins1" })
        assertTrue(center.insights.any { it.sourceId == "ins2" })
    }

    // ==========================================
    // 5. COMBINED ALERTS, ACTIONS, AND INSIGHTS
    // ==========================================

    @Test
    fun test05_combinedAlertsActionsAndInsights() {
        val alerts = listOf(createAlert("a1", productId = "p1"))
        val actions = listOf(createAction("act1", targetId = "p2"))
        val insights = listOf(createInsight("ins1", entityId = "p3"))

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = alerts,
            businessActions = actions,
            crossBusinessInsights = insights
        )

        assertTrue(center.summary.totalItems >= 3)
        assertFalse(center.attentionItems.isEmpty())
        assertFalse(center.actions.isEmpty())
        assertFalse(center.insights.isEmpty())
    }

    // ==========================================
    // 6. CRITICAL ITEM PRIORITIZATION
    // ==========================================

    @Test
    fun test06_criticalItemPrioritization() {
        val items = listOf(
            createAlert("a_crit", severity = AlertSeverity.CRITICAL, productId = "p1"),
            createAlert("a_warn", severity = AlertSeverity.WARNING, productId = "p2")
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = items)

        val firstItem = center.allItems.first()
        assertEquals(CommandCenterPriority.CRITICAL, firstItem.priority)
    }

    // ==========================================
    // 7. HIGH ITEM PRIORITIZATION
    // ==========================================

    @Test
    fun test07_highItemPrioritization() {
        val actions = listOf(
            createAction("act_med", priority = BusinessActionPriority.MEDIUM, targetId = "p_med"),
            createAction("act_high", priority = BusinessActionPriority.HIGH, targetId = "p_high")
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = actions)

        assertEquals(CommandCenterPriority.HIGH, center.actions.first().priority)
    }

    // ==========================================
    // 8. MEDIUM ITEM PRIORITIZATION
    // ==========================================

    @Test
    fun test08_mediumItemPrioritization() {
        val actions = listOf(
            createAction("act_low", priority = BusinessActionPriority.LOW, targetId = "p_low"),
            createAction("act_med", priority = BusinessActionPriority.MEDIUM, targetId = "p_med")
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = actions)

        assertEquals(CommandCenterPriority.MEDIUM, center.actions.first().priority)
    }

    // ==========================================
    // 9. POSITIVE / OPPORTUNITY HANDLING
    // ==========================================

    @Test
    fun test09_positiveOpportunityHandling() {
        val starPerformer = createInsight(
            id = "star1",
            category = CrossInsightCategory.STAR_PERFORMER,
            severity = CrossInsightSeverity.POSITIVE,
            entityId = "p_star",
            title = "Star Performer: Basmati Rice"
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(starPerformer))

        assertEquals(1, center.opportunities.size)
        assertEquals(1, center.summary.opportunityCount)
        val opp = center.opportunities.first()
        assertEquals(CommandCenterPriority.POSITIVE, opp.priority)
        assertEquals(CommandCenterSection.OPPORTUNITIES, opp.section)
        assertEquals("p_star", opp.targetId)
    }

    // ==========================================
    // 10. DETERMINISTIC ORDERING ACROSS SECTIONS
    // ==========================================

    @Test
    fun test10_deterministicOrderingAcrossSections() {
        val alert = createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1")
        val action = createAction("act1", priority = BusinessActionPriority.HIGH, targetId = "p2")
        val insight = createInsight("ins1", severity = CrossInsightSeverity.MEDIUM, entityId = "p3")
        val opp = createInsight("opp1", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE, entityId = "p4")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight, opp)
        )

        // allItems must strictly follow Priority order: CRITICAL (0), HIGH (1), MEDIUM (2), POSITIVE (4)
        val priorities = center.allItems.map { it.priority }
        val expected = listOf(CommandCenterPriority.CRITICAL, CommandCenterPriority.HIGH, CommandCenterPriority.MEDIUM, CommandCenterPriority.POSITIVE)
        assertEquals(expected, priorities)
    }

    // ==========================================
    // 11. EQUAL PRIORITY DETERMINISTIC ORDERING
    // ==========================================

    @Test
    fun test11_equalPriorityDeterministicOrdering() {
        // Two actions of equal priority (HIGH) on different products
        val actA = createAction("actA", priority = BusinessActionPriority.HIGH, targetId = "pA", targetName = "Almonds")
        val actB = createAction("actB", priority = BusinessActionPriority.HIGH, targetId = "pB", targetName = "Cashews")

        val center1 = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(actB, actA))
        val center2 = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(actA, actB))

        assertEquals(center1.actions.map { it.itemId }, center2.actions.map { it.itemId })
        // "Almonds" ("A") precedes "Cashews" ("C")
        assertEquals("Almonds", center1.actions.first().targetName)
    }

    // ==========================================
    // 12. DUPLICATE SOURCE IDS DEDUPLICATION
    // ==========================================

    @Test
    fun test12_duplicateSourceIdsDeduplication() {
        val alert1 = createAlert("a_dup", productId = "p1")
        val alert2 = createAlert("a_dup", productId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert1, alert2))

        assertEquals(1, center.attentionItems.size)
    }

    // ==========================================
    // 13. SAME TARGET ACROSS ALERT, ACTION, AND INSIGHT CONSOLIDATION
    // ==========================================

    @Test
    fun test13_sameTargetAcrossAlertActionAndInsightConsolidation() {
        // Product p1 has:
        // 1. Stockout alert
        // 2. Restock action
        // 3. High-margin stockout insight
        val alert = createAlert("a_stock", severity = AlertSeverity.CRITICAL, category = AlertCategory.INVENTORY, productId = "p1", title = "Low Stock Alert: Item 1")
        val action = createAction("act_restock", priority = BusinessActionPriority.HIGH, category = BusinessActionCategory.RESTOCK, targetId = "p1")
        val insight = createInsight("ins_hm_oos", category = CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK, severity = CrossInsightSeverity.CRITICAL, entityId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight)
        )

        // In NEEDS_ATTENTION, these three overlapping stockout signals for p1 must be consolidated into 1 unified item
        val p1AttentionItems = center.attentionItems.filter { it.targetId == "p1" }
        assertEquals(1, p1AttentionItems.size)

        val unifiedItem = p1AttentionItems.first()
        assertEquals("unified_product_p1_stockout", unifiedItem.itemId)
        assertEquals(CommandCenterPriority.CRITICAL, unifiedItem.priority)
        assertEquals("p1", unifiedItem.targetId)
        assertTrue(unifiedItem.contributingSources.contains(CommandCenterSourceType.BUSINESS_ALERT))
        assertTrue(unifiedItem.contributingSources.contains(CommandCenterSourceType.BUSINESS_ACTION))
        assertTrue(unifiedItem.contributingSources.contains(CommandCenterSourceType.CROSS_BUSINESS_INSIGHT))
        assertEquals("a_stock", unifiedItem.relatedAlertId)
        assertEquals("act_restock", unifiedItem.relatedActionId)
        assertEquals("ins_hm_oos", unifiedItem.relatedInsightId)
    }

    // ==========================================
    // 14. RELATED ALERT + ACTION CONSOLIDATION
    // ==========================================

    @Test
    fun test14_relatedAlertAndActionConsolidation() {
        val alert = createAlert("a_dead", severity = AlertSeverity.CRITICAL, productId = "p_dead", title = "Dead Stock Alert")
        val action = createAction("act_clear", priority = BusinessActionPriority.HIGH, category = BusinessActionCategory.CLEAR_STOCK, targetId = "p_dead")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action)
        )

        val attentionDead = center.attentionItems.filter { it.targetId == "p_dead" }
        assertEquals(1, attentionDead.size)
        val unified = attentionDead.first()
        assertEquals(CommandCenterPriority.CRITICAL, unified.priority)
        assertEquals("a_dead", unified.relatedAlertId)
        assertEquals("act_clear", unified.relatedActionId)
    }

    // ==========================================
    // 15. UNRELATED SAME-PRIORITY ITEMS REMAIN SEPARATE
    // ==========================================

    @Test
    fun test15_unrelatedSamePriorityItemsRemainSeparate() {
        val alertP1 = createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1")
        val alertP2 = createAlert("a2", severity = AlertSeverity.CRITICAL, productId = "p2")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alertP1, alertP2))

        val attentionP1 = center.attentionItems.filter { it.targetId == "p1" }
        val attentionP2 = center.attentionItems.filter { it.targetId == "p2" }
        assertEquals(1, attentionP1.size)
        assertEquals(1, attentionP2.size)
    }

    // ==========================================
    // 16. SAME PRODUCT DIFFERENT ISSUES REMAIN SEPARATE
    // ==========================================

    @Test
    fun test16_sameProductDifferentIssuesRemainSeparate() {
        // Product p1 has stockout alert, AND product p1 has review pricing action (different issue domains)
        val alertStock = createAlert("a_stock", severity = AlertSeverity.CRITICAL, category = AlertCategory.INVENTORY, productId = "p1", title = "Stockout Alert")
        val actionPricing = createAction("act_price", priority = BusinessActionPriority.HIGH, category = BusinessActionCategory.REVIEW_PRICING, targetId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alertStock),
            businessActions = listOf(actionPricing)
        )

        // Because STOCKOUT and PRICING are different issue domains, they remain separate items in NEEDS_ATTENTION
        val p1Items = center.attentionItems.filter { it.targetId == "p1" }
        assertEquals(2, p1Items.size)
    }

    // ==========================================
    // 17. SAME CUSTOMER DIFFERENT ISSUES REMAIN SEPARATE
    // ==========================================

    @Test
    fun test17_sameCustomerDifferentIssuesRemainSeparate() {
        val actCollect = createAction("act_coll", priority = BusinessActionPriority.HIGH, category = BusinessActionCategory.COLLECT_PAYMENT, targetId = "c1")
        val actReengage = createAction("act_reeng", priority = BusinessActionPriority.HIGH, category = BusinessActionCategory.REENGAGE_CUSTOMER, targetId = "c1")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(actCollect, actReengage))

        val c1Items = center.attentionItems.filter { it.targetId == "c1" }
        assertEquals(2, c1Items.size)
    }

    // ==========================================
    // 18. MULTI-SIGNAL CONVERGENCE PRESERVED
    // ==========================================

    @Test
    fun test18_multiSignalConvergencePreserved() {
        val convInsight = createInsight(
            id = "conv1",
            category = CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE,
            severity = CrossInsightSeverity.HIGH,
            entityId = "p_conv",
            title = "Multi-Signal Focus: Focal Item"
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(convInsight))

        val convItem = center.insights.find { it.sourceId == "conv1" }
        assertNotNull(convItem)
        assertEquals(CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE, convItem!!.sourceType)
        assertEquals("p_conv", convItem.targetId)
    }

    // ==========================================
    // 19. SUPPORTING SIGNALS PRESERVED
    // ==========================================

    @Test
    fun test19_supportingSignalsPreserved() {
        val insight = createInsight("ins_sig", entityId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(insight))

        val item = center.insights.first()
        assertEquals(2, item.supportingSignals.size)
        assertTrue(item.supportingSignals.any { it.contains("Stock 0") })
        assertTrue(item.supportingSignals.any { it.contains("High margin") })
    }

    // ==========================================
    // 20. TARGET ID PRESERVATION
    // ==========================================

    @Test
    fun test20_targetIdPreservation() {
        val action = createAction("act_target", targetId = "prod_999")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertEquals("prod_999", center.actions.first().targetId)
    }

    // ==========================================
    // 21. TARGET NAME PRESERVATION
    // ==========================================

    @Test
    fun test21_targetNamePreservation() {
        val action = createAction("act_name", targetName = "Sunflower Oil 1L")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertEquals("Sunflower Oil 1L", center.actions.first().targetName)
    }

    // ==========================================
    // 22. DESTINATION ROUTE PRESERVATION
    // ==========================================

    @Test
    fun test22_destinationRoutePreservation() {
        val action = createAction("act_route", route = "product_detail/p_target")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertEquals("product_detail/p_target", center.actions.first().destinationRoute)
    }

    // ==========================================
    // 23. RELATED ACTION PRESERVATION
    // ==========================================

    @Test
    fun test23_relatedActionPreservation() {
        val alert = createAlert("a1", productId = "p1", title = "Low Stock")
        val action = createAction("act1", category = BusinessActionCategory.RESTOCK, targetId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action)
        )

        val unified = center.attentionItems.find { it.targetId == "p1" }
        assertNotNull(unified)
        assertEquals("act1", unified!!.relatedActionId)
    }

    // ==========================================
    // 24. RELATED ALERT PRESERVATION
    // ==========================================

    @Test
    fun test24_relatedAlertPreservation() {
        val alert = createAlert("a1", productId = "p1", title = "Low Stock")
        val action = createAction("act1", category = BusinessActionCategory.RESTOCK, targetId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action)
        )

        val unified = center.attentionItems.find { it.targetId == "p1" }
        assertNotNull(unified)
        assertEquals("a1", unified!!.relatedAlertId)
    }

    // ==========================================
    // 25. MISSING TARGET ID HANDLED SAFELY
    // ==========================================

    @Test
    fun test25_missingTargetIdHandledSafely() {
        val alert = createAlert("a_notarget", productId = null, productName = null, title = "General Alert")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert))

        assertFalse(center.attentionItems.isEmpty())
        assertNull(center.attentionItems.first().targetId)
    }

    // ==========================================
    // 26. MISSING TARGET NAME HANDLED SAFELY
    // ==========================================

    @Test
    fun test26_missingTargetNameHandledSafely() {
        val action = createAction("act_noname", targetId = "p1", targetName = null)

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertNull(center.actions.first().targetName)
        assertEquals("p1", center.actions.first().targetId)
    }

    // ==========================================
    // 27. MISSING DESTINATION HANDLED SAFELY
    // ==========================================

    @Test
    fun test27_missingDestinationHandledSafely() {
        val action = BusinessAction(
            actionId = "act_noroute",
            priority = BusinessActionPriority.LOW,
            category = BusinessActionCategory.COLLECT_PAYMENT,
            title = "Review Accounts",
            description = "Review all customer khata",
            actionLabel = "Review",
            destinationRoute = null
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertNull(center.actions.first().destinationRoute)
    }

    // ==========================================
    // 28. NaN METRIC VALUE SAFETY
    // ==========================================

    @Test
    fun test28_nanMetricValueSafety() {
        val action = BusinessAction(
            actionId = "act_nan",
            priority = BusinessActionPriority.HIGH,
            category = BusinessActionCategory.RESTOCK,
            title = "Restock Item",
            description = "Stockout",
            metricValue = Double.NaN,
            actionLabel = "Restock"
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertNull(center.actions.first().metricValue)
    }

    // ==========================================
    // 29. POSITIVE INFINITY METRIC VALUE SAFETY
    // ==========================================

    @Test
    fun test29_positiveInfinityMetricValueSafety() {
        val insight = createInsight("ins_inf", metricValue = Double.POSITIVE_INFINITY)

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(insight))

        assertNull(center.insights.first().metricValue)
    }

    // ==========================================
    // 30. NEGATIVE INFINITY METRIC VALUE SAFETY
    // ==========================================

    @Test
    fun test30_negativeInfinityMetricValueSafety() {
        val alert = BusinessAlert(
            alertId = "a_ninf",
            severity = AlertSeverity.CRITICAL,
            category = AlertCategory.INVENTORY,
            productId = "p1",
            productName = "Product 1",
            title = "Alert",
            description = "Desc",
            supportingMetric = "Days",
            metricValue = Double.NEGATIVE_INFINITY,
            unit = "days"
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert))

        assertNull(center.attentionItems.first().metricValue)
    }

    // ==========================================
    // 31. INPUT IMMUTABILITY
    // ==========================================

    @Test
    fun test31_inputImmutability() {
        val alertList = listOf(createAlert("a1"))
        val actionList = listOf(createAction("act1"))
        val insightList = listOf(createInsight("ins1"))

        UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = alertList,
            businessActions = actionList,
            crossBusinessInsights = insightList
        )

        assertEquals(1, alertList.size)
        assertEquals("a1", alertList.first().alertId)
        assertEquals(1, actionList.size)
        assertEquals("act1", actionList.first().actionId)
        assertEquals(1, insightList.size)
        assertEquals("ins1", insightList.first().id)
    }

    // ==========================================
    // 32. REPEATED EXECUTION PRODUCES STABLE IDS
    // ==========================================

    @Test
    fun test32_repeatedExecutionProducesStableIds() {
        val alert = createAlert("a1", productId = "p1")
        val action = createAction("act1", targetId = "p1")

        val run1 = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert), businessActions = listOf(action))
        val run2 = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert), businessActions = listOf(action))

        assertEquals(run1.allItems.map { it.itemId }, run2.allItems.map { it.itemId })
    }

    // ==========================================
    // 33. NO RANDOM ORDERING ACROSS REPEATED CALLS
    // ==========================================

    @Test
    fun test33_noRandomOrderingAcrossRepeatedCalls() {
        val alerts = listOf(
            createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1"),
            createAlert("a2", severity = AlertSeverity.WARNING, productId = "p2")
        )
        val actions = listOf(
            createAction("act1", priority = BusinessActionPriority.HIGH, targetId = "p3"),
            createAction("act2", priority = BusinessActionPriority.MEDIUM, targetId = "p4")
        )

        val baseline = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = alerts, businessActions = actions)

        for (i in 1..10) {
            val repeat = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = alerts, businessActions = actions)
            assertEquals(baseline.allItems.map { it.itemId }, repeat.allItems.map { it.itemId })
            assertEquals(baseline.summary.totalItems, repeat.summary.totalItems)
        }
    }

    // ==========================================
    // 34. SUMMARY COUNTS ACCURACY
    // ==========================================

    @Test
    fun test34_summaryCountsAccuracy() {
        val alert = createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1")
        val action = createAction("act1", priority = BusinessActionPriority.HIGH, targetId = "p2")
        val insight = createInsight("ins1", category = CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP, severity = CrossInsightSeverity.HIGH, entityId = "p3")
        val opp = createInsight("opp1", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE, entityId = "p4")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight, opp)
        )

        assertEquals(4, center.summary.totalItems)
        assertEquals(3, center.summary.attentionCount) // a1, act1, ins1 are all in attention
        assertEquals(1, center.summary.actionCount)
        assertEquals(1, center.summary.insightCount)
        assertEquals(1, center.summary.opportunityCount)
        assertEquals(1, center.summary.criticalCount)
        assertEquals(2, center.summary.highCount)
        assertEquals(1, center.summary.positiveCount)
    }

    // ==========================================
    // 35. SECTION COUNTS ACCURACY
    // ==========================================

    @Test
    fun test35_sectionCountsAccuracy() {
        val actions = listOf(
            createAction("act1", priority = BusinessActionPriority.HIGH),
            createAction("act2", priority = BusinessActionPriority.LOW)
        )
        val insights = listOf(
            createInsight("ins1", category = CrossInsightCategory.WORKING_CAPITAL_STRAIN, severity = CrossInsightSeverity.HIGH),
            createInsight("opp1", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE)
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessActions = actions,
            crossBusinessInsights = insights
        )

        assertEquals(2, center.actions.size)
        assertEquals(1, center.insights.size)
        assertEquals(1, center.opportunities.size)
    }

    // ==========================================
    // 36. RAW MODEL DELEGATION END-TO-END
    // ==========================================

    @Test
    fun test36_rawModelDelegationEndToEnd() {
        val p1 = Product(
            id = "prod_del",
            businessId = "biz1",
            name = "Sugar 1kg",
            sku = "SKU-SUGAR",
            barcode = "BAR-SUGAR",
            category = "General",
            purchasePrice = 30.0,
            sellingPrice = 40.0,
            currentStock = 0, // Stockout
            minStock = 10,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )
        val c1 = Customer(
            id = "cust_del",
            businessId = "biz1",
            name = "Ramesh Kumar",
            phone = "9876543210",
            email = "ramesh@example.com",
            address = "Main Market",
            totalPurchases = 5000.0,
            totalPaid = 1000.0,
            outstandingAmount = 4000.0,
            createdAt = referenceTime - (60 * oneDayMs),
            updatedAt = referenceTime
        )
        val s1 = Sale(
            id = "sale_del",
            businessId = "biz1",
            invoiceNumber = "INV-DEL-1",
            customerId = "cust_del",
            items = listOf(
                SaleItem(
                    productId = "prod_del",
                    productName = "Sugar 1kg",
                    quantity = 20,
                    unitPrice = 40.0,
                    totalPrice = 800.0
                )
            ),
            subtotal = 800.0,
            discount = 0.0,
            tax = 0.0,
            grandTotal = 800.0,
            amountPaid = 800.0,
            outstanding = 0.0,
            paymentStatus = "PAID",
            paymentMethod = "CASH",
            createdAt = referenceTime - (5 * oneDayMs),
            updatedAt = referenceTime - (5 * oneDayMs)
        )

        val center = UnifiedBusinessCommandCenterEngine.generateFromRawModels(
            products = listOf(p1),
            sales = listOf(s1),
            customers = listOf(c1)
        )

        assertNotNull(center)
        assertTrue(center.summary.totalItems > 0)
        assertFalse(center.allItems.isEmpty())
    }

    // ==========================================
    // PHASE 10A.1 PROFIT INTELLIGENCE EXTENSION TESTS
    // ==========================================

    // 37. PROFIT SNAPSHOT WITH VALID DATA
    @Test
    fun test37_profitSnapshotWithValidData() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 100000.0,
            cogs = 60000.0,
            operatingExpenses = 15000.0,
            productProfit = 40000.0
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(
            profitSnapshot = snapshot
        )

        assertNotNull(center.profitSnapshot)
        assertEquals("This Month", center.profitSnapshot!!.periodLabel)
        assertEquals(100000.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(60000.0, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(40000.0, center.profitSnapshot!!.grossProfit, 0.001)
        assertEquals(15000.0, center.profitSnapshot!!.operatingExpenses, 0.001)
        assertEquals(25000.0, center.profitSnapshot!!.netProfit, 0.001)
        assertEquals(40.0, center.profitSnapshot!!.grossMarginPercent, 0.001)
        assertEquals(25.0, center.profitSnapshot!!.netMarginPercent, 0.001)
        assertEquals(40000.0, center.profitSnapshot!!.productProfit!!, 0.001)
        assertEquals(40000.0, center.profitSnapshot!!.stockProfit!!, 0.001)
        assertEquals(ProfitDataAvailability.FULL, center.profitSnapshot!!.dataAvailability)
        assertTrue(center.profitSnapshot!!.isProfitable)
        assertFalse(center.profitSnapshot!!.isOperatingAtLoss)
        assertFalse(center.profitSnapshot!!.hasNegativeGrossMargin)
    }

    // 38. REVENUE PRESERVATION
    @Test
    fun test38_revenuePreservation() {
        val authoritativeRevenue = 154320.75
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Last 30 Days",
            revenue = authoritativeRevenue,
            cogs = 90000.0,
            operatingExpenses = 20000.0
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        assertEquals(authoritativeRevenue, center.profitSnapshot!!.revenue, 0.0001)
    }

    // 39. COGS PRESERVATION
    @Test
    fun test39_cogsPreservation() {
        val authoritativeCogs = 82450.50
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 120000.0,
            cogs = authoritativeCogs,
            operatingExpenses = 15000.0
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        assertEquals(authoritativeCogs, center.profitSnapshot!!.cogs, 0.0001)
    }

    // 40. GROSS PROFIT PRESERVATION
    @Test
    fun test40_grossProfitPreservation() {
        val authoritativeGross = 45000.0
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 100000.0,
            cogs = 55000.0,
            grossProfit = authoritativeGross
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        assertEquals(authoritativeGross, center.profitSnapshot!!.grossProfit, 0.0001)
    }

    // 41. OPERATING EXPENSE PRESERVATION
    @Test
    fun test41_operatingExpensePreservation() {
        val authoritativeExpenses = 18750.25
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 100000.0,
            cogs = 50000.0,
            operatingExpenses = authoritativeExpenses
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        assertEquals(authoritativeExpenses, center.profitSnapshot!!.operatingExpenses, 0.0001)
    }

    // 42. NET PROFIT PRESERVATION
    @Test
    fun test42_netProfitPreservation() {
        val authoritativeNet = 31249.75
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 100000.0,
            cogs = 50000.0,
            operatingExpenses = 18750.25,
            netProfit = authoritativeNet
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        assertEquals(authoritativeNet, center.profitSnapshot!!.netProfit, 0.0001)
    }

    // 43. GROSS MARGIN CALCULATION AND REUSE
    @Test
    fun test43_grossMarginCalculationAndReuse() {
        // Revenue = 200, Gross Profit = 50 -> 25.0%
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 200.0,
            cogs = 150.0
        )
        assertEquals(25.0, snapshot.grossMarginPercent, 0.001)
    }

    // 44. NET MARGIN CALCULATION AND REUSE
    @Test
    fun test44_netMarginCalculationAndReuse() {
        // Revenue = 200, Net Profit = 20 -> 10.0%
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 200.0,
            cogs = 150.0,
            operatingExpenses = 30.0
        )
        assertEquals(10.0, snapshot.netMarginPercent, 0.001)
    }

    // 45. ZERO REVENUE HANDLING
    @Test
    fun test45_zeroRevenueHandling() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Today",
            revenue = 0.0,
            cogs = 0.0,
            operatingExpenses = 500.0
        )
        assertEquals(0.0, snapshot.grossMarginPercent, 0.0)
        assertEquals(0.0, snapshot.netMarginPercent, 0.0)
        assertFalse(snapshot.grossMarginPercent.isNaN())
        assertFalse(snapshot.netMarginPercent.isNaN())
        assertFalse(snapshot.grossMarginPercent.isInfinite())
        assertFalse(snapshot.netMarginPercent.isInfinite())
        assertEquals(-500.0, snapshot.netProfit, 0.001)
        assertTrue(snapshot.isOperatingAtLoss)
    }

    // 46. ZERO COGS HANDLING
    @Test
    fun test46_zeroCogsHandling() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 1000.0,
            cogs = 0.0,
            operatingExpenses = 200.0
        )
        assertEquals(1000.0, snapshot.grossProfit, 0.001)
        assertEquals(100.0, snapshot.grossMarginPercent, 0.001)
        assertEquals(800.0, snapshot.netProfit, 0.001)
        assertEquals(80.0, snapshot.netMarginPercent, 0.001)
    }

    // 47. ZERO OPERATING EXPENSES HANDLING
    @Test
    fun test47_zeroOperatingExpensesHandling() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 5000.0,
            cogs = 3000.0,
            operatingExpenses = 0.0,
            productProfit = 2000.0
        )
        assertEquals(2000.0, snapshot.grossProfit, 0.001)
        assertEquals(2000.0, snapshot.netProfit, 0.001)
        assertEquals(ProfitDataAvailability.PARTIAL, snapshot.dataAvailability)
    }

    // 48. NEGATIVE NET PROFIT
    @Test
    fun test48_negativeNetProfit() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 10000.0,
            cogs = 8000.0,
            operatingExpenses = 5000.0
        )
        assertEquals(2000.0, snapshot.grossProfit, 0.001)
        assertEquals(-3000.0, snapshot.netProfit, 0.001)
        assertEquals(-30.0, snapshot.netMarginPercent, 0.001)
        assertTrue(snapshot.isOperatingAtLoss)
        assertFalse(snapshot.isProfitable)
        assertFalse(snapshot.hasNegativeGrossMargin)

        // Verify Command Center does NOT create unauthorized alerts or mutate existing sections
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        assertTrue(center.attentionItems.isEmpty())
        assertTrue(center.actions.isEmpty())
    }

    // 49. PRODUCT PROFITABILITY INTEGRATION
    @Test
    fun test49_productProfitabilityIntegration() {
        val p1 = Product(
            id = "prod_p1",
            businessId = "biz1",
            name = "Wheat Flour 5kg",
            sku = "SKU-WHEAT",
            barcode = "BAR-WHEAT",
            category = "Grocery",
            purchasePrice = 150.0,
            sellingPrice = 200.0,
            currentStock = 20,
            minStock = 5,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )
        val s1 = Sale(
            id = "sale_p1",
            businessId = "biz1",
            invoiceNumber = "INV-P1",
            customerId = "cust1",
            items = listOf(
                SaleItem(
                    productId = "prod_p1",
                    productName = "Wheat Flour 5kg",
                    quantity = 10,
                    unitPrice = 200.0,
                    totalPrice = 2000.0
                )
            ),
            subtotal = 2000.0,
            discount = 0.0,
            tax = 0.0,
            grandTotal = 2000.0,
            amountPaid = 2000.0,
            outstanding = 0.0,
            paymentStatus = "PAID",
            paymentMethod = "CASH",
            createdAt = referenceTime - (5 * oneDayMs),
            updatedAt = referenceTime - (5 * oneDayMs)
        )

        val prodSummary = ProductProfitabilityEngine.analyze(listOf(p1), listOf(s1))
        val profitSnapshot = CommandCenterProfitSnapshot.fromProductProfitabilitySummary(
            summary = prodSummary,
            periodLabel = "Last 30 Days",
            operatingExpenses = 200.0
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = profitSnapshot)
        assertNotNull(center.profitSnapshot)
        assertEquals(prodSummary.totalRevenue, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(prodSummary.totalCostOfGoodsSold, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(prodSummary.totalGrossProfit, center.profitSnapshot!!.grossProfit, 0.001)
        assertEquals(prodSummary.totalGrossProfit, center.profitSnapshot!!.productProfit!!, 0.001)
        assertEquals(center.profitSnapshot!!.productProfit, center.profitSnapshot!!.stockProfit)
        assertTrue(center.profitSnapshot!!.isStockProfitSupported)
    }

    // 50. PRODUCT PROFIT AGGREGATION
    @Test
    fun test50_productProfitAggregation() {
        val p1 = Product(id = "p1", businessId = "b1", name = "Item 1", sku = "SKU1", barcode = "BAR-1", category = "Cat", purchasePrice = 40.0, sellingPrice = 60.0, currentStock = 10, minStock = 2, supplierId = "s1", expiryDate = null, createdAt = referenceTime, updatedAt = referenceTime, isActive = true)
        val p2 = Product(id = "p2", businessId = "b1", name = "Item 2", sku = "SKU2", barcode = "BAR-2", category = "Cat", purchasePrice = 100.0, sellingPrice = 150.0, currentStock = 5, minStock = 1, supplierId = "s1", expiryDate = null, createdAt = referenceTime, updatedAt = referenceTime, isActive = true)
        val sale = Sale(
            id = "s1", businessId = "b1", invoiceNumber = "INV-AGG", customerId = "c1",
            items = listOf(
                SaleItem(productId = "p1", productName = "Item 1", quantity = 5, unitPrice = 60.0, totalPrice = 300.0),
                SaleItem(productId = "p2", productName = "Item 2", quantity = 2, unitPrice = 150.0, totalPrice = 300.0)
            ),
            subtotal = 600.0, discount = 0.0, tax = 0.0, grandTotal = 600.0, amountPaid = 600.0, outstanding = 0.0, paymentStatus = "PAID", paymentMethod = "CASH",
            createdAt = referenceTime - (2 * oneDayMs), updatedAt = referenceTime - (2 * oneDayMs)
        )

        val prodSummary = ProductProfitabilityEngine.analyze(listOf(p1, p2), listOf(sale), referenceTimeMs = referenceTime)
        val snapshot = CommandCenterProfitSnapshot.fromProductProfitabilitySummary(prodSummary)

        val sumIndividualProfits = prodSummary.products.sumOf { it.grossProfit }
        assertEquals(200.0, sumIndividualProfits, 0.001)
        assertEquals(sumIndividualProfits, snapshot.productProfit!!, 0.001)
    }

    // 51. UNSOLD INVENTORY IS NOT TREATED AS REALIZED PROFIT
    @Test
    fun test51_unsoldInventoryIsNotTreatedAsRealizedProfit() {
        val unsoldProduct = Product(
            id = "unsold_1",
            businessId = "biz1",
            name = "Luxury Watch",
            sku = "SKU-WATCH",
            barcode = "BAR-WATCH",
            category = "Accessories",
            purchasePrice = 5000.0,
            sellingPrice = 10000.0,
            currentStock = 50,
            minStock = 5,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )

        val prodSummary = ProductProfitabilityEngine.analyze(listOf(unsoldProduct), emptyList())
        val snapshot = CommandCenterProfitSnapshot.fromProductProfitabilitySummary(prodSummary)

        assertEquals(0.0, snapshot.revenue, 0.0)
        assertEquals(0.0, snapshot.cogs, 0.0)
        assertEquals(0.0, snapshot.grossProfit, 0.0)
        assertEquals(0.0, snapshot.productProfit ?: 0.0, 0.0)
    }

    // 52. INVENTORY VALUE IS NOT TREATED AS PROFIT
    @Test
    fun test52_inventoryValueIsNotTreatedAsProfit() {
        val productWithStock = Product(
            id = "stock_item",
            businessId = "biz1",
            name = "Cooking Oil 1L",
            sku = "SKU-OIL",
            barcode = "BAR-OIL",
            category = "Grocery",
            purchasePrice = 120.0,
            sellingPrice = 150.0,
            currentStock = 100,
            minStock = 10,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )

        val center = UnifiedBusinessCommandCenterEngine.generateFromRawModels(
            products = listOf(productWithStock),
            sales = emptyList()
        )

        assertEquals(12000.0, center.summary.workingCapitalLocked, 0.001)
        assertNotNull(center.profitSnapshot)
        assertEquals(0.0, center.profitSnapshot!!.grossProfit, 0.0)
        assertEquals(0.0, center.profitSnapshot!!.netProfit, 0.0)
    }

    // 53. SAME PERIOD CONSISTENCY
    @Test
    fun test53_samePeriodConsistency() {
        val snapshotMonth = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 50000.0,
            cogs = 30000.0,
            operatingExpenses = 8000.0
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshotMonth)
        assertEquals("This Month", center.profitSnapshot!!.periodLabel)
        assertEquals(50000.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(30000.0, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(20000.0, center.profitSnapshot!!.grossProfit, 0.001)
        assertEquals(8000.0, center.profitSnapshot!!.operatingExpenses, 0.001)
        assertEquals(12000.0, center.profitSnapshot!!.netProfit, 0.001)
    }

    // 54. NO DATA STATE
    @Test
    fun test54_noDataState() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Today",
            revenue = 0.0,
            cogs = 0.0,
            operatingExpenses = 0.0,
            productProfit = null
        )
        assertEquals(ProfitDataAvailability.NO_DATA, snapshot.dataAvailability)
        assertEquals(0.0, snapshot.revenue, 0.0)
        assertEquals(0.0, snapshot.grossProfit, 0.0)
        assertEquals(0.0, snapshot.grossMarginPercent, 0.0)
        assertNull(snapshot.productProfit)
        assertFalse(snapshot.isStockProfitSupported)
    }

    // 55. NAN SAFETY
    @Test
    fun test55_nanSafety() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Custom",
            revenue = Double.NaN,
            cogs = Double.NaN,
            operatingExpenses = Double.NaN,
            grossProfit = Double.NaN,
            netProfit = Double.NaN,
            productProfit = Double.NaN
        )
        assertFalse(snapshot.revenue.isNaN())
        assertFalse(snapshot.cogs.isNaN())
        assertFalse(snapshot.grossProfit.isNaN())
        assertFalse(snapshot.operatingExpenses.isNaN())
        assertFalse(snapshot.netProfit.isNaN())
        assertFalse(snapshot.grossMarginPercent.isNaN())
        assertFalse(snapshot.netMarginPercent.isNaN())
        assertEquals(0.0, snapshot.revenue, 0.0)
        assertEquals(0.0, snapshot.cogs, 0.0)
        assertEquals(0.0, snapshot.grossProfit, 0.0)
        assertEquals(0.0, snapshot.netProfit, 0.0)
        assertNull(snapshot.productProfit)
    }

    // 56. INFINITY SAFETY
    @Test
    fun test56_infinitySafety() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Custom",
            revenue = Double.POSITIVE_INFINITY,
            cogs = Double.NEGATIVE_INFINITY,
            operatingExpenses = Double.POSITIVE_INFINITY,
            grossProfit = Double.POSITIVE_INFINITY,
            netProfit = Double.NEGATIVE_INFINITY,
            productProfit = Double.POSITIVE_INFINITY
        )
        assertFalse(snapshot.revenue.isInfinite())
        assertFalse(snapshot.cogs.isInfinite())
        assertFalse(snapshot.grossProfit.isInfinite())
        assertFalse(snapshot.operatingExpenses.isInfinite())
        assertFalse(snapshot.netProfit.isInfinite())
        assertFalse(snapshot.grossMarginPercent.isInfinite())
        assertFalse(snapshot.netMarginPercent.isInfinite())
        assertEquals(0.0, snapshot.revenue, 0.0)
        assertEquals(0.0, snapshot.cogs, 0.0)
        assertNull(snapshot.productProfit)
    }

    // 57. INPUT IMMUTABILITY
    @Test
    fun test57_inputImmutability() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 40000.0,
            cogs = 25000.0,
            operatingExpenses = 5000.0
        )
        val center1 = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)
        val center2 = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)

        assertEquals(center1.profitSnapshot, center2.profitSnapshot)
        assertEquals(snapshot, center1.profitSnapshot)
    }

    // 58. EXISTING SECTIONS REMAIN UNCHANGED
    @Test
    fun test58_existingSectionsRemainUnchanged() {
        val alert = createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1")
        val action = createAction("act1", priority = BusinessActionPriority.HIGH, targetId = "p2")
        val insight = createInsight("ins1", category = CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP, severity = CrossInsightSeverity.HIGH)
        val opp = createInsight("opp1", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE)

        val centerWithoutProfit = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight, opp)
        )

        val profitSnapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 10000.0,
            cogs = 6000.0,
            operatingExpenses = 2000.0
        )

        val centerWithProfit = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight, opp),
            profitSnapshot = profitSnapshot
        )

        assertEquals(centerWithoutProfit.attentionItems.size, centerWithProfit.attentionItems.size)
        assertEquals(centerWithoutProfit.actions.size, centerWithProfit.actions.size)
        assertEquals(centerWithoutProfit.insights.size, centerWithProfit.insights.size)
        assertEquals(centerWithoutProfit.opportunities.size, centerWithProfit.opportunities.size)
        assertEquals(centerWithoutProfit.allItems.size, centerWithProfit.allItems.size)
        assertEquals(centerWithoutProfit.summary.criticalCount, centerWithProfit.summary.criticalCount)
        assertEquals(centerWithoutProfit.summary.highCount, centerWithProfit.summary.highCount)
        assertEquals(centerWithoutProfit.summary.positiveCount, centerWithProfit.summary.positiveCount)
        assertNotNull(centerWithProfit.profitSnapshot)
        assertNull(centerWithoutProfit.profitSnapshot)
    }

    // 59. EXISTING DEDUPLICATION AND PRIORITIZATION PRESERVED
    @Test
    fun test59_existingDeduplicationAndPrioritizationPreserved() {
        val alert = createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1", title = "Stockout Alert")
        val action = createAction("act1", priority = BusinessActionPriority.CRITICAL, targetId = "p1", title = "Restock Item")

        val profitSnapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "This Month",
            revenue = 50000.0,
            cogs = 30000.0,
            operatingExpenses = 5000.0
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            profitSnapshot = profitSnapshot
        )

        assertEquals(1, center.attentionItems.size)
        assertEquals("Restock Item", center.attentionItems.first().title)
        assertTrue(center.attentionItems.first().contributingSources.contains(CommandCenterSourceType.BUSINESS_ALERT))
        assertTrue(center.attentionItems.first().contributingSources.contains(CommandCenterSourceType.BUSINESS_ACTION))
    }

    // 60. SOURCE OF TRUTH PNL AGREEMENT
    @Test
    fun test60_sourceOfTruthPnlAgreement() {
        val authoritativePnl = ProfitPeriodSnapshot(
            periodLabel = "Last Month",
            revenue = 85000.0,
            cogs = 51000.0,
            operatingExpenses = 12000.0,
            grossProfit = 34000.0,
            netProfit = 22000.0
        )

        val snapshot = CommandCenterProfitSnapshot.fromProfitPeriodSnapshot(authoritativePnl)
        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)

        assertEquals(authoritativePnl.revenue, center.profitSnapshot!!.revenue, 0.0001)
        assertEquals(authoritativePnl.cogs, center.profitSnapshot!!.cogs, 0.0001)
        assertEquals(authoritativePnl.grossProfit, center.profitSnapshot!!.grossProfit, 0.0001)
        assertEquals(authoritativePnl.operatingExpenses, center.profitSnapshot!!.operatingExpenses, 0.0001)
        assertEquals(authoritativePnl.netProfit, center.profitSnapshot!!.netProfit, 0.0001)
        assertEquals(authoritativePnl.periodLabel, center.profitSnapshot!!.periodLabel)
    }

    // 61. RAW MODEL DELEGATION WITH PROFIT SNAPSHOT
    @Test
    fun test61_rawModelDelegationWithProfitSnapshot() {
        val p1 = Product(
            id = "p_raw",
            businessId = "biz1",
            name = "Tea Powder 500g",
            sku = "SKU-TEA",
            barcode = "BAR-TEA",
            category = "Beverages",
            purchasePrice = 80.0,
            sellingPrice = 120.0,
            currentStock = 15,
            minStock = 5,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )
        val s1 = Sale(
            id = "s_raw",
            businessId = "biz1",
            invoiceNumber = "INV-TEA-1",
            customerId = "c1",
            items = listOf(
                SaleItem(
                    productId = "p_raw",
                    productName = "Tea Powder 500g",
                    quantity = 5,
                    unitPrice = 120.0,
                    totalPrice = 600.0
                )
            ),
            subtotal = 600.0,
            discount = 0.0,
            tax = 0.0,
            grandTotal = 600.0,
            amountPaid = 600.0,
            outstanding = 0.0,
            paymentStatus = "PAID",
            paymentMethod = "CASH",
            createdAt = referenceTime - (2 * oneDayMs),
            updatedAt = referenceTime - (2 * oneDayMs)
        )

        val center = UnifiedBusinessCommandCenterEngine.generateFromRawModels(
            products = listOf(p1),
            sales = listOf(s1),
            currentTime = referenceTime
        )

        assertNotNull(center.profitSnapshot)
        assertEquals(600.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(400.0, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(200.0, center.profitSnapshot!!.grossProfit, 0.001)
        assertEquals(200.0, center.profitSnapshot!!.productProfit!!, 0.001)
    }

    // ==========================================
    // PHASE 10B — COMPREHENSIVE TESTS (62 - 95)
    // ==========================================

    // 62. EMPTY INPUT COMPREHENSIVE
    @Test
    fun test62_emptyInputComprehensive() {
        val center = UnifiedBusinessCommandCenterEngine.generate()
        assertTrue(center.attentionItems.isEmpty())
        assertTrue(center.actions.isEmpty())
        assertTrue(center.insights.isEmpty())
        assertTrue(center.opportunities.isEmpty())
        assertTrue(center.allItems.isEmpty())
        assertEquals(0, center.summary.totalItems)
        assertEquals(0, center.summary.attentionCount)
        assertEquals(0, center.summary.actionCount)
        assertEquals(0, center.summary.insightCount)
        assertEquals(0, center.summary.opportunityCount)
        assertEquals(0, center.summary.criticalCount)
        assertEquals(0, center.summary.highCount)
        assertEquals(0, center.summary.mediumCount)
        assertEquals(0, center.summary.lowCount)
        assertEquals(0, center.summary.positiveCount)
        assertEquals(0.0, center.summary.workingCapitalLocked, 0.001)
        assertNull(center.workingCapitalLockedTotal)
        assertNull(center.profitExplanationSummary)
        assertNull(center.profitSnapshot)
    }

    // 63. SINGLE ALERT PRODUCES ATTENTION ITEM WITH TRACEABILITY
    @Test
    fun test63_singleAlertProducesAttentionItemWithTraceability() {
        val alert = createAlert("a_crit_1", severity = AlertSeverity.CRITICAL, productId = "prod_x", productName = "X Brand")
        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert))

        assertEquals(1, center.attentionItems.size)
        val item = center.attentionItems.first()
        assertEquals(CommandCenterPriority.CRITICAL, item.priority)
        assertEquals(CommandCenterSection.NEEDS_ATTENTION, item.section)
        assertEquals(CommandCenterSourceType.BUSINESS_ALERT, item.sourceType)
        assertEquals("prod_x", item.targetId)
        assertEquals("X Brand", item.targetName)
        assertEquals(CommandCenterTargetType.PRODUCT, item.targetType)
        assertTrue(item.contributingSources.contains(CommandCenterSourceType.BUSINESS_ALERT))
        assertEquals(1, center.summary.attentionCount)
        assertEquals(1, center.summary.criticalCount)
    }

    // 64. SINGLE ACTION PRODUCES ACTION ITEM WITH ROUTE PRESERVATION
    @Test
    fun test64_singleActionProducesActionItemWithRoutePreservation() {
        val action = createAction(
            id = "act_restock_1",
            priority = BusinessActionPriority.HIGH,
            category = BusinessActionCategory.RESTOCK,
            targetId = "prod_y",
            targetName = "Y Brand",
            actionLabel = "Reorder Now",
            route = "product_detail/prod_y"
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertEquals(1, center.actions.size)
        val item = center.actions.first()
        assertEquals(CommandCenterPriority.HIGH, item.priority)
        assertEquals(CommandCenterSection.ACTIONS, item.section)
        assertEquals(CommandCenterSourceType.BUSINESS_ACTION, item.sourceType)
        assertEquals("product_detail/prod_y", item.destinationRoute)
        assertEquals("Reorder Now", item.actionLabel)
        assertEquals("prod_y", item.targetId)
        assertTrue(item.contributingSources.contains(CommandCenterSourceType.BUSINESS_ACTION))
    }

    // 65. SINGLE INSIGHT PRODUCES BUSINESS INSIGHT ITEM
    @Test
    fun test65_singleInsightProducesBusinessInsightItem() {
        val insight = createInsight(
            id = "ins_account_1",
            category = CrossInsightCategory.KEY_ACCOUNT_EXPOSURE,
            severity = CrossInsightSeverity.HIGH,
            entityId = "cust_vip",
            entityName = "VIP Client",
            entityType = CrossInsightEntityType.CUSTOMER
        )
        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(insight))

        assertEquals(1, center.insights.size)
        assertTrue(center.opportunities.isEmpty())
        val item = center.insights.first()
        assertEquals(CommandCenterPriority.HIGH, item.priority)
        assertEquals(CommandCenterSection.BUSINESS_INSIGHTS, item.section)
        assertEquals(CommandCenterTargetType.CUSTOMER, item.targetType)
        assertEquals("cust_vip", item.targetId)
        assertTrue(item.contributingSources.contains(CommandCenterSourceType.CROSS_BUSINESS_INSIGHT))
    }

    // 66. SINGLE PROFIT EXPANSION PRODUCES OPPORTUNITY ITEM
    @Test
    fun test66_singleProfitExpansionProducesOpportunityItem() {
        val prev = ProfitPeriodSnapshot("Q2 2024", 80000.0, 50000.0, 15000.0, 30000.0, 15000.0)
        val curr = ProfitPeriodSnapshot("Q3 2024", 110000.0, 60000.0, 20000.0, 50000.0, 30000.0)
        val profitExp = ProfitExplainabilityEngine.explain(prev, curr)
        val center = UnifiedBusinessCommandCenterEngine.generate(profitExplanation = profitExp)

        assertEquals(1, center.opportunities.size)
        val opp = center.opportunities.first()
        assertEquals(CommandCenterPriority.POSITIVE, opp.priority)
        assertEquals(CommandCenterSection.OPPORTUNITIES, opp.section)
        assertEquals(CommandCenterSourceType.PROFIT_EXPLANATION, opp.sourceType)
        assertEquals(15000.0, opp.metricValue!!, 0.001)
        assertEquals("profit_explainability", opp.destinationRoute)
    }

    // 67. ALL FOUR SECTIONS POPULATED SIMULTANEOUSLY
    @Test
    fun test67_allFourSectionsPopulatedSimultaneously() {
        val alert = createAlert("a1", severity = AlertSeverity.CRITICAL, productId = "p1")
        val action = createAction("act1", priority = BusinessActionPriority.MEDIUM, targetId = "p2")
        val insight = createInsight("ins1", category = CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP, severity = CrossInsightSeverity.MEDIUM, entityId = "p3")
        val opp = createInsight("opp1", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE, entityId = "p4")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight, opp)
        )

        assertFalse(center.attentionItems.isEmpty())
        assertFalse(center.actions.isEmpty())
        assertFalse(center.insights.isEmpty())
        assertFalse(center.opportunities.isEmpty())
        assertEquals(center.attentionItems.size, center.summary.attentionCount)
        assertEquals(center.actions.size, center.summary.actionCount)
        assertEquals(center.insights.size, center.summary.insightCount)
        assertEquals(center.opportunities.size, center.summary.opportunityCount)
    }

    // 68. STRICT PRIORITY ORDERING ACROSS MIXED SOURCES
    @Test
    fun test68_strictPriorityOrderingAcrossMixedSources() {
        val alertCrit = createAlert("a_crit", severity = AlertSeverity.CRITICAL, productId = "p_crit")
        val actionHigh = createAction("act_high", priority = BusinessActionPriority.HIGH, targetId = "p_high")
        val actionMed = createAction("act_med", priority = BusinessActionPriority.MEDIUM, targetId = "p_med")
        val alertLow = createAlert("a_low", severity = AlertSeverity.INFO, productId = "p_low")
        val oppPos = createInsight("opp_pos", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE, entityId = "p_pos")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alertLow, alertCrit),
            businessActions = listOf(actionMed, actionHigh),
            crossBusinessInsights = listOf(oppPos)
        )

        val rank = mapOf(
            CommandCenterPriority.CRITICAL to 0,
            CommandCenterPriority.HIGH to 1,
            CommandCenterPriority.MEDIUM to 2,
            CommandCenterPriority.LOW to 3,
            CommandCenterPriority.POSITIVE to 4
        )

        for (i in 0 until center.allItems.size - 1) {
            val currRank = rank[center.allItems[i].priority]!!
            val nextRank = rank[center.allItems[i + 1].priority]!!
            assertTrue("Ordering failed between ${center.allItems[i]} and ${center.allItems[i+1]}", currRank <= nextRank)
        }
    }

    // 69. STABLE DETERMINISTIC ORDERING TIE BREAKING
    @Test
    fun test69_stableDeterministicOrderingTieBreaking() {
        val actA = createAction("act_a", priority = BusinessActionPriority.MEDIUM, targetId = "p_alpha", title = "Alpha")
        val actB = createAction("act_b", priority = BusinessActionPriority.MEDIUM, targetId = "p_beta", title = "Beta")

        val center1 = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(actB, actA))
        val center2 = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(actA, actB))

        val ids1 = center1.allItems.map { it.itemId }
        val ids2 = center2.allItems.map { it.itemId }
        assertEquals(ids1, ids2)
    }

    // 70. DEDUPLICATION OF IDENTICAL ALERT IDS
    @Test
    fun test70_deduplicationIdenticalAlertIds() {
        val a1 = createAlert("alert_duplicate", severity = AlertSeverity.WARNING, productId = "p1")
        val a2 = createAlert("alert_duplicate", severity = AlertSeverity.WARNING, productId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(a1, a2))
        assertEquals(1, center.attentionItems.size)
        assertEquals(1, center.summary.totalItems)
    }

    // 71. DEDUPLICATION OF IDENTICAL ACTION IDS
    @Test
    fun test71_deduplicationIdenticalActionIds() {
        val act1 = createAction("action_duplicate", priority = BusinessActionPriority.LOW, targetId = "p1")
        val act2 = createAction("action_duplicate", priority = BusinessActionPriority.LOW, targetId = "p1")

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(act1, act2))
        assertEquals(1, center.actions.size)
        assertEquals(1, center.summary.actionCount)
    }

    // 72. DEDUPLICATION OF IDENTICAL INSIGHT IDS
    @Test
    fun test72_deduplicationIdenticalInsightIds() {
        val ins1 = createInsight("insight_duplicate", category = CrossInsightCategory.DORMANT_DEBT_RISK, severity = CrossInsightSeverity.MEDIUM)
        val ins2 = createInsight("insight_duplicate", category = CrossInsightCategory.DORMANT_DEBT_RISK, severity = CrossInsightSeverity.MEDIUM)

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(ins1, ins2))
        assertEquals(1, center.insights.size)
        assertEquals(1, center.summary.insightCount)
    }

    // 73. DEDUPLICATION OF IDENTICAL COLLECTION CUSTOMER IDS
    @Test
    fun test73_deduplicationIdenticalCollectionIds() {
        val col1 = CollectionPriorityResult(
            customerId = "cust_1",
            customerName = "Customer 1",
            phone = "9999999999",
            outstandingAmount = 10000.0,
            priority = CollectionPriorityLevel.HIGH,
            reasons = listOf("Overdue balance"),
            daysSinceLastPurchase = 30L,
            activityStatus = CustomerActivityStatus.ACTIVE,
            segment = CustomerSegment.REGULAR,
            lifetimePurchases = 20000.0,
            lifetimeOrders = 5,
            totalPaid = 10000.0
        )
        val col2 = CollectionPriorityResult(
            customerId = "cust_1",
            customerName = "Customer 1",
            phone = "9999999999",
            outstandingAmount = 10000.0,
            priority = CollectionPriorityLevel.HIGH,
            reasons = listOf("Overdue balance"),
            daysSinceLastPurchase = 30L,
            activityStatus = CustomerActivityStatus.ACTIVE,
            segment = CustomerSegment.REGULAR,
            lifetimePurchases = 20000.0,
            lifetimeOrders = 5,
            totalPaid = 10000.0
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(collectionResults = listOf(col1, col2))
        assertEquals(1, center.attentionItems.size)
    }

    // 74. SAME TARGET AND SAME ISSUE DOMAIN CONSOLIDATES
    @Test
    fun test74_sameTargetAndSameIssueDomainConsolidates() {
        val alert = createAlert("a_stock", severity = AlertSeverity.CRITICAL, productId = "p_consolidate", title = "Stockout Alert")
        val action = createAction("act_restock", priority = BusinessActionPriority.CRITICAL, category = BusinessActionCategory.RESTOCK, targetId = "p_consolidate", title = "Restock Stockout")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action)
        )

        assertEquals(1, center.attentionItems.size)
        val consolidated = center.attentionItems.first()
        assertTrue(consolidated.contributingSources.contains(CommandCenterSourceType.BUSINESS_ALERT))
        assertTrue(consolidated.contributingSources.contains(CommandCenterSourceType.BUSINESS_ACTION))
        assertEquals("p_consolidate", consolidated.targetId)
    }

    // 75. DIFFERENT ISSUE DOMAINS ON SAME TARGET REMAIN SEPARATE
    @Test
    fun test75_differentIssueDomainsOnSameTargetRemainSeparate() {
        val alertStock = createAlert("a_stock", severity = AlertSeverity.CRITICAL, category = AlertCategory.INVENTORY, productId = "prod_dual", title = "Stockout Alert")
        val actionPrice = createAction("act_price", priority = BusinessActionPriority.HIGH, category = BusinessActionCategory.REVIEW_PRICING, targetId = "prod_dual", title = "Review Pricing")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alertStock),
            businessActions = listOf(actionPrice)
        )

        assertTrue(center.allItems.size >= 2)
        assertTrue(center.allItems.any { it.sourceType == CommandCenterSourceType.BUSINESS_ALERT && it.targetId == "prod_dual" })
        assertTrue(center.allItems.any { it.sourceType == CommandCenterSourceType.BUSINESS_ACTION && it.targetId == "prod_dual" })
    }

    // 76. MULTI-SIGNAL CONVERGENCE INSIGHT PRESERVES SOURCE TYPE
    @Test
    fun test76_multiSignalConvergenceInsightPreserved() {
        val multiInsight = createInsight(
            id = "multi_1",
            category = CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE,
            severity = CrossInsightSeverity.CRITICAL,
            entityId = "p_multi",
            title = "Multi-Signal Focus: Prod Multi"
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(multiInsight))
        val found = center.allItems.find { it.targetId == "p_multi" }
        assertNotNull(found)
        assertEquals(CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE, found!!.sourceType)
    }

    // 77. SOURCE TRACEABILITY FULL
    @Test
    fun test77_sourceTraceabilityFull() {
        val alert = createAlert("a_trace", severity = AlertSeverity.CRITICAL, productId = "p_trace", title = "Stockout Alert")
        val action = createAction("act_trace", priority = BusinessActionPriority.CRITICAL, targetId = "p_trace", title = "Restock Item")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action)
        )

        val item = center.attentionItems.first()
        assertEquals(2, item.contributingSources.size)
        assertTrue(item.contributingSources.contains(CommandCenterSourceType.BUSINESS_ALERT))
        assertTrue(item.contributingSources.contains(CommandCenterSourceType.BUSINESS_ACTION))
    }

    // 78. STABLE DETERMINISTIC IDS
    @Test
    fun test78_stableDeterministicIds() {
        val action = createAction("my_action_123", priority = BusinessActionPriority.MEDIUM)
        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        val item = center.actions.first()
        assertEquals("action_my_action_123", item.itemId)
        assertFalse(item.itemId.contains("random"))
    }

    // 79. MISSING PRODUCT ID HANDLED SAFELY
    @Test
    fun test79_missingProductIdHandledSafely() {
        val alert = createAlert("a_null_prod", severity = AlertSeverity.WARNING, productId = null, productName = "All Products", title = "General Alert")
        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert))

        assertEquals(1, center.attentionItems.size)
        val item = center.attentionItems.first()
        assertNull(item.targetId)
        assertEquals(CommandCenterTargetType.BUSINESS_WIDE, item.targetType)
    }

    // 80. MISSING CUSTOMER ID HANDLED SAFELY
    @Test
    fun test80_missingCustomerIdHandledSafely() {
        val action = createAction("act_null_cust", category = BusinessActionCategory.COLLECT_PAYMENT, targetId = null, targetName = null)
        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertEquals(1, center.actions.size)
        val item = center.actions.first()
        assertNull(item.targetId)
        assertNull(item.targetName)
        assertEquals(CommandCenterTargetType.CUSTOMER, item.targetType)
    }

    // 81. NULL AND EXTREME OPTIONAL DATA HANDLED SAFELY
    @Test
    fun test81_nullAndExtremeOptionalDataHandledSafely() {
        val action = BusinessAction(
            actionId = "extreme_act",
            priority = BusinessActionPriority.LOW,
            category = BusinessActionCategory.RESTOCK,
            title = "",
            description = "",
            targetId = null,
            targetName = null,
            supportingMetric = null,
            metricValue = null,
            metricUnit = null,
            actionLabel = "",
            destinationRoute = null
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))
        assertEquals(1, center.actions.size)
        assertNull(center.actions.first().metricValue)
        assertNull(center.actions.first().destinationRoute)
    }

    // 82. ZERO REVENUE SAFETY
    @Test
    fun test82_zeroRevenueSafety() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Empty Month",
            revenue = 0.0,
            cogs = 0.0,
            operatingExpenses = 0.0
        )

        assertEquals(0.0, snapshot.grossMarginPercent, 0.001)
        assertEquals(0.0, snapshot.netMarginPercent, 0.001)
        assertEquals(ProfitDataAvailability.NO_DATA, snapshot.dataAvailability)
        assertFalse(snapshot.isProfitable)
        assertFalse(snapshot.isOperatingAtLoss)
    }

    // 83. NEGATIVE PROFIT OPERATING AT LOSS
    @Test
    fun test83_negativeProfitOperatingAtLoss() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Loss Month",
            revenue = 10000.0,
            cogs = 7000.0,
            operatingExpenses = 5000.0
        )

        assertEquals(-2000.0, snapshot.netProfit, 0.001)
        assertTrue(snapshot.isOperatingAtLoss)
        assertFalse(snapshot.isProfitable)
    }

    // 84. NEGATIVE GROSS MARGIN
    @Test
    fun test84_negativeGrossMargin() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Underpriced Month",
            revenue = 5000.0,
            cogs = 8000.0,
            operatingExpenses = 1000.0
        )

        assertEquals(-3000.0, snapshot.grossProfit, 0.001)
        assertTrue(snapshot.hasNegativeGrossMargin)
        assertTrue(snapshot.grossMarginPercent < 0.0)
    }

    // 85. NAN METRIC VALUE IN ACTION SAFELY SANITIZED
    @Test
    fun test85_nanMetricValueInActionSafelySanitized() {
        val action = createAction("act_nan", priority = BusinessActionPriority.HIGH).copy(metricValue = Double.NaN)
        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        assertNull(center.actions.first().metricValue)
    }

    // 86. INFINITY METRIC VALUE IN INSIGHT SAFELY SANITIZED
    @Test
    fun test86_infinityMetricValueInInsightSafelySanitized() {
        val insight = createInsight("ins_inf", metricValue = Double.POSITIVE_INFINITY)
        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(insight))

        assertNull(center.insights.first().metricValue)
    }

    // 87. PROFIT SNAPSHOT PRESERVED ALONGSIDE INTELLIGENCE
    @Test
    fun test87_profitSnapshotPreservedAlongSideIntelligence() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Current",
            revenue = 50000.0,
            cogs = 30000.0,
            operatingExpenses = 5000.0
        )
        val alert = createAlert("a1")
        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            profitSnapshot = snapshot
        )

        assertNotNull(center.profitSnapshot)
        assertEquals(50000.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(15000.0, center.profitSnapshot!!.netProfit, 0.001)
    }

    // 88. AUTHORITATIVE PNL AGREEMENT
    @Test
    fun test88_authoritativePnlAgreement() {
        val pnlSnapshot = ProfitPeriodSnapshot(
            periodLabel = "Quarter 1",
            revenue = 120000.0,
            cogs = 72000.0,
            operatingExpenses = 18000.0,
            grossProfit = 48000.0,
            netProfit = 30000.0
        )
        val snapshot = CommandCenterProfitSnapshot.fromProfitPeriodSnapshot(pnlSnapshot)

        assertEquals("Quarter 1", snapshot.periodLabel)
        assertEquals(120000.0, snapshot.revenue, 0.001)
        assertEquals(72000.0, snapshot.cogs, 0.001)
        assertEquals(48000.0, snapshot.grossProfit, 0.001)
        assertEquals(18000.0, snapshot.operatingExpenses, 0.001)
        assertEquals(30000.0, snapshot.netProfit, 0.001)
        assertEquals(40.0, snapshot.grossMarginPercent, 0.001)
        assertEquals(25.0, snapshot.netMarginPercent, 0.001)
    }

    // 89. EXISTING ALERT PRESERVATION
    @Test
    fun test89_existingAlertPreservation() {
        val alert = createAlert("a_check", severity = AlertSeverity.CRITICAL, title = "Critical Alert", desc = "Alert Description")
        val center = UnifiedBusinessCommandCenterEngine.generate(businessAlerts = listOf(alert))

        val item = center.attentionItems.first()
        assertEquals("Critical Alert", item.title)
        assertEquals("Alert Description", item.description)
        assertEquals(CommandCenterPriority.CRITICAL, item.priority)
    }

    // 90. EXISTING ACTION PRESERVATION
    @Test
    fun test90_existingActionPreservation() {
        val action = createAction("act_check", priority = BusinessActionPriority.MEDIUM, title = "Action Title", desc = "Action Description", actionLabel = "Do It", route = "custom_route")
        val center = UnifiedBusinessCommandCenterEngine.generate(businessActions = listOf(action))

        val item = center.actions.first()
        assertEquals("Action Title", item.title)
        assertEquals("Action Description", item.description)
        assertEquals("Do It", item.actionLabel)
        assertEquals("custom_route", item.destinationRoute)
    }

    // 91. EXISTING INSIGHT PRESERVATION
    @Test
    fun test91_existingInsightPreservation() {
        val insight = createInsight("ins_check", title = "Insight Title", summary = "Insight Summary")
        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(insight))

        val item = center.insights.first()
        assertEquals("Insight Title", item.title)
        assertEquals("Insight Summary", item.description)
    }

    // 92. EXISTING OPPORTUNITY PRESERVATION
    @Test
    fun test92_existingOpportunityPreservation() {
        val opp = createInsight("opp_star", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE, title = "Top Seller")
        val center = UnifiedBusinessCommandCenterEngine.generate(crossBusinessInsights = listOf(opp))

        assertEquals(1, center.opportunities.size)
        assertEquals("Top Seller", center.opportunities.first().title)
        assertEquals(CommandCenterSection.OPPORTUNITIES, center.opportunities.first().section)
    }

    // 93. RAW MODEL DELEGATION COMPREHENSIVE
    @Test
    fun test93_rawModelDelegationComprehensive() {
        val prod = Product(
            id = "p_raw",
            businessId = "b1",
            name = "Raw Prod",
            sku = "SKU-R",
            barcode = "BAR-R",
            category = "General",
            purchasePrice = 50.0,
            sellingPrice = 100.0,
            currentStock = 2,
            minStock = 10,
            supplierId = "sup1",
            expiryDate = null,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )
        val sale = Sale(
            id = "s_raw", businessId = "b1", invoiceNumber = "INV-R", customerId = "c_raw",
            items = listOf(SaleItem(productId = "p_raw", productName = "Raw Prod", quantity = 3, unitPrice = 100.0, totalPrice = 300.0)),
            subtotal = 300.0, discount = 0.0, tax = 0.0, grandTotal = 300.0, amountPaid = 300.0, outstanding = 0.0, paymentStatus = "PAID", paymentMethod = "CASH",
            createdAt = referenceTime - (2 * oneDayMs), updatedAt = referenceTime - (2 * oneDayMs)
        )
        val cust = Customer(
            id = "c_raw",
            businessId = "b1",
            name = "Raw Cust",
            phone = "9999999999",
            email = "cust@example.com",
            address = "Main Street",
            totalPurchases = 300.0,
            totalPaid = 300.0,
            outstandingAmount = 0.0,
            createdAt = referenceTime - (30 * oneDayMs),
            updatedAt = referenceTime,
            isActive = true
        )

        val center = UnifiedBusinessCommandCenterEngine.generateFromRawModels(
            products = listOf(prod),
            sales = listOf(sale),
            customers = listOf(cust),
            currentTime = referenceTime
        )

        assertNotNull(center)
        assertNotNull(center.profitSnapshot)
        assertEquals(300.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(150.0, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(150.0, center.profitSnapshot!!.grossProfit, 0.001)
        assertTrue(center.summary.totalItems > 0)
    }

    // 94. DETERMINISTIC REPEATED GENERATION
    @Test
    fun test94_deterministicRepeatedGeneration() {
        val alert = createAlert("a1")
        val action = createAction("act1")
        val insight = createInsight("ins1")

        val run1 = UnifiedBusinessCommandCenterEngine.generate(listOf(alert), listOf(action), listOf(insight))
        val run2 = UnifiedBusinessCommandCenterEngine.generate(listOf(alert), listOf(action), listOf(insight))

        assertEquals(run1.attentionItems.map { it.itemId }, run2.attentionItems.map { it.itemId })
        assertEquals(run1.actions.map { it.itemId }, run2.actions.map { it.itemId })
        assertEquals(run1.insights.map { it.itemId }, run2.insights.map { it.itemId })
        assertEquals(run1.summary, run2.summary)
    }

    // 95. INPUT IMMUTABILITY
    @Test
    fun test95_inputImmutability() {
        val alerts = mutableListOf(createAlert("a1"), createAlert("a2"))
        val actions = mutableListOf(createAction("act1"), createAction("act2"))
        val originalAlerts = alerts.toList()
        val originalActions = actions.toList()

        UnifiedBusinessCommandCenterEngine.generate(businessAlerts = alerts, businessActions = actions)

        assertEquals(originalAlerts, alerts)
        assertEquals(originalActions, actions)
    }

    // ==========================================
    // PHASE 11: FINAL VALIDATION & INTEGRATION TESTS (96 - 105)
    // ==========================================

    // 96. REPEATED GENERATION FULL PARITY
    @Test
    fun test96_phase11_repeatedGenerationFullParity() {
        val alert = createAlert("a1", severity = AlertSeverity.CRITICAL)
        val action = createAction("act1", priority = BusinessActionPriority.HIGH)
        val insight = createInsight("ins1", severity = CrossInsightSeverity.HIGH)
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "March 2026",
            revenue = 100000.0,
            cogs = 60000.0,
            grossProfit = 40000.0,
            operatingExpenses = 15000.0,
            netProfit = 25000.0
        )

        val run1 = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight),
            profitSnapshot = snapshot
        )
        val run2 = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight),
            profitSnapshot = snapshot
        )

        assertEquals(run1.attentionItems.map { it.itemId }, run2.attentionItems.map { it.itemId })
        assertEquals(run1.actions.map { it.itemId }, run2.actions.map { it.itemId })
        assertEquals(run1.insights.map { it.itemId }, run2.insights.map { it.itemId })
        assertEquals(run1.opportunities.map { it.itemId }, run2.opportunities.map { it.itemId })
        assertEquals(run1.allItems.map { it.itemId }, run2.allItems.map { it.itemId })
        assertEquals(run1.summary, run2.summary)
        assertEquals(run1.profitSnapshot, run2.profitSnapshot)
    }

    // 97. EMPTY INPUT DOES NOT CRASH AND PROVIDES EMPTY SUMMARY
    @Test
    fun test97_phase11_emptyInputDoesNotCrashAndProvidesEmptySummary() {
        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = emptyList(),
            businessActions = emptyList(),
            crossBusinessInsights = emptyList(),
            collectionResults = emptyList(),
            profitSnapshot = null
        )

        assertTrue(center.attentionItems.isEmpty())
        assertTrue(center.actions.isEmpty())
        assertTrue(center.insights.isEmpty())
        assertTrue(center.opportunities.isEmpty())
        assertTrue(center.allItems.isEmpty())
        assertEquals(0, center.summary.totalItems)
        assertEquals(0, center.summary.attentionCount)
        assertEquals(0, center.summary.actionCount)
        assertEquals(0, center.summary.insightCount)
        assertEquals(0, center.summary.opportunityCount)
        assertNull(center.profitSnapshot)
    }

    // 98. MALFORMED AND MISSING IDS HANDLED SAFELY
    @Test
    fun test98_phase11_malformedMissingIdsHandledSafely() {
        val malformedAlert = BusinessAlert(
            alertId = "",
            severity = AlertSeverity.WARNING,
            category = AlertCategory.INVENTORY,
            productId = null,
            productName = null,
            title = "Missing IDs Alert",
            description = "Handles blank / null target IDs safely",
            supportingMetric = null,
            metricValue = null,
            unit = null
        )

        val malformedAction = BusinessAction(
            actionId = "",
            priority = BusinessActionPriority.MEDIUM,
            category = BusinessActionCategory.REVIEW_PRICING,
            title = "Missing IDs Action",
            description = "Handles blank IDs",
            targetId = null,
            targetName = null,
            supportingMetric = null,
            metricValue = null,
            metricUnit = null,
            actionLabel = "Review",
            destinationRoute = "business_actions"
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(malformedAlert),
            businessActions = listOf(malformedAction)
        )

        assertNotNull(center)
        center.allItems.forEach { item ->
            assertTrue(item.itemId.isNotBlank())
            assertFalse(item.itemId.contains("null"))
            assertNotNull(item.section)
            assertNotNull(item.priority)
        }
    }

    // 99. SINGLE CUSTOMER SINGLE PRODUCT EDGE CASE
    @Test
    fun test99_phase11_singleCustomerSingleProductEdgeCase() {
        val refTime = 1710000000000L
        val prod = Product(
            id = "single_p", businessId = "b1", name = "Single Prod", sku = "SKU-S", barcode = "111",
            category = "Cat", purchasePrice = 100.0, sellingPrice = 200.0, currentStock = 2,
            minStock = 10, supplierId = "sup1", expiryDate = null,
            createdAt = refTime, updatedAt = refTime, isActive = true
        )
        val cust = Customer(
            id = "single_c", businessId = "b1", name = "Single Cust", phone = "9876543210",
            email = "single@test.com", address = "Road 1", totalPurchases = 1000.0,
            totalPaid = 200.0, outstandingAmount = 800.0,
            createdAt = refTime - 1000000L, updatedAt = refTime, isActive = true
        )
        val sale = Sale(
            id = "single_s", businessId = "b1", invoiceNumber = "INV-S1", customerId = "single_c",
            items = listOf(SaleItem(productId = "single_p", productName = "Single Prod", quantity = 1, unitPrice = 200.0, totalPrice = 200.0)),
            subtotal = 200.0, discount = 0.0, tax = 0.0, grandTotal = 200.0, amountPaid = 0.0,
            outstanding = 200.0, paymentStatus = "UNPAID", paymentMethod = "CREDIT",
            createdAt = refTime - 50000L, updatedAt = refTime - 50000L
        )

        val center = UnifiedBusinessCommandCenterEngine.generateFromRawModels(
            products = listOf(prod),
            sales = listOf(sale),
            customers = listOf(cust),
            currentTime = refTime
        )

        assertNotNull(center)
        assertTrue(center.summary.totalItems > 0)
        assertNotNull(center.profitSnapshot)
        assertEquals(200.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(100.0, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(100.0, center.profitSnapshot!!.grossProfit, 0.001)
    }

    // 100. EXTREME FINANCIAL VALUES NAN INFINITY NEGATIVE PROFITS
    @Test
    fun test100_phase11_extremeFinancialValuesNaNInfinityNegativeProfits() {
        val extremeSnapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "Extreme Period",
            revenue = 0.0,
            cogs = 5000.0,
            grossProfit = -5000.0,
            operatingExpenses = 2000.0,
            netProfit = -7000.0
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(
            profitSnapshot = extremeSnapshot
        )

        assertNotNull(center.profitSnapshot)
        assertFalse(center.profitSnapshot!!.grossMarginPercent.isNaN())
        assertFalse(center.profitSnapshot!!.grossMarginPercent.isInfinite())
        assertFalse(center.profitSnapshot!!.netMarginPercent.isNaN())
        assertFalse(center.profitSnapshot!!.netMarginPercent.isInfinite())
        assertTrue(center.profitSnapshot!!.isOperatingAtLoss)
        assertTrue(center.profitSnapshot!!.hasNegativeGrossMargin)
        assertFalse(center.profitSnapshot!!.isProfitable)
    }

    // 101. ALL FOUR SECTIONS INTEGRITY PRESERVED
    @Test
    fun test101_phase11_allFourSectionsIntegrityPreserved() {
        val alert = createAlert("crit_alert", severity = AlertSeverity.CRITICAL, productId = "p_alert")
        val action = createAction("act_1", priority = BusinessActionPriority.MEDIUM, targetId = "p_action")
        val insight = createInsight("ins_1", category = CrossInsightCategory.WORKING_CAPITAL_STRAIN, severity = CrossInsightSeverity.MEDIUM, entityId = "p_insight")
        val oppInsight = createInsight("opp_ins", category = CrossInsightCategory.STAR_PERFORMER, severity = CrossInsightSeverity.POSITIVE, entityId = "p_opp")

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight, oppInsight)
        )

        assertEquals(1, center.attentionItems.size)
        assertEquals(1, center.actions.size)
        assertEquals(1, center.insights.size)
        assertEquals(1, center.opportunities.size)

        center.attentionItems.forEach { assertEquals(CommandCenterSection.NEEDS_ATTENTION, it.section) }
        center.actions.forEach { assertEquals(CommandCenterSection.ACTIONS, it.section) }
        center.insights.forEach { assertEquals(CommandCenterSection.BUSINESS_INSIGHTS, it.section) }
        center.opportunities.forEach { assertEquals(CommandCenterSection.OPPORTUNITIES, it.section) }
        assertEquals(4, center.allItems.size)
    }

    // 102. DEDUPLICATION PRESERVES DISTINCT ISSUE DOMAINS
    @Test
    fun test102_phase11_deduplicationPreservesDistinctIssueDomains() {
        val lowStockAlert = createAlert("a_stock", productId = "p1", category = AlertCategory.INVENTORY, severity = AlertSeverity.CRITICAL)
        val restockAction = createAction("act_restock", targetId = "p1", category = BusinessActionCategory.RESTOCK, priority = BusinessActionPriority.CRITICAL)
        val pricingAction = createAction("act_pricing", targetId = "p1", category = BusinessActionCategory.REVIEW_PRICING, priority = BusinessActionPriority.MEDIUM)

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(lowStockAlert),
            businessActions = listOf(restockAction, pricingAction)
        )

        // Low stock + Restock consolidate, while pricing action remains distinct
        val p1Items = center.allItems.filter { it.targetId == "p1" }
        assertEquals(2, p1Items.size)
    }

    // 103. PROFIT SNAPSHOT PRESERVES AUTHORITATIVE METRICS WITHOUT RECOMPUTATION
    @Test
    fun test103_phase11_profitSnapshotPreservesAuthoritativeMetricsWithoutRecomputation() {
        val snapshot = CommandCenterProfitSnapshot.create(
            periodLabel = "FY 2025-26",
            revenue = 500000.0,
            cogs = 320000.0,
            grossProfit = 180000.0,
            operatingExpenses = 70000.0,
            netProfit = 110000.0,
            productProfit = 180000.0
        )

        val center = UnifiedBusinessCommandCenterEngine.generate(profitSnapshot = snapshot)

        assertEquals(snapshot, center.profitSnapshot)
        assertEquals(500000.0, center.profitSnapshot!!.revenue, 0.001)
        assertEquals(320000.0, center.profitSnapshot!!.cogs, 0.001)
        assertEquals(180000.0, center.profitSnapshot!!.grossProfit, 0.001)
        assertEquals(70000.0, center.profitSnapshot!!.operatingExpenses, 0.001)
        assertEquals(110000.0, center.profitSnapshot!!.netProfit, 0.001)
        assertEquals(36.0, center.profitSnapshot!!.grossMarginPercent, 0.01)
        assertEquals(22.0, center.profitSnapshot!!.netMarginPercent, 0.01)
    }

    // 104. SOURCE TRACEABILITY COMPLETE COVERAGE
    @Test
    fun test104_phase11_sourceTraceabilityCompleteCoverage() {
        val alert = createAlert("a1", category = AlertCategory.INVENTORY)
        val action = createAction("act1", category = BusinessActionCategory.COLLECT_PAYMENT)
        val insight = createInsight("ins1", category = CrossInsightCategory.WORKING_CAPITAL_STRAIN)

        val center = UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = listOf(alert),
            businessActions = listOf(action),
            crossBusinessInsights = listOf(insight)
        )

        assertTrue(center.allItems.all { it.contributingSources.isNotEmpty() })
        assertTrue(center.allItems.any { it.sourceType == CommandCenterSourceType.BUSINESS_ALERT })
        assertTrue(center.allItems.any { it.sourceType == CommandCenterSourceType.BUSINESS_ACTION })
        assertTrue(center.allItems.any { it.sourceType == CommandCenterSourceType.CROSS_BUSINESS_INSIGHT })
    }

    // 105. OFFLINE DETERMINISTIC EXECUTION GUARANTEED
    @Test
    fun test105_phase11_offlineDeterministicExecutionGuaranteed() {
        // Runs 50 iterations; verify zero variance in all fields
        val alert = createAlert("a_det")
        val action = createAction("act_det")
        val insight = createInsight("ins_det")

        val baseline = UnifiedBusinessCommandCenterEngine.generate(listOf(alert), listOf(action), listOf(insight))

        repeat(50) {
            val testRun = UnifiedBusinessCommandCenterEngine.generate(listOf(alert), listOf(action), listOf(insight))
            assertEquals(baseline.allItems.size, testRun.allItems.size)
            assertEquals(baseline.allItems.map { it.itemId }, testRun.allItems.map { it.itemId })
            assertEquals(baseline.allItems.map { it.priority }, testRun.allItems.map { it.priority })
            assertEquals(baseline.allItems.map { it.section }, testRun.allItems.map { it.section })
            assertEquals(baseline.summary, testRun.summary)
        }
    }
}

