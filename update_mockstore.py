import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

old_ledger = """        val ledgerEntry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            businessId = customer.businessId,
            partyId = customerId,
            type = "OPENING_BALANCE",
            amount = amount,
            balanceAfter = updatedCustomer.outstandingAmount,
            description = notes,
            timestamp = System.currentTimeMillis()
        )"""

new_ledger = """        val ledgerEntry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            businessId = customer.businessId,
            partyId = customerId,
            type = "OPENING_BALANCE",
            amount = amount,
            balanceAfter = updatedCustomer.outstandingAmount,
            description = notes,
            timestamp = System.currentTimeMillis(),
            syncStatus = getSyncStatus()
        )"""

text = text.replace(old_ledger, new_ledger)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
