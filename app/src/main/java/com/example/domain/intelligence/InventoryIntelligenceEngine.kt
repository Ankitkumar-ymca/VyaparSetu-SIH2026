package com.example.domain.intelligence

import com.example.domain.model.Product
import com.example.domain.model.Sale

/**
 * Velocity classification for inventory items based on deterministic sales velocity
 * and inventory coverage.
 */
enum class VelocityCategory {
    FAST_MOVING,
    NORMAL,
    SLOW_MOVING,
    DEAD_STOCK
}

/**
 * Historical demand trend comparing the earlier half of the analysis window
 * against the more recent half.
 */
enum class DemandTrend {
    RISING,
    STABLE,
    FALLING,
    INSUFFICIENT_DATA
}

/**
 * Granular intelligence and velocity metrics calculated for an individual product.
 */
data class InventoryProductMetrics(
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val minimumStock: Int,
    val unitsSold: Int,
    val salesVelocityPerDay: Double,
    val daysOfStockRemaining: Double?,
    val inventoryValue: Double,
    val velocityCategory: VelocityCategory,
    val isDeadStock: Boolean,
    val capitalLocked: Double,
    val demandTrend: DemandTrend,
    val hasSufficientData: Boolean
)

/**
 * Aggregated inventory intelligence summary across an entire business catalog.
 */
data class InventoryIntelligenceSummary(
    val totalInventoryValue: Double,
    val capitalLockedInSlowAndDeadStock: Double,
    val fastMovingCount: Int,
    val normalCount: Int,
    val slowMovingCount: Int,
    val deadStockCount: Int,
    val products: List<InventoryProductMetrics>
)

/**
 * Pure, deterministic intelligence engine for inventory analysis.
 *
 * This engine does not perform network, database, or UI operations, nor does it
 * modify any existing models or application state. All analytics are derived
 * strictly from the immutable data passed into it.
 */
object InventoryIntelligenceEngine {

    const val DEFAULT_ANALYSIS_PERIOD_DAYS = 30
    const val DEFAULT_DEAD_STOCK_LOOKBACK_DAYS = 45
    const val FAST_MOVING_VELOCITY_THRESHOLD = 1.5 // > 1.5 units/day
    const val SLOW_MOVING_COVERAGE_DAYS_THRESHOLD = 45.0 // stock coverage > 45 days or very low velocity

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    /**
     * Analyzes inventory across a given set of products and sales records.
     *
     * @param products The active product catalog to evaluate.
     * @param sales The historical sales transactions.
     * @param referenceTime The epoch timestamp representing "now" (defaults to current system time).
     * @param analysisPeriodDays The evaluation window for sales velocity (defaults to 30 days).
     * @param deadStockLookbackDays The inactive duration required to classify dead stock (defaults to 45 days).
     * @return [InventoryIntelligenceSummary] with product-level metrics and aggregate metrics.
     */
    fun analyze(
        products: List<Product>,
        sales: List<Sale>,
        referenceTime: Long = System.currentTimeMillis(),
        analysisPeriodDays: Int = DEFAULT_ANALYSIS_PERIOD_DAYS,
        deadStockLookbackDays: Int = DEFAULT_DEAD_STOCK_LOOKBACK_DAYS
    ): InventoryIntelligenceSummary {
        val safePeriodDays = if (analysisPeriodDays <= 0) DEFAULT_ANALYSIS_PERIOD_DAYS else analysisPeriodDays
        val safeDeadStockDays = if (deadStockLookbackDays <= 0) DEFAULT_DEAD_STOCK_LOOKBACK_DAYS else deadStockLookbackDays

        val analysisStartTimestamp = referenceTime - (safePeriodDays.toLong() * MILLIS_PER_DAY)
        val deadStockStartTimestamp = referenceTime - (safeDeadStockDays.toLong() * MILLIS_PER_DAY)

        // Filter sales for relevant windows
        val salesInDeadStockWindow = sales.filter { it.createdAt in deadStockStartTimestamp..referenceTime }
        val salesInAnalysisPeriod = sales.filter { it.createdAt in analysisStartTimestamp..referenceTime }

        // Precompute product sales in dead-stock window (45 days)
        val unitsSoldInDeadStockWindow = mutableMapOf<String, Int>()
        salesInDeadStockWindow.forEach { sale ->
            sale.items.forEach { item ->
                unitsSoldInDeadStockWindow[item.productId] =
                    (unitsSoldInDeadStockWindow[item.productId] ?: 0) + item.quantity
            }
        }

        // Precompute product sales in analysis window (30 days)
        val unitsSoldInAnalysisWindow = mutableMapOf<String, Int>()
        salesInAnalysisPeriod.forEach { sale ->
            sale.items.forEach { item ->
                unitsSoldInAnalysisWindow[item.productId] =
                    (unitsSoldInAnalysisWindow[item.productId] ?: 0) + item.quantity
            }
        }

        // Half-period splits for demand trend analysis
        val halfPeriodDays = safePeriodDays / 2.0
        val midTimestamp = referenceTime - (halfPeriodDays * MILLIS_PER_DAY).toLong()

        val unitsFirstHalf = mutableMapOf<String, Int>()
        val unitsSecondHalf = mutableMapOf<String, Int>()

        salesInAnalysisPeriod.forEach { sale ->
            if (sale.createdAt < midTimestamp) {
                sale.items.forEach { item ->
                    unitsFirstHalf[item.productId] = (unitsFirstHalf[item.productId] ?: 0) + item.quantity
                }
            } else {
                sale.items.forEach { item ->
                    unitsSecondHalf[item.productId] = (unitsSecondHalf[item.productId] ?: 0) + item.quantity
                }
            }
        }

        val productMetricsList = products.map { product ->
            val unitsSold = unitsSoldInAnalysisWindow[product.id] ?: 0
            val unitsSoldInDeadStockPeriod = unitsSoldInDeadStockWindow[product.id] ?: 0

            // 1. Sales velocity: units sold / period days
            val salesVelocityPerDay = unitsSold.toDouble() / safePeriodDays.toDouble()

            // 2. Days of stock remaining: currentStock / velocity (safe from zero/negative)
            val daysOfStockRemaining: Double? = if (salesVelocityPerDay > 0.0 && product.currentStock > 0) {
                product.currentStock / salesVelocityPerDay
            } else {
                null
            }

            // 3. Inventory valuation: currentStock * purchasePrice (non-negative)
            val unitPurchaseCost = if (product.purchasePrice > 0.0) product.purchasePrice else 0.0
            val effectiveStock = if (product.currentStock > 0) product.currentStock else 0
            val inventoryValue = effectiveStock * unitPurchaseCost

            // 4. Dead stock determination: currentStock > 0 AND no sales in 45-day lookback window
            val isDeadStock = product.currentStock > 0 && unitsSoldInDeadStockPeriod == 0

            // 5. Velocity classification
            val velocityCategory = when {
                isDeadStock -> VelocityCategory.DEAD_STOCK
                salesVelocityPerDay > FAST_MOVING_VELOCITY_THRESHOLD -> VelocityCategory.FAST_MOVING
                salesVelocityPerDay > 0.0 && (daysOfStockRemaining != null && daysOfStockRemaining > SLOW_MOVING_COVERAGE_DAYS_THRESHOLD) -> VelocityCategory.SLOW_MOVING
                salesVelocityPerDay > 0.0 -> VelocityCategory.NORMAL
                product.currentStock > 0 -> VelocityCategory.DEAD_STOCK
                else -> VelocityCategory.NORMAL // Zero stock and zero recent sales
            }

            // 6. Capital locked: inventoryValue of slow-moving or dead-stock products
            val capitalLocked = if (velocityCategory == VelocityCategory.SLOW_MOVING || velocityCategory == VelocityCategory.DEAD_STOCK) {
                inventoryValue
            } else {
                0.0
            }

            // 7. Demand trend calculation
            val u1 = unitsFirstHalf[product.id] ?: 0
            val u2 = unitsSecondHalf[product.id] ?: 0
            val demandTrend = calculateDemandTrend(u1, u2)

            // 8. Sufficient data check: has cataloged stock and tracked period
            val hasSufficientData = product.currentStock >= 0 && (unitsSold > 0 || isDeadStock)

            InventoryProductMetrics(
                productId = product.id,
                productName = product.name,
                currentStock = product.currentStock,
                minimumStock = product.minStock,
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

        val totalInventoryValue = productMetricsList.sumOf { it.inventoryValue }
        val capitalLockedInSlowAndDeadStock = productMetricsList.sumOf { it.capitalLocked }
        val fastMovingCount = productMetricsList.count { it.velocityCategory == VelocityCategory.FAST_MOVING }
        val normalCount = productMetricsList.count { it.velocityCategory == VelocityCategory.NORMAL }
        val slowMovingCount = productMetricsList.count { it.velocityCategory == VelocityCategory.SLOW_MOVING }
        val deadStockCount = productMetricsList.count { it.velocityCategory == VelocityCategory.DEAD_STOCK }

        return InventoryIntelligenceSummary(
            totalInventoryValue = totalInventoryValue,
            capitalLockedInSlowAndDeadStock = capitalLockedInSlowAndDeadStock,
            fastMovingCount = fastMovingCount,
            normalCount = normalCount,
            slowMovingCount = slowMovingCount,
            deadStockCount = deadStockCount,
            products = productMetricsList
        )
    }

    /**
     * Deterministically calculates demand trend by comparing earlier and later sub-period sales.
     *
     * @param unitsFirstHalf Units sold during the first half of the analysis window.
     * @param unitsSecondHalf Units sold during the second half of the analysis window.
     */
    private fun calculateDemandTrend(unitsFirstHalf: Int, unitsSecondHalf: Int): DemandTrend {
        val totalVolume = unitsFirstHalf + unitsSecondHalf
        if (totalVolume == 0) {
            return DemandTrend.INSUFFICIENT_DATA
        }

        // Relative change threshold: >= 20% shift indicates trend direction
        val diff = unitsSecondHalf - unitsFirstHalf
        val percentageChange = (diff.toDouble() / totalVolume.toDouble()) * 100.0

        return when {
            percentageChange >= 15.0 -> DemandTrend.RISING
            percentageChange <= -15.0 -> DemandTrend.FALLING
            else -> DemandTrend.STABLE
        }
    }
}
