package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.Sale
import java.util.Locale
import java.util.UUID

/**
 * Types of what-if business scenarios supported by VyaparSetu.
 */
enum class ScenarioType {
    ADDITIONAL_SALES,
    PRICE_CHANGE,
    EXPENSE_CHANGE,
    RECEIVABLE_RECOVERY,
    COST_CHANGE,
    UNSUPPORTED
}

/**
 * Strongly-typed request inputs for business what-if scenario simulations.
 */
sealed class ScenarioRequest {

    /**
     * "What if I sell N additional units of Product X?"
     *
     * @param productId Unique identifier of the product.
     * @param additionalUnits Number of incremental units sold (must be > 0).
     * @param customUnitPrice Optional override for hypothetical selling price per unit.
     * @param customCostPerUnit Optional override for procurement cost per unit.
     */
    data class AdditionalSales(
        val productId: String,
        val additionalUnits: Int,
        val customUnitPrice: Double? = null,
        val customCostPerUnit: Double? = null
    ) : ScenarioRequest()

    /**
     * "What if I change Product X price by +5% or -5%?"
     *
     * @param productId Unique identifier of the product.
     * @param newPrice Explicit new selling price.
     * @param percentageChange Percentage change in selling price (e.g., +5.0 or -5.0).
     * @param referenceUnits Number of units to evaluate against (defaults to past sales or 1).
     */
    data class PriceChange(
        val productId: String,
        val newPrice: Double? = null,
        val percentageChange: Double? = null,
        val referenceUnits: Int? = null
    ) : ScenarioRequest()

    /**
     * "What if monthly expenses increase/decrease by ₹5,000?"
     *
     * @param amountDelta Absolute currency change (e.g. +5000.0 or -2000.0).
     * @param percentageDelta Percentage change in total operating expenses.
     * @param category Specific expense category filter (optional).
     * @param baselineExpenses Manual baseline override if no expense history available.
     */
    data class ExpenseChange(
        val amountDelta: Double? = null,
        val percentageDelta: Double? = null,
        val category: String? = null,
        val baselineExpenses: Double? = null
    ) : ScenarioRequest()

    /**
     * "What if Customer X pays ₹Y of their outstanding balance?" or
     * "What if ₹Y of pending receivables are recovered?"
     *
     * @param customerId Specific customer ID, or null for aggregated receivables recovery.
     * @param recoveryAmount Absolute amount recovered.
     * @param recoveryPercentage Percentage of outstanding balance recovered.
     */
    data class ReceivableRecovery(
        val customerId: String? = null,
        val recoveryAmount: Double? = null,
        val recoveryPercentage: Double? = null
    ) : ScenarioRequest()

    /**
     * "What if the cost/purchase price of Product X changes by X%?"
     *
     * @param productId Unique identifier of the product.
     * @param newUnitCost Explicit new purchase price per unit.
     * @param percentageChange Percentage change in procurement cost.
     * @param referenceUnits Number of units evaluated.
     */
    data class CostChange(
        val productId: String,
        val newUnitCost: Double? = null,
        val percentageChange: Double? = null,
        val referenceUnits: Int? = null
    ) : ScenarioRequest()

    /**
     * Request for an unsupported scenario type.
     */
    data class Unsupported(
        val scenarioName: String,
        val explanation: String
    ) : ScenarioRequest()
}

/**
 * Metric comparison detailing baseline, simulated value, delta, and directional favorability.
 */
data class ScenarioMetricComparison(
    val metricName: String,
    val baselineValue: Double,
    val simulatedValue: Double,
    val delta: Double,
    val percentageDelta: Double,
    val unit: String = "₹",
    val isFavorable: Boolean
)

/**
 * Information describing scenario types that cannot be deterministically computed.
 */
data class UnsupportedScenarioInfo(
    val type: String,
    val title: String,
    val reason: String,
    val missingDataSource: String
)

/**
 * Complete result of a hypothetical business scenario simulation.
 *
 * Guaranteed to be a simulation: NEVER writes to DB, NEVER modifies actual business data.
 */
data class ScenarioResult(
    val scenarioId: String,
    val scenarioType: ScenarioType,
    val title: String,
    val summary: String,
    val targetEntityId: String? = null,
    val targetEntityName: String? = null,
    val isValid: Boolean,
    val validationErrors: List<String> = emptyList(),
    val isSimulated: Boolean = true,
    val revenueImpact: ScenarioMetricComparison? = null,
    val cogsImpact: ScenarioMetricComparison? = null,
    val grossProfitImpact: ScenarioMetricComparison? = null,
    val netProfitImpact: ScenarioMetricComparison? = null,
    val marginImpact: ScenarioMetricComparison? = null,
    val expenseImpact: ScenarioMetricComparison? = null,
    val outstandingImpact: ScenarioMetricComparison? = null,
    val cashFlowImpact: ScenarioMetricComparison? = null,
    val allMetrics: List<ScenarioMetricComparison> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val limitations: List<String> = emptyList()
)

/**
 * Pure, deterministic domain engine for running What-If business scenarios.
 *
 * Rules:
 * - Deterministic, offline, and zero database mutations.
 * - Reuses existing pricing, cost basis, sales, and customer outstanding data.
 * - Does NOT use Gemini or AI for numerical calculations.
 * - Explicitly marks all outputs as hypothetical simulations.
 */
object BusinessScenarioEngine {

    /**
     * High-level entry point to simulate any supported ScenarioRequest.
     */
    fun simulate(
        request: ScenarioRequest,
        products: List<Product> = emptyList(),
        sales: List<Sale> = emptyList(),
        expenses: List<Expense> = emptyList(),
        customers: List<Customer> = emptyList()
    ): ScenarioResult {
        return when (request) {
            is ScenarioRequest.AdditionalSales -> {
                val product = products.find { it.id == request.productId }
                if (product == null) {
                    createInvalidResult(
                        scenarioType = ScenarioType.ADDITIONAL_SALES,
                        title = "Additional Sales Simulation",
                        error = "Product with ID '${request.productId}' not found."
                    )
                } else {
                    simulateAdditionalSales(
                        product = product,
                        additionalUnits = request.additionalUnits,
                        recentSales = sales,
                        customUnitPrice = request.customUnitPrice,
                        customCostPerUnit = request.customCostPerUnit
                    )
                }
            }

            is ScenarioRequest.PriceChange -> {
                val product = products.find { it.id == request.productId }
                if (product == null) {
                    createInvalidResult(
                        scenarioType = ScenarioType.PRICE_CHANGE,
                        title = "Price Change Simulation",
                        error = "Product with ID '${request.productId}' not found."
                    )
                } else {
                    simulatePriceChange(
                        product = product,
                        newPrice = request.newPrice,
                        percentageChange = request.percentageChange,
                        referenceUnits = request.referenceUnits,
                        recentSales = sales
                    )
                }
            }

            is ScenarioRequest.ExpenseChange -> {
                simulateExpenseChange(
                    recentExpenses = expenses,
                    amountDelta = request.amountDelta,
                    percentageDelta = request.percentageDelta,
                    recentSales = sales,
                    category = request.category,
                    baselineExpensesOverride = request.baselineExpenses
                )
            }

            is ScenarioRequest.ReceivableRecovery -> {
                val targetCustomer = request.customerId?.let { cid ->
                    customers.find { it.id == cid }
                }
                if (request.customerId != null && targetCustomer == null) {
                    createInvalidResult(
                        scenarioType = ScenarioType.RECEIVABLE_RECOVERY,
                        title = "Receivable Recovery Simulation",
                        error = "Customer with ID '${request.customerId}' not found."
                    )
                } else {
                    simulateReceivableRecovery(
                        customer = targetCustomer,
                        allCustomers = customers,
                        recoveryAmount = request.recoveryAmount,
                        recoveryPercentage = request.recoveryPercentage
                    )
                }
            }

            is ScenarioRequest.CostChange -> {
                val product = products.find { it.id == request.productId }
                if (product == null) {
                    createInvalidResult(
                        scenarioType = ScenarioType.COST_CHANGE,
                        title = "Cost Change Simulation",
                        error = "Product with ID '${request.productId}' not found."
                    )
                } else {
                    simulateCostChange(
                        product = product,
                        newUnitCost = request.newUnitCost,
                        percentageChange = request.percentageChange,
                        referenceUnits = request.referenceUnits,
                        recentSales = sales
                    )
                }
            }

            is ScenarioRequest.Unsupported -> {
                createUnsupportedResult(request.scenarioName, request.explanation)
            }
        }
    }

    /**
     * Scenario A: Additional Sales
     * "What if I sell N additional units of Product X?"
     */
    fun simulateAdditionalSales(
        product: Product,
        additionalUnits: Int,
        recentSales: List<Sale> = emptyList(),
        customUnitPrice: Double? = null,
        customCostPerUnit: Double? = null
    ): ScenarioResult {
        val scenarioId = "scenario_sales_${product.id}_${UUID.randomUUID().toString().take(8)}"

        if (additionalUnits <= 0) {
            return createInvalidResult(
                scenarioType = ScenarioType.ADDITIONAL_SALES,
                title = "Additional Sales: ${product.name}",
                error = "Additional units must be greater than zero (received $additionalUnits)."
            )
        }

        val unitPrice = customUnitPrice ?: product.sellingPrice
        val unitCost = customCostPerUnit ?: product.purchasePrice

        if (unitPrice < 0.0 || unitCost < 0.0) {
            return createInvalidResult(
                scenarioType = ScenarioType.ADDITIONAL_SALES,
                title = "Additional Sales: ${product.name}",
                error = "Unit price and cost basis cannot be negative."
            )
        }

        // Calculate baseline from actual past sales of this product if available
        var baselineUnitsSold = 0
        var baselineRevenue = 0.0
        var baselineCogs = 0.0

        for (sale in recentSales) {
            for (item in sale.items) {
                if (item.productId == product.id) {
                    baselineUnitsSold += item.quantity
                    baselineRevenue += item.totalPrice
                    val itemCost = if (item.purchasePriceAtSale > 0.0) {
                        item.purchasePriceAtSale
                    } else {
                        product.purchasePrice
                    }
                    baselineCogs += item.quantity * itemCost
                }
            }
        }

        val baselineGrossProfit = baselineRevenue - baselineCogs
        val baselineMargin = if (baselineRevenue > 0.0) {
            (baselineGrossProfit / baselineRevenue) * 100.0
        } else {
            0.0
        }

        // Additional incremental impacts
        val deltaRevenue = additionalUnits * unitPrice
        val deltaCogs = additionalUnits * unitCost
        val deltaGrossProfit = deltaRevenue - deltaCogs

        // Simulated resulting figures
        val simulatedRevenue = baselineRevenue + deltaRevenue
        val simulatedCogs = baselineCogs + deltaCogs
        val simulatedGrossProfit = baselineGrossProfit + deltaGrossProfit
        val simulatedMargin = if (simulatedRevenue > 0.0) {
            (simulatedGrossProfit / simulatedRevenue) * 100.0
        } else {
            0.0
        }

        val revenueMetric = ScenarioMetricComparison(
            metricName = "Revenue",
            baselineValue = round2(baselineRevenue),
            simulatedValue = round2(simulatedRevenue),
            delta = round2(deltaRevenue),
            percentageDelta = safePercentageDelta(simulatedRevenue, baselineRevenue),
            unit = "₹",
            isFavorable = deltaRevenue > 0.0
        )

        val cogsMetric = ScenarioMetricComparison(
            metricName = "COGS",
            baselineValue = round2(baselineCogs),
            simulatedValue = round2(simulatedCogs),
            delta = round2(deltaCogs),
            percentageDelta = safePercentageDelta(simulatedCogs, baselineCogs),
            unit = "₹",
            isFavorable = false // higher cost is unfavorable
        )

        val profitMetric = ScenarioMetricComparison(
            metricName = "Gross Profit",
            baselineValue = round2(baselineGrossProfit),
            simulatedValue = round2(simulatedGrossProfit),
            delta = round2(deltaGrossProfit),
            percentageDelta = safePercentageDelta(simulatedGrossProfit, baselineGrossProfit),
            unit = "₹",
            isFavorable = deltaGrossProfit >= 0.0
        )

        val marginMetric = ScenarioMetricComparison(
            metricName = "Gross Margin",
            baselineValue = round2(baselineMargin),
            simulatedValue = round2(simulatedMargin),
            delta = round2(simulatedMargin - baselineMargin),
            percentageDelta = safePercentageDelta(simulatedMargin, baselineMargin),
            unit = "%",
            isFavorable = simulatedMargin >= baselineMargin
        )

        val summary = String.format(
            Locale.US,
            "Selling %d extra unit%s of %s yields +₹%,.2f in revenue and +₹%,.2f in gross profit.",
            additionalUnits,
            if (additionalUnits > 1) "s" else "",
            product.name,
            deltaRevenue,
            deltaGrossProfit
        )

        val assumptions = listOf(
            "Assumes constant unit selling price of ₹${formatCurrency(unitPrice)}.",
            "Assumes procurement cost remains constant at ₹${formatCurrency(unitCost)} per unit.",
            "Assumes customer demand exists for $additionalUnits additional unit(s) without discounting.",
            "Simulation does NOT write to database, modify physical inventory (${product.currentStock} units), or add real sales."
        )

        val limitations = mutableListOf<String>()
        if (product.currentStock < additionalUnits) {
            limitations.add(
                "Current stock (${product.currentStock} units) is less than hypothetical demand ($additionalUnits units). Supplier restock would be required."
            )
        }

        return ScenarioResult(
            scenarioId = scenarioId,
            scenarioType = ScenarioType.ADDITIONAL_SALES,
            title = "What-if: Sell $additionalUnits extra ${product.name}",
            summary = summary,
            targetEntityId = product.id,
            targetEntityName = product.name,
            isValid = true,
            revenueImpact = revenueMetric,
            cogsImpact = cogsMetric,
            grossProfitImpact = profitMetric,
            marginImpact = marginMetric,
            allMetrics = listOf(revenueMetric, cogsMetric, profitMetric, marginMetric),
            assumptions = assumptions,
            limitations = limitations
        )
    }

    /**
     * Scenario B: Price Change
     * "What if I change Product X price by +5% or -5%?"
     */
    fun simulatePriceChange(
        product: Product,
        newPrice: Double? = null,
        percentageChange: Double? = null,
        referenceUnits: Int? = null,
        recentSales: List<Sale> = emptyList()
    ): ScenarioResult {
        val scenarioId = "scenario_price_${product.id}_${UUID.randomUUID().toString().take(8)}"

        val baselinePrice = product.sellingPrice
        val unitCost = product.purchasePrice

        if (newPrice == null && percentageChange == null) {
            return createInvalidResult(
                scenarioType = ScenarioType.PRICE_CHANGE,
                title = "Price Change: ${product.name}",
                error = "Either newPrice or percentageChange must be specified."
            )
        }

        val hypotheticalPrice = when {
            percentageChange != null -> {
                if (percentageChange < -100.0) {
                    return createInvalidResult(
                        scenarioType = ScenarioType.PRICE_CHANGE,
                        title = "Price Change: ${product.name}",
                        error = "Price cannot decrease by more than 100%."
                    )
                }
                baselinePrice * (1.0 + percentageChange / 100.0)
            }
            newPrice != null -> {
                if (newPrice < 0.0) {
                    return createInvalidResult(
                        scenarioType = ScenarioType.PRICE_CHANGE,
                        title = "Price Change: ${product.name}",
                        error = "Hypothetical selling price cannot be negative."
                    )
                }
                newPrice
            }
            else -> baselinePrice
        }

        // Determine reference units: explicit units, or sales history, or fallback 1 unit
        val pastUnitsSold = recentSales.sumOf { sale ->
            sale.items.filter { it.productId == product.id }.sumOf { it.quantity }
        }
        val evaluatedUnits = when {
            referenceUnits != null && referenceUnits > 0 -> referenceUnits
            pastUnitsSold > 0 -> pastUnitsSold
            else -> 1
        }

        val baselineRevenue = evaluatedUnits * baselinePrice
        val simulatedRevenue = evaluatedUnits * hypotheticalPrice
        val deltaRevenue = simulatedRevenue - baselineRevenue

        val cogs = evaluatedUnits * unitCost

        val baselineGrossProfit = baselineRevenue - cogs
        val simulatedGrossProfit = simulatedRevenue - cogs
        val deltaGrossProfit = simulatedGrossProfit - baselineGrossProfit

        val baselineMargin = if (baselineRevenue > 0.0) (baselineGrossProfit / baselineRevenue) * 100.0 else 0.0
        val simulatedMargin = if (simulatedRevenue > 0.0) (simulatedGrossProfit / simulatedRevenue) * 100.0 else 0.0

        val effectivePercentChange = if (baselinePrice > 0.0) {
            ((hypotheticalPrice - baselinePrice) / baselinePrice) * 100.0
        } else {
            0.0
        }

        val revenueMetric = ScenarioMetricComparison(
            metricName = "Revenue",
            baselineValue = round2(baselineRevenue),
            simulatedValue = round2(simulatedRevenue),
            delta = round2(deltaRevenue),
            percentageDelta = safePercentageDelta(simulatedRevenue, baselineRevenue),
            unit = "₹",
            isFavorable = deltaRevenue >= 0.0
        )

        val profitMetric = ScenarioMetricComparison(
            metricName = "Gross Profit",
            baselineValue = round2(baselineGrossProfit),
            simulatedValue = round2(simulatedGrossProfit),
            delta = round2(deltaGrossProfit),
            percentageDelta = safePercentageDelta(simulatedGrossProfit, baselineGrossProfit),
            unit = "₹",
            isFavorable = deltaGrossProfit >= 0.0
        )

        val marginMetric = ScenarioMetricComparison(
            metricName = "Gross Margin",
            baselineValue = round2(baselineMargin),
            simulatedValue = round2(simulatedMargin),
            delta = round2(simulatedMargin - baselineMargin),
            percentageDelta = safePercentageDelta(simulatedMargin, baselineMargin),
            unit = "%",
            isFavorable = simulatedMargin >= baselineMargin
        )

        val summary = String.format(
            Locale.US,
            "Adjusting %s price by %+.1f%% (₹%.2f → ₹%.2f) on %d units changes gross profit by %s₹%,.2f (margin: %.1f%% → %.1f%%).",
            product.name,
            effectivePercentChange,
            baselinePrice,
            hypotheticalPrice,
            evaluatedUnits,
            if (deltaGrossProfit >= 0) "+" else "-",
            Math.abs(deltaGrossProfit),
            baselineMargin,
            simulatedMargin
        )

        val assumptions = listOf(
            "Assumes sales volume remains fixed at $evaluatedUnits unit(s) regardless of price elasticity.",
            "Assumes procurement cost remains unchanged at ₹${formatCurrency(unitCost)} per unit.",
            "Does NOT update product catalog sellingPrice or any live pricing in database."
        )

        val limitations = listOf(
            "Price elasticity of demand is not modeled (real-world price increases may reduce unit sales volume)."
        )

        return ScenarioResult(
            scenarioId = scenarioId,
            scenarioType = ScenarioType.PRICE_CHANGE,
            title = "What-if: Adjust ${product.name} Price by ${String.format(Locale.US, "%+.1f%%", effectivePercentChange)}",
            summary = summary,
            targetEntityId = product.id,
            targetEntityName = product.name,
            isValid = true,
            revenueImpact = revenueMetric,
            grossProfitImpact = profitMetric,
            marginImpact = marginMetric,
            allMetrics = listOf(revenueMetric, profitMetric, marginMetric),
            assumptions = assumptions,
            limitations = limitations
        )
    }

    /**
     * Scenario C: Expense Change
     * "What if monthly expenses increase/decrease by ₹5,000?"
     */
    fun simulateExpenseChange(
        recentExpenses: List<Expense>,
        amountDelta: Double? = null,
        percentageDelta: Double? = null,
        recentSales: List<Sale> = emptyList(),
        category: String? = null,
        baselineExpensesOverride: Double? = null
    ): ScenarioResult {
        val scenarioId = "scenario_expense_${UUID.randomUUID().toString().take(8)}"

        if (amountDelta == null && percentageDelta == null) {
            return createInvalidResult(
                scenarioType = ScenarioType.EXPENSE_CHANGE,
                title = "Expense Change Simulation",
                error = "Either amountDelta or percentageDelta must be specified."
            )
        }

        val filteredExpenses = if (category != null) {
            recentExpenses.filter { it.category.equals(category, ignoreCase = true) }
        } else {
            recentExpenses
        }

        val baselineExpenses = baselineExpensesOverride ?: filteredExpenses.sumOf { it.amount }

        val deltaExpense = when {
            amountDelta != null -> amountDelta
            percentageDelta != null -> baselineExpenses * (percentageDelta / 100.0)
            else -> 0.0
        }

        val simulatedExpenses = maxOf(0.0, baselineExpenses + deltaExpense)
        val actualDelta = simulatedExpenses - baselineExpenses

        // Contextualize profit impact with sales if available
        val totalRevenue = recentSales.sumOf { it.grandTotal }
        val totalCogs = recentSales.sumOf { sale ->
            sale.items.sumOf { it.quantity * it.purchasePriceAtSale }
        }
        val grossProfit = totalRevenue - totalCogs

        val baselineNetProfit = grossProfit - baselineExpenses
        val simulatedNetProfit = grossProfit - simulatedExpenses
        val deltaNetProfit = simulatedNetProfit - baselineNetProfit // -actualDelta

        val expenseMetric = ScenarioMetricComparison(
            metricName = "Operating Expenses",
            baselineValue = round2(baselineExpenses),
            simulatedValue = round2(simulatedExpenses),
            delta = round2(actualDelta),
            percentageDelta = safePercentageDelta(simulatedExpenses, baselineExpenses),
            unit = "₹",
            isFavorable = actualDelta <= 0.0 // expense reduction is favorable
        )

        val netProfitMetric = ScenarioMetricComparison(
            metricName = "Net Profit",
            baselineValue = round2(baselineNetProfit),
            simulatedValue = round2(simulatedNetProfit),
            delta = round2(deltaNetProfit),
            percentageDelta = safePercentageDelta(simulatedNetProfit, baselineNetProfit),
            unit = "₹",
            isFavorable = deltaNetProfit >= 0.0
        )

        val summary = if (actualDelta >= 0) {
            String.format(
                Locale.US,
                "An expense increase of ₹%,.2f raises operating expenses to ₹%,.2f, reducing net profit by ₹%,.2f.",
                actualDelta,
                simulatedExpenses,
                actualDelta
            )
        } else {
            String.format(
                Locale.US,
                "An expense reduction of ₹%,.2f lowers operating expenses to ₹%,.2f, increasing net profit by ₹%,.2f.",
                Math.abs(actualDelta),
                simulatedExpenses,
                Math.abs(actualDelta)
            )
        }

        val assumptions = listOf(
            "Assumes sales revenue (₹${formatCurrency(totalRevenue)}) and cost of goods sold remain fixed.",
            if (category != null) "Applied specifically to category: $category." else "Applied across all operating expenses.",
            "Does NOT create, edit, or delete any actual Expense records."
        )

        return ScenarioResult(
            scenarioId = scenarioId,
            scenarioType = ScenarioType.EXPENSE_CHANGE,
            title = "What-if: Operating Expense ${if (actualDelta >= 0) "+₹" else "-₹"}${formatCurrency(Math.abs(actualDelta))}",
            summary = summary,
            isValid = true,
            expenseImpact = expenseMetric,
            netProfitImpact = netProfitMetric,
            allMetrics = listOf(expenseMetric, netProfitMetric),
            assumptions = assumptions
        )
    }

    /**
     * Scenario D: Receivable Recovery (Customer Payment / Khata Recovery)
     * "What if Customer X pays ₹Y of their outstanding balance?" or "What if ₹Y of pending receivables are recovered?"
     */
    fun simulateReceivableRecovery(
        customer: Customer? = null,
        allCustomers: List<Customer> = emptyList(),
        recoveryAmount: Double? = null,
        recoveryPercentage: Double? = null
    ): ScenarioResult {
        val scenarioId = "scenario_recovery_${customer?.id ?: "all"}_${UUID.randomUUID().toString().take(8)}"

        if (recoveryAmount == null && recoveryPercentage == null) {
            return createInvalidResult(
                scenarioType = ScenarioType.RECEIVABLE_RECOVERY,
                title = "Receivable Recovery Simulation",
                error = "Either recoveryAmount or recoveryPercentage must be provided."
            )
        }

        val baselineOutstanding = customer?.outstandingAmount
            ?: allCustomers.sumOf { it.outstandingAmount }

        if (baselineOutstanding <= 0.0) {
            return createInvalidResult(
                scenarioType = ScenarioType.RECEIVABLE_RECOVERY,
                title = "Receivable Recovery",
                error = "There is no outstanding balance to simulate recovery against (₹0.00)."
            )
        }

        val rawRecovery = when {
            recoveryAmount != null -> {
                if (recoveryAmount <= 0.0) {
                    return createInvalidResult(
                        scenarioType = ScenarioType.RECEIVABLE_RECOVERY,
                        title = "Receivable Recovery",
                        error = "Recovery amount must be greater than zero."
                    )
                }
                recoveryAmount
            }
            recoveryPercentage != null -> {
                if (recoveryPercentage <= 0.0 || recoveryPercentage > 100.0) {
                    return createInvalidResult(
                        scenarioType = ScenarioType.RECEIVABLE_RECOVERY,
                        title = "Receivable Recovery",
                        error = "Recovery percentage must be between 0.1% and 100.0%."
                    )
                }
                baselineOutstanding * (recoveryPercentage / 100.0)
            }
            else -> 0.0
        }

        val actualRecovery = minOf(rawRecovery, baselineOutstanding)
        val simulatedOutstanding = maxOf(0.0, baselineOutstanding - actualRecovery)
        val deltaOutstanding = simulatedOutstanding - baselineOutstanding // negative

        val outstandingMetric = ScenarioMetricComparison(
            metricName = "Receivables Outstanding",
            baselineValue = round2(baselineOutstanding),
            simulatedValue = round2(simulatedOutstanding),
            delta = round2(deltaOutstanding),
            percentageDelta = safePercentageDelta(simulatedOutstanding, baselineOutstanding),
            unit = "₹",
            isFavorable = true // lower outstanding is favorable
        )

        val cashFlowMetric = ScenarioMetricComparison(
            metricName = "Cash Inflow",
            baselineValue = 0.0,
            simulatedValue = round2(actualRecovery),
            delta = round2(actualRecovery),
            percentageDelta = 100.0,
            unit = "₹",
            isFavorable = true
        )

        val targetDescription = customer?.name ?: "All Customers"
        val summary = String.format(
            Locale.US,
            "Recovering ₹%,.2f from %s reduces outstanding from ₹%,.2f to ₹%,.2f (+₹%,.2f immediate cash inflow).",
            actualRecovery,
            targetDescription,
            baselineOutstanding,
            simulatedOutstanding,
            actualRecovery
        )

        val limitations = mutableListOf<String>()
        if (rawRecovery > baselineOutstanding) {
            limitations.add(
                "Requested recovery (₹${formatCurrency(rawRecovery)}) exceeded total outstanding balance (₹${formatCurrency(baselineOutstanding)}). Capped at full settlement."
            )
        }

        val assumptions = listOf(
            "Assumes collected payments are deposited directly into liquid business cash flow.",
            "Does NOT record a CustomerPayment or alter customer ledger in the database."
        )

        return ScenarioResult(
            scenarioId = scenarioId,
            scenarioType = ScenarioType.RECEIVABLE_RECOVERY,
            title = "What-if: Recover ₹${formatCurrency(actualRecovery)} ($targetDescription)",
            summary = summary,
            targetEntityId = customer?.id,
            targetEntityName = customer?.name,
            isValid = true,
            outstandingImpact = outstandingMetric,
            cashFlowImpact = cashFlowMetric,
            allMetrics = listOf(outstandingMetric, cashFlowMetric),
            assumptions = assumptions,
            limitations = limitations
        )
    }

    /**
     * Scenario E: Procurement Cost / COGS Change
     * "What if supplier procurement cost for Product X increases by 10%?"
     */
    fun simulateCostChange(
        product: Product,
        newUnitCost: Double? = null,
        percentageChange: Double? = null,
        referenceUnits: Int? = null,
        recentSales: List<Sale> = emptyList()
    ): ScenarioResult {
        val scenarioId = "scenario_cost_${product.id}_${UUID.randomUUID().toString().take(8)}"

        val baselineCost = product.purchasePrice
        val sellingPrice = product.sellingPrice

        if (newUnitCost == null && percentageChange == null) {
            return createInvalidResult(
                scenarioType = ScenarioType.COST_CHANGE,
                title = "Cost Change: ${product.name}",
                error = "Either newUnitCost or percentageChange must be specified."
            )
        }

        val hypotheticalCost = when {
            percentageChange != null -> {
                if (percentageChange < -100.0) {
                    return createInvalidResult(
                        scenarioType = ScenarioType.COST_CHANGE,
                        title = "Cost Change: ${product.name}",
                        error = "Procurement cost cannot decrease by more than 100%."
                    )
                }
                baselineCost * (1.0 + percentageChange / 100.0)
            }
            newUnitCost != null -> {
                if (newUnitCost < 0.0) {
                    return createInvalidResult(
                        scenarioType = ScenarioType.COST_CHANGE,
                        title = "Cost Change: ${product.name}",
                        error = "Procurement cost cannot be negative."
                    )
                }
                newUnitCost
            }
            else -> baselineCost
        }

        val pastUnitsSold = recentSales.sumOf { sale ->
            sale.items.filter { it.productId == product.id }.sumOf { it.quantity }
        }
        val evaluatedUnits = when {
            referenceUnits != null && referenceUnits > 0 -> referenceUnits
            pastUnitsSold > 0 -> pastUnitsSold
            else -> 1
        }

        val revenue = evaluatedUnits * sellingPrice
        val baselineCogs = evaluatedUnits * baselineCost
        val simulatedCogs = evaluatedUnits * hypotheticalCost
        val deltaCogs = simulatedCogs - baselineCogs

        val baselineGrossProfit = revenue - baselineCogs
        val simulatedGrossProfit = revenue - simulatedCogs
        val deltaGrossProfit = simulatedGrossProfit - baselineGrossProfit

        val baselineMargin = if (revenue > 0.0) (baselineGrossProfit / revenue) * 100.0 else 0.0
        val simulatedMargin = if (revenue > 0.0) (simulatedGrossProfit / revenue) * 100.0 else 0.0

        val effectivePercentChange = if (baselineCost > 0.0) {
            ((hypotheticalCost - baselineCost) / baselineCost) * 100.0
        } else {
            0.0
        }

        val cogsMetric = ScenarioMetricComparison(
            metricName = "COGS",
            baselineValue = round2(baselineCogs),
            simulatedValue = round2(simulatedCogs),
            delta = round2(deltaCogs),
            percentageDelta = safePercentageDelta(simulatedCogs, baselineCogs),
            unit = "₹",
            isFavorable = deltaCogs <= 0.0
        )

        val profitMetric = ScenarioMetricComparison(
            metricName = "Gross Profit",
            baselineValue = round2(baselineGrossProfit),
            simulatedValue = round2(simulatedGrossProfit),
            delta = round2(deltaGrossProfit),
            percentageDelta = safePercentageDelta(simulatedGrossProfit, baselineGrossProfit),
            unit = "₹",
            isFavorable = deltaGrossProfit >= 0.0
        )

        val marginMetric = ScenarioMetricComparison(
            metricName = "Gross Margin",
            baselineValue = round2(baselineMargin),
            simulatedValue = round2(simulatedMargin),
            delta = round2(simulatedMargin - baselineMargin),
            percentageDelta = safePercentageDelta(simulatedMargin, baselineMargin),
            unit = "%",
            isFavorable = simulatedMargin >= baselineMargin
        )

        val summary = String.format(
            Locale.US,
            "Cost change of %+.1f%% (₹%.2f → ₹%.2f) on %s shifts gross profit by %s₹%,.2f across %d units (margin: %.1f%% → %.1f%%).",
            effectivePercentChange,
            baselineCost,
            hypotheticalCost,
            product.name,
            if (deltaGrossProfit >= 0) "+" else "-",
            Math.abs(deltaGrossProfit),
            evaluatedUnits,
            baselineMargin,
            simulatedMargin
        )

        val assumptions = listOf(
            "Assumes retail selling price remains unchanged at ₹${formatCurrency(sellingPrice)}.",
            "Assumes procurement changes do not impact supplier minimum order quantities.",
            "Does NOT modify product purchasePrice or supplier purchase history in database."
        )

        return ScenarioResult(
            scenarioId = scenarioId,
            scenarioType = ScenarioType.COST_CHANGE,
            title = "What-if: Cost Change for ${product.name} (${String.format(Locale.US, "%+.1f%%", effectivePercentChange)})",
            summary = summary,
            targetEntityId = product.id,
            targetEntityName = product.name,
            isValid = true,
            cogsImpact = cogsMetric,
            grossProfitImpact = profitMetric,
            marginImpact = marginMetric,
            allMetrics = listOf(cogsMetric, profitMetric, marginMetric),
            assumptions = assumptions
        )
    }

    /**
     * Catalog of scenarios explicitly unsupported due to lack of deterministic offline data.
     */
    fun getUnsupportedScenarios(): List<UnsupportedScenarioInfo> {
        return listOf(
            UnsupportedScenarioInfo(
                type = "MACRO_INFLATION",
                title = "Macroeconomic Inflation Impact",
                reason = "Requires external economic indices (CPI, RBI interest rates) not available in local transactional data.",
                missingDataSource = "External Government/RBI Financial Feeds"
            ),
            UnsupportedScenarioInfo(
                type = "COMPETITOR_PRICING",
                title = "Competitor Pricing Shifts",
                reason = "Requires live market competitive intelligence and foot-traffic elasticity models.",
                missingDataSource = "Competitor Market Price Scraper"
            ),
            UnsupportedScenarioInfo(
                type = "ADVERTISING_ROI",
                title = "Marketing Campaign Conversion ROI",
                reason = "Requires attribution tracking across digital marketing channels (CPC, impressions, lead conversions).",
                missingDataSource = "Digital Ad Platform Integration"
            ),
            UnsupportedScenarioInfo(
                type = "FOOT_TRAFFIC_SHIFTS",
                title = "Retail Store Foot Traffic Simulation",
                reason = "Requires physical presence detection or camera foot-fall counters.",
                missingDataSource = "In-Store IoT Sensor Data"
            ),
            UnsupportedScenarioInfo(
                type = "SUPPLY_CHAIN_DISRUPTIONS",
                title = "Logistics Route Disruption Prediction",
                reason = "Requires national highway logistics tracking and weather/geopolitical alerts.",
                missingDataSource = "Fleet Tracking & Logistics Telematics"
            )
        )
    }

    // Helper functions

    private fun createInvalidResult(
        scenarioType: ScenarioType,
        title: String,
        error: String
    ): ScenarioResult {
        return ScenarioResult(
            scenarioId = "scenario_invalid_${UUID.randomUUID().toString().take(8)}",
            scenarioType = scenarioType,
            title = title,
            summary = "Simulation cannot be computed: $error",
            isValid = false,
            validationErrors = listOf(error),
            assumptions = emptyList(),
            limitations = listOf("Invalid input parameters prevented calculation.")
        )
    }

    private fun createUnsupportedResult(scenarioName: String, explanation: String): ScenarioResult {
        return ScenarioResult(
            scenarioId = "scenario_unsupported_${UUID.randomUUID().toString().take(8)}",
            scenarioType = ScenarioType.UNSUPPORTED,
            title = "Unsupported Scenario: $scenarioName",
            summary = "Cannot simulate '$scenarioName': $explanation",
            isValid = false,
            validationErrors = listOf("This scenario cannot be deterministically computed from local business data: $explanation"),
            limitations = listOf(
                "Deterministic engines cannot simulate variables with missing external telemetry.",
                "See getUnsupportedScenarios() for documented limitations."
            )
        )
    }

    private fun safePercentageDelta(simulated: Double, baseline: Double): Double {
        if (!baseline.isFinite() || !simulated.isFinite()) return 0.0
        if (Math.abs(baseline) < 0.0001) {
            return if (Math.abs(simulated) < 0.0001) 0.0 else 100.0
        }
        val pct = ((simulated - baseline) / Math.abs(baseline)) * 100.0
        return round2(pct)
    }

    private fun round2(value: Double): Double {
        if (!value.isFinite()) return 0.0
        return Math.round(value * 100.0) / 100.0
    }

    private fun formatCurrency(amount: Double): String {
        return String.format(Locale.US, "%,.2f", amount)
    }
}
