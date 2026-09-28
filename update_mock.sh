sed -i 's/private var prefs: SharedPreferences? = null/private var prefs: SharedPreferences? = null\n    private var db: AppDatabase? = null\n    private var dao: AppDao? = null/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/prefs = context.getSharedPreferences("mock_data_store", Context.MODE_PRIVATE)/prefs = context.getSharedPreferences("mock_data_store", Context.MODE_PRIVATE)\n        db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "vyapar_db").allowMainThreadQueries().build()\n        dao = db?.appDao()/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i '/val usersJson = prefs.getString("users", "\[\]") ?: "\[\]"/d' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i '/val businessesJson = prefs.getString("businesses", "\[\]") ?: "\[\]"/d' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i '/storedUsers = Json.decodeFromString(usersJson)/d' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i '/storedBusinesses = Json.decodeFromString(businessesJson)/d' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i '/try {/a\
            storedUsers = dao?.getUsers()?.toMutableList() ?: mutableListOf()\
            storedBusinesses = dao?.getBusinesses()?.toMutableList() ?: mutableListOf()\
            _products.value = dao?.getProducts() ?: emptyList()\
            _customers.value = dao?.getCustomers() ?: emptyList()\
            _suppliers.value = dao?.getSuppliers() ?: emptyList()\
            _sales.value = dao?.getSales() ?: emptyList()\
            _purchases.value = dao?.getPurchases() ?: emptyList()\
            _expenses.value = dao?.getExpenses() ?: emptyList()\
            _stockAdjustments.value = dao?.getStockAdjustments() ?: emptyList()\
            _customerPayments.value = dao?.getCustomerPayments() ?: emptyList()\
            _stockTransactions.value = dao?.getStockTransactions() ?: emptyList()\
            _ledgerEntries.value = dao?.getLedgerEntries() ?: emptyList()\
            _auditLogs.value = dao?.getAuditLogs() ?: emptyList()\
' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i '/putString("users", Json.encodeToString(storedUsers))/d' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i '/putString("businesses", Json.encodeToString(storedBusinesses))/d' app/src/main/java/com/example/data/repository/MockDataStore.kt

