package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.domain.model.Sale
import java.util.Locale

/**
 * Priority levels for business actions based on operational urgency and financial impact.
 */
enum class BusinessActionPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Operational categories for shopkeeper business actions.
 */
enum class BusinessActionCategory {
    RESTOCK,
    COLLECT_PAYMENT,
    CLEAR_STOCK,
    REVIEW_PRICING,
    REENGAGE_CUSTOMER
}

/**
 * An actionable item for the business owner derived deterministically from existing intelligence engines.
 */
data class BusinessAction(
    val actionId: String,
    val priority: BusinessActionPriority,
    val category: BusinessActionCategory,
    val title: String,
    val description: String,
    val targetId: String? = null,
    val targetName: String? = null,
    val supportingMetric: String? = null,
    val metricValue: Double? = null,
    val metricUnit: String? = null,
    val actionLabel: String,
    val destinationRoute: String? = null
)

/**
 * Aggregated summary of today's business actions for the shopkeeper.
 */
data class BusinessActionSummary(
    val totalActions: Int,
    val criticalActionsCount: Int,
    val highActionsCount: Int,
    val mediumActionsCount: Int,
    val lowActionsCount: Int,
    val topActions: List<BusinessAction>,
    val allActions: List<BusinessAction>
)

/**
 * Today's Business Actions Engine
 *
 * Consolidates outputs from existing domain intelligence engines:
 * - InventoryIntelligenceEngine
 * - ProductProfitabilityEngine
 * - CustomerIntelligenceEngine
 * - CollectionPriorityEngine
 * - BusinessAlertEngine
 *
 * Produces a concise, deduplicated, and prioritized list of actionable items for the shopkeeper.
 *
 * Deterministic, offline, safe against NaN/Infinity, and zero external side effects.
 */
object BusinessActionEngine {

    const val DEFAULT_MAX_ACTIONS = 10

    object Thresholds {
        const val LOW_STOCK_DAYS_THRESHOLD = 3.0
        const val HIGH_COLLECTION_AMOUNT = 5000.0
        const val HIGH_DEAD_STOCK_CAPITAL = 2000.0
        const val CHURN_INACTIVE_DAYS = 45L
        const val LOSS_MAKING_MARGIN = 0.0
        const val LOW_MARGIN_THRESHOLD = 15.0
    }

    /**
     * Evaluates business actions from raw entities by delegating to existing intelligence engines.
     */
    fun generateActions(
        products: List<Product> = emptyList(),
        sales: List<Sale> = emptyList(),
        customers: List<Customer> = emptyList(),
        currentTime: Long = System.currentTimeMillis(),
        maxActions: Int = DEFAULT_MAX_ACTIONS
    ): BusinessActionSummary {
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

        return generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            businessAlerts = businessAlerts,
            maxActions = maxActions
        )
    }

    /**
     * Evaluates actions directly from pre-computed intelligence metrics, avoiding duplicate computations.
     */
    fun generateFromIntelligence(
        inventorySummary: InventoryIntelligenceSummary? = null,
        inventoryMetrics: List<InventoryProductMetrics> = inventorySummary?.products ?: emptyList(),
        profitabilitySummary: ProductProfitabilitySummary? = null,
        profitabilityMetrics: List<ProductProfitabilityMetrics> = profitabilitySummary?.products ?: emptyList(),
        collectionSummary: CollectionPrioritySummary? = null,
        collectionResults: List<CollectionPriorityResult> = collectionSummary?.results ?: emptyList(),
        customerSummary: CustomerIntelligenceSummary? = null,
        customerMetrics: List<CustomerMetrics> = customerSummary?.customers ?: emptyList(),
        businessAlerts: List<BusinessAlert> = emptyList(),
        maxActions: Int = DEFAULT_MAX_ACTIONS
    ): BusinessActionSummary {
        val actions = mutableListOf<BusinessAction>()

        // 1. RESTOCK ACTIONS (From Inventory Intelligence)
        for (item in inventoryMetrics) {
            // Dead stock should never be restocked
            if (item.isDeadStock) continue

            if (item.currentStock <= 0) {
                // Out of stock with past sales activity
                if (item.unitsSold > 0 || (isValidNumber(item.salesVelocityPerDay) && item.salesVelocityPerDay > 0.0)) {
                    actions.add(
                        BusinessAction(
                            actionId = "action_restock_oos_${item.productId}",
                            priority = BusinessActionPriority.CRITICAL,
                            category = BusinessActionCategory.RESTOCK,
                            title = "Restock ${item.productName}",
                            description = "${item.productName} is completely out of stock with active customer demand.",
                            targetId = item.productId,
                            targetName = item.productName,
                            supportingMetric = "Stock: 0 units (Velocity: ${formatDouble(item.salesVelocityPerDay)}/day)",
                            metricValue = safeMetricValue(item.salesVelocityPerDay),
                            metricUnit = "units/day",
                            actionLabel = "Order Stock",
                            destinationRoute = "inventory"
                        )
                    )
                }
            } else if (item.hasSufficientData) {
                val stockRemainingDays = item.daysOfStockRemaining
                val isCoverageCriticallyLow = stockRemainingDays != null &&
                        stockRemainingDays.isFinite() &&
                        stockRemainingDays <= Thresholds.LOW_STOCK_DAYS_THRESHOLD
                val isBelowMinStock = item.currentStock <= item.minimumStock && item.minimumStock > 0

                if (isCoverageCriticallyLow || isBelowMinStock) {
                    val daysText = if (stockRemainingDays != null && stockRemainingDays.isFinite()) {
                        "~${formatDouble(stockRemainingDays)} days left"
                    } else {
                        "below min threshold (${item.minimumStock})"
                    }
                    actions.add(
                        BusinessAction(
                            actionId = "action_restock_low_${item.productId}",
                            priority = BusinessActionPriority.HIGH,
                            category = BusinessActionCategory.RESTOCK,
                            title = "Reorder ${item.productName}",
                            description = "Only ${item.currentStock} units remaining ($daysText). Restock before running out.",
                            targetId = item.productId,
                            targetName = item.productName,
                            supportingMetric = "Stock: ${item.currentStock} units",
                            metricValue = safeMetricValue(item.currentStock.toDouble()),
                            metricUnit = "units",
                            actionLabel = "Reorder",
                            destinationRoute = "inventory"
                        )
                    )
                }
            }
        }

        // 2. PAYMENT COLLECTION ACTIONS (From Collection Priority Engine)
        val customersWithCriticalCollection = mutableSetOf<String>()
        for (col in collectionResults) {
            if (col.outstandingAmount <= 0.0 || !col.outstandingAmount.isFinite()) continue

            if (col.priority == CollectionPriorityLevel.HIGH) {
                customersWithCriticalCollection.add(col.customerId)
                val primaryReason = col.reasons.firstOrNull() ?: "Substantial outstanding balance"
                val isOverdueProlonged = col.daysSinceLastPurchase != null && col.daysSinceLastPurchase >= Thresholds.CHURN_INACTIVE_DAYS
                val isVeryLargeDue = col.outstandingAmount >= Thresholds.HIGH_COLLECTION_AMOUNT

                val priority = if (isOverdueProlonged || isVeryLargeDue) {
                    BusinessActionPriority.CRITICAL
                } else {
                    BusinessActionPriority.HIGH
                }

                actions.add(
                    BusinessAction(
                        actionId = "action_collect_${col.customerId}",
                        priority = priority,
                        category = BusinessActionCategory.COLLECT_PAYMENT,
                        title = "Collect ₹${formatAmount(col.outstandingAmount)} from ${col.customerName}",
                        description = primaryReason,
                        targetId = col.customerId,
                        targetName = col.customerName,
                        supportingMetric = "Overdue: ₹${formatAmount(col.outstandingAmount)}",
                        metricValue = safeMetricValue(col.outstandingAmount),
                        metricUnit = "₹",
                        actionLabel = "Collect Payment",
                        destinationRoute = "khata"
                    )
                )
            } else if (col.priority == CollectionPriorityLevel.MEDIUM) {
                val primaryReason = col.reasons.firstOrNull() ?: "Outstanding balance requires routine review"
                actions.add(
                    BusinessAction(
                        actionId = "action_collect_${col.customerId}",
                        priority = BusinessActionPriority.MEDIUM,
                        category = BusinessActionCategory.COLLECT_PAYMENT,
                        title = "Follow up with ${col.customerName}",
                        description = primaryReason,
                        targetId = col.customerId,
                        targetName = col.customerName,
                        supportingMetric = "Due: ₹${formatAmount(col.outstandingAmount)}",
                        metricValue = safeMetricValue(col.outstandingAmount),
                        metricUnit = "₹",
                        actionLabel = "View Khata",
                        destinationRoute = "khata"
                    )
                )
            }
        }

        // 3. CLEAR DEAD STOCK ACTIONS (From Inventory Metrics)
        for (item in inventoryMetrics) {
            if (item.isDeadStock && item.currentStock > 0 && isValidNumber(item.capitalLocked) && item.capitalLocked > 0.0) {
                val priority = if (item.capitalLocked >= Thresholds.HIGH_DEAD_STOCK_CAPITAL) {
                    BusinessActionPriority.HIGH
                } else {
                    BusinessActionPriority.MEDIUM
                }
                actions.add(
                    BusinessAction(
                        actionId = "action_clear_stock_${item.productId}",
                        priority = priority,
                        category = BusinessActionCategory.CLEAR_STOCK,
                        title = "Clear Dead Stock: ${item.productName}",
                        description = "${item.currentStock} units unsold with ₹${formatAmount(item.capitalLocked)} capital locked. Consider a discount or promotional bundle.",
                        targetId = item.productId,
                        targetName = item.productName,
                        supportingMetric = "Capital Locked: ₹${formatAmount(item.capitalLocked)}",
                        metricValue = safeMetricValue(item.capitalLocked),
                        metricUnit = "₹",
                        actionLabel = "Clear Stock",
                        destinationRoute = "inventory"
                    )
                )
            }
        }

        // 4. REVIEW PRICING ACTIONS (From Product Profitability)
        for (profit in profitabilityMetrics) {
            if (profit.marginCategory == MarginCategory.NO_PROFIT && profit.unitsSold > 0) {
                // Loss-making product
                val marginStr = if (isValidNumber(profit.grossMarginPercentage)) {
                    "${formatDouble(profit.grossMarginPercentage)}%"
                } else "Negative"
                actions.add(
                    BusinessAction(
                        actionId = "action_price_loss_${profit.productId}",
                        priority = BusinessActionPriority.CRITICAL,
                        category = BusinessActionCategory.REVIEW_PRICING,
                        title = "Fix Loss-Making Price: ${profit.productName}",
                        description = "Selling at a loss (Gross Margin: $marginStr). Cost price (₹${formatAmount(profit.averageCostPerUnit)}) exceeds selling price.",
                        targetId = profit.productId,
                        targetName = profit.productName,
                        supportingMetric = "Gross Margin: $marginStr",
                        metricValue = safeMetricValue(profit.grossMarginPercentage),
                        metricUnit = "%",
                        actionLabel = "Adjust Price",
                        destinationRoute = "products"
                    )
                )
            } else if (profit.marginCategory == MarginCategory.LOW_MARGIN && profit.unitsSold >= 5) {
                // Low margin on moderate/high volume
                val marginStr = if (isValidNumber(profit.grossMarginPercentage)) {
                    "${formatDouble(profit.grossMarginPercentage)}%"
                } else "Low"
                actions.add(
                    BusinessAction(
                        actionId = "action_price_low_${profit.productId}",
                        priority = BusinessActionPriority.MEDIUM,
                        category = BusinessActionCategory.REVIEW_PRICING,
                        title = "Review Margins: ${profit.productName}",
                        description = "Low margin ($marginStr) despite healthy volume (${profit.unitsSold} units sold). Check supplier cost or adjust pricing.",
                        targetId = profit.productId,
                        targetName = profit.productName,
                        supportingMetric = "Gross Margin: $marginStr",
                        metricValue = safeMetricValue(profit.grossMarginPercentage),
                        metricUnit = "%",
                        actionLabel = "Review Price",
                        destinationRoute = "products"
                    )
                )
            }
        }

        // 5. RE-ENGAGE CUSTOMER ACTIONS (From Customer Intelligence)
        for (cust in customerMetrics) {
            // Only re-engage valuable customers who are inactive and NOT already flagged for collection review
            if (customersWithCriticalCollection.contains(cust.customerId)) continue

            val isValuable = cust.segment == CustomerSegment.HIGH_VALUE ||
                    cust.segment == CustomerSegment.FREQUENT ||
                    cust.segment == CustomerSegment.REGULAR

            if (isValuable && cust.activityStatus == CustomerActivityStatus.INACTIVE) {
                val daysText = cust.daysSinceLastPurchase?.let { "$it days" } ?: "extended period"
                actions.add(
                    BusinessAction(
                        actionId = "action_reengage_${cust.customerId}",
                        priority = BusinessActionPriority.MEDIUM,
                        category = BusinessActionCategory.REENGAGE_CUSTOMER,
                        title = "Re-engage ${cust.customerName}",
                        description = "Valuable customer has not visited in $daysText. Reach out with a personalized greeting or offer.",
                        targetId = cust.customerId,
                        targetName = cust.customerName,
                        supportingMetric = "Last Visit: $daysText ago",
                        metricValue = safeMetricValue(cust.daysSinceLastPurchase?.toDouble()),
                        metricUnit = "days",
                        actionLabel = "Contact",
                        destinationRoute = "khata"
                    )
                )
            }
        }

        // Deduplicate by actionId (preserving first occurrence)
        val deduplicated = actions.distinctBy { it.actionId }

        // Deterministic sorting:
        // 1. Priority rank (CRITICAL=0, HIGH=1, MEDIUM=2, LOW=3)
        // 2. Metric magnitude descending (higher financial/volume impact first)
        // 3. Alphabetical tie-breaking on actionId
        val sortedActions = deduplicated.sortedWith(
            compareBy<BusinessAction> { priorityRank(it.priority) }
                .thenByDescending { it.metricValue?.let { v -> if (v.isFinite()) Math.abs(v) else 0.0 } ?: 0.0 }
                .thenBy { it.actionId }
        )

        val criticalCount = sortedActions.count { it.priority == BusinessActionPriority.CRITICAL }
        val highCount = sortedActions.count { it.priority == BusinessActionPriority.HIGH }
        val mediumCount = sortedActions.count { it.priority == BusinessActionPriority.MEDIUM }
        val lowCount = sortedActions.count { it.priority == BusinessActionPriority.LOW }

        val cappedLimit = if (maxActions > 0) maxActions else DEFAULT_MAX_ACTIONS
        val topActions = sortedActions.take(cappedLimit)

        return BusinessActionSummary(
            totalActions = sortedActions.size,
            criticalActionsCount = criticalCount,
            highActionsCount = highCount,
            mediumActionsCount = mediumCount,
            lowActionsCount = lowCount,
            topActions = topActions,
            allActions = sortedActions
        )
    }

    private fun priorityRank(priority: BusinessActionPriority): Int = when (priority) {
        BusinessActionPriority.CRITICAL -> 0
        BusinessActionPriority.HIGH -> 1
        BusinessActionPriority.MEDIUM -> 2
        BusinessActionPriority.LOW -> 3
    }

    private fun isValidNumber(value: Double?): Boolean {
        return value != null && !value.isNaN() && !value.isInfinite()
    }

    private fun safeMetricValue(value: Double?): Double? {
        return if (value != null && value.isFinite()) value else null
    }

    private fun formatAmount(amount: Double): String {
        return if (amount.isFinite()) {
            if (amount % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", amount)
            } else {
                String.format(Locale.US, "%.2f", amount)
            }
        } else "0"
    }

    private fun formatDouble(value: Double): String {
        return if (value.isFinite()) {
            String.format(Locale.US, "%.1f", value)
        } else "0"
    }
}
