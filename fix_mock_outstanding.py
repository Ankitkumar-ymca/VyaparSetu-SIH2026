import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

fn_code = """
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
            timestamp = System.currentTimeMillis()
        )
        val ledgerList = _ledgerEntries.value.toMutableList()
        ledgerList.add(ledgerEntry)
        _ledgerEntries.value = ledgerList
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertLedgerEntry(ledgerEntry) }
        
        return Result.success(true)
    }
"""

text = text.replace("    suspend fun recordCustomerPayment(customerId: String, amount: Double, method: String, notes: String): Result<Boolean> {", fn_code + "\n    suspend fun recordCustomerPayment(customerId: String, amount: Double, method: String, notes: String): Result<Boolean> {")

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)

