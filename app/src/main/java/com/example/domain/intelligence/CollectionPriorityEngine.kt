package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Sale

/**
 * Deterministic collection review priority level.
 * Categorizes accounts for business review priority without predicting credit risk or repayment probability.
 */
enum class CollectionPriorityLevel {
    HIGH,
    MEDIUM,
    LOW,
    NONE,
    INSUFFICIENT_DATA
}

/**
 * Deterministic result containing measurable collection-review indicators for a customer.
 */
data class CollectionPriorityResult(
    val customerId: String,
    val customerName: String,
    val phone: String,
    val outstandingAmount: Double,
    val priority: CollectionPriorityLevel,
    val reasons: List<String>,
    val daysSinceLastPurchase: Long?,
    val activityStatus: CustomerActivityStatus,
    val segment: CustomerSegment,
    val lifetimePurchases: Double,
    val lifetimeOrders: Int,
    val totalPaid: Double
)

/**
 * Aggregated summary of collection review priorities across all analyzed accounts.
 */
data class CollectionPrioritySummary(
    val customersAnalyzed: Int,
    val customersWithOutstanding: Int,
    val highPriorityCount: Int,
    val mediumPriorityCount: Int,
    val lowPriorityCount: Int,
    val noneCount: Int,
    val insufficientDataCount: Int,
    val totalOutstanding: Double,
    val results: List<CollectionPriorityResult>
)

/**
 * Smart Collection Priority Engine
 *
 * Deterministic, rule-based analytics layer that consumes CustomerIntelligenceEngine metrics
 * and existing Customer outstanding amounts to prioritize accounts for business review.
 *
 * Does not calculate credit scores or default probabilities.
 */
object CollectionPriorityEngine {

    /**
     * Centralized, documented review thresholds.
     */
    object Thresholds {
        // High outstanding threshold in ₹
        const val HIGH_OUTSTANDING_AMOUNT = 5000.0

        // Moderate outstanding threshold in ₹
        const val MODERATE_OUTSTANDING_AMOUNT = 1000.0

        // Inactivity threshold (days since last transaction/purchase)
        const val INACTIVE_DAYS_THRESHOLD = 45L

        // Moderate inactivity threshold
        const val MODERATE_INACTIVE_DAYS_THRESHOLD = 20L
    }

    /**
     * Standard deterministic explanation reasons.
     */
    object Reasons {
        const val NO_OUTSTANDING = "No outstanding due on account"
        const val INSUFFICIENT_HISTORY = "Insufficient transaction history available"
        const val HIGH_DUE_INACTIVE = "Substantial outstanding due with prolonged inactivity"
        const val HIGH_DUE_ACTIVE = "Substantial outstanding due requires credit limit review"
        const val MODERATE_DUE_INACTIVE = "Outstanding balance with no recent purchases"
        const val MODERATE_DUE_ACTIVE = "Outstanding balance within standard operating activity"
        const val LOW_DUE = "Nominal outstanding balance under standard review threshold"
    }

    /**
     * Evaluate collection review priorities by analyzing customers and sales through CustomerIntelligenceEngine.
     */
    fun evaluate(
        customers: List<Customer>,
        sales: List<Sale>,
        currentTime: Long = System.currentTimeMillis()
    ): CollectionPrioritySummary {
        if (customers.isEmpty()) {
            return CollectionPrioritySummary(
                customersAnalyzed = 0,
                customersWithOutstanding = 0,
                highPriorityCount = 0,
                mediumPriorityCount = 0,
                lowPriorityCount = 0,
                noneCount = 0,
                insufficientDataCount = 0,
                totalOutstanding = 0.0,
                results = emptyList()
            )
        }

        // Reusing CustomerIntelligenceEngine for deterministic customer metrics
        val intelligenceSummary = CustomerIntelligenceEngine.analyze(
            customers = customers,
            sales = sales,
            currentTime = currentTime
        )

        return evaluateFromMetrics(intelligenceSummary.customers)
    }

    /**
     * Evaluate directly from pre-calculated CustomerMetrics (avoids recalculation).
     */
    fun evaluateFromMetrics(metricsList: List<CustomerMetrics>): CollectionPrioritySummary {
        if (metricsList.isEmpty()) {
            return CollectionPrioritySummary(
                customersAnalyzed = 0,
                customersWithOutstanding = 0,
                highPriorityCount = 0,
                mediumPriorityCount = 0,
                lowPriorityCount = 0,
                noneCount = 0,
                insufficientDataCount = 0,
                totalOutstanding = 0.0,
                results = emptyList()
            )
        }

        val results = metricsList.map { metric ->
            evaluateCustomer(metric)
        }.sortedWith(
            compareBy<CollectionPriorityResult> { priorityRank(it.priority) }
                .thenByDescending { it.outstandingAmount }
        )

        val totalOutstanding = results.sumOf { if (it.outstandingAmount.isFinite()) it.outstandingAmount else 0.0 }
        val withOutstanding = results.count { it.outstandingAmount > 0.0 }

        val highCount = results.count { it.priority == CollectionPriorityLevel.HIGH }
        val mediumCount = results.count { it.priority == CollectionPriorityLevel.MEDIUM }
        val lowCount = results.count { it.priority == CollectionPriorityLevel.LOW }
        val noneCount = results.count { it.priority == CollectionPriorityLevel.NONE }
        val insufficientCount = results.count { it.priority == CollectionPriorityLevel.INSUFFICIENT_DATA }

        return CollectionPrioritySummary(
            customersAnalyzed = results.size,
            customersWithOutstanding = withOutstanding,
            highPriorityCount = highCount,
            mediumPriorityCount = mediumCount,
            lowPriorityCount = lowCount,
            noneCount = noneCount,
            insufficientDataCount = insufficientCount,
            totalOutstanding = totalOutstanding,
            results = results
        )
    }

    private fun evaluateCustomer(metric: CustomerMetrics): CollectionPriorityResult {
        val safeOutstanding = if (metric.outstandingAmount.isFinite() && metric.outstandingAmount > 0.0) {
            metric.outstandingAmount
        } else {
            0.0
        }

        val reasons = mutableListOf<String>()

        // Rule 1: No outstanding due
        if (safeOutstanding <= 0.0) {
            reasons.add(Reasons.NO_OUTSTANDING)
            return CollectionPriorityResult(
                customerId = metric.customerId,
                customerName = metric.customerName,
                phone = metric.phone,
                outstandingAmount = 0.0,
                priority = CollectionPriorityLevel.NONE,
                reasons = reasons,
                daysSinceLastPurchase = metric.daysSinceLastPurchase,
                activityStatus = metric.activityStatus,
                segment = metric.segment,
                lifetimePurchases = if (metric.lifetimePurchases.isFinite()) metric.lifetimePurchases else 0.0,
                lifetimeOrders = metric.lifetimeOrders,
                totalPaid = if (metric.totalPaid.isFinite()) metric.totalPaid else 0.0
            )
        }

        // Rule 2: Insufficient data check (e.g. invalid state or no data at all)
        if (!metric.hasSufficientData && metric.lifetimeOrders == 0) {
            reasons.add(Reasons.INSUFFICIENT_HISTORY)
            // Even with insufficient history, if outstanding is high, flag for moderate attention, otherwise INSUFFICIENT_DATA
            val priority = if (safeOutstanding >= Thresholds.HIGH_OUTSTANDING_AMOUNT) {
                CollectionPriorityLevel.MEDIUM
            } else {
                CollectionPriorityLevel.INSUFFICIENT_DATA
            }
            return CollectionPriorityResult(
                customerId = metric.customerId,
                customerName = metric.customerName,
                phone = metric.phone,
                outstandingAmount = safeOutstanding,
                priority = priority,
                reasons = reasons,
                daysSinceLastPurchase = metric.daysSinceLastPurchase,
                activityStatus = metric.activityStatus,
                segment = metric.segment,
                lifetimePurchases = if (metric.lifetimePurchases.isFinite()) metric.lifetimePurchases else 0.0,
                lifetimeOrders = metric.lifetimeOrders,
                totalPaid = if (metric.totalPaid.isFinite()) metric.totalPaid else 0.0
            )
        }

        val daysSinceLast = metric.daysSinceLastPurchase ?: 0L
        val isInactive = metric.activityStatus == CustomerActivityStatus.INACTIVE ||
                daysSinceLast >= Thresholds.INACTIVE_DAYS_THRESHOLD

        // Rule 3: High Priority
        // High outstanding AND inactive/prolonged absence
        val isHighOutstanding = safeOutstanding >= Thresholds.HIGH_OUTSTANDING_AMOUNT
        val isModerateOutstanding = safeOutstanding >= Thresholds.MODERATE_OUTSTANDING_AMOUNT

        val priority: CollectionPriorityLevel
        when {
            isHighOutstanding && isInactive -> {
                priority = CollectionPriorityLevel.HIGH
                reasons.add(Reasons.HIGH_DUE_INACTIVE)
            }
            isHighOutstanding -> {
                priority = CollectionPriorityLevel.HIGH
                reasons.add(Reasons.HIGH_DUE_ACTIVE)
            }
            isModerateOutstanding && isInactive -> {
                priority = CollectionPriorityLevel.MEDIUM
                reasons.add(Reasons.MODERATE_DUE_INACTIVE)
            }
            isModerateOutstanding -> {
                priority = CollectionPriorityLevel.LOW
                reasons.add(Reasons.MODERATE_DUE_ACTIVE)
            }
            else -> {
                priority = CollectionPriorityLevel.LOW
                reasons.add(Reasons.LOW_DUE)
            }
        }

        return CollectionPriorityResult(
            customerId = metric.customerId,
            customerName = metric.customerName,
            phone = metric.phone,
            outstandingAmount = safeOutstanding,
            priority = priority,
            reasons = reasons,
            daysSinceLastPurchase = metric.daysSinceLastPurchase,
            activityStatus = metric.activityStatus,
            segment = metric.segment,
            lifetimePurchases = if (metric.lifetimePurchases.isFinite()) metric.lifetimePurchases else 0.0,
            lifetimeOrders = metric.lifetimeOrders,
            totalPaid = if (metric.totalPaid.isFinite()) metric.totalPaid else 0.0
        )
    }

    private fun priorityRank(level: CollectionPriorityLevel): Int = when (level) {
        CollectionPriorityLevel.HIGH -> 1
        CollectionPriorityLevel.MEDIUM -> 2
        CollectionPriorityLevel.LOW -> 3
        CollectionPriorityLevel.NONE -> 4
        CollectionPriorityLevel.INSUFFICIENT_DATA -> 5
    }
}
