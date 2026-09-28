sed -i 's/_products.value = list/_products.value = list\n        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/_customers.value = list/_customers.value = list\n        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/_suppliers.value = list/_suppliers.value = list\n        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/_expenses.value = list/_expenses.value = list\n        list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_products.value = prodList/_products.value = prodList\n        prodList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/_customers.value = cList/_customers.value = cList\n        cList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/_suppliers.value = sList/_suppliers.value = sList\n        sList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_sales.value = _sales.value + sale.copy(items = newSaleItems, syncStatus = getSyncStatus())/val finalizedSale = sale.copy(items = newSaleItems, syncStatus = getSyncStatus())\n        _sales.value = _sales.value + finalizedSale\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSale(finalizedSale) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_purchases.value = _purchases.value + purchase/_purchases.value = _purchases.value + purchase\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertPurchase(purchase) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_ledgerEntries.value = _ledgerEntries.value + LedgerEntry(/val newLedger = LedgerEntry(\n/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/syncStatus = SyncStatus.SYNCED/syncStatus = SyncStatus.SYNCED\n        )\n        _ledgerEntries.value = _ledgerEntries.value + newLedger\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertLedgerEntry(newLedger) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

# Replace missing parenthesis in LedgerEntry if needed
