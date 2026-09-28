import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    content = f.read()

forecast_function = """
    fun getBusinessForecast(): com.example.domain.model.BusinessForecast? {
        val bid = currentBusiness.value?.id ?: return null
        
        val now = java.util.Calendar.getInstance()
        val currentMonth = now.get(java.util.Calendar.MONTH)
        val currentYear = now.get(java.util.Calendar.YEAR)
        
        val thirtyDaysAgo = java.util.Calendar.getInstance()
        thirtyDaysAgo.add(java.util.Calendar.DAY_OF_YEAR, -30)
        val sixtyDaysAgo = java.util.Calendar.getInstance()
        sixtyDaysAgo.add(java.util.Calendar.DAY_OF_YEAR, -60)
        
        val allSales = sales.value.filter { it.businessId == bid }
        val allPurchases = purchases.value.filter { it.businessId == bid }
        val allExpenses = expenses.value.filter { it.businessId == bid }
        val allCustomers = customers.value.filter { it.businessId == bid && it.isActive }
        val allSuppliers = suppliers.value.filter { it.businessId == bid && it.isActive }
        val allProducts = products.value.filter { it.businessId == bid && it.isActive }
        
        val recentSales = allSales.filter { it.createdAt >= thirtyDaysAgo.timeInMillis }
        val previousSales = allSales.filter { it.createdAt in sixtyDaysAgo.timeInMillis until thirtyDaysAgo.timeInMillis }
        
        val recentExpenses = allExpenses.filter { it.createdAt >= thirtyDaysAgo.timeInMillis }
        val previousExpenses = allExpenses.filter { it.createdAt in sixtyDaysAgo.timeInMillis until thirtyDaysAgo.timeInMillis }
        
        val recentPurchases = allPurchases.filter { it.createdAt >= thirtyDaysAgo.timeInMillis }
        
        // Confidence calculation
        val transactionCount = allSales.size + allExpenses.size + allPurchases.size
        val confidence = when {
            transactionCount < 5 -> "Low"
            transactionCount < 20 -> "Medium"
            else -> "High"
        }
        
        if (transactionCount < 2) {
             return com.example.domain.model.BusinessForecast(
                 cashFlowRisk = "Unknown",
                 cashFlowMessage = "Not enough business history to make a reliable forecast.",
                 salesTrend = "Unknown",
                 salesMessage = "Not enough business history to make a reliable forecast.",
                 expenseTrend = "Unknown",
                 expenseMessage = "Not enough business history to make a reliable forecast.",
                 inventoryRisk = "Unknown",
                 inventoryMessage = "Not enough sales history to estimate stock demand.",
                 outstandingRisk = "Unknown",
                 outstandingMessage = "Not enough business history to make a reliable forecast.",
                 overallRisk = "Unknown",
                 confidence = "Low"
             )
        }
        
        // Cash Flow
        val recentInflow = recentSales.sumOf { it.amountPaid }
        val recentOutflow = recentExpenses.sumOf { it.amount } + recentPurchases.sumOf { it.amountPaid }
        val netCashFlow = recentInflow - recentOutflow
        val projectedCash30Days = netCashFlow
        
        val cashFlowRisk: String
        val cashFlowMessage: String
        if (netCashFlow < 0) {
            cashFlowRisk = "High"
            cashFlowMessage = "Your cash flow may become tight in the next 30 days. Expected net cash position: ₹${String.format("%,.0f", projectedCash30Days)}"
        } else if (netCashFlow < 5000) {
            cashFlowRisk = "Moderate"
            cashFlowMessage = "Cash flow is positive but low. Expected net cash position in next 30 days: ₹${String.format("%,.0f", projectedCash30Days)}"
        } else {
            cashFlowRisk = "Low"
            cashFlowMessage = "Healthy cash flow expected. Expected net cash position in next 30 days: ₹${String.format("%,.0f", projectedCash30Days)}"
        }
        
        // Sales Trend
        val recentSalesTotal = recentSales.sumOf { it.grandTotal }
        val previousSalesTotal = previousSales.sumOf { it.grandTotal }
        val salesTrend: String
        val salesMessage: String
        
        if (previousSalesTotal == 0.0) {
             salesTrend = "Stable"
             salesMessage = "Recent sales total ₹${String.format("%,.0f", recentSalesTotal)}. Keep recording to see trends."
        } else {
            val change = ((recentSalesTotal - previousSalesTotal) / previousSalesTotal) * 100
            if (change > 10) {
                salesTrend = "Increasing"
                salesMessage = "Sales increased by ${String.format("%.0f", change)}% compared with the previous 30 days."
            } else if (change < -10) {
                salesTrend = "Decreasing"
                salesMessage = "Sales have declined by ${String.format("%.0f", Math.abs(change))}% recently."
            } else {
                salesTrend = "Stable"
                salesMessage = "Sales are stable compared to the previous period."
            }
        }
        
        // Expense Trend
        val recentExpensesTotal = recentExpenses.sumOf { it.amount }
        val previousExpensesTotal = previousExpenses.sumOf { it.amount }
        val expenseTrend: String
        val expenseMessage: String
        
        if (previousExpensesTotal == 0.0 && recentExpensesTotal > 0) {
            expenseTrend = "Increasing"
            expenseMessage = "Expenses recorded recently. Monitor for trends."
        } else if (previousExpensesTotal == 0.0) {
            expenseTrend = "Stable"
            expenseMessage = "No expenses recorded recently."
        } else {
             val change = ((recentExpensesTotal - previousExpensesTotal) / previousExpensesTotal) * 100
             if (change > 15) {
                 expenseTrend = "Increasing"
                 expenseMessage = "Operating expenses increased significantly by ${String.format("%.0f", change)}% recently."
             } else {
                 expenseTrend = "Stable"
                 expenseMessage = "Expenses are under control and stable."
             }
        }
        
        // Inventory Risk
        var lowStockCount = 0
        var fastMovingStockOutRisk = 0
        allProducts.forEach { product ->
            if (product.currentStock <= product.minStock) lowStockCount++
            
            // Check sales velocity
            val productSales = recentSales.sumOf { sale -> sale.items.filter { it.productId == product.id }.sumOf { it.quantity } }
            if (productSales > 0 && product.currentStock > 0) {
                val dailySales = productSales / 30.0
                val daysRemaining = product.currentStock / dailySales
                if (daysRemaining < 7) {
                    fastMovingStockOutRisk++
                }
            }
        }
        
        val inventoryRisk: String
        val inventoryMessage: String
        if (fastMovingStockOutRisk > 0) {
            inventoryRisk = "High"
            inventoryMessage = "$fastMovingStockOutRisk fast-moving product(s) may run out in approximately 7 days based on recent sales."
        } else if (lowStockCount > 0) {
            inventoryRisk = "Moderate"
            inventoryMessage = "$lowStockCount product(s) are below minimum stock level."
        } else {
            inventoryRisk = "Low"
            inventoryMessage = "Inventory levels appear healthy based on recent sales."
        }
        
        // Outstanding Risk
        val customerOutstanding = allCustomers.sumOf { it.outstandingAmount }
        val outstandingRisk: String
        val outstandingMessage: String
        
        if (customerOutstanding > recentSalesTotal * 0.5 && recentSalesTotal > 0) {
            outstandingRisk = "High"
            outstandingMessage = "High outstanding relative to sales. ₹${String.format("%,.0f", customerOutstanding)} is pending."
        } else if (customerOutstanding > 0) {
            val highConcentration = allCustomers.any { it.outstandingAmount > customerOutstanding * 0.5 }
            if (highConcentration && allCustomers.size > 2) {
                outstandingRisk = "Moderate"
                outstandingMessage = "A significant portion of your receivables is concentrated among a few customers."
            } else {
                outstandingRisk = "Low"
                outstandingMessage = "Outstanding amounts are within typical limits."
            }
        } else {
            outstandingRisk = "Low"
            outstandingMessage = "No significant outstanding balances."
        }
        
        // Overall Risk
        var highRiskCount = 0
        var modRiskCount = 0
        listOf(cashFlowRisk, outstandingRisk, inventoryRisk).forEach {
            if (it == "High") highRiskCount++
            else if (it == "Moderate") modRiskCount++
        }
        
        val overallRisk = when {
            highRiskCount >= 1 -> "High"
            modRiskCount >= 2 -> "Moderate"
            else -> "Low"
        }
        
        return com.example.domain.model.BusinessForecast(
            cashFlowRisk = cashFlowRisk,
            cashFlowMessage = cashFlowMessage,
            salesTrend = salesTrend,
            salesMessage = salesMessage,
            expenseTrend = expenseTrend,
            expenseMessage = expenseMessage,
            inventoryRisk = inventoryRisk,
            inventoryMessage = inventoryMessage,
            outstandingRisk = outstandingRisk,
            outstandingMessage = outstandingMessage,
            overallRisk = overallRisk,
            confidence = confidence
        )
    }
"""

if 'fun getBusinessForecast' not in content:
    content = content.replace("object MockDataStore {", "object MockDataStore {\n" + forecast_function)
    with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
        f.write(content)
    print("Added getBusinessForecast")
else:
    print("Already exists")
