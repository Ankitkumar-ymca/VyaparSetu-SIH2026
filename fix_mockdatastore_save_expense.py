import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

new_save_expense = """
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
"""

# Replace the existing saveExpense
pattern = r'    suspend fun saveExpense\(expense: Expense\): Result<Expense> \{.*?return Result\.success\(finalExpense\)\n    \}'
text = re.sub(pattern, new_save_expense.strip(), text, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
