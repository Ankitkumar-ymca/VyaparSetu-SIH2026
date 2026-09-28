import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

outstanding_dialog = """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousOutstandingDialog(customer: Customer, onDismiss: () -> Unit) {
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Opening Balance") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Previous Outstanding", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter the amount this customer owed you before starting to use the app.", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text("Outstanding Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth())
                if (errorMsg != null) Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) { errorMsg = "Enter valid amount > 0"; return@Button }
                    
                    isLoading = true
                    scope.launch {
                        val res = MockDataStore.addCustomerPreviousOutstanding(customer.id, amount, notes)
                        isLoading = false
                        if (res.isSuccess) onDismiss() else errorMsg = "Failed to add outstanding"
                    }
                },
                enabled = !isLoading
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
"""
text = text.replace("@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun PaymentDialog", outstanding_dialog + "\n@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun PaymentDialog")

btn_code = """
                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueLight, contentColor = BlueNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Receive", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showPreviousOutstandingDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BlueNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BlueNavy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Previous Outstanding", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
"""

text = text.replace("""                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueLight, contentColor = BlueNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Record Payment", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))""", btn_code)

text = text.replace("var showPaymentDialog by remember { mutableStateOf(false) }", "var showPaymentDialog by remember { mutableStateOf(false) }\n    var showPreviousOutstandingDialog by remember { mutableStateOf(false) }")
text = text.replace("if (showPaymentDialog) PaymentDialog(customer) { showPaymentDialog = false }", "if (showPaymentDialog) PaymentDialog(customer) { showPaymentDialog = false }\n    if (showPreviousOutstandingDialog) PreviousOutstandingDialog(customer) { showPreviousOutstandingDialog = false }")

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)

