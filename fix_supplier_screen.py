import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

old_supplier = """    if (supplier == null) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_supplier_details), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { onNavigateToEdit(supplierId) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },"""

new_supplier = """    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }

    if (supplier == null) {
        onNavigateBack()
        return
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_delete_supplier)) },
            text = { Text(stringResource(R.string.str_delete_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        if(isDeleting) return@Button
                        isDeleting = true
                        scope.launch {
                            MockDataStore.deactivateSupplier(supplierId)
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
                title = { Text(stringResource(R.string.str_supplier_details), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_edit)) },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit(supplierId)
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

text = text.replace(old_supplier, new_supplier)

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)
