import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

# Fix StatCardSimple definition
text = text.replace(
    "fun StatCardSimple(title: String, count: String, modifier: Modifier = Modifier) {",
    "fun StatCardSimple(title: String, count: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {"
)
text = text.replace(
    ".background(Color.White, RoundedCornerShape(16.dp))",
    ".background(Color.White, RoundedCornerShape(16.dp))\n            .clickable { onClick() }"
)

# Fix StatCardSimple calls
text = text.replace(
    'StatCardSimple("Products", myProducts.size.toString(), Modifier.weight(1f))',
    'StatCardSimple("Products", myProducts.size.toString(), Modifier.weight(1f)) { currentTab = "Inventory" }'
)
text = text.replace(
    'StatCardSimple("Customers", myCustomers.size.toString(), Modifier.weight(1f))',
    'StatCardSimple("Customers", myCustomers.size.toString(), Modifier.weight(1f)) { khataSelectedTab = 0; currentTab = "Khata" }'
)
text = text.replace(
    'StatCardSimple("Suppliers", mySuppliers.size.toString(), Modifier.weight(1f))',
    'StatCardSimple("Suppliers", mySuppliers.size.toString(), Modifier.weight(1f)) { khataSelectedTab = 1; currentTab = "Khata" }'
)

# Fix Recent Transactions
recent_tx_code = """
                val salesTx = MockDataStore.sales.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map { 
                    Triple("SALE", "Sale to ${it.customerId ?: "Cash"}", Pair(it.grandTotal, it.createdAt)) 
                }
                val purchasesTx = MockDataStore.purchases.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map { 
                    Triple("PURCHASE", "Purchase from ${it.supplierId}", Pair(it.grandTotal, it.createdAt)) 
                }
                val expensesTx = MockDataStore.expenses.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map { 
                    Triple("EXPENSE", it.category, Pair(it.amount, it.createdAt)) 
                }
                val paymentsTx = MockDataStore.ledgerEntries.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map {
                    Triple(it.type, it.description, Pair(it.amount, it.timestamp))
                }
                
                val recentTransactions = (salesTx + purchasesTx + expensesTx + paymentsTx)
                    .sortedByDescending { it.third.second }
                    .take(5)
                    
                if (recentTransactions.isEmpty()) {
                    EmptyStateCard("Transactions", "No recent transactions. Start billing to see them here.")
                } else {
                    recentTransactions.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueNavy)
                                Text(entry.second, fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(
                                text = "₹%.2f".format(entry.third.first),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (entry.first == "PAYMENT" || entry.first == "SALE") AccentGreen else TextPrimary
                            )
                        }
                    }
                }
"""

# Replace the existing ledger entries logic
pattern = r"val recentLedger = MockDataStore\.ledgerEntries.*?\}\n                \}"
text = re.sub(pattern, recent_tx_code.strip(), text, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)

