import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    content = f.read()

summary_function = """
    fun getBusinessSummary(): com.example.domain.model.BusinessSummary? {
        val bid = currentBusiness.value?.id ?: return null
        
        val now = java.util.Calendar.getInstance()
        val currentMonth = now.get(java.util.Calendar.MONTH)
        val currentYear = now.get(java.util.Calendar.YEAR)
        
        now.add(java.util.Calendar.MONTH, -1)
        val previousMonth = now.get(java.util.Calendar.MONTH)
        val previousYear = now.get(java.util.Calendar.YEAR)
        
        val allSales = sales.value.filter { it.businessId == bid }
        val allPurchases = purchases.value.filter { it.businessId == bid }
        val allExpenses = expenses.value.filter { it.businessId == bid }
        val allCustomers = customers.value.filter { it.businessId == bid && it.isActive }
        val allSuppliers = suppliers.value.filter { it.businessId == bid && it.isActive }
        val allProducts = products.value.filter { it.businessId == bid && it.isActive }
        
        fun isInMonth(timestamp: Long, month: Int, year: Int): Boolean {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = timestamp
            return cal.get(java.util.Calendar.MONTH) == month && cal.get(java.util.Calendar.YEAR) == year
        }
        
        fun calculateMetrics(periodName: String, month: Int, year: Int): com.example.domain.model.PeriodMetrics {
            val periodSales = allSales.filter { isInMonth(it.timestamp, month, year) }
            val periodPurchases = allPurchases.filter { isInMonth(it.timestamp, month, year) }
            val periodExpenses = allExpenses.filter { isInMonth(it.timestamp, month, year) }
            
            val totalSales = periodSales.sumOf { it.grandTotal }
            val totalPurchases = periodPurchases.sumOf { it.grandTotal }
            val totalExpenses = periodExpenses.sumOf { it.amount }
            
            val cogs = periodSales.sumOf { sale -> sale.items.sumOf { it.quantity * it.purchasePriceAtSale } }
            val profit = totalSales - cogs - totalExpenses
            val profitMargin = if (totalSales > 0) (profit / totalSales) * 100.0 else 0.0
            
            return com.example.domain.model.PeriodMetrics(
                periodName = periodName,
                totalSales = totalSales,
                totalPurchases = totalPurchases,
                totalExpenses = totalExpenses,
                profit = profit,
                profitMargin = profitMargin,
                salesTransactionCount = periodSales.size,
                purchaseTransactionCount = periodPurchases.size,
                expenseTransactionCount = periodExpenses.size
            )
        }
        
        val currentMetrics = calculateMetrics("Current Month", currentMonth, currentYear)
        val previousMetrics = calculateMetrics("Previous Month", previousMonth, previousYear)
        
        val customerOutstanding = allCustomers.sumOf { it.outstandingAmount }
        val supplierBalance = allSuppliers.sumOf { it.outstandingAmount } // Note: Check how supplier outstanding is tracked. Might be outstandingAmount or totalPaid vs purchases. Let's assume outstandingAmount for consistency.
        val inventoryValue = allProducts.sumOf { it.currentStock * it.purchasePrice }
        
        // Calculate Business Health Score (0-100) exactly as in UI, but more robust
        var healthScore = 100
        val reasons = mutableListOf<String>()
        
        if (allProducts.isEmpty() && allSales.isEmpty()) {
            healthScore = 0
            reasons.add("Add products and sales to calculate health.")
        } else {
            if (customerOutstanding > 50000) {
                healthScore -= 10
                reasons.add("High customer outstanding")
            }
            if (currentMetrics.profit < 0) {
                healthScore -= 20
                reasons.add("Operating at a loss this month")
            }
            if (previousMetrics.totalSales > 0 && currentMetrics.totalSales < previousMetrics.totalSales) {
                healthScore -= 10
                reasons.add("Sales decreased compared to last month")
            }
            if (currentMetrics.totalSales > 0 && currentMetrics.profitMargin > 15.0) {
                reasons.add("Healthy profit margin")
            }
            if (currentMetrics.totalSales > previousMetrics.totalSales) {
                reasons.add("Sales are growing")
            }
        }
        
        val healthStatus = when {
            healthScore >= 80 -> "Excellent"
            healthScore >= 60 -> "Good"
            healthScore >= 40 -> "Needs Attention"
            else -> "Critical"
        }
        
        // Top expenses
        val expenseMap = allExpenses.filter { isInMonth(it.timestamp, currentMonth, currentYear) }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        val topExpenses = expenseMap.entries.sortedByDescending { it.value }.take(3).map { it.key }
        
        // Top products
        val productSalesMap = mutableMapOf<String, Int>()
        allSales.filter { isInMonth(it.timestamp, currentMonth, currentYear) }.forEach { sale ->
            sale.items.forEach { item ->
                productSalesMap[item.productId] = productSalesMap.getOrDefault(item.productId, 0) + item.quantity
            }
        }
        val topProductIds = productSalesMap.entries.sortedByDescending { it.value }.take(3).map { it.key }
        val topProductNames = topProductIds.mapNotNull { pid -> allProducts.find { it.id == pid }?.name }
        
        val lowStockProducts = allProducts.filter { it.currentStock <= it.minStock }.map { it.name }
        
        return com.example.domain.model.BusinessSummary(
            currentPeriod = currentMetrics,
            previousPeriod = previousMetrics,
            customerOutstanding = customerOutstanding,
            supplierBalance = supplierBalance,
            inventoryValue = inventoryValue,
            numberOfCustomers = allCustomers.size,
            numberOfSuppliers = allSuppliers.size,
            numberOfProducts = allProducts.size,
            healthScore = healthScore,
            healthStatus = healthStatus,
            healthReasons = reasons,
            topExpenseCategories = topExpenses,
            topProducts = topProductNames,
            lowStockProducts = lowStockProducts
        )
    }
"""

if 'fun getBusinessSummary' not in content:
    content = content.replace("object MockDataStore {", "object MockDataStore {\n" + summary_function)
    with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
        f.write(content)
    print("Added getBusinessSummary")
else:
    print("Already exists")
