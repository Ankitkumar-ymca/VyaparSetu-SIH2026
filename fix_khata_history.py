import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

old_history = """            Text(stringResource(R.string.str_transaction_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.str_no_transactions_yet), color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.str_billing_features_coming_in_pha), color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }"""

new_history = """            Text(stringResource(R.string.str_transaction_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            val ledgerEntries = MockDataStore.ledgerEntries.collectAsState().value.filter { it.partyId == customerId }.sortedByDescending { it.timestamp }
            if (ledgerEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.str_no_transactions_yet), color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                ledgerEntries.forEach { entry ->
                    KhataEntryCard(entry)
                }
            }"""

text = text.replace(old_history, new_history)

# Add KhataEntryCard component
card_code = """
@Composable
fun KhataEntryCard(entry: com.example.domain.model.LedgerEntry) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_reverse_transaction)) },
            text = { Text(stringResource(R.string.str_reverse_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        if(isDeleting) return@Button
                        isDeleting = true
                        scope.launch {
                            MockDataStore.deleteKhataEntry(entry.id)
                            isDeleting = false
                            showDeleteConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.str_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.str_cancel)) }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(entry.type, fontSize = 12.sp, color = TextSecondary)
            Text("Balance: ₹${entry.balanceAfter}", fontSize = 12.sp, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "₹${entry.amount}",
                fontWeight = FontWeight.Bold,
                color = if (entry.type == "PAYMENT") AccentGreen else AccentRed
            )
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.str_delete)) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }
}
"""

text += card_code

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)
