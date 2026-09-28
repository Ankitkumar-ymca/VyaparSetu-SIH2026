sed -i '143,172c\
    private fun triggerSync() {\
        CoroutineScope(Dispatchers.IO).launch {\
            delay(1000)\
            _sales.value = _sales.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }\
            _purchases.value = _purchases.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }\
            _expenses.value = _expenses.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }\
            _customerPayments.value = _customerPayments.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }\
            _ledgerEntries.value = _ledgerEntries.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }\
            _stockTransactions.value = _stockTransactions.value.map { it.copy(syncStatus = SyncStatus.SYNCED) }\
        }\
    }\
' app/src/main/java/com/example/data/repository/MockDataStore.kt
