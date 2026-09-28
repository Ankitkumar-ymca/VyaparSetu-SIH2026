package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

import com.example.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * MOCK DEVELOPMENT AUTHENTICATION & DATABASE
 * This completely isolates the UI from the backend implementation.
 * Currently uses an in-memory datastore to simulate backend latency and state.
 * To be replaced with actual Firebase implementations in a future phase.
 */
object MockDataStore {

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
            val periodSales = allSales.filter { isInMonth(it.createdAt, month, year) }
            val periodPurchases = allPurchases.filter { isInMonth(it.createdAt, month, year) }
            val periodExpenses = allExpenses.filter { isInMonth(it.createdAt, month, year) }
            
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
        val supplierBalance = allSuppliers.sumOf { it.outstandingPayable } // Note: Check how supplier outstanding is tracked. Might be outstandingAmount or totalPaid vs purchases. Let's assume outstandingAmount for consistency.
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
        val expenseMap = allExpenses.filter { isInMonth(it.createdAt, currentMonth, currentYear) }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        val topExpenses = expenseMap.entries.sortedByDescending { it.value }.take(3).map { it.key }
        
        // Top products
        val productSalesMap = mutableMapOf<String, Int>()
        allSales.filter { isInMonth(it.createdAt, currentMonth, currentYear) }.forEach { sale ->
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

    private var prefs: SharedPreferences? = null
    private var db: AppDatabase? = null
    private var dao: AppDao? = null
    private var storedUsers: MutableList<User> = mutableListOf()
    private var storedBusinesses: MutableList<Business> = mutableListOf()
    
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("mock_data_store", Context.MODE_PRIVATE)
                val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Product ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE Customer ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE Supplier ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }
        db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "vyapar_db").addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        dao = db?.appDao()
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val prefs = prefs ?: return
        try {
            storedUsers = dao?.getUsers()?.toMutableList() ?: mutableListOf()
            storedBusinesses = dao?.getBusinesses()?.toMutableList() ?: mutableListOf()
            _products.value = dao?.getProducts() ?: emptyList()
            _customers.value = dao?.getCustomers() ?: emptyList()
            _suppliers.value = dao?.getSuppliers() ?: emptyList()
            _sales.value = dao?.getSales() ?: emptyList()
            _purchases.value = dao?.getPurchases() ?: emptyList()
            _expenses.value = dao?.getExpenses() ?: emptyList()
            _stockAdjustments.value = dao?.getStockAdjustments() ?: emptyList()
            _customerPayments.value = dao?.getCustomerPayments() ?: emptyList()
            _stockTransactions.value = dao?.getStockTransactions() ?: emptyList()
            _ledgerEntries.value = dao?.getLedgerEntries() ?: emptyList()
            _auditLogs.value = dao?.getAuditLogs() ?: emptyList()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val chatJson = prefs.getString("chat_history", null)
            if (chatJson != null) {
                _chatMessages.value = Json.decodeFromString<List<ChatMessage>>(chatJson)
            } else {
                _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
            }
        } catch (e: Exception) {
            _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
        }

        val currentUserId = prefs.getString("currentUserId", null)
        if (currentUserId != null) {
            val user = storedUsers.find { it.id == currentUserId }
            if (user != null) {
                _currentUser.value = user
                if (user.businessId != null) {
                    _currentBusiness.value = storedBusinesses.find { it.id == user.businessId }
                }
            }
        }
    }

    private fun saveToPrefs() {
        val prefs = prefs ?: return
        prefs.edit().apply {
            putString("currentUserId", _currentUser.value?.id)
            apply()
        }
    }
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun clearChatHistory() {
        _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
        saveToPrefs()
    }
    
    fun deleteChatMessage(index: Int) {
        val list = _chatMessages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _chatMessages.value = list
            saveToPrefs()
        }
    }
    
    fun updateChatMessages(messages: List<ChatMessage>) {
        _chatMessages.value = messages
        saveChatToPrefs()
    }
    
    private fun saveChatToPrefs() {
        val prefs = prefs ?: return
        try {
            val jsonString = Json.encodeToString(_chatMessages.value)
            prefs.edit().putString("chat_history", jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isOnline = MutableStateFlow(true)
    val isDemoMode = MutableStateFlow(false)
    val targetDashboardTab = MutableStateFlow<String?>(null)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentBusiness = MutableStateFlow<Business?>(null)
    val currentBusiness: StateFlow<Business?> = _currentBusiness.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    private val _stockAdjustments = MutableStateFlow<List<StockAdjustment>>(emptyList())
    val stockAdjustments: StateFlow<List<StockAdjustment>> = _stockAdjustments.asStateFlow()

    private val _customerPayments = MutableStateFlow<List<CustomerPayment>>(emptyList())
    val customerPayments: StateFlow<List<CustomerPayment>> = _customerPayments.asStateFlow()

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _stockTransactions = MutableStateFlow<List<StockTransaction>>(emptyList())
    val stockTransactions: StateFlow<List<StockTransaction>> = _stockTransactions.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val ledgerEntries: StateFlow<List<LedgerEntry>> = _ledgerEntries.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    // Real state backups for Demo Mode
    private var realProducts = emptyList<Product>()
    private var realCustomers = emptyList<Customer>()
    private var realSuppliers = emptyList<Supplier>()
    private var realSales = emptyList<Sale>()
    private var realPurchases = emptyList<Purchase>()
    private var realExpenses = emptyList<Expense>()
    private var realLedger = emptyList<LedgerEntry>()
    private var realStockTx = emptyList<StockTransaction>()
    private var realAuditLogs = emptyList<AuditLog>()

    // Simulated network delay
    private const val NETWORK_DELAY = 100L

    fun toggleNetwork() {
        isOnline.value = !isOnline.value
        if (isOnline.value) {
            triggerSync()
        }
    }

    private fun triggerSync() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            _sales.value = _sales.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
            _purchases.value = _purchases.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
            _expenses.value = _expenses.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
            _customerPayments.value = _customerPayments.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
            _ledgerEntries.value = _ledgerEntries.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
            _stockTransactions.value = _stockTransactions.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }
        }
    }

    private fun getSyncStatus(): SyncStatus = if (isOnline.value) SyncStatus.SYNCED else SyncStatus.PENDING

    private fun logAudit(action: String, entityType: String, entityId: String, details: String = "") {
        val businessId = _currentBusiness.value?.id ?: return
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            timestamp = System.currentTimeMillis(),
            details = details
        )
        _auditLogs.value = _auditLogs.value + log
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertAuditLog(log) }
    }

    fun enterDemoMode() {
        if (isDemoMode.value) return
        
        // Backup real state
        realProducts = _products.value
        realCustomers = _customers.value
        realSuppliers = _suppliers.value
        realSales = _sales.value
        realPurchases = _purchases.value
        realExpenses = _expenses.value
        realLedger = _ledgerEntries.value
        realStockTx = _stockTransactions.value
        realAuditLogs = _auditLogs.value

        isDemoMode.value = true
        
        val bid = _currentBusiness.value?.id ?: "demo_business"
        val now = System.currentTimeMillis()
        val oneDay = 86400000L
        
        // Setup Demo Data (10 Products)
        val p1 = Product("p1", bid, "Parle G Biscuits", "SKU-001", "8901719102434", "Groceries", 8.0, 10.0, 150, 50, null, null, now, now)
        val p2 = Product("p2", bid, "Aashirvaad Atta 5kg", "SKU-002", "8901725112245", "Groceries", 195.0, 220.0, 12, 10, null, null, now, now)
        val p3 = Product("p3", bid, "Maggi Noodles 140g", "SKU-003", "8901058000451", "Groceries", 22.0, 25.0, 80, 20, null, null, now, now)
        val p4 = Product("p4", bid, "Amul Butter 100g", "SKU-004", "8901262010041", "Dairy", 45.0, 52.0, 45, 15, null, null, now, now)
        val p5 = Product("p5", bid, "Syska LED Bulb 9W", "SKU-005", "8906053350257", "Electronics", 70.0, 120.0, 8, 20, null, null, now, now) // Low Stock Example
        val p6 = Product("p6", bid, "Lifebuoy Soap 100g", "SKU-006", "8901030310247", "Personal Care", 25.0, 32.0, 120, 30, null, null, now, now)
        val p7 = Product("p7", bid, "Dettol Antiseptic 125ml", "SKU-007", "8901396340209", "Personal Care", 55.0, 68.0, 25, 10, null, null, now, now)
        val p8 = Product("p8", bid, "Tide Surf 1kg", "SKU-008", "8901323330101", "Home Care", 110.0, 135.0, 18, 10, null, null, now, now)
        val p9 = Product("p9", bid, "Santoor Soap 150g", "SKU-009", "8901138350111", "Personal Care", 35.0, 45.0, 60, 20, null, null, now, now)
        val p10 = Product("p10", bid, "Tata Salt 1kg", "SKU-010", "8901234567890", "Groceries", 20.0, 25.0, 40, 15, null, null, now, now)
        _products.value = listOf(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
        
        // Setup Demo Data (5 Customers)
        val c1 = Customer("c1", bid, "Rahul Sharma", "9876543210", "Sector 14", "rahul@test.com", 2500.0, 1000.0, 1500.0, now - oneDay*5, now)
        val c2 = Customer("c2", bid, "Priya Singh", "9988776655", "MG Road", "", 520.0, 520.0, 0.0, now - oneDay*10, now)
        val c3 = Customer("c3", bid, "Amit Verma", "9123456780", "Main Market", "", 3400.0, 0.0, 3400.0, now - oneDay*2, now) // High Oustanding
        val c4 = Customer("c4", bid, "Neha Gupta", "8899001122", "Civil Lines", "", 1200.0, 800.0, 400.0, now - oneDay*8, now)
        val c5 = Customer("c5", bid, "Vikram Yadav", "7766554433", "Phase 3", "", 0.0, 0.0, 0.0, now, now)
        _customers.value = listOf(c1, c2, c3, c4, c5)
        
        // Setup Demo Data (3 Suppliers)
        val s1 = Supplier("s1", bid, "Gupta Distributors", "9900112233", "Industrial Area", "gupta@dist.com", 15000.0, 10000.0, 5000.0, now - oneDay*15, now)
        val s2 = Supplier("s2", bid, "MegaMart Wholesale", "8811223344", "New Mandi", "", 8500.0, 8500.0, 0.0, now - oneDay*20, now)
        val s3 = Supplier("s3", bid, "Tech Vision (Electronics)", "9988112233", "Electronic City", "", 4000.0, 1000.0, 3000.0, now - oneDay*5, now)
        _suppliers.value = listOf(s1, s2, s3)
        
        // Setup Some Demo Sales
        val sale1 = Sale("sale1", bid, "INV-1001", c1.id, listOf(SaleItem(p1.id, p1.name, 5, p1.sellingPrice, 50.0, p1.purchasePrice), SaleItem(p2.id, p2.name, 1, p2.sellingPrice, 220.0, p2.purchasePrice)), 270.0, 0.0, 0.0, 270.0, 270.0, 0.0, "PAID", "Cash", now - oneDay*4, now - oneDay*4, SyncStatus.SYNCED)
        val sale2 = Sale("sale2", bid, "INV-1002", c3.id, listOf(SaleItem(p5.id, p5.name, 10, p5.sellingPrice, 1200.0, p5.purchasePrice)), 1200.0, 0.0, 0.0, 1200.0, 0.0, 1200.0, "CREDIT", "UPI", now - oneDay*2, now - oneDay*2, SyncStatus.SYNCED)
        _sales.value = listOf(sale1, sale2)
        
        // Setup Demo Expenses
        val exp1 = Expense("exp1", bid, "Transport", "Auto fare for delivery", 350.0, "Cash", "", now - oneDay*3, now - oneDay*3, SyncStatus.SYNCED)
        val exp2 = Expense("exp2", bid, "Electricity", "Shop bill", 1200.0, "UPI", "", now - oneDay*1, now - oneDay*1, SyncStatus.SYNCED)
        _expenses.value = listOf(exp1, exp2)
        
        // Reset others to empty or derived initial states
        _purchases.value = emptyList()
        _ledgerEntries.value = emptyList()
        _stockTransactions.value = emptyList()
        _auditLogs.value = emptyList()
    }

    fun resetDemoData() {
        if (!isDemoMode.value) return
        isDemoMode.value = false // Temporarily off to bypass the block
        enterDemoMode() // Will reset and repopulate
    }

    fun exitDemoMode() {
        if (!isDemoMode.value) return
        isDemoMode.value = false
        
        // Restore real state
        _products.value = realProducts
        _customers.value = realCustomers
        _suppliers.value = realSuppliers
        _sales.value = realSales
        _purchases.value = realPurchases
        _expenses.value = realExpenses
        _ledgerEntries.value = realLedger
        _stockTransactions.value = realStockTx
        _auditLogs.value = realAuditLogs
    }

    suspend fun login(email: String, password: String): Result<User> {
        delay(NETWORK_DELAY)
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email and password cannot be empty"))
        }
        val existingUser = storedUsers.find { it.email == email || it.phone == email }
        if (existingUser != null) {
            _currentUser.value = existingUser
            _currentBusiness.value = existingUser.businessId?.let { bid -> storedBusinesses.find { it.id == bid } }
            saveToPrefs()
            return Result.success(existingUser)
        }
        return Result.failure(Exception("Invalid credentials. Please try again."))
    }

    suspend fun register(name: String, email: String, phone: String, password: String): Result<User> {
        delay(NETWORK_DELAY)
        val existingUser = storedUsers.find { it.email == email || it.phone == phone }
        if (existingUser != null) {
            return Result.failure(Exception("User already exists"))
        }
        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            phone = phone,
            role = "OWNER"
        )
        storedUsers.add(newUser)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertUser(newUser) }
        _currentUser.value = newUser
        saveToPrefs()
        return Result.success(newUser)
    }

    suspend fun setupBusiness(
        name: String,
        ownerName: String,
        category: String,
        phone: String,
        address: String,
        gstin: String?
    ): Result<Business> {
        delay(NETWORK_DELAY)
        val user = _currentUser.value ?: return Result.failure(Exception("User not authenticated"))
        
        val newBusiness = Business(
            id = UUID.randomUUID().toString(),
            name = name,
            ownerName = ownerName,
            category = category,
            phone = phone,
            address = address,
            gstin = gstin,
            createdAt = System.currentTimeMillis()
        )
        storedBusinesses.add(newBusiness)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertBusiness(newBusiness) }
        val updatedUser = user.copy(businessId = newBusiness.id)
        val userIndex = storedUsers.indexOfFirst { it.id == updatedUser.id }
        if (userIndex != -1) {
            storedUsers[userIndex] = updatedUser
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertUser(updatedUser) }
        }
        _currentBusiness.value = newBusiness
        _currentUser.value = updatedUser
        saveToPrefs()
        return Result.success(newBusiness)
    }

    suspend fun logout() {
        delay(300)
        _currentUser.value = null
        _currentBusiness.value = null
        saveToPrefs()
        _products.value = emptyList()
        _customers.value = emptyList()
        _suppliers.value = emptyList()
        _stockAdjustments.value = emptyList()
        _customerPayments.value = emptyList()
        _sales.value = emptyList()
        _purchases.value = emptyList()
        _expenses.value = emptyList()
        _stockTransactions.value = emptyList()
        _ledgerEntries.value = emptyList()
    }

    // --- PRODUCTS ---
    suspend fun saveProduct(product: Product): Result<Product> {
        delay(NETWORK_DELAY)
        val list = _products.value.toMutableList()
        val index = list.indexOfFirst { it.id == product.id }
        if (index != -1) {
            list[index] = product
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }
        } else {
            list.add(product)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }
        }
        _products.value = list
        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }
        logAudit(if (index != -1) "EDIT" else "CREATE", "Product", product.id, product.name)
        return Result.success(product)
    }

    suspend fun deleteProduct(productId: String) {
        delay(NETWORK_DELAY)
        _products.value = _products.value.filter { it.id != productId }
    }

    suspend fun adjustStock(productId: String, quantityChange: Int, reason: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val businessId = _currentBusiness.value?.id ?: return Result.failure(Exception("No business"))
        val list = _products.value.toMutableList()
        val index = list.indexOfFirst { it.id == productId }
        if (index == -1) return Result.failure(Exception("Product not found"))

        val product = list[index]
        val newStock = product.currentStock + quantityChange
        list[index] = product
        list[index] = product.copy(currentStock = newStock, updatedAt = System.currentTimeMillis())
        _products.value = list
        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }

        val adjustment = StockAdjustment(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            productId = productId,
            quantityChange = quantityChange,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        _stockAdjustments.value = _stockAdjustments.value + adjustment
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertStockAdjustment(adjustment) }
        return Result.success(true)
    }

    // --- CUSTOMERS ---
    suspend fun saveCustomer(customer: Customer): Result<Customer> {
        delay(NETWORK_DELAY)
        val list = _customers.value.toMutableList()
        val index = list.indexOfFirst { it.id == customer.id }
        if (index != -1) {
            list[index] = customer
        } else {
            list.add(customer)
        }
        _customers.value = list
        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(it) } }
        logAudit(if (index != -1) "EDIT" else "CREATE", "Customer", customer.id, customer.name)
        return Result.success(customer)
    }

    suspend fun deleteCustomer(customerId: String) {
        delay(NETWORK_DELAY)
        _customers.value = _customers.value.filter { it.id != customerId }
    }


    suspend fun addCustomerPreviousOutstanding(customerId: String, amount: Double, notes: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val customer = _customers.value.find { it.id == customerId } ?: return Result.failure(Exception("Customer not found"))
        val updatedCustomer = customer.copy(
            outstandingAmount = customer.outstandingAmount + amount
        )
        val list = _customers.value.toMutableList()
        val index = list.indexOfFirst { it.id == customerId }
        list[index] = updatedCustomer
        _customers.value = list
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(updatedCustomer) }
        
        // Add a ledger entry to reflect the opening balance
        val ledgerEntry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            businessId = customer.businessId,
            partyId = customerId,
            type = "OPENING_BALANCE",
            amount = amount,
            balanceAfter = updatedCustomer.outstandingAmount,
            description = notes,
            timestamp = System.currentTimeMillis(),
            syncStatus = getSyncStatus()
        )
        val ledgerList = _ledgerEntries.value.toMutableList()
        ledgerList.add(ledgerEntry)
        _ledgerEntries.value = ledgerList
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertLedgerEntry(ledgerEntry) }
        
        return Result.success(true)
    }

    suspend fun recordCustomerPayment(customerId: String, amount: Double, method: String, notes: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val businessId = _currentBusiness.value?.id ?: return Result.failure(Exception("No business"))
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than zero"))

        val list = _customers.value.toMutableList()
        val index = list.indexOfFirst { it.id == customerId }
        if (index == -1) return Result.failure(Exception("Customer not found"))

        val customer = list[index]
        if (customer.outstandingAmount <= 0) return Result.failure(Exception("No outstanding amount"))
        if (amount > customer.outstandingAmount) return Result.failure(Exception("Cannot pay more than outstanding amount for now"))

        list[index] = customer.copy(
            totalPaid = customer.totalPaid + amount,
            outstandingAmount = customer.outstandingAmount - amount,
            updatedAt = System.currentTimeMillis()
        )
        _customers.value = list
        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(it) } }

        val payment = CustomerPayment(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            customerId = customerId,
            amount = amount,
            paymentMethod = method,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            syncStatus = getSyncStatus()
        )
        _customerPayments.value = _customerPayments.value + payment
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomerPayment(payment) }
        
        val newLedger = LedgerEntry(

            id = UUID.randomUUID().toString(),
            businessId = businessId,
            partyId = customerId,
            type = "PAYMENT",
            amount = amount,
            balanceAfter = list[index].outstandingAmount,
            description = "Payment via $method. $notes",
            timestamp = System.currentTimeMillis(),
            syncStatus = getSyncStatus()
        )
        logAudit("RECORD_PAYMENT", "Customer", customerId, "Amount: $amount")
        return Result.success(true)
    }

    // --- SUPPLIERS ---
    suspend fun saveSupplier(supplier: Supplier): Result<Supplier> {
        delay(NETWORK_DELAY)
        val list = _suppliers.value.toMutableList()
        val index = list.indexOfFirst { it.id == supplier.id }
        if (index != -1) {
            list[index] = supplier
        } else {
            list.add(supplier)
        }
        _suppliers.value = list
        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(it) } }
        return Result.success(supplier)
    }

    suspend fun deleteSupplier(supplierId: String) {
        delay(NETWORK_DELAY)
        _suppliers.value = _suppliers.value.filter { it.id != supplierId }
    }

    // --- EXPENSES ---
suspend fun saveExpense(expense: Expense): Result<Expense> {
        delay(NETWORK_DELAY)
        
        val syncStatus = getSyncStatus()
        val index = _expenses.value.indexOfFirst { it.id == expense.id }
        val finalExpense = if (index != -1) expense else expense.copy(syncStatus = syncStatus)
        
        val list = _expenses.value.toMutableList()
        if (index != -1) {
            list[index] = finalExpense
        } else {
            list.add(finalExpense)
        }
        _expenses.value = list
        
        // Save ONLY the single finalExpense to the DB, not the whole list
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(finalExpense) }
        
        // Record ledger entry
        if (index == -1) { // Only create ledger on new expense
            val newEntry = LedgerEntry(
                id = java.util.UUID.randomUUID().toString(),
                businessId = expense.businessId,
                partyId = expense.id,
                type = "EXPENSE",
                amount = -expense.amount,
                balanceAfter = 0.0,
                description = "Expense: ${expense.category}",
                timestamp = expense.createdAt,
                syncStatus = syncStatus
            )
            _ledgerEntries.value = _ledgerEntries.value + newEntry
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertLedgerEntry(newEntry) }
        }
        
        logAudit(if (index != -1) "EDIT" else "CREATE", "Expense", finalExpense.id, finalExpense.category)
        return Result.success(finalExpense)
    }

    suspend fun deleteExpense(expenseId: String) {
        delay(NETWORK_DELAY)
        _expenses.value = _expenses.value.filter { it.id != expenseId }
    }

    // --- TRANSACTIONS ---
    suspend fun createSale(sale: Sale): Result<Sale> {
        delay(NETWORK_DELAY)
        // 1. Verify stock first
        val prodList = _products.value.toMutableList()
        val stockTxList = _stockTransactions.value.toMutableList()
        val rollbackProdList = _products.value.toList() // For simple in-memory rollback

        val newSaleItems = mutableListOf<SaleItem>()

        for (item in sale.items) {
            val idx = prodList.indexOfFirst { it.id == item.productId }
            if (idx == -1) return Result.failure(Exception("Product ${item.productName} not found"))
            val prod = prodList[idx]
            if (prod.currentStock < item.quantity) {
                return Result.failure(Exception("Insufficient stock for ${item.productName}. Available: ${prod.currentStock}"))
            }
            
            // Add with purchase price
            newSaleItems.add(item.copy(purchasePriceAtSale = prod.purchasePrice))

            val prevStock = prod.currentStock
            val newStock = prevStock - item.quantity
            prodList[idx] = prod.copy(currentStock = newStock, updatedAt = System.currentTimeMillis())
            
            stockTxList.add(
                StockTransaction(
                    id = UUID.randomUUID().toString(),
                    businessId = sale.businessId,
                    productId = item.productId,
                    quantityChange = -item.quantity,
                    previousStock = prevStock,
                    newStock = newStock,
                    transactionType = "SALE",
                    referenceId = sale.id,
                    reason = "Sold via ${sale.invoiceNumber}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // 2. Update Customer if credit/partial and logged
        var updatedCustomer: Customer? = null
        if (sale.customerId != null) {
            val custIdx = _customers.value.indexOfFirst { it.id == sale.customerId }
            if (custIdx != -1) {
                val c = _customers.value[custIdx]
                updatedCustomer = c.copy(
                    totalPurchases = c.totalPurchases + sale.grandTotal,
                    totalPaid = c.totalPaid + sale.amountPaid,
                    outstandingAmount = c.outstandingAmount + sale.outstanding,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

        // 3. Commit ALL
        _products.value = prodList
        prodList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }
        _stockTransactions.value = stockTxList
        stockTxList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertStockTransaction(it) } }
        val finalizedSale = sale.copy(items = newSaleItems, syncStatus = getSyncStatus())
        _sales.value = _sales.value + finalizedSale
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSale(finalizedSale) }
        
        if (updatedCustomer != null) {
            val cList = _customers.value.toMutableList()
            val idx = cList.indexOfFirst { it.id == updatedCustomer.id }
            cList[idx] = updatedCustomer
            _customers.value = cList
        cList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(it) } }
            
            // Add Ledger Entry
            val newLedger = LedgerEntry(

                id = UUID.randomUUID().toString(),
                businessId = sale.businessId,
                partyId = updatedCustomer.id,
                type = "SALE",
                amount = sale.grandTotal,
                balanceAfter = updatedCustomer.outstandingAmount,
                description = "Sale Invoice ${sale.invoiceNumber}",
                timestamp = System.currentTimeMillis(),
                syncStatus = getSyncStatus()
            )
            
            if (sale.amountPaid > 0) {
                 val newLedger = LedgerEntry(

                    id = UUID.randomUUID().toString(),
                    businessId = sale.businessId,
                    partyId = updatedCustomer.id,
                    type = "PAYMENT",
                    amount = sale.amountPaid,
                    balanceAfter = updatedCustomer.outstandingAmount, // Already reflects net
                    description = "Payment for ${sale.invoiceNumber} via ${sale.paymentMethod}",
                    timestamp = System.currentTimeMillis() + 1,
                    syncStatus = getSyncStatus()
                )
            }
        }
        logAudit("CREATE", "Sale", sale.id, sale.invoiceNumber)
        return Result.success(sale)
    }

    suspend fun createPurchase(purchase: Purchase): Result<Purchase> {
        delay(NETWORK_DELAY)
        
        val prodList = _products.value.toMutableList()
        val stockTxList = _stockTransactions.value.toMutableList()
        
        for (item in purchase.items) {
            val idx = prodList.indexOfFirst { it.id == item.productId }
            if (idx != -1) {
                val prod = prodList[idx]
                val prevStock = prod.currentStock
                val newStock = prevStock + item.quantity
                prodList[idx] = prod.copy(currentStock = newStock, updatedAt = System.currentTimeMillis())
                
                stockTxList.add(
                    StockTransaction(
                        id = UUID.randomUUID().toString(),
                        businessId = purchase.businessId,
                        productId = item.productId,
                        quantityChange = item.quantity,
                        previousStock = prevStock,
                        newStock = newStock,
                        transactionType = "PURCHASE",
                        referenceId = purchase.id,
                        reason = "Purchased via ${purchase.referenceNumber}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // Update Supplier
        var updatedSupplier: Supplier? = null
        val supIdx = _suppliers.value.indexOfFirst { it.id == purchase.supplierId }
        if (supIdx != -1) {
            val s = _suppliers.value[supIdx]
            updatedSupplier = s.copy(
                totalPurchases = s.totalPurchases + purchase.grandTotal,
                totalPaid = s.totalPaid + purchase.amountPaid,
                outstandingPayable = s.outstandingPayable + purchase.outstanding,
                updatedAt = System.currentTimeMillis()
            )
        }

        // Commit
        _products.value = prodList
        prodList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }
        _stockTransactions.value = stockTxList
        stockTxList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertStockTransaction(it) } }
        _purchases.value = _purchases.value + purchase
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertPurchase(purchase) }
        
        if (updatedSupplier != null) {
            val sList = _suppliers.value.toMutableList()
            val idx = sList.indexOfFirst { it.id == updatedSupplier.id }
            sList[idx] = updatedSupplier
            _suppliers.value = sList
        sList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(it) } }
            
            val newLedger = LedgerEntry(

                id = UUID.randomUUID().toString(),
                businessId = purchase.businessId,
                partyId = updatedSupplier.id,
                type = "PURCHASE",
                amount = purchase.grandTotal,
                balanceAfter = updatedSupplier.outstandingPayable,
                description = "Purchase Ref ${purchase.referenceNumber}",
                timestamp = System.currentTimeMillis()
            )
            
            if (purchase.amountPaid > 0) {
                 val newLedger = LedgerEntry(

                    id = UUID.randomUUID().toString(),
                    businessId = purchase.businessId,
                    partyId = updatedSupplier.id,
                    type = "PAYMENT",
                    amount = purchase.amountPaid,
                    balanceAfter = updatedSupplier.outstandingPayable,
                    description = "Payment via ${purchase.paymentMethod}",
                    timestamp = System.currentTimeMillis() + 1
                )
            }
        }
        return Result.success(purchase)
    }
    
    suspend fun recordSupplierPayment(supplierId: String, amount: Double, method: String, notes: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val businessId = _currentBusiness.value?.id ?: return Result.failure(Exception("No business"))
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than zero"))

        val list = _suppliers.value.toMutableList()
        val index = list.indexOfFirst { it.id == supplierId }
        if (index == -1) return Result.failure(Exception("Supplier not found"))

        val supplier = list[index]
        if (supplier.outstandingPayable <= 0) return Result.failure(Exception("No outstanding amount"))
        if (amount > supplier.outstandingPayable) return Result.failure(Exception("Cannot pay more than outstanding amount for now"))

        list[index] = supplier.copy(
            totalPaid = supplier.totalPaid + amount,
            outstandingPayable = supplier.outstandingPayable - amount,
            updatedAt = System.currentTimeMillis()
        )
        _suppliers.value = list
        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(it) } }
        
        val newLedger = LedgerEntry(

            id = UUID.randomUUID().toString(),
            businessId = businessId,
            partyId = supplierId,
            type = "PAYMENT",
            amount = amount,
            balanceAfter = list[index].outstandingPayable,
            description = "Payment via $method. $notes",
            timestamp = System.currentTimeMillis()
        )
        return Result.success(true)
    }


    suspend fun updateExpense(expense: Expense): Result<Boolean> {
        delay(NETWORK_DELAY)
        val list = _expenses.value.toMutableList()
        val index = list.indexOfFirst { it.id == expense.id }
        if (index != -1) {
            list[index] = expense
            _expenses.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(expense) }
            return Result.success(true)
        }
        return Result.failure(Exception("Expense not found"))
    }
    
    suspend fun deleteKhataEntry(entryId: String) {
        delay(NETWORK_DELAY)
        val list = _ledgerEntries.value.toMutableList()
        val index = list.indexOfFirst { it.id == entryId }
        if (index != -1) {
            list.removeAt(index)
            _ledgerEntries.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                val d = dao ?: return@launch
                d.deleteLedgerEntry(entryId)
            }
        }
    }
    
    suspend fun deactivateCustomer(customerId: String) {
        delay(NETWORK_DELAY)
        val list = _customers.value.toMutableList()
        val index = list.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val c = list[index]
            val updated = c.copy(isActive = false)
            list[index] = updated
            _customers.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(updated) }
        }
    }
    
    suspend fun deactivateSupplier(supplierId: String) {
        delay(NETWORK_DELAY)
        val list = _suppliers.value.toMutableList()
        val index = list.indexOfFirst { it.id == supplierId }
        if (index != -1) {
            val s = list[index]
            val updated = s.copy(isActive = false)
            list[index] = updated
            _suppliers.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(updated) }
        }
    }
    
    suspend fun reverseSale(saleId: String) {
        delay(NETWORK_DELAY)
        val sale = _sales.value.find { it.id == saleId } ?: return
        
        _sales.value = _sales.value.filter { it.id != saleId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val d = dao ?: return@launch
            d.deleteSale(saleId)
        }
        
        val prodList = _products.value.toMutableList()
        sale.items.forEach { item ->
            val pIdx = prodList.indexOfFirst { it.id == item.productId }
            if (pIdx != -1) {
                val p = prodList[pIdx]
                val updated = p.copy(currentStock = p.currentStock + item.quantity)
                prodList[pIdx] = updated
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(updated) }
            }
        }
        _products.value = prodList
        
        val cList = _customers.value.toMutableList()
        val cIdx = cList.indexOfFirst { it.id == sale.customerId }
        if (cIdx != -1) {
            val c = cList[cIdx]
            val updated = c.copy(
                totalPurchases = c.totalPurchases - sale.grandTotal,
                outstandingAmount = c.outstandingAmount - (sale.grandTotal - sale.amountPaid)
            )
            cList[cIdx] = updated
            _customers.value = cList
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(updated) }
        }
    }
    
    suspend fun reversePurchase(purchaseId: String) {
        delay(NETWORK_DELAY)
        val purchase = _purchases.value.find { it.id == purchaseId } ?: return
        
        _purchases.value = _purchases.value.filter { it.id != purchaseId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val d = dao ?: return@launch
            d.deletePurchase(purchaseId)
        }
        
        val prodList = _products.value.toMutableList()
        purchase.items.forEach { item ->
            val pIdx = prodList.indexOfFirst { it.id == item.productId }
            if (pIdx != -1) {
                val p = prodList[pIdx]
                val updated = p.copy(currentStock = p.currentStock - item.quantity)
                prodList[pIdx] = updated
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(updated) }
            }
        }
        _products.value = prodList
        
        val sList = _suppliers.value.toMutableList()
        val sIdx = sList.indexOfFirst { it.id == purchase.supplierId }
        if (sIdx != -1) {
            val s = sList[sIdx]
            val updated = s.copy(
                totalPurchases = s.totalPurchases - purchase.grandTotal,
                outstandingPayable = s.outstandingPayable - (purchase.grandTotal - purchase.amountPaid)
            )
            sList[sIdx] = updated
            _suppliers.value = sList
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(updated) }
        }
    }
}
