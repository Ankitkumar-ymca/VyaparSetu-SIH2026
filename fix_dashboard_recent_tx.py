import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

old_code = """                val salesTx = MockDataStore.sales.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map { 
                    Triple("SALE", "Sale to ${it.customerId ?: "Cash"}", Pair(it.grandTotal, it.createdAt)) 
                }
                val purchasesTx = MockDataStore.purchases.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map { 
                    Triple("PURCHASE", "Purchase from ${it.supplierId}", Pair(it.grandTotal, it.createdAt)) 
                }"""

new_code = """                val salesTx = mySales.map { sale -> 
                    val cName = myCustomers.find { it.id == sale.customerId }?.name ?: "Cash"
                    Triple("SALE", "Sale to $cName", Pair(sale.grandTotal, sale.createdAt)) 
                }
                val purchasesTx = myPurchases.map { purchase -> 
                    val sName = mySuppliers.find { it.id == purchase.supplierId }?.name ?: "Unknown"
                    Triple("PURCHASE", "Purchase from $sName", Pair(purchase.grandTotal, purchase.createdAt)) 
                }"""

if old_code in text:
    text = text.replace(old_code, new_code)
else:
    # use regex
    pattern = r'val salesTx = .*?Triple\("SALE".*?\n\s+\}\n\s+val purchasesTx = .*?Triple\("PURCHASE".*?\n\s+\}'
    text = re.sub(pattern, new_code, text, flags=re.DOTALL)
    
with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)
