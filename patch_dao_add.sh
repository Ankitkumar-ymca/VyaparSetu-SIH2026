sed -i 's/_stockAdjustments.value = _stockAdjustments.value + adjustment/_stockAdjustments.value = _stockAdjustments.value + adjustment\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertStockAdjustment(adjustment) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_customerPayments.value = _customerPayments.value + payment/_customerPayments.value = _customerPayments.value + payment\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomerPayment(payment) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_auditLogs.value = _auditLogs.value + log/_auditLogs.value = _auditLogs.value + log\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertAuditLog(log) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_stockTransactions.value = _stockTransactions.value + tx/_stockTransactions.value = _stockTransactions.value + tx\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertStockTransaction(tx) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/_stockTransactions.value = stockTxList/_stockTransactions.value = stockTxList\n        stockTxList.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertStockTransaction(it) } }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
