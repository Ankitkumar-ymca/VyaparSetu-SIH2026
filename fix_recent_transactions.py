import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

old_payments = """val paymentsTx = MockDataStore.ledgerEntries.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map {
                    Triple(it.type, it.description, Pair(it.amount, it.timestamp))
                }"""

new_payments = """val paymentsTx = MockDataStore.ledgerEntries.collectAsState().value.filter { it.businessId == currentBusiness?.id && (it.type == "PAYMENT" || it.type == "OPENING_BALANCE") }.map {
                    Triple(if (it.type == "OPENING_BALANCE") "BALANCE" else "PAYMENT", it.description, Pair(it.amount, it.timestamp))
                }"""

text = text.replace(old_payments, new_payments)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)
