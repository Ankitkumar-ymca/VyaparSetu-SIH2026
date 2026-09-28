import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

old_supp_hist = """            Text(stringResource(R.string.str_purchase_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.str_no_purchases_yet), color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.str_purchase_tracking_will_be_av), color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }"""

new_supp_hist = """            Text(stringResource(R.string.str_purchase_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            val ledgerEntries = MockDataStore.ledgerEntries.collectAsState().value.filter { it.partyId == supplierId }.sortedByDescending { it.timestamp }
            if (ledgerEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.str_no_purchases_yet), color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                ledgerEntries.forEach { entry ->
                    KhataEntryCard(entry)
                }
            }"""

text = text.replace(old_supp_hist, new_supp_hist)

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)
