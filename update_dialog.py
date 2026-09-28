import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

old_dialog = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousOutstandingDialog(customer: Customer, onDismiss: () -> Unit) {
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Opening Balance") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.str_add_previous_outstanding), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.str_enter_the_amount_this_customer), fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text(stringResource(R.string.str_outstanding_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.str_notes_optional)) }, modifier = Modifier.fillMaxWidth())
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
            ) { Text(stringResource(R.string.str_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.str_cancel)) } }
    )
}"""

new_dialog = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousOutstandingDialog(customer: Customer, onDismiss: () -> Unit) {
    val allCustomers by MockDataStore.customers.collectAsState()
    var selectedCustomer by remember { mutableStateOf(customer) }
    var showDropdown by remember { mutableStateOf(false) }
    
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Opening Balance") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.str_add_previous_outstanding), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.str_enter_the_amount_this_customer), fontSize = 12.sp, color = TextSecondary)
                
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = !showDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Customer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        allCustomers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedCustomer = c
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text(stringResource(R.string.str_outstanding_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.str_notes_optional)) }, modifier = Modifier.fillMaxWidth())
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
                        val res = MockDataStore.addCustomerPreviousOutstanding(selectedCustomer.id, amount, notes)
                        isLoading = false
                        if (res.isSuccess) onDismiss() else errorMsg = "Failed to add outstanding"
                    }
                },
                enabled = !isLoading
            ) { Text(stringResource(R.string.str_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.str_cancel)) } }
    )
}"""

if old_dialog in text:
    text = text.replace(old_dialog, new_dialog)
else:
    print("Could not find old dialog exactly, using regex")
    import re
    pattern = r'@OptIn\(ExperimentalMaterial3Api::class\)\n@Composable\nfun PreviousOutstandingDialog\(.*?\n\}'
    text = re.sub(pattern, new_dialog, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)
