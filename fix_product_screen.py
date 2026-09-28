import re

with open('app/src/main/java/com/example/presentation/inventory/ProductDetailScreen.kt', 'r') as f:
    text = f.read()

old_product = """    val allProducts by MockDataStore.products.collectAsState()
    val product = allProducts.find { it.id == productId }

    var showAdjustStockDialog by remember { mutableStateOf(false) }

    if (product == null) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_product_details), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(productId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },"""

new_product = """    val allProducts by MockDataStore.products.collectAsState()
    val product = allProducts.find { it.id == productId }

    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }

    if (product == null) {
        onNavigateBack()
        return
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_delete_product)) },
            text = { Text(stringResource(R.string.str_delete_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        if(isDeleting) return@Button
                        isDeleting = true
                        scope.launch {
                            MockDataStore.deleteProduct(productId)
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
                title = { Text(stringResource(R.string.str_product_details), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_edit)) },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit(productId)
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

text = text.replace(old_product, new_product)
text = text.replace("import androidx.compose.material.icons.filled.Edit", "import androidx.compose.material.icons.filled.Edit\nimport androidx.compose.material.icons.filled.MoreVert")

with open('app/src/main/java/com/example/presentation/inventory/ProductDetailScreen.kt', 'w') as f:
    f.write(text)
