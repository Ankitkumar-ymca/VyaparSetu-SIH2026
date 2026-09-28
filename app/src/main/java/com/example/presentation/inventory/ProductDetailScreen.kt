package com.example.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.intelligence.InventoryIntelligenceEngine
import com.example.domain.intelligence.ProductProfitabilityEngine
import com.example.domain.model.Product
import com.example.presentation.components.VyaparButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val allProducts by MockDataStore.products.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val product = allProducts.find { it.id == productId }

    // Derive deterministic product intelligence metrics
    val productMetrics = remember(product, allSales) {
        if (product != null) {
            InventoryIntelligenceEngine.analyze(
                products = listOf(product),
                sales = allSales.filter { it.businessId == product.businessId }
            ).products.firstOrNull()
        } else null
    }

    // Derive deterministic product profitability metrics
    val profitabilityMetrics = remember(product, allSales) {
        if (product != null) {
            ProductProfitabilityEngine.analyze(
                products = listOf(product),
                sales = allSales.filter { it.businessId == product.businessId }
            ).products.firstOrNull()
        } else null
    }

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
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
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
        },
        containerColor = BlueBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(product.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                Spacer(modifier = Modifier.height(4.dp))
                Text(product.category.ifBlank { "Uncategorized" }, fontSize = 14.sp, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.str_purchase_price), fontSize = 12.sp, color = TextSecondary)
                        Text("₹${product.purchasePrice}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.str_selling_price), fontSize = 12.sp, color = TextSecondary)
                        Text("₹${product.sellingPrice}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                    }
                }
            }

            // Stock Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.str_current_stock), fontSize = 14.sp, color = TextSecondary)
                        Text("${product.currentStock}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        Text(stringResource(R.string.str_min_level, product.minStock), fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = { showAdjustStockDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueLight, contentColor = BlueNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.str_adjust_stock), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Deterministic Inventory Intelligence Card
            if (productMetrics != null) {
                ProductIntelligenceDetails(metrics = productMetrics)
            }

            // Deterministic Product Profitability Card
            if (profitabilityMetrics != null) {
                ProductProfitabilityCard(metrics = profitabilityMetrics)
            }

            // AI Smart Insights
            var aiInsight by remember { mutableStateOf<String?>(null) }
            var isGeneratingInsight by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlueLight.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .border(1.dp, BlueAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.str_smart_product_insights), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueNavy)
                    if (aiInsight == null && !isGeneratingInsight) {
                        TextButton(onClick = {
                            isGeneratingInsight = true
                            coroutineScope.launch {
                                val result = com.example.data.repository.AiService.getProductInsights(product.id)
                                isGeneratingInsight = false
                                aiInsight = result.getOrNull() ?: "Unable to generate insights right now."
                            }
                        }) {
                            Text(stringResource(R.string.str_analyze), fontSize = 12.sp, color = BlueAccent)
                        }
                    }
                }
                
                if (isGeneratingInsight) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueAccent, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.str_analyzing_product_data), fontSize = 12.sp, color = TextSecondary)
                    }
                } else if (aiInsight != null) {
                    Text(aiInsight!!, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Text(stringResource(R.string.str_analyze_this_products_performa), fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // History Placeholders
            Text(stringResource(R.string.str_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.str_sales_history), fontWeight = FontWeight.SemiBold, color = BlueNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.str_sales_tracking_will_be_availab), fontSize = 12.sp, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showAdjustStockDialog) {
        AdjustStockDialog(
            product = product,
            onDismiss = { showAdjustStockDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockDialog(product: Product, onDismiss: () -> Unit) {
    var quantityStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Manual correction") }
    var isAdding by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.str_adjust_stock), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.str_current_stock_val, product.currentStock), fontSize = 14.sp, color = TextSecondary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(
                        selected = isAdding,
                        onClick = { isAdding = true },
                        label = { Text(stringResource(R.string.str_add)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentGreenLight.copy(alpha = 0.2f))
                    )
                    FilterChip(
                        selected = !isAdding,
                        onClick = { isAdding = false },
                        label = { Text(stringResource(R.string.str_remove)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentRed.copy(alpha = 0.1f))
                    )
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(stringResource(R.string.str_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.str_reason)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toIntOrNull()
                    if (qty == null || qty <= 0) {
                        errorMsg = "Enter a valid quantity > 0"
                        return@Button
                    }
                    if (reason.isBlank()) {
                        errorMsg = "Reason is required"
                        return@Button
                    }
                    val finalChange = if (isAdding) qty else -qty
                    if (!isAdding && (product.currentStock + finalChange) < 0) {
                        errorMsg = "Cannot remove more than current stock"
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        val res = MockDataStore.adjustStock(product.id, finalChange, reason)
                        isLoading = false
                        if (res.isSuccess) {
                            onDismiss()
                        } else {
                            errorMsg = "Failed to adjust stock"
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.str_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.str_cancel)) }
        }
    )
}
