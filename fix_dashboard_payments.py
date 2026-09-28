import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

old_code = """                val paymentsTx = MockDataStore.ledgerEntries.collectAsState().value.filter { it.businessId == currentBusiness?.id && (it.type == "PAYMENT" || it.type == "OPENING_BALANCE") }.map {
                    Triple(if (it.type == "OPENING_BALANCE") "BALANCE" else "PAYMENT", it.description, Pair(it.amount, it.timestamp))
                }"""

new_code = """                val paymentsTx = MockDataStore.ledgerEntries.collectAsState().value.filter { it.businessId == currentBusiness?.id && (it.type == "PAYMENT" || it.type == "OPENING_BALANCE") }.map { entry ->
                    val cName = myCustomers.find { it.id == entry.partyId }?.name
                    val sName = mySuppliers.find { it.id == entry.partyId }?.name
                    val partyName = cName ?: sName ?: ""
                    val desc = if (partyName.isNotEmpty()) "$partyName - ${entry.description}" else entry.description
                    Triple(if (entry.type == "OPENING_BALANCE") "BALANCE" else "PAYMENT", desc, Pair(entry.amount, entry.timestamp))
                }"""

text = text.replace(old_code, new_code)
    
with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)
