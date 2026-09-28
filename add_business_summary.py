import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    content = f.read()

summary_logic = """
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
            
            // Simplified profit: Sales - Purchases - Expenses
            // Note: A real accounting system might use COGS instead of Purchases, but for this phase we use Cash-flow approximation or Purchases.
            // Wait, standard accounting for small shops: Profit = Sales - (COGS) - Expenses. Let's use Purchases as proxy for simplicity, or just Sales - Expenses if Purchases aren't recorded. Let's do Sales - Expenses - Purchases for now, or just Sales - Expenses. The prompt mentions "estimated profit according to the application's existing accounting logic".
            // Let's check if there's existing profit calculation.
"""
