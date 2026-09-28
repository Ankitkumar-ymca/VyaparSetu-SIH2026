import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

old_customer = """    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPreviousOutstandingDialog by remember { mutableStateOf(false) }

    if (customer == null) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_customer_khata), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { onNavigateToEdit(customerId) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },"""

new_customer = """    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPreviousOutstandingDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }

    if (customer == null) {
        onNavigateBack()
        return
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_delete_customer)) },
            text = { Text(stringResource(R.string.str_delete_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        if(isDeleting) return@Button
                        isDeleting = true
                        scope.launch {
                            MockDataStore.deactivateCustomer(customerId)
                            isDeleting = false
                            showDeleteConfirm = false
                            onNavigateBack()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_customer_khata), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_edit)) },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit(customerId)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_delete)) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },"""

text = text.replace(old_customer, new_customer)

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)
