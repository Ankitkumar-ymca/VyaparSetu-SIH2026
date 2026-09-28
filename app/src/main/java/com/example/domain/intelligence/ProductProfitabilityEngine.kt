package com.example.domain.intelligence

import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem

/**
 * Deterministic rule-based classification for product gross margin.
 *
 * Internal analytics rules for VyaparSetu:
 * - HIGH_MARGIN: grossMarginPercentage >= 40.0%
 * - MEDIUM_MARGIN: grossMarginPercentage >= 20.0% and < 40.0%
 * - LOW_MARGIN: grossMarginPercentage > 0.0% and < 20.0%
 * - NO_PROFIT: grossProfit <= 0.0 (or negative)
 * - INSUFFICIENT_DATA: 0 units sold / no recorded revenue
 */
enum class MarginCategory {
    HIGH_MARGIN,
    MEDIUM_MARGIN,
    LOW_MARGIN,
    NO_PROFIT,
    INSUFFICIENT_DATA
}

/**
 * Immutable product-level profitability metrics derived deterministically from recorded sales.
 */
data class ProductProfitabilityMetrics(
    val productId: String,
    val productName: String,
    val unitsSold: Int,
    val revenue: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val grossMarginPercentage: Double,
    val averageSellingPrice: Double,
    val averageCostPerUnit: Double,
    val profitPerUnit: Double,
    val marginCategory: MarginCategory
)

/**
 * Aggregated business-level profitability summary across products analyzed.
 */
data class ProductProfitabilitySummary(
    val totalRevenue: Double,
    val totalCostOfGoodsSold: Double,
    val totalGrossProfit: Double,
    val overallGrossMarginPercentage: Double,
    val totalUnitsSold: Int,
    val productCount: Int,
    val highMarginCount: Int,
    val mediumMarginCount: Int,
    val lowMarginCount: Int,
    val noProfitCount: Int,
    val insufficientDataCount: Int,
    val highestGrossProfitProduct: ProductProfitabilityMetrics?,
    val highestMarginProduct: ProductProfitabilityMetrics?,
    val products: List<ProductProfitabilityMetrics>
)

/**
 * Pure domain-level engine for calculating product-level gross profitability.
 *
 * Deterministic, offline, zero database mutation, zero Gemini/network calls.
 */
object ProductProfitabilityEngine {

    const val DEFAULT_ANALYSIS_PERIOD_DAYS = 30
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    // Centralized classification thresholds
    const val THRESHOLD_HIGH_MARGIN = 40.0
    const val THRESHOLD_MEDIUM_MARGIN = 20.0
    const val THRESHOLD_LOW_MARGIN = 0.0

    /**
     * Analyzes product-level profitability over a configurable period.
     *
     * @param products The business product catalog.
     * @param sales The recorded sales list.
     * @param analysisPeriodDays Analysis window in days (default 30).
     * @param referenceTimeMs Epoch reference timestamp in ms (defaults to System.currentTimeMillis()).
     */
    fun analyze(
        products: List<Product>,
        sales: List<Sale>,
        analysisPeriodDays: Int = DEFAULT_ANALYSIS_PERIOD_DAYS,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): ProductProfitabilitySummary {
        if (products.isEmpty()) {
            return ProductProfitabilitySummary(
                totalRevenue = 0.0,
                totalCostOfGoodsSold = 0.0,
                totalGrossProfit = 0.0,
                overallGrossMarginPercentage = 0.0,
                totalUnitsSold = 0,
                productCount = 0,
                highMarginCount = 0,
                mediumMarginCount = 0,
                lowMarginCount = 0,
                noProfitCount = 0,
                insufficientDataCount = 0,
                highestGrossProfitProduct = null,
                highestMarginProduct = null,
                products = emptyList()
            )
        }

        val safePeriodDays = if (analysisPeriodDays <= 0) DEFAULT_ANALYSIS_PERIOD_DAYS else analysisPeriodDays
        val periodStartMs = referenceTimeMs - (safePeriodDays.toLong() * ONE_DAY_MS)

        // Filter sales within the target time window [periodStartMs .. referenceTimeMs]
        val periodSales = sales.filter { it.createdAt in periodStartMs..referenceTimeMs }

        // Map product lookup for fast access and fallback cost basis
        val productMap = products.associateBy { it.id }

        // Accumulators per productId: (unitsSold, totalRevenue, totalCogs)
        class ProductSaleAccumulator {
            var unitsSold: Int = 0
            var revenue: Double = 0.0
            var cogs: Double = 0.0
        }

        val accumulators = mutableMapOf<String, ProductSaleAccumulator>()

        for (sale in periodSales) {
            for (item in sale.items) {
                val product = productMap[item.productId]
                // Missing product references: Exclude safely without crashing or manufacturing fake data
                if (product == null) {
                    continue
                }

                val acc = accumulators.getOrPut(item.productId) { ProductSaleAccumulator() }
                
                val itemQty = item.quantity
                val itemRevenue = item.totalPrice
                
                // Cost basis: Use purchasePriceAtSale if recorded (> 0.0); fallback to product.purchasePrice
                val unitCost = if (item.purchasePriceAtSale > 0.0) {
                    item.purchasePriceAtSale
                } else {
                    product.purchasePrice
                }
                val itemCogs = itemQty * unitCost

                acc.unitsSold += itemQty
                acc.revenue += itemRevenue
                acc.cogs += itemCogs
            }
        }

        // Build individual ProductProfitabilityMetrics for each product in the catalog
        val metricsList = products.map { product ->
            val acc = accumulators[product.id]
            val unitsSold = acc?.unitsSold ?: 0
            val revenue = acc?.revenue ?: 0.0
            val cogs = acc?.cogs ?: 0.0

            val grossProfit = revenue - cogs

            val grossMarginPercentage = if (revenue > 0.0) {
                safeDouble((grossProfit / revenue) * 100.0)
            } else {
                0.0
            }

            val averageSellingPrice = if (unitsSold > 0) {
                safeDouble(revenue / unitsSold.toDouble())
            } else {
                product.sellingPrice
            }

            val averageCostPerUnit = if (unitsSold > 0) {
                safeDouble(cogs / unitsSold.toDouble())
            } else {
                product.purchasePrice
            }

            val profitPerUnit = if (unitsSold > 0) {
                safeDouble(grossProfit / unitsSold.toDouble())
            } else {
                safeDouble(product.sellingPrice - product.purchasePrice)
            }

            val category = classifyMargin(unitsSold, revenue, grossProfit, grossMarginPercentage)

            ProductProfitabilityMetrics(
                productId = product.id,
                productName = product.name,
                unitsSold = unitsSold,
                revenue = safeDouble(revenue),
                costOfGoodsSold = safeDouble(cogs),
                grossProfit = safeDouble(grossProfit),
                grossMarginPercentage = grossMarginPercentage,
                averageSellingPrice = averageSellingPrice,
                averageCostPerUnit = averageCostPerUnit,
                profitPerUnit = profitPerUnit,
                marginCategory = category
            )
        }

        // Aggregate summary metrics
        val totalRevenue = safeDouble(metricsList.sumOf { it.revenue })
        val totalCogs = safeDouble(metricsList.sumOf { it.costOfGoodsSold })
        val totalGrossProfit = safeDouble(totalRevenue - totalCogs)
        val overallGrossMargin = if (totalRevenue > 0.0) {
            safeDouble((totalGrossProfit / totalRevenue) * 100.0)
        } else {
            0.0
        }
        val totalUnitsSold = metricsList.sumOf { it.unitsSold }

        var highMarginCount = 0
        var mediumMarginCount = 0
        var lowMarginCount = 0
        var noProfitCount = 0
        var insufficientDataCount = 0

        for (m in metricsList) {
            when (m.marginCategory) {
                MarginCategory.HIGH_MARGIN -> highMarginCount++
                MarginCategory.MEDIUM_MARGIN -> mediumMarginCount++
                MarginCategory.LOW_MARGIN -> lowMarginCount++
                MarginCategory.NO_PROFIT -> noProfitCount++
                MarginCategory.INSUFFICIENT_DATA -> insufficientDataCount++
            }
        }

        // Products with sales to determine highest gross profit and highest margin
        val productsWithSales = metricsList.filter { it.unitsSold > 0 }
        
        val highestGrossProfitProduct = productsWithSales.maxByOrNull { it.grossProfit }
        val highestMarginProduct = productsWithSales.maxByOrNull { it.grossMarginPercentage }

        return ProductProfitabilitySummary(
            totalRevenue = totalRevenue,
            totalCostOfGoodsSold = totalCogs,
            totalGrossProfit = totalGrossProfit,
            overallGrossMarginPercentage = overallGrossMargin,
            totalUnitsSold = totalUnitsSold,
            productCount = products.size,
            highMarginCount = highMarginCount,
            mediumMarginCount = mediumMarginCount,
            lowMarginCount = lowMarginCount,
            noProfitCount = noProfitCount,
            insufficientDataCount = insufficientDataCount,
            highestGrossProfitProduct = highestGrossProfitProduct,
            highestMarginProduct = highestMarginProduct,
            products = metricsList
        )
    }

    /**
     * Rule-based categorization of product profitability.
     */
    fun classifyMargin(
        unitsSold: Int,
        revenue: Double,
        grossProfit: Double,
        grossMarginPercentage: Double
    ): MarginCategory {
        if (unitsSold <= 0 || revenue <= 0.0) {
            return MarginCategory.INSUFFICIENT_DATA
        }
        if (grossProfit <= 0.0) {
            return MarginCategory.NO_PROFIT
        }
        return when {
            grossMarginPercentage >= THRESHOLD_HIGH_MARGIN -> MarginCategory.HIGH_MARGIN
            grossMarginPercentage >= THRESHOLD_MEDIUM_MARGIN -> MarginCategory.MEDIUM_MARGIN
            grossMarginPercentage > THRESHOLD_LOW_MARGIN -> MarginCategory.LOW_MARGIN
            else -> MarginCategory.NO_PROFIT
        }
    }

    /**
     * Sanitizes floating-point results against NaN, Infinity, and Negative Zero.
     */
    private fun safeDouble(value: Double): Double {
        return when {
            value.isNaN() || value.isInfinite() -> 0.0
            value == -0.0 -> 0.0
            else -> value
        }
    }
}
