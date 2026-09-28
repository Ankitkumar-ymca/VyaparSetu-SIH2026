package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.domain.model.Sale
import java.util.Locale

/**
 * Functional categories for cross-intelligence business insights.
 */
enum class CrossInsightCategory {
    HIGH_MARGIN_STOCKOUT_RISK,
    UNPROFITABLE_CAPITAL_TRAP,
    STAR_PERFORMER,
    VOLUME_MARGIN_VULNERABILITY,
    KEY_ACCOUNT_EXPOSURE,
    DORMANT_DEBT_RISK,
    WORKING_CAPITAL_STRAIN,
    MULTI_SIGNAL_CONVERGENCE
}

/**
 * Severity level of a cross-business insight.
 */
enum class CrossInsightSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    POSITIVE
}

/**
 * Target entity type associated with an insight.
 */
enum class CrossInsightEntityType {
    PRODUCT,
    CUSTOMER,
    BUSINESS_WIDE
}

/**
 * An actionable cross-domain business insight correlating independent intelligence signals.
 */
data class CrossBusinessInsight(
    val id: String,
    val category: CrossInsightCategory,
    val severity: CrossInsightSeverity,
    val title: String,
    val summary: String,
    val detailedExplanation: String,
    val contributingSignals: List<String>,
    val entityId: String? = null,
    val entityName: String? = null,
    val entityType: CrossInsightEntityType,
    val metricValue: Double? = null,
    val metricUnit: String? = null,
    val recommendedStrategy: String
)

/**
 * Aggregated summary of cross-business insights across the business.
 */
data class CrossBusinessInsightsSummary(
    val totalInsights: Int,
    val criticalInsightsCount: Int,
    val highInsightsCount: Int,
    val mediumInsightsCount: Int,
    val positiveInsightsCount: Int,
    val workingCapitalLockedTotal: Double,
    val insights: List<CrossBusinessInsight>
)

/**
 * Pure, deterministic Cross-Business Intelligence Engine for VyaparSetu.
 *
 * Correlates existing intelligence outputs across:
 * - Inventory Intelligence (stock, velocity, dead stock, days coverage)
 * - Product Profitability (margins, profit, COGS)
 * - Customer Intelligence (segments, recency, order frequency)
 * - Collection Priority (outstanding dues, aging, priority)
 * - Business Alerts (active anomalies)
 * - Business Actions (recommended operational tasks)
 *
 * Rules:
 * - Deterministic, offline, zero Gemini/network calls, zero database mutations.
 * - Does NOT recalculate raw financial truth or recreate P&L.
 * - Safe against NaN/Infinity and empty/null data.
 */
object CrossBusinessInsightsEngine {

    object Thresholds {
        const val HIGH_MARGIN_PERCENT = 35.0
        const val LOW_MARGIN_PERCENT = 15.0
        const val CRITICAL_STOCK_DAYS = 3.0
        const val HIGH_OUTSTANDING_AMOUNT = 5000.0
        const val CAPITAL_STRAIN_HIGH = 10000.0
    }

    /**
     * Evaluates cross-business insights from raw models by delegating to existing intelligence engines.
     */
    fun generateInsights(
        products: List<Product> = emptyList(),
        sales: List<Sale> = emptyList(),
        customers: List<Customer> = emptyList(),
        currentTime: Long = System.currentTimeMillis()
    ): CrossBusinessInsightsSummary {
        val inventorySummary = if (products.isNotEmpty()) {
            InventoryIntelligenceEngine.analyze(products, sales, currentTime)
        } else null

        val profitabilitySummary = if (products.isNotEmpty()) {
            ProductProfitabilityEngine.analyze(products, sales)
        } else null

        val customerSummary = if (customers.isNotEmpty()) {
            CustomerIntelligenceEngine.analyze(customers = customers, sales = sales, currentTime = currentTime)
        } else null

        val collectionSummary = if (customerSummary != null) {
            CollectionPriorityEngine.evaluateFromMetrics(customerSummary.customers)
        } else if (customers.isNotEmpty()) {
            CollectionPriorityEngine.evaluate(customers, sales, currentTime)
        } else null

        val businessAlerts = if (inventorySummary != null || profitabilitySummary != null) {
            BusinessAlertEngine.generateAlerts(
                inventorySummary = inventorySummary,
                profitabilitySummary = profitabilitySummary
            )
        } else emptyList()

        val businessActions = BusinessActionEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            businessAlerts = businessAlerts
        ).allActions

        return generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            businessAlerts = businessAlerts,
            businessActions = businessActions
        )
    }

    /**
     * Evaluates cross-business insights directly from pre-computed intelligence metrics,
     * avoiding redundant calculations.
     */
    fun generateFromIntelligence(
        inventorySummary: InventoryIntelligenceSummary? = null,
        inventoryMetrics: List<InventoryProductMetrics> = inventorySummary?.products ?: emptyList(),
        profitabilitySummary: ProductProfitabilitySummary? = null,
        profitabilityMetrics: List<ProductProfitabilityMetrics> = profitabilitySummary?.products ?: emptyList(),
        customerSummary: CustomerIntelligenceSummary? = null,
        customerMetrics: List<CustomerMetrics> = customerSummary?.customers ?: emptyList(),
        collectionSummary: CollectionPrioritySummary? = null,
        collectionResults: List<CollectionPriorityResult> = collectionSummary?.results ?: emptyList(),
        businessAlerts: List<BusinessAlert> = emptyList(),
        businessActions: List<BusinessAction> = emptyList()
    ): CrossBusinessInsightsSummary {
        val insights = mutableListOf<CrossBusinessInsight>()

        val invMap = inventoryMetrics.associateBy { it.productId }
        val profMap = profitabilityMetrics.associateBy { it.productId }
        val custMap = customerMetrics.associateBy { it.customerId }
        val collMap = collectionResults.associateBy { it.customerId }

        // 1. INVENTORY + PROFITABILITY CORRELATIONS (Product level)
        for (item in inventoryMetrics) {
            val prof = profMap[item.productId] ?: continue

            val isHighMargin = prof.marginCategory == MarginCategory.HIGH_MARGIN ||
                    (isValidNumber(prof.grossMarginPercentage) && prof.grossMarginPercentage >= Thresholds.HIGH_MARGIN_PERCENT)
            val isLowOrZeroMargin = prof.marginCategory == MarginCategory.LOW_MARGIN ||
                    prof.marginCategory == MarginCategory.NO_PROFIT ||
                    (isValidNumber(prof.grossMarginPercentage) && prof.grossMarginPercentage < Thresholds.LOW_MARGIN_PERCENT)

            // Pattern A: High Margin Stockout Risk
            if (isHighMargin) {
                val isOutOfStock = item.currentStock <= 0 && (item.unitsSold > 0 || (isValidNumber(item.salesVelocityPerDay) && item.salesVelocityPerDay > 0.0))
                val isCriticallyLow = item.currentStock > 0 && (
                        (item.daysOfStockRemaining != null && item.daysOfStockRemaining.isFinite() && item.daysOfStockRemaining <= Thresholds.CRITICAL_STOCK_DAYS) ||
                                (item.minimumStock > 0 && item.currentStock <= item.minimumStock)
                        )

                if (isOutOfStock) {
                    insights.add(
                        CrossBusinessInsight(
                            id = "insight_high_margin_oos_${item.productId}",
                            category = CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK,
                            severity = CrossInsightSeverity.CRITICAL,
                            title = "High-Margin Stockout: ${item.productName}",
                            summary = "${item.productName} is completely out of stock but yields high profit margin (${formatPercent(prof.grossMarginPercentage)}).",
                            detailedExplanation = "This product delivers superior gross margin (${formatPercent(prof.grossMarginPercentage)}), but zero units are available to sell. Every day out of stock forfeits peak-margin earnings.",
                            contributingSignals = listOf(
                                "Inventory: Stock 0 units (Velocity: ${formatDouble(item.salesVelocityPerDay)}/day)",
                                "Profitability: High Margin (${formatPercent(prof.grossMarginPercentage)})"
                            ),
                            entityId = item.productId,
                            entityName = item.productName,
                            entityType = CrossInsightEntityType.PRODUCT,
                            metricValue = safeValue(prof.grossMarginPercentage),
                            metricUnit = "%",
                            recommendedStrategy = "Expedite supplier reorder immediately to recover high-margin cash flow."
                        )
                    )
                } else if (isCriticallyLow) {
                    insights.add(
                        CrossBusinessInsight(
                            id = "insight_high_margin_low_${item.productId}",
                            category = CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK,
                            severity = CrossInsightSeverity.HIGH,
                            title = "High-Margin Stock Running Out: ${item.productName}",
                            summary = "High-margin generator (${formatPercent(prof.grossMarginPercentage)}) has only ${item.currentStock} units remaining.",
                            detailedExplanation = "Inventory coverage is critically low. Given its strong profitability (${formatPercent(prof.grossMarginPercentage)}), running out of stock will directly compress business net earnings.",
                            contributingSignals = listOf(
                                "Inventory: ${item.currentStock} units left (${formatCoverage(item.daysOfStockRemaining)})",
                                "Profitability: High Margin (${formatPercent(prof.grossMarginPercentage)})"
                            ),
                            entityId = item.productId,
                            entityName = item.productName,
                            entityType = CrossInsightEntityType.PRODUCT,
                            metricValue = safeValue(prof.grossMarginPercentage),
                            metricUnit = "%",
                            recommendedStrategy = "Place a preventive purchase order before stock reaches zero."
                        )
                    )
                }
            }

            // Pattern B: Star Performer (Fast Moving + High Margin)
            if (item.velocityCategory == VelocityCategory.FAST_MOVING && isHighMargin) {
                insights.add(
                    CrossBusinessInsight(
                        id = "insight_star_performer_${item.productId}",
                        category = CrossInsightCategory.STAR_PERFORMER,
                        severity = CrossInsightSeverity.POSITIVE,
                        title = "Star Performer: ${item.productName}",
                        summary = "Optimal dual performance: high sales velocity (${formatDouble(item.salesVelocityPerDay)}/day) and strong gross margin (${formatPercent(prof.grossMarginPercentage)}).",
                        detailedExplanation = "${item.productName} is in the business's top quadrant, pairing rapid sales velocity with excellent unit profitability. It serves as a primary profit engine.",
                        contributingSignals = listOf(
                            "Inventory: Fast Moving (${formatDouble(item.salesVelocityPerDay)} units/day)",
                            "Profitability: High Margin (${formatPercent(prof.grossMarginPercentage)})"
                        ),
                        entityId = item.productId,
                        entityName = item.productName,
                        entityType = CrossInsightEntityType.PRODUCT,
                        metricValue = safeValue(prof.grossProfit),
                        metricUnit = "₹",
                        recommendedStrategy = "Protect supply buffer and feature prominently in store displays."
                    )
                )
            }

            // Pattern C: Unprofitable Capital Trap (Dead Stock / Slow Moving + Low/Zero Margin)
            if ((item.isDeadStock || item.velocityCategory == VelocityCategory.SLOW_MOVING) && isLowOrZeroMargin && item.currentStock > 0) {
                val isCritical = item.isDeadStock && prof.marginCategory == MarginCategory.NO_PROFIT
                val locked = if (isValidNumber(item.capitalLocked) && item.capitalLocked > 0.0) item.capitalLocked else item.inventoryValue
                insights.add(
                    CrossBusinessInsight(
                        id = "insight_capital_trap_${item.productId}",
                        category = CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP,
                        severity = if (isCritical) CrossInsightSeverity.CRITICAL else CrossInsightSeverity.HIGH,
                        title = "Unprofitable Capital Trap: ${item.productName}",
                        summary = "${item.productName} locks ₹${formatDouble(locked)} in capital with sluggish turnover and low margin (${formatPercent(prof.grossMarginPercentage)}).",
                        detailedExplanation = "This inventory represents a double drag on the business: capital is locked up in slow or dead stock, and even when sold, the margin (${formatPercent(prof.grossMarginPercentage)}) fails to yield meaningful returns.",
                        contributingSignals = listOf(
                            "Inventory: ${if (item.isDeadStock) "Dead Stock" else "Slow Moving"} (₹${formatDouble(locked)} locked)",
                            "Profitability: ${if (prof.marginCategory == MarginCategory.NO_PROFIT) "No Profit / Negative" else "Low Margin (${formatPercent(prof.grossMarginPercentage)})"}"
                        ),
                        entityId = item.productId,
                        entityName = item.productName,
                        entityType = CrossInsightEntityType.PRODUCT,
                        metricValue = safeValue(locked),
                        metricUnit = "₹",
                        recommendedStrategy = "Discount or bundle to liquidate stock and reinvest capital into fast-moving, higher-margin goods."
                    )
                )
            }

            // Pattern D: Volume-Margin Vulnerability (Fast Moving + Low Margin)
            if (item.velocityCategory == VelocityCategory.FAST_MOVING && isLowOrZeroMargin) {
                insights.add(
                    CrossBusinessInsight(
                        id = "insight_volume_vulnerability_${item.productId}",
                        category = CrossInsightCategory.VOLUME_MARGIN_VULNERABILITY,
                        severity = CrossInsightSeverity.MEDIUM,
                        title = "High Volume, Thin Margin: ${item.productName}",
                        summary = "High turnover (${formatDouble(item.salesVelocityPerDay)} units/day) but thin margin (${formatPercent(prof.grossMarginPercentage)}) leaves little buffer.",
                        detailedExplanation = "While sales volume is vigorous, minimal gross margin makes this product highly vulnerable to supplier price increases or handling costs.",
                        contributingSignals = listOf(
                            "Inventory: Fast Moving (${formatDouble(item.salesVelocityPerDay)} units/day)",
                            "Profitability: Low Margin (${formatPercent(prof.grossMarginPercentage)})"
                        ),
                        entityId = item.productId,
                        entityName = item.productName,
                        entityType = CrossInsightEntityType.PRODUCT,
                        metricValue = safeValue(prof.grossMarginPercentage),
                        metricUnit = "%",
                        recommendedStrategy = "Explore incremental price adjustments or negotiate volume discounts with the supplier."
                    )
                )
            }
        }

        // 2. CUSTOMER + COLLECTION CORRELATIONS (Customer level)
        for (cust in customerMetrics) {
            val coll = collMap[cust.customerId]

            // Pattern E: Key Account Exposure (High Value / Frequent + High Collection Priority or substantial outstanding)
            val isHighTierCustomer = cust.segment == CustomerSegment.HIGH_VALUE || cust.segment == CustomerSegment.FREQUENT
            val hasHighDue = cust.outstandingAmount >= Thresholds.HIGH_OUTSTANDING_AMOUNT ||
                    (coll?.priority == CollectionPriorityLevel.HIGH)

            if (isHighTierCustomer && hasHighDue && cust.outstandingAmount > 0.0) {
                insights.add(
                    CrossBusinessInsight(
                        id = "insight_key_account_due_${cust.customerId}",
                        category = CrossInsightCategory.KEY_ACCOUNT_EXPOSURE,
                        severity = CrossInsightSeverity.HIGH,
                        title = "Key Account Credit Exposure: ${cust.customerName}",
                        summary = "Top-tier customer (${cust.segment.name}) carries ₹${formatDouble(cust.outstandingAmount)} outstanding balance.",
                        detailedExplanation = "${cust.customerName} is a valuable customer with ₹${formatDouble(cust.lifetimePurchases)} in lifetime sales, but has accumulated a high balance of ₹${formatDouble(cust.outstandingAmount)}. Preserving the relationship requires diplomatic collection.",
                        contributingSignals = listOf(
                            "Customer Intelligence: ${cust.segment.name} (Lifetime ₹${formatDouble(cust.lifetimePurchases)})",
                            "Collection Priority: ${coll?.priority?.name ?: "HIGH"} (Outstanding ₹${formatDouble(cust.outstandingAmount)})"
                        ),
                        entityId = cust.customerId,
                        entityName = cust.customerName,
                        entityType = CrossInsightEntityType.CUSTOMER,
                        metricValue = safeValue(cust.outstandingAmount),
                        metricUnit = "₹",
                        recommendedStrategy = "Send a gentle polite statement or offer structured installment payment to preserve patronage."
                    )
                )
            }

            // Pattern F: Dormant Debt Risk (Inactive Customer + Overdue Balance)
            val isInactive = cust.activityStatus == CustomerActivityStatus.INACTIVE ||
                    (cust.daysSinceLastPurchase != null && cust.daysSinceLastPurchase >= 45)
            if (isInactive && cust.hasOutstanding && cust.outstandingAmount > 0.0) {
                val isCriticalDue = cust.outstandingAmount >= Thresholds.HIGH_OUTSTANDING_AMOUNT
                val daysInactive = cust.daysSinceLastPurchase ?: 45L
                insights.add(
                    CrossBusinessInsight(
                        id = "insight_dormant_debt_${cust.customerId}",
                        category = CrossInsightCategory.DORMANT_DEBT_RISK,
                        severity = if (isCriticalDue) CrossInsightSeverity.CRITICAL else CrossInsightSeverity.HIGH,
                        title = "Dormant Account Due: ${cust.customerName}",
                        summary = "Customer inactive for ${daysInactive} days with ₹${formatDouble(cust.outstandingAmount)} uncollected balance.",
                        detailedExplanation = "${cust.customerName} has stopped purchasing but still owes ₹${formatDouble(cust.outstandingAmount)}. Risk of uncollectible bad debt escalates as time elapses.",
                        contributingSignals = listOf(
                            "Customer Intelligence: Inactive (${daysInactive} days since purchase)",
                            "Collection Priority: ₹${formatDouble(cust.outstandingAmount)} outstanding"
                        ),
                        entityId = cust.customerId,
                        entityName = cust.customerName,
                        entityType = CrossInsightEntityType.CUSTOMER,
                        metricValue = safeValue(cust.outstandingAmount),
                        metricUnit = "₹",
                        recommendedStrategy = "Direct phone call or friendly payment reminder before debt ages further."
                    )
                )
            }
        }

        // 3. BUSINESS-WIDE WORKING CAPITAL STRAIN
        val totalCapitalLockedInStock = inventorySummary?.capitalLockedInSlowAndDeadStock ?: 0.0
        val totalOutstandingReceivables = collectionSummary?.totalOutstanding ?: customerSummary?.totalOutstandingAmount ?: 0.0
        val totalWorkingCapitalLocked = (if (totalCapitalLockedInStock.isFinite()) totalCapitalLockedInStock else 0.0) +
                (if (totalOutstandingReceivables.isFinite()) totalOutstandingReceivables else 0.0)

        if (totalCapitalLockedInStock > 0.0 && totalOutstandingReceivables > 0.0) {
            val isCritical = totalWorkingCapitalLocked >= Thresholds.CAPITAL_STRAIN_HIGH
            insights.add(
                CrossBusinessInsight(
                    id = "insight_working_capital_strain_business",
                    category = CrossInsightCategory.WORKING_CAPITAL_STRAIN,
                    severity = if (isCritical) CrossInsightSeverity.CRITICAL else CrossInsightSeverity.HIGH,
                    title = "Compound Working Capital Squeeze",
                    summary = "₹${formatDouble(totalWorkingCapitalLocked)} locked across unsold inventory (₹${formatDouble(totalCapitalLockedInStock)}) and customer khata (₹${formatDouble(totalOutstandingReceivables)}).",
                    detailedExplanation = "Liquidity is simultaneously constrained on two fronts: cash is tied up in slow/dead inventory on shelves AND waiting to be collected from customers. This restricts operating cash flow for new inventory purchases.",
                    contributingSignals = listOf(
                        "Inventory: ₹${formatDouble(totalCapitalLockedInStock)} locked in slow & dead stock",
                        "Collections: ₹${formatDouble(totalOutstandingReceivables)} outstanding from customers"
                    ),
                    entityId = null,
                    entityName = null,
                    entityType = CrossInsightEntityType.BUSINESS_WIDE,
                    metricValue = safeValue(totalWorkingCapitalLocked),
                    metricUnit = "₹",
                    recommendedStrategy = "Pair dead stock clearance sales with an active collection drive to unlock working cash."
                )
            )
        }

        // 4. MULTI-SIGNAL CONVERGENCE (Alerts + Actions reinforcing same product or customer)
        val productAlertCount = businessAlerts.filter { it.productId != null }.groupBy { it.productId!! }
        val productActionCount = businessActions.filter { it.targetId != null }.groupBy { it.targetId!! }

        for ((productId, pAlerts) in productAlertCount) {
            val pActions = productActionCount[productId] ?: emptyList()
            // If an item has >= 2 alerts or an alert + action reinforcing it
            if (pAlerts.size >= 2 || (pAlerts.isNotEmpty() && pActions.isNotEmpty())) {
                val productName = pAlerts.firstOrNull()?.productName ?: pActions.firstOrNull()?.targetName ?: "Product"
                val signals = mutableListOf<String>()
                pAlerts.take(2).forEach { signals.add("Alert: ${it.title}") }
                pActions.take(2).forEach { signals.add("Action: ${it.title}") }

                insights.add(
                    CrossBusinessInsight(
                        id = "insight_convergence_product_$productId",
                        category = CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE,
                        severity = CrossInsightSeverity.HIGH,
                        title = "Multi-Signal Focus: $productName",
                        summary = "$productName is flagged by ${pAlerts.size} alerts and ${pActions.size} business actions.",
                        detailedExplanation = "Multiple independent intelligence engines are simultaneously pointing to $productName, indicating this product requires immediate operational attention.",
                        contributingSignals = signals,
                        entityId = productId,
                        entityName = productName,
                        entityType = CrossInsightEntityType.PRODUCT,
                        metricValue = (pAlerts.size + pActions.size).toDouble(),
                        metricUnit = "signals",
                        recommendedStrategy = "Review both inventory stocking and pricing for this item together."
                    )
                )
            }
        }

        // Deduplication by ID
        val deduplicated = insights.distinctBy { it.id }

        // Deterministic sorting:
        // 1. Severity: CRITICAL (0), HIGH (1), MEDIUM (2), POSITIVE (3)
        // 2. Category name
        // 3. Entity Name (or "")
        // 4. ID
        val sortedInsights = deduplicated.sortedWith(
            compareBy<CrossBusinessInsight> { insight ->
                when (insight.severity) {
                    CrossInsightSeverity.CRITICAL -> 0
                    CrossInsightSeverity.HIGH -> 1
                    CrossInsightSeverity.MEDIUM -> 2
                    CrossInsightSeverity.POSITIVE -> 3
                }
            }.thenBy { it.category.name }
             .thenBy { it.entityName ?: "" }
             .thenBy { it.id }
        )

        val criticalCount = sortedInsights.count { it.severity == CrossInsightSeverity.CRITICAL }
        val highCount = sortedInsights.count { it.severity == CrossInsightSeverity.HIGH }
        val mediumCount = sortedInsights.count { it.severity == CrossInsightSeverity.MEDIUM }
        val positiveCount = sortedInsights.count { it.severity == CrossInsightSeverity.POSITIVE }

        return CrossBusinessInsightsSummary(
            totalInsights = sortedInsights.size,
            criticalInsightsCount = criticalCount,
            highInsightsCount = highCount,
            mediumInsightsCount = mediumCount,
            positiveInsightsCount = positiveCount,
            workingCapitalLockedTotal = totalWorkingCapitalLocked,
            insights = sortedInsights
        )
    }

    private fun isValidNumber(v: Double?): Boolean {
        return v != null && !v.isNaN() && !v.isInfinite()
    }

    private fun safeValue(v: Double?): Double? {
        return if (isValidNumber(v)) v else null
    }

    private fun formatDouble(v: Double?): String {
        return if (isValidNumber(v)) {
            String.format(Locale.US, "%.1f", v)
        } else {
            "0.0"
        }
    }

    private fun formatPercent(v: Double?): String {
        return if (isValidNumber(v)) {
            String.format(Locale.US, "%.1f%%", v)
        } else {
            "0.0%"
        }
    }

    private fun formatCoverage(days: Double?): String {
        return if (days != null && days.isFinite()) {
            "~${String.format(Locale.US, "%.1f", days)} days left"
        } else {
            "low coverage"
        }
    }
}
