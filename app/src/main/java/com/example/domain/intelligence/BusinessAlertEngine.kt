package com.example.domain.intelligence

/**
 * Severity levels for business alerts based on deterministic business conditions.
 */
enum class AlertSeverity {
    CRITICAL,
    WARNING,
    INFO
}

/**
 * Functional categories for business alerts derived from domain intelligence engines.
 */
enum class AlertCategory {
    INVENTORY,
    DEMAND,
    PROFITABILITY,
    CASH_FLOW,
    ATTENTION
}

/**
 * Immutable business alert model representing an actionable deterministic business insight.
 */
data class BusinessAlert(
    val alertId: String,
    val severity: AlertSeverity,
    val category: AlertCategory,
    val productId: String?,
    val productName: String?,
    val title: String,
    val description: String,
    val supportingMetric: String?,
    val metricValue: Double?,
    val unit: String?
)

/**
 * Pure domain-level engine that interprets outputs from InventoryIntelligenceEngine
 * and ProductProfitabilityEngine to produce deduplicated, deterministically ordered business alerts.
 *
 * Rules:
 * - Deterministic, offline, zero Gemini/network calls, zero database mutation.
 * - Does not recalculate velocity, coverage, dead stock, revenue, COGS, gross profit, or margins.
 * - Interprets existing results safely.
 */
object BusinessAlertEngine {

    /**
     * Generates a deduplicated, deterministically sorted list of business alerts.
     */
    fun generateAlerts(
        inventorySummary: InventoryIntelligenceSummary? = null,
        inventoryMetrics: List<InventoryProductMetrics> = inventorySummary?.products ?: emptyList(),
        profitabilitySummary: ProductProfitabilitySummary? = null,
        profitabilityMetrics: List<ProductProfitabilityMetrics> = profitabilitySummary?.products ?: emptyList()
    ): List<BusinessAlert> {
        val alerts = mutableListOf<BusinessAlert>()

        // 1. Business-wide Capital Locked Alert (CASH_FLOW)
        // Rule 8: If InventoryIntelligenceSummary contains meaningful capital locked (> 0), expose alert
        if (inventorySummary != null && isValidNumber(inventorySummary.capitalLockedInSlowAndDeadStock) && inventorySummary.capitalLockedInSlowAndDeadStock > 0.0) {
            val locked = inventorySummary.capitalLockedInSlowAndDeadStock
            alerts.add(
                BusinessAlert(
                    alertId = "alert_capital_locked_business",
                    severity = AlertSeverity.WARNING,
                    category = AlertCategory.CASH_FLOW,
                    productId = null,
                    productName = null,
                    title = "Capital Locked in Inventory",
                    description = "Capital is currently locked in slow-moving or dead stock across your inventory.",
                    supportingMetric = "Capital Locked",
                    metricValue = locked,
                    unit = "₹"
                )
            )
        }

        // Map profitability by productId for cross-engine rules
        val profitMap = profitabilityMetrics.associateBy { it.productId }

        // 2. Inventory & Demand Alerts (Per product)
        for (inv in inventoryMetrics) {
            if (!inv.hasSufficientData && inv.currentStock <= 0) {
                // Insufficient data with 0 stock has no actionable alert
                continue
            }

            // Rule 1: DEAD_STOCK with currentStock > 0
            if (inv.isDeadStock && inv.currentStock > 0 && isValidNumber(inv.capitalLocked) && inv.capitalLocked >= 0.0) {
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_dead_stock_${inv.productId}",
                        severity = AlertSeverity.CRITICAL,
                        category = AlertCategory.INVENTORY,
                        productId = inv.productId,
                        productName = inv.productName,
                        title = "Dead Stock Alert",
                        description = "No sales recorded in over 45 days with unsold stock remaining.",
                        supportingMetric = "Units / Capital",
                        metricValue = inv.capitalLocked,
                        unit = "₹"
                    )
                )
            }
            // Rule 2: SLOW_MOVING
            else if (inv.velocityCategory == VelocityCategory.SLOW_MOVING && inv.currentStock > 0) {
                val stockDays = inv.daysOfStockRemaining
                val metricVal = if (stockDays != null && isValidNumber(stockDays)) stockDays else inv.capitalLocked
                val metricUnit = if (stockDays != null && isValidNumber(stockDays)) "days" else "₹"
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_slow_moving_${inv.productId}",
                        severity = AlertSeverity.WARNING,
                        category = AlertCategory.INVENTORY,
                        productId = inv.productId,
                        productName = inv.productName,
                        title = "Slow-Moving Inventory",
                        description = "Product has high days of inventory coverage with low sales velocity.",
                        supportingMetric = if (stockDays != null && isValidNumber(stockDays)) "Stock Coverage" else "Capital Locked",
                        metricValue = if (isValidNumber(metricVal)) metricVal else null,
                        unit = metricUnit
                    )
                )
            }

            // Rule 3: FALLING Demand Trend
            if (inv.demandTrend == DemandTrend.FALLING && inv.hasSufficientData) {
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_falling_demand_${inv.productId}",
                        severity = AlertSeverity.WARNING,
                        category = AlertCategory.DEMAND,
                        productId = inv.productId,
                        productName = inv.productName,
                        title = "Falling Demand Trend",
                        description = "Sales velocity has declined compared to the earlier analysis period.",
                        supportingMetric = "Velocity",
                        metricValue = if (isValidNumber(inv.salesVelocityPerDay)) inv.salesVelocityPerDay else null,
                        unit = "units/day"
                    )
                )
            }

            // Rule 4: Low Stock Alert
            // Product has minimumStock configured (> 0), currentStock <= minimumStock, and positive velocity or stock > 0
            if (inv.minimumStock > 0 && inv.currentStock <= inv.minimumStock) {
                val isOut = inv.currentStock <= 0
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_low_stock_${inv.productId}",
                        severity = if (isOut) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        category = AlertCategory.INVENTORY,
                        productId = inv.productId,
                        productName = inv.productName,
                        title = if (isOut) "Out of Stock" else "Low Stock Warning",
                        description = if (isOut) "Stock is completely depleted." else "Current stock has reached or fallen below minimum threshold.",
                        supportingMetric = "Current Stock",
                        metricValue = inv.currentStock.toDouble(),
                        unit = "units"
                    )
                )
            }
        }

        // 3. Profitability & Cross-Engine Alerts
        for (prof in profitabilityMetrics) {
            // Ignore insufficient data
            if (prof.marginCategory == MarginCategory.INSUFFICIENT_DATA || prof.unitsSold <= 0) {
                continue
            }

            if (!isValidNumber(prof.revenue) || !isValidNumber(prof.grossProfit) || !isValidNumber(prof.grossMarginPercentage)) {
                continue
            }

            // Rule 6: NO_PROFIT with meaningful sales (unitsSold > 0)
            if (prof.marginCategory == MarginCategory.NO_PROFIT) {
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_no_profit_${prof.productId}",
                        severity = AlertSeverity.CRITICAL,
                        category = AlertCategory.PROFITABILITY,
                        productId = prof.productId,
                        productName = prof.productName,
                        title = "Unprofitable Sales Activity",
                        description = "Product recorded sales but generated zero or negative gross profit.",
                        supportingMetric = "Gross Profit",
                        metricValue = prof.grossProfit,
                        unit = "₹"
                    )
                )
            }

            // Rule 5: High sales + LOW_MARGIN (Cross-engine / Joint)
            if (prof.marginCategory == MarginCategory.LOW_MARGIN) {
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_low_margin_${prof.productId}",
                        severity = AlertSeverity.WARNING,
                        category = AlertCategory.PROFITABILITY,
                        productId = prof.productId,
                        productName = prof.productName,
                        title = "Low Margin on Active Product",
                        description = "Product has sales activity but a low gross margin.",
                        supportingMetric = "Gross Margin",
                        metricValue = prof.grossMarginPercentage,
                        unit = "%"
                    )
                )
            }

            // Rule 7: HIGH_MARGIN with meaningful sales (unitsSold > 0)
            if (prof.marginCategory == MarginCategory.HIGH_MARGIN && prof.unitsSold > 0) {
                alerts.add(
                    BusinessAlert(
                        alertId = "alert_high_margin_${prof.productId}",
                        severity = AlertSeverity.INFO,
                        category = AlertCategory.PROFITABILITY,
                        productId = prof.productId,
                        productName = prof.productName,
                        title = "High Margin Performance",
                        description = "Product has high gross margin with recorded sales activity.",
                        supportingMetric = "Gross Margin",
                        metricValue = prof.grossMarginPercentage,
                        unit = "%"
                    )
                )
            }
        }

        // Deduplication by alertId
        val deduplicated = alerts.distinctBy { it.alertId }

        // Deterministic Sorting:
        // 1. Severity: CRITICAL (0), WARNING (1), INFO (2)
        // 2. Category name
        // 3. Product name (empty for business-wide)
        // 4. AlertId
        return deduplicated.sortedWith(
            compareBy<BusinessAlert> { alert ->
                when (alert.severity) {
                    AlertSeverity.CRITICAL -> 0
                    AlertSeverity.WARNING -> 1
                    AlertSeverity.INFO -> 2
                }
            }.thenBy { it.category.name }
             .thenBy { it.productName ?: "" }
             .thenBy { it.alertId }
        )
    }

    private fun isValidNumber(value: Double?): Boolean {
        return value != null && !value.isNaN() && !value.isInfinite()
    }
}
