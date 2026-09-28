package com.example.domain.intelligence

import java.util.Locale

/**
 * Deterministic direction of net profit change between two periods.
 */
enum class ProfitChangeDirection {
    EXPANSION,
    CONTRACTION,
    LOSS,
    NO_CHANGE
}

/**
 * Structured reason code for deterministic profit explanation.
 */
enum class ProfitExplanationReasonCode {
    REVENUE_DRIVEN_EXPANSION,
    COST_REDUCTION_EXPANSION,
    EXPENSE_REDUCTION_EXPANSION,
    REVENUE_DECLINE_CONTRACTION,
    COST_INCREASE_CONTRACTION,
    EXPENSE_INCREASE_CONTRACTION,
    MIXED_DRIVERS,
    NO_CHANGE,
    DATA_INCONSISTENT
}

/**
 * Authoritative financial period snapshot input.
 *
 * All financial values are pre-computed authoritative values supplied by the caller.
 * The engine does not perform independent queries or duplicate P&L aggregations.
 */
data class ProfitPeriodSnapshot(
    val periodLabel: String,
    val revenue: Double,
    val cogs: Double,
    val operatingExpenses: Double,
    val grossProfit: Double,
    val netProfit: Double
)

/**
 * Product-level revenue driver attribution input.
 */
data class ProductRevenueDriver(
    val productId: String,
    val productName: String,
    val previousRevenue: Double,
    val currentRevenue: Double,
    val revenueChange: Double = currentRevenue - previousRevenue
)

/**
 * Product-level cost driver attribution input.
 */
data class ProductCostDriver(
    val productId: String,
    val productName: String,
    val previousCOGS: Double,
    val currentCOGS: Double,
    val cogsChange: Double = currentCOGS - previousCOGS
)

/**
 * Operating expense category driver attribution input.
 */
data class ExpenseCategoryDriver(
    val category: String,
    val previousAmount: Double,
    val currentAmount: Double,
    val expenseChange: Double = currentAmount - previousAmount
)

/**
 * Driver attribution summary identifying top positive and negative contributors.
 */
data class ProfitDriverAttribution(
    val topPositiveRevenueDriver: ProductRevenueDriver? = null,
    val topNegativeRevenueDriver: ProductRevenueDriver? = null,
    val topCostIncreaseDriver: ProductCostDriver? = null,
    val topCostReductionDriver: ProductCostDriver? = null,
    val topExpenseIncreaseDriver: ExpenseCategoryDriver? = null,
    val topExpenseReductionDriver: ExpenseCategoryDriver? = null
)

/**
 * Immutable explanation of profit change between two authoritative period snapshots.
 */
data class ProfitChangeExplanation(
    val previousPeriodLabel: String,
    val currentPeriodLabel: String,
    val previousNetProfit: Double,
    val currentNetProfit: Double,
    val netProfitChange: Double,
    val changeDirection: ProfitChangeDirection,
    val revenueContribution: Double,
    val cogsContribution: Double,
    val expenseContribution: Double,
    val contributionTotal: Double,
    val isConsistent: Boolean,
    val reconciliationTolerance: Double,
    val reasonCode: ProfitExplanationReasonCode,
    val primaryDriverSummary: String,
    val driverAttribution: ProfitDriverAttribution,
    val notes: List<String> = emptyList()
)

/**
 * Pure, deterministic domain engine for explaining business profit changes.
 *
 * Rules:
 * - Pure Kotlin domain logic.
 * - Zero Room, DAO, SharedPreferences, or MockDataStore queries.
 * - Zero database writes or schema modifications.
 * - Zero LLM or Gemini API calls.
 * - Reconciles: Net Profit Change = Revenue Contribution + COGS Contribution + Expense Contribution.
 * - Safe against NaN, Infinity, zero divisions, and negative zero.
 */
object ProfitExplainabilityEngine {

    const val RECONCILIATION_TOLERANCE = 0.05
    private const val ZERO_THRESHOLD = 0.0001

    /**
     * Explains the mathematical and driver-level changes between two authoritative P&L period snapshots.
     *
     * @param previous Period snapshot representing the baseline.
     * @param current Period snapshot representing the comparison period.
     * @param revenueDrivers Optional product-level revenue driver inputs.
     * @param costDrivers Optional product-level COGS driver inputs.
     * @param expenseDrivers Optional category-level expense driver inputs.
     */
    fun explain(
        previous: ProfitPeriodSnapshot,
        current: ProfitPeriodSnapshot,
        revenueDrivers: List<ProductRevenueDriver> = emptyList(),
        costDrivers: List<ProductCostDriver> = emptyList(),
        expenseDrivers: List<ExpenseCategoryDriver> = emptyList()
    ): ProfitChangeExplanation {
        // Sanitize numbers against NaN/Infinity
        val prevRev = sanitize(previous.revenue)
        val currRev = sanitize(current.revenue)
        val prevCogs = sanitize(previous.cogs)
        val currCogs = sanitize(current.cogs)
        val prevExp = sanitize(previous.operatingExpenses)
        val currExp = sanitize(current.operatingExpenses)
        val prevNet = sanitize(previous.netProfit)
        val currNet = sanitize(current.netProfit)

        // Mathematical contributions to Net Profit:
        // Net Profit = Revenue - COGS - Expenses
        // ΔNetProfit = ΔRevenue - ΔCOGS - ΔExpenses
        val revenueContribution = round2(currRev - prevRev)
        val cogsContribution = round2(-(currCogs - prevCogs)) // Cost increase reduces profit
        val expenseContribution = round2(-(currExp - prevExp)) // Expense increase reduces profit
        val contributionTotal = round2(revenueContribution + cogsContribution + expenseContribution)

        val netProfitChange = round2(currNet - prevNet)
        val reconciliationDiff = Math.abs(contributionTotal - netProfitChange)
        val isConsistent = reconciliationDiff <= RECONCILIATION_TOLERANCE

        // Determine profit direction
        val changeDirection = when {
            currNet < -ZERO_THRESHOLD -> ProfitChangeDirection.LOSS
            Math.abs(netProfitChange) <= ZERO_THRESHOLD -> ProfitChangeDirection.NO_CHANGE
            netProfitChange > ZERO_THRESHOLD -> ProfitChangeDirection.EXPANSION
            else -> ProfitChangeDirection.CONTRACTION
        }

        // Driver Attribution
        val attribution = evaluateDrivers(revenueDrivers, costDrivers, expenseDrivers)

        // Determine Primary Reason Code & Deterministic Narrative
        val (reasonCode, narrative) = determineReasonAndNarrative(
            isConsistent = isConsistent,
            changeDirection = changeDirection,
            netProfitChange = netProfitChange,
            revenueContribution = revenueContribution,
            cogsContribution = cogsContribution,
            expenseContribution = expenseContribution
        )

        val notes = mutableListOf<String>()
        notes.add("Financial returns and refunds are outside the current ledger scope and not modeled.")
        if (!isConsistent) {
            notes.add(
                String.format(
                    Locale.US,
                    "Supplied period metrics do not reconcile within tolerance (diff: ₹%.2f). Verification recommended.",
                    reconciliationDiff
                )
            )
        }

        return ProfitChangeExplanation(
            previousPeriodLabel = previous.periodLabel,
            currentPeriodLabel = current.periodLabel,
            previousNetProfit = prevNet,
            currentNetProfit = currNet,
            netProfitChange = netProfitChange,
            changeDirection = changeDirection,
            revenueContribution = revenueContribution,
            cogsContribution = cogsContribution,
            expenseContribution = expenseContribution,
            contributionTotal = contributionTotal,
            isConsistent = isConsistent,
            reconciliationTolerance = RECONCILIATION_TOLERANCE,
            reasonCode = reasonCode,
            primaryDriverSummary = narrative,
            driverAttribution = attribution,
            notes = notes
        )
    }

    private fun evaluateDrivers(
        revenueDrivers: List<ProductRevenueDriver>,
        costDrivers: List<ProductCostDriver>,
        expenseDrivers: List<ExpenseCategoryDriver>
    ): ProfitDriverAttribution {
        // Aggregate any duplicate revenue drivers by productId deterministically
        val consolidatedRevenue = revenueDrivers
            .filter { isValidNumber(it.previousRevenue) && isValidNumber(it.currentRevenue) }
            .groupBy { it.productId }
            .map { (pid, list) ->
                val name = list.firstOrNull { it.productName.isNotBlank() }?.productName ?: pid
                val prev = list.sumOf { sanitize(it.previousRevenue) }
                val curr = list.sumOf { sanitize(it.currentRevenue) }
                ProductRevenueDriver(pid, name, prev, curr, curr - prev)
            }

        // Positive revenue drivers: sorted descending by revenueChange, tie-breaker productId ascending
        val topPosRev = consolidatedRevenue
            .filter { it.revenueChange > ZERO_THRESHOLD }
            .sortedWith(compareByDescending<ProductRevenueDriver> { it.revenueChange }.thenBy { it.productId })
            .firstOrNull()

        // Negative revenue drivers: sorted ascending by revenueChange (largest decline first), tie-breaker productId
        val topNegRev = consolidatedRevenue
            .filter { it.revenueChange < -ZERO_THRESHOLD }
            .sortedWith(compareBy<ProductRevenueDriver> { it.revenueChange }.thenBy { it.productId })
            .firstOrNull()

        // Aggregate duplicate cost drivers by productId
        val consolidatedCost = costDrivers
            .filter { isValidNumber(it.previousCOGS) && isValidNumber(it.currentCOGS) }
            .groupBy { it.productId }
            .map { (pid, list) ->
                val name = list.firstOrNull { it.productName.isNotBlank() }?.productName ?: pid
                val prev = list.sumOf { sanitize(it.previousCOGS) }
                val curr = list.sumOf { sanitize(it.currentCOGS) }
                ProductCostDriver(pid, name, prev, curr, curr - prev)
            }

        // Cost increase: sorted descending by cogsChange, tie-breaker productId
        val topCostInc = consolidatedCost
            .filter { it.cogsChange > ZERO_THRESHOLD }
            .sortedWith(compareByDescending<ProductCostDriver> { it.cogsChange }.thenBy { it.productId })
            .firstOrNull()

        // Cost reduction: sorted ascending by cogsChange (largest decrease first), tie-breaker productId
        val topCostRed = consolidatedCost
            .filter { it.cogsChange < -ZERO_THRESHOLD }
            .sortedWith(compareBy<ProductCostDriver> { it.cogsChange }.thenBy { it.productId })
            .firstOrNull()

        // Aggregate duplicate expense drivers by category
        val consolidatedExpense = expenseDrivers
            .filter { isValidNumber(it.previousAmount) && isValidNumber(it.currentAmount) }
            .groupBy { it.category.trim().lowercase(Locale.ROOT) }
            .map { (_, list) ->
                val categoryName = list.firstOrNull { it.category.isNotBlank() }?.category ?: "Other"
                val prev = list.sumOf { sanitize(it.previousAmount) }
                val curr = list.sumOf { sanitize(it.currentAmount) }
                ExpenseCategoryDriver(categoryName, prev, curr, curr - prev)
            }

        // Expense increase: sorted descending by expenseChange, tie-breaker category
        val topExpInc = consolidatedExpense
            .filter { it.expenseChange > ZERO_THRESHOLD }
            .sortedWith(compareByDescending<ExpenseCategoryDriver> { it.expenseChange }.thenBy { it.category })
            .firstOrNull()

        // Expense reduction: sorted ascending by expenseChange, tie-breaker category
        val topExpRed = consolidatedExpense
            .filter { it.expenseChange < -ZERO_THRESHOLD }
            .sortedWith(compareBy<ExpenseCategoryDriver> { it.expenseChange }.thenBy { it.category })
            .firstOrNull()

        return ProfitDriverAttribution(
            topPositiveRevenueDriver = topPosRev,
            topNegativeRevenueDriver = topNegRev,
            topCostIncreaseDriver = topCostInc,
            topCostReductionDriver = topCostRed,
            topExpenseIncreaseDriver = topExpInc,
            topExpenseReductionDriver = topExpRed
        )
    }

    private fun determineReasonAndNarrative(
        isConsistent: Boolean,
        changeDirection: ProfitChangeDirection,
        netProfitChange: Double,
        revenueContribution: Double,
        cogsContribution: Double,
        expenseContribution: Double
    ): Pair<ProfitExplanationReasonCode, String> {
        if (!isConsistent) {
            return Pair(
                ProfitExplanationReasonCode.DATA_INCONSISTENT,
                "Profit change could not be fully reconciled from the supplied period components."
            )
        }

        if (changeDirection == ProfitChangeDirection.NO_CHANGE) {
            return Pair(
                ProfitExplanationReasonCode.NO_CHANGE,
                "Net profit remained unchanged between the two periods."
            )
        }

        val absRev = Math.abs(revenueContribution)
        val absCogs = Math.abs(cogsContribution)
        val absExp = Math.abs(expenseContribution)
        val maxMagnitude = maxOf(absRev, absCogs, absExp)

        if (maxMagnitude <= ZERO_THRESHOLD) {
            return Pair(
                ProfitExplanationReasonCode.NO_CHANGE,
                "No significant variance detected in revenue, cost, or expenses."
            )
        }

        // Check if a single driver clearly dominates (>= 70% of sum of magnitudes)
        val sumMagnitudes = absRev + absCogs + absExp
        val isSingleDominant = (maxMagnitude / sumMagnitudes) >= 0.70

        if (isSingleDominant) {
            when (maxMagnitude) {
                absRev -> {
                    return if (revenueContribution > 0) {
                        Pair(
                            ProfitExplanationReasonCode.REVENUE_DRIVEN_EXPANSION,
                            String.format(Locale.US, "Profit expanded primarily due to higher revenue (+₹%,.2f).", revenueContribution)
                        )
                    } else {
                        Pair(
                            ProfitExplanationReasonCode.REVENUE_DECLINE_CONTRACTION,
                            String.format(Locale.US, "Profit declined primarily due to lower sales revenue (-₹%,.2f).", absRev)
                        )
                    }
                }
                absCogs -> {
                    return if (cogsContribution > 0) {
                        Pair(
                            ProfitExplanationReasonCode.COST_REDUCTION_EXPANSION,
                            String.format(Locale.US, "Profit expanded primarily due to reduced cost of goods sold (+₹%,.2f).", cogsContribution)
                        )
                    } else {
                        Pair(
                            ProfitExplanationReasonCode.COST_INCREASE_CONTRACTION,
                            String.format(Locale.US, "Profit declined primarily due to higher procurement/goods costs (-₹%,.2f).", absCogs)
                        )
                    }
                }
                absExp -> {
                    return if (expenseContribution > 0) {
                        Pair(
                            ProfitExplanationReasonCode.EXPENSE_REDUCTION_EXPANSION,
                            String.format(Locale.US, "Profit expanded primarily due to lower operating expenses (+₹%,.2f).", expenseContribution)
                        )
                    } else {
                        Pair(
                            ProfitExplanationReasonCode.EXPENSE_INCREASE_CONTRACTION,
                            String.format(Locale.US, "Profit declined primarily due to higher operating expenses (-₹%,.2f).", absExp)
                        )
                    }
                }
            }
        }

        val prefix = if (netProfitChange > 0) "Profit expanded" else "Profit declined"
        return Pair(
            ProfitExplanationReasonCode.MIXED_DRIVERS,
            "$prefix due to a combination of revenue, cost, and operating expense variations."
        )
    }

    private fun sanitize(value: Double): Double {
        if (!value.isFinite() || value.isNaN()) return 0.0
        val rounded = round2(value)
        return if (rounded == -0.0) 0.0 else rounded
    }

    private fun round2(value: Double): Double {
        if (!value.isFinite() || value.isNaN()) return 0.0
        return Math.round(value * 100.0) / 100.0
    }

    private fun isValidNumber(value: Double?): Boolean {
        return value != null && value.isFinite() && !value.isNaN()
    }
}
