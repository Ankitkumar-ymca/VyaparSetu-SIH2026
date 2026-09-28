package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Sale

/**
 * Deterministic rule-based customer segment.
 */
enum class CustomerSegment {
    FREQUENT,
    HIGH_VALUE,
    REGULAR,
    INACTIVE,
    NEW,
    INSUFFICIENT_DATA
}

/**
 * Deterministic customer activity status based on purchase recency and history.
 */
enum class CustomerActivityStatus {
    ACTIVE,
    INACTIVE,
    NEW,
    INSUFFICIENT_DATA
}

/**
 * Granular intelligence metrics calculated for an individual customer.
 * Contains both lifetime data (from customer record and all matched sales)
 * and analysis-period data (within the specified window).
 */
data class CustomerMetrics(
    val customerId: String,
    val customerName: String,
    val phone: String,
    // Period-specific metrics (within analysis window)
    val periodPurchases: Double,
    val periodOrders: Int,
    val periodUnitsPurchased: Int,
    // Lifetime metrics (across all recorded sales for this customer)
    val lifetimePurchases: Double,
    val lifetimeOrders: Int,
    val lifetimeUnitsPurchased: Int,
    val averageOrderValue: Double,
    val firstPurchaseDate: Long?,
    val lastPurchaseDate: Long?,
    val daysSinceLastPurchase: Long?,
    val purchaseFrequencyPerMonth: Double,
    // Outstanding / Khata metrics directly from customer model
    val outstandingAmount: Double,
    val hasOutstanding: Boolean,
    val totalPaid: Double,
    // Classifications
    val segment: CustomerSegment,
    val activityStatus: CustomerActivityStatus,
    val hasSufficientData: Boolean
)

/**
 * Aggregated customer intelligence summary across all analyzed customers.
 */
data class CustomerIntelligenceSummary(
    val totalCustomersAnalyzed: Int,
    val activeCustomerCount: Int,
    val inactiveCustomerCount: Int,
    val newCustomerCount: Int,
    val frequentCustomerCount: Int,
    val highValueCustomerCount: Int,
    val regularCustomerCount: Int,
    val insufficientDataCount: Int,
    val totalCustomerSalesPeriod: Double,
    val totalCustomerSalesLifetime: Double,
    val totalOutstandingAmount: Double,
    val customersWithOutstandingCount: Int,
    val customers: List<CustomerMetrics>
)

/**
 * Customer Intelligence Engine
 *
 * Deterministic analytics layer that consumes real Customer and Sale models
 * without modifying database, DAOs, or existing business logic.
 */
object CustomerIntelligenceEngine {

    const val DEFAULT_ANALYSIS_DAYS = 30
    private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

    // Centralized classification thresholds
    object Thresholds {
        // High Value: Lifetime purchases >= ₹10,000 OR Period purchases >= ₹5,000
        const val HIGH_VALUE_LIFETIME_SALES = 10000.0
        const val HIGH_VALUE_PERIOD_SALES = 5000.0

        // Frequent: >= 3 lifetime orders AND purchase frequency >= 1.5 orders per month
        const val FREQUENT_MIN_LIFETIME_ORDERS = 3
        const val FREQUENT_MIN_ORDERS_PER_MONTH = 1.5

        // Inactivity: Days since last purchase >= 45 days
        const val INACTIVE_DAYS_THRESHOLD = 45L

        // Active: Days since last purchase <= 30 days
        const val ACTIVE_DAYS_THRESHOLD = 30L

        // New customer: First purchase within last 14 days OR customer account created within last 14 days
        const val NEW_CUSTOMER_DAYS_THRESHOLD = 14L
    }

    /**
     * Analyze a list of customers and sales deterministically.
     *
     * @param customers List of Customer entities
     * @param sales List of Sale entities
     * @param analysisDays Analysis period window (default 30 days)
     * @param currentTime Reference epoch timestamp in milliseconds (defaults to System.currentTimeMillis)
     */
    fun analyze(
        customers: List<Customer>,
        sales: List<Sale>,
        analysisDays: Int = DEFAULT_ANALYSIS_DAYS,
        currentTime: Long = System.currentTimeMillis()
    ): CustomerIntelligenceSummary {
        if (customers.isEmpty()) {
            return CustomerIntelligenceSummary(
                totalCustomersAnalyzed = 0,
                activeCustomerCount = 0,
                inactiveCustomerCount = 0,
                newCustomerCount = 0,
                frequentCustomerCount = 0,
                highValueCustomerCount = 0,
                regularCustomerCount = 0,
                insufficientDataCount = 0,
                totalCustomerSalesPeriod = 0.0,
                totalCustomerSalesLifetime = 0.0,
                totalOutstandingAmount = 0.0,
                customersWithOutstandingCount = 0,
                customers = emptyList()
            )
        }

        val effectiveAnalysisDays = if (analysisDays <= 0) DEFAULT_ANALYSIS_DAYS else analysisDays
        val periodCutoff = currentTime - (effectiveAnalysisDays * MS_PER_DAY)

        // Group sales by customerId (exclude sales with null/blank customerId or unresolvable customers)
        val validCustomerIds = customers.map { it.id }.toSet()
        val customerSalesMap = sales
            .filter { it.customerId != null && it.customerId in validCustomerIds }
            .groupBy { it.customerId!! }

        val metricsList = customers.map { customer ->
            analyzeCustomer(
                customer = customer,
                customerSales = customerSalesMap[customer.id] ?: emptyList(),
                periodCutoff = periodCutoff,
                currentTime = currentTime
            )
        }

        val activeCount = metricsList.count { it.activityStatus == CustomerActivityStatus.ACTIVE }
        val inactiveCount = metricsList.count { it.activityStatus == CustomerActivityStatus.INACTIVE }
        val newCount = metricsList.count { it.activityStatus == CustomerActivityStatus.NEW }
        val insufficientCount = metricsList.count { it.activityStatus == CustomerActivityStatus.INSUFFICIENT_DATA }

        val frequentCount = metricsList.count { it.segment == CustomerSegment.FREQUENT }
        val highValueCount = metricsList.count { it.segment == CustomerSegment.HIGH_VALUE }
        val regularCount = metricsList.count { it.segment == CustomerSegment.REGULAR }

        val totalPeriodSales = metricsList.sumOf { it.periodPurchases }
        val totalLifetimeSales = metricsList.sumOf { it.lifetimePurchases }
        val totalOutstanding = metricsList.sumOf { it.outstandingAmount }
        val customersWithOutstanding = metricsList.count { it.hasOutstanding }

        return CustomerIntelligenceSummary(
            totalCustomersAnalyzed = metricsList.size,
            activeCustomerCount = activeCount,
            inactiveCustomerCount = inactiveCount,
            newCustomerCount = newCount,
            frequentCustomerCount = frequentCount,
            highValueCustomerCount = highValueCount,
            regularCustomerCount = regularCount,
            insufficientDataCount = insufficientCount,
            totalCustomerSalesPeriod = totalPeriodSales,
            totalCustomerSalesLifetime = totalLifetimeSales,
            totalOutstandingAmount = totalOutstanding,
            customersWithOutstandingCount = customersWithOutstanding,
            customers = metricsList
        )
    }

    private fun analyzeCustomer(
        customer: Customer,
        customerSales: List<Sale>,
        periodCutoff: Long,
        currentTime: Long
    ): CustomerMetrics {
        val outstanding = if (customer.outstandingAmount.isFinite() && customer.outstandingAmount > 0.0) {
            customer.outstandingAmount
        } else {
            0.0
        }
        val totalPaid = if (customer.totalPaid.isFinite() && customer.totalPaid > 0.0) {
            customer.totalPaid
        } else {
            0.0
        }

        // Handle case with no recorded sales
        if (customerSales.isEmpty()) {
            val accountAgeDays = if (customer.createdAt > 0 && customer.createdAt <= currentTime) {
                ((currentTime - customer.createdAt) / MS_PER_DAY).coerceAtLeast(0L)
            } else {
                null
            }

            val isNewAccount = accountAgeDays != null && accountAgeDays <= Thresholds.NEW_CUSTOMER_DAYS_THRESHOLD
            val segment = if (isNewAccount) CustomerSegment.NEW else CustomerSegment.INSUFFICIENT_DATA
            val activityStatus = if (isNewAccount) CustomerActivityStatus.NEW else CustomerActivityStatus.INSUFFICIENT_DATA

            // Fallback to customer's stored totalPurchases if recorded
            val fallbackLifetime = if (customer.totalPurchases.isFinite() && customer.totalPurchases > 0.0) {
                customer.totalPurchases
            } else {
                0.0
            }

            return CustomerMetrics(
                customerId = customer.id,
                customerName = customer.name,
                phone = customer.phone,
                periodPurchases = 0.0,
                periodOrders = 0.0.toInt(),
                periodUnitsPurchased = 0,
                lifetimePurchases = fallbackLifetime,
                lifetimeOrders = 0,
                lifetimeUnitsPurchased = 0,
                averageOrderValue = 0.0,
                firstPurchaseDate = null,
                lastPurchaseDate = null,
                daysSinceLastPurchase = null,
                purchaseFrequencyPerMonth = 0.0,
                outstandingAmount = outstanding,
                hasOutstanding = outstanding > 0.0,
                totalPaid = totalPaid,
                segment = segment,
                activityStatus = activityStatus,
                hasSufficientData = false
            )
        }

        // Filter valid sales (timestamp <= currentTime)
        val validSales = customerSales.filter { it.createdAt in 1..currentTime }
        if (validSales.isEmpty()) {
            return CustomerMetrics(
                customerId = customer.id,
                customerName = customer.name,
                phone = customer.phone,
                periodPurchases = 0.0,
                periodOrders = 0,
                periodUnitsPurchased = 0,
                lifetimePurchases = 0.0,
                lifetimeOrders = 0,
                lifetimeUnitsPurchased = 0,
                averageOrderValue = 0.0,
                firstPurchaseDate = null,
                lastPurchaseDate = null,
                daysSinceLastPurchase = null,
                purchaseFrequencyPerMonth = 0.0,
                outstandingAmount = outstanding,
                hasOutstanding = outstanding > 0.0,
                totalPaid = totalPaid,
                segment = CustomerSegment.INSUFFICIENT_DATA,
                activityStatus = CustomerActivityStatus.INSUFFICIENT_DATA,
                hasSufficientData = false
            )
        }

        // Lifetime calculations
        val sortedSales = validSales.sortedBy { it.createdAt }
        val firstSaleDate = sortedSales.first().createdAt
        val lastSaleDate = sortedSales.last().createdAt

        val lifetimeOrders = sortedSales.size
        val lifetimePurchases = sortedSales.sumOf { if (it.grandTotal.isFinite() && it.grandTotal > 0.0) it.grandTotal else 0.0 }
        val lifetimeUnits = sortedSales.sumOf { sale ->
            sale.items.sumOf { item -> if (item.quantity > 0) item.quantity else 0 }
        }

        val rawAov = if (lifetimeOrders > 0) lifetimePurchases / lifetimeOrders else 0.0
        val averageOrderValue = if (rawAov.isFinite()) rawAov else 0.0

        val daysSinceLastPurchase = ((currentTime - lastSaleDate) / MS_PER_DAY).coerceAtLeast(0L)

        // Frequency calculation: orders per 30-day period across customer's lifespan
        val customerSpanDays = ((currentTime - firstSaleDate) / MS_PER_DAY).coerceAtLeast(1L)
        val monthsActive = (customerSpanDays.toDouble() / 30.0).coerceAtLeast(1.0)
        val rawFrequency = lifetimeOrders.toDouble() / monthsActive
        val purchaseFrequencyPerMonth = if (rawFrequency.isFinite()) rawFrequency else 0.0

        // Analysis period calculations
        val periodSales = sortedSales.filter { it.createdAt >= periodCutoff }
        val periodOrders = periodSales.size
        val periodPurchases = periodSales.sumOf { if (it.grandTotal.isFinite() && it.grandTotal > 0.0) it.grandTotal else 0.0 }
        val periodUnits = periodSales.sumOf { sale ->
            sale.items.sumOf { item -> if (item.quantity > 0) item.quantity else 0 }
        }

        // Determine activity status
        val firstPurchaseAgeDays = ((currentTime - firstSaleDate) / MS_PER_DAY).coerceAtLeast(0L)
        val isNewCustomer = firstPurchaseAgeDays <= Thresholds.NEW_CUSTOMER_DAYS_THRESHOLD

        val activityStatus = when {
            isNewCustomer -> CustomerActivityStatus.NEW
            daysSinceLastPurchase <= Thresholds.ACTIVE_DAYS_THRESHOLD -> CustomerActivityStatus.ACTIVE
            daysSinceLastPurchase >= Thresholds.INACTIVE_DAYS_THRESHOLD -> CustomerActivityStatus.INACTIVE
            else -> CustomerActivityStatus.ACTIVE
        }

        // Determine customer segment (Priority: HIGH_VALUE -> FREQUENT -> NEW -> INACTIVE -> REGULAR)
        val isHighValue = lifetimePurchases >= Thresholds.HIGH_VALUE_LIFETIME_SALES ||
                periodPurchases >= Thresholds.HIGH_VALUE_PERIOD_SALES

        val isFrequent = lifetimeOrders >= Thresholds.FREQUENT_MIN_LIFETIME_ORDERS &&
                purchaseFrequencyPerMonth >= Thresholds.FREQUENT_MIN_ORDERS_PER_MONTH

        val isInactive = daysSinceLastPurchase >= Thresholds.INACTIVE_DAYS_THRESHOLD

        val segment = when {
            isHighValue -> CustomerSegment.HIGH_VALUE
            isFrequent -> CustomerSegment.FREQUENT
            isNewCustomer -> CustomerSegment.NEW
            isInactive -> CustomerSegment.INACTIVE
            else -> CustomerSegment.REGULAR
        }

        return CustomerMetrics(
            customerId = customer.id,
            customerName = customer.name,
            phone = customer.phone,
            periodPurchases = periodPurchases,
            periodOrders = periodOrders,
            periodUnitsPurchased = periodUnits,
            lifetimePurchases = lifetimePurchases,
            lifetimeOrders = lifetimeOrders,
            lifetimeUnitsPurchased = lifetimeUnits,
            averageOrderValue = averageOrderValue,
            firstPurchaseDate = firstSaleDate,
            lastPurchaseDate = lastSaleDate,
            daysSinceLastPurchase = daysSinceLastPurchase,
            purchaseFrequencyPerMonth = purchaseFrequencyPerMonth,
            outstandingAmount = outstanding,
            hasOutstanding = outstanding > 0.0,
            totalPaid = totalPaid,
            segment = segment,
            activityStatus = activityStatus,
            hasSufficientData = true
        )
    }
}
