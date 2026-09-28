package com.example.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessAlertEngineTest {

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
    ): InventoryProductMetrics {
        return InventoryProductMetrics(
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
    }

    private fun createProfitabilityMetric(
        productId: String,
        productName: String = "Product $productId",
        unitsSold: Int = 10,
        revenue: Double = 1000.0,
        costOfGoodsSold: Double = 600.0,
        grossProfit: Double = 400.0,
        grossMarginPercentage: Double = 40.0,
        marginCategory: MarginCategory = MarginCategory.HIGH_MARGIN
    ): ProductProfitabilityMetrics {
        return ProductProfitabilityMetrics(
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
    }

    // 1. Empty intelligence results
    @Test
    fun test1_emptyIntelligenceResults() {
        val alerts = BusinessAlertEngine.generateAlerts()
        assertTrue(alerts.isEmpty())
    }

    // 2. Dead stock alert
    @Test
    fun test2_deadStockAlert() {
        val deadProduct = createInventoryMetric(
            productId = "p1",
            currentStock = 15,
            isDeadStock = true,
            velocityCategory = VelocityCategory.DEAD_STOCK,
            capitalLocked = 1500.0
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(deadProduct)
        )

        val deadAlert = alerts.find { it.alertId == "alert_dead_stock_p1" }
        assertNotNull(deadAlert)
        assertEquals(AlertSeverity.CRITICAL, deadAlert?.severity)
        assertEquals(AlertCategory.INVENTORY, deadAlert?.category)
        assertEquals("p1", deadAlert?.productId)
        assertEquals(1500.0, deadAlert?.metricValue ?: 0.0, 0.001)
    }

    // 3. Slow-moving alert
    @Test
    fun test3_slowMovingAlert() {
        val slowProduct = createInventoryMetric(
            productId = "p2",
            currentStock = 20,
            velocityCategory = VelocityCategory.SLOW_MOVING,
            daysOfStockRemaining = 120.0,
            capitalLocked = 2000.0
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(slowProduct)
        )

        val slowAlert = alerts.find { it.alertId == "alert_slow_moving_p2" }
        assertNotNull(slowAlert)
        assertEquals(AlertSeverity.WARNING, slowAlert?.severity)
        assertEquals(AlertCategory.INVENTORY, slowAlert?.category)
        assertEquals(120.0, slowAlert?.metricValue ?: 0.0, 0.001)
    }

    // 4. Falling demand alert
    @Test
    fun test4_fallingDemandAlert() {
        val fallingProduct = createInventoryMetric(
            productId = "p3",
            demandTrend = DemandTrend.FALLING,
            salesVelocityPerDay = 0.2,
            hasSufficientData = true
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(fallingProduct)
        )

        val demandAlert = alerts.find { it.alertId == "alert_falling_demand_p3" }
        assertNotNull(demandAlert)
        assertEquals(AlertSeverity.WARNING, demandAlert?.severity)
        assertEquals(AlertCategory.DEMAND, demandAlert?.category)
        assertEquals(0.2, demandAlert?.metricValue ?: 0.0, 0.001)
    }

    // 5. Low-stock alert when existing engine data supports it
    @Test
    fun test5_lowStockAlert() {
        val lowStockProduct = createInventoryMetric(
            productId = "p4",
            currentStock = 2,
            minimumStock = 5
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(lowStockProduct)
        )

        val lowStockAlert = alerts.find { it.alertId == "alert_low_stock_p4" }
        assertNotNull(lowStockAlert)
        assertEquals(AlertSeverity.WARNING, lowStockAlert?.severity)
        assertEquals(AlertCategory.INVENTORY, lowStockAlert?.category)
        assertEquals(2.0, lowStockAlert?.metricValue ?: 0.0, 0.001)
    }

    // 6. High-sales + low-margin alert
    @Test
    fun test6_highSalesLowMarginAlert() {
        val lowMarginProduct = createProfitabilityMetric(
            productId = "p5",
            unitsSold = 50,
            revenue = 5000.0,
            costOfGoodsSold = 4500.0,
            grossProfit = 500.0,
            grossMarginPercentage = 10.0,
            marginCategory = MarginCategory.LOW_MARGIN
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            profitabilityMetrics = listOf(lowMarginProduct)
        )

        val marginAlert = alerts.find { it.alertId == "alert_low_margin_p5" }
        assertNotNull(marginAlert)
        assertEquals(AlertSeverity.WARNING, marginAlert?.severity)
        assertEquals(AlertCategory.PROFITABILITY, marginAlert?.category)
        assertEquals(10.0, marginAlert?.metricValue ?: 0.0, 0.001)
        assertEquals("%", marginAlert?.unit)
    }

    // 7. No-profit alert
    @Test
    fun test7_noProfitAlert() {
        val lossProduct = createProfitabilityMetric(
            productId = "p6",
            unitsSold = 10,
            revenue = 1000.0,
            costOfGoodsSold = 1200.0,
            grossProfit = -200.0,
            grossMarginPercentage = -20.0,
            marginCategory = MarginCategory.NO_PROFIT
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            profitabilityMetrics = listOf(lossProduct)
        )

        val noProfitAlert = alerts.find { it.alertId == "alert_no_profit_p6" }
        assertNotNull(noProfitAlert)
        assertEquals(AlertSeverity.CRITICAL, noProfitAlert?.severity)
        assertEquals(AlertCategory.PROFITABILITY, noProfitAlert?.category)
        assertEquals(-200.0, noProfitAlert?.metricValue ?: 0.0, 0.001)
    }

    // 8. High-margin + meaningful-sales informational alert
    @Test
    fun test8_highMarginSalesInfoAlert() {
        val highMarginProduct = createProfitabilityMetric(
            productId = "p7",
            unitsSold = 20,
            revenue = 2000.0,
            costOfGoodsSold = 1000.0,
            grossProfit = 1000.0,
            grossMarginPercentage = 50.0,
            marginCategory = MarginCategory.HIGH_MARGIN
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            profitabilityMetrics = listOf(highMarginProduct)
        )

        val infoAlert = alerts.find { it.alertId == "alert_high_margin_p7" }
        assertNotNull(infoAlert)
        assertEquals(AlertSeverity.INFO, infoAlert?.severity)
        assertEquals(AlertCategory.PROFITABILITY, infoAlert?.category)
        assertEquals(50.0, infoAlert?.metricValue ?: 0.0, 0.001)
    }

    // 9. Capital-locked business summary alert
    @Test
    fun test9_capitalLockedSummaryAlert() {
        val summary = InventoryIntelligenceSummary(
            totalInventoryValue = 50000.0,
            capitalLockedInSlowAndDeadStock = 12000.0,
            fastMovingCount = 5,
            normalCount = 10,
            slowMovingCount = 3,
            deadStockCount = 2,
            products = emptyList()
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            inventorySummary = summary
        )

        val capitalAlert = alerts.find { it.alertId == "alert_capital_locked_business" }
        assertNotNull(capitalAlert)
        assertEquals(AlertSeverity.WARNING, capitalAlert?.severity)
        assertEquals(AlertCategory.CASH_FLOW, capitalAlert?.category)
        assertEquals(12000.0, capitalAlert?.metricValue ?: 0.0, 0.001)
        assertNull(capitalAlert?.productId)
    }

    // 10. Multiple alerts generated correctly
    @Test
    fun test10_multipleAlerts() {
        val inv = createInventoryMetric("p1", isDeadStock = true, currentStock = 5, capitalLocked = 500.0)
        val prof = createProfitabilityMetric("p2", unitsSold = 10, grossProfit = -100.0, marginCategory = MarginCategory.NO_PROFIT)

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(inv),
            profitabilityMetrics = listOf(prof)
        )

        assertTrue(alerts.size >= 2)
    }

    // 11. Alert deduplication
    @Test
    fun test11_alertDeduplication() {
        val inv1 = createInventoryMetric("p1", isDeadStock = true, currentStock = 5, capitalLocked = 500.0)
        val inv2 = createInventoryMetric("p1", isDeadStock = true, currentStock = 5, capitalLocked = 500.0)

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(inv1, inv2)
        )

        val deadStockAlerts = alerts.filter { it.alertId == "alert_dead_stock_p1" }
        assertEquals(1, deadStockAlerts.size)
    }

    // 12. Severity ordering: CRITICAL -> WARNING -> INFO
    @Test
    fun test12_severityOrdering() {
        val deadStock = createInventoryMetric("p1", isDeadStock = true, currentStock = 5, capitalLocked = 500.0) // CRITICAL
        val slowMoving = createInventoryMetric("p2", velocityCategory = VelocityCategory.SLOW_MOVING, currentStock = 5) // WARNING
        val highMargin = createProfitabilityMetric("p3", unitsSold = 5, marginCategory = MarginCategory.HIGH_MARGIN) // INFO

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(deadStock, slowMoving),
            profitabilityMetrics = listOf(highMargin)
        )

        assertTrue(alerts.isNotEmpty())
        for (i in 0 until alerts.size - 1) {
            val currSeverity = alerts[i].severity
            val nextSeverity = alerts[i + 1].severity
            assertTrue(currSeverity.ordinal <= nextSeverity.ordinal)
        }
    }

    // 13. Deterministic ordering across runs
    @Test
    fun test13_deterministicOrdering() {
        val inv1 = createInventoryMetric("p1", isDeadStock = true, currentStock = 5, capitalLocked = 500.0)
        val inv2 = createInventoryMetric("p2", velocityCategory = VelocityCategory.SLOW_MOVING, currentStock = 5)
        val prof = createProfitabilityMetric("p3", unitsSold = 10, marginCategory = MarginCategory.HIGH_MARGIN)

        val run1 = BusinessAlertEngine.generateAlerts(inventoryMetrics = listOf(inv1, inv2), profitabilityMetrics = listOf(prof))
        val run2 = BusinessAlertEngine.generateAlerts(inventoryMetrics = listOf(inv1, inv2), profitabilityMetrics = listOf(prof))

        assertEquals(run1.map { it.alertId }, run2.map { it.alertId })
    }

    // 14. Insufficient-data suppression
    @Test
    fun test14_insufficientDataSuppression() {
        val unanalyzedProfit = createProfitabilityMetric(
            productId = "pEmpty",
            unitsSold = 0,
            revenue = 0.0,
            grossProfit = 0.0,
            grossMarginPercentage = 0.0,
            marginCategory = MarginCategory.INSUFFICIENT_DATA
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            profitabilityMetrics = listOf(unanalyzedProfit)
        )

        assertTrue(alerts.isEmpty())
    }

    // 15. NaN and Infinity safety
    @Test
    fun test15_nanInfinitySafety() {
        val nanProfit = createProfitabilityMetric(
            productId = "pNan",
            unitsSold = 5,
            revenue = Double.NaN,
            grossProfit = Double.POSITIVE_INFINITY,
            grossMarginPercentage = Double.NaN,
            marginCategory = MarginCategory.LOW_MARGIN
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            profitabilityMetrics = listOf(nanProfit)
        )

        assertTrue(alerts.isEmpty())
    }

    // 16. Zero-value safety (capital locked == 0 should not alert)
    @Test
    fun test16_zeroCapitalLockedSafety() {
        val summary = InventoryIntelligenceSummary(
            totalInventoryValue = 1000.0,
            capitalLockedInSlowAndDeadStock = 0.0,
            fastMovingCount = 1,
            normalCount = 1,
            slowMovingCount = 0,
            deadStockCount = 0,
            products = emptyList()
        )

        val alerts = BusinessAlertEngine.generateAlerts(inventorySummary = summary)
        val capitalAlert = alerts.find { it.alertId == "alert_capital_locked_business" }
        assertNull(capitalAlert)
    }

    // 17. Missing product data / Dead stock with 0 current stock
    @Test
    fun test17_deadStockWithZeroStockDoesNotAlert() {
        val deadStockZeroUnits = createInventoryMetric(
            productId = "pZero",
            currentStock = 0,
            isDeadStock = true,
            velocityCategory = VelocityCategory.DEAD_STOCK,
            capitalLocked = 0.0
        )

        val alerts = BusinessAlertEngine.generateAlerts(inventoryMetrics = listOf(deadStockZeroUnits))
        val deadAlert = alerts.find { it.alertId == "alert_dead_stock_pZero" }
        assertNull(deadAlert)
    }

    // 18. No fake alerts generated on normal inventory
    @Test
    fun test18_noFakeAlertsOnHealthyInventory() {
        val healthyInv = createInventoryMetric(
            productId = "pHealthy",
            currentStock = 50,
            minimumStock = 5,
            velocityCategory = VelocityCategory.NORMAL,
            isDeadStock = false,
            demandTrend = DemandTrend.STABLE
        )
        val healthyProf = createProfitabilityMetric(
            productId = "pHealthy",
            unitsSold = 20,
            revenue = 2000.0,
            costOfGoodsSold = 1500.0,
            grossProfit = 500.0,
            grossMarginPercentage = 25.0,
            marginCategory = MarginCategory.MEDIUM_MARGIN
        )

        val alerts = BusinessAlertEngine.generateAlerts(
            inventoryMetrics = listOf(healthyInv),
            profitabilityMetrics = listOf(healthyProf)
        )

        assertTrue(alerts.isEmpty())
    }
}
