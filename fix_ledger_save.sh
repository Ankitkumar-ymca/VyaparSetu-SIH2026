sed -i '/val newLedger = LedgerEntry(/,/)/ {
    /)/!b
    /)/ {
        a\
            _ledgerEntries.value = _ledgerEntries.value + newLedger\
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertLedgerEntry(newLedger) }
    }
}' app/src/main/java/com/example/data/repository/MockDataStore.kt
