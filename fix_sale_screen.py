import re

with open('app/src/main/java/com/example/presentation/sales/SaleDetailScreen.kt', 'r') as f:
    text = f.read()

old_sale = """    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_invoice), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },"""

new_sale = """    var showMenu by remember { mutableStateOf(false) }
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
                            MockDataStore.reverseSale(saleId)
                            isDeleting = false
                            showDeleteConfirm = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.str_reverse_transaction))
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
                title = { Text(stringResource(R.string.str_invoice), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_reverse_transaction)) },
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

text = text.replace(old_sale, new_sale)

with open('app/src/main/java/com/example/presentation/sales/SaleDetailScreen.kt', 'w') as f:
    f.write(text)
