package com.example.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.model.Product
import com.example.presentation.components.VyaparButton
import com.example.presentation.components.VyaparTextField
import com.example.ui.theme.*
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditing = productId != "new"
    
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allProducts by MockDataStore.products.collectAsState()
    val existingProduct = if (isEditing) allProducts.find { it.id == productId } else null
    
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var sku by remember { mutableStateOf(existingProduct?.sku ?: "") }
    var barcodeStr by remember { mutableStateOf(existingProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "") }
    var purchasePrice by remember { mutableStateOf(existingProduct?.purchasePrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(existingProduct?.sellingPrice?.toString() ?: "") }
    var currentStock by remember { mutableStateOf(existingProduct?.currentStock?.toString() ?: "") }
    var minStock by remember { mutableStateOf(existingProduct?.minStock?.toString() ?: "") }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Product" else "Add Product", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            VyaparTextField(
                value = name,
                onValueChange = { name = it },
                label = "Product Name *"
            )
            VyaparTextField(
                value = category,
                onValueChange = { category = it },
                label = "Category"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VyaparTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = "Purchase Price (₹) *",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                VyaparTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = "Selling Price (₹) *",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (!isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VyaparTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = "Current Stock *",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    VyaparTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = "Min Stock Level *",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                VyaparTextField(
                    value = minStock,
                    onValueChange = { minStock = it },
                    label = "Min Stock Level *",
                    keyboardType = KeyboardType.Number
                )
                Text(
                    stringResource(R.string.str_note_to_adjust_current_stock_u),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            VyaparTextField(
                value = sku,
                onValueChange = { sku = it },
                label = "SKU (Optional)"
            )
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VyaparTextField(
                    value = barcodeStr,
                    onValueChange = { barcodeStr = it },
                    label = "Barcode",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
                        val scanner = GmsBarcodeScanning.getClient(context, options)
                        scanner.startScan()
                            .addOnSuccessListener { barcodeObj ->
                                barcodeObj.rawValue?.let { 
                                    val scannedValue = it
                                    val exists = allProducts.any { p -> p.businessId == currentBusiness?.id && p.barcode == scannedValue && p.id != productId }
                                    if (exists) {
                                        errorMsg = "Barcode already assigned to another product."
                                    } else {
                                        barcodeStr = scannedValue 
                                        errorMsg = null
                                    }
                                }
                            }
                            .addOnFailureListener { e -> errorMsg = "Scan failed: ${e.message}" }
                    },
                    modifier = Modifier.height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.str_scan))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            VyaparButton(
                text = "Save Product",
                isLoading = isLoading,
                onClick = {
                    val pPrice = purchasePrice.toDoubleOrNull()
                    val sPrice = sellingPrice.toDoubleOrNull()
                    val cStock = currentStock.toIntOrNull()
                    val mStock = minStock.toIntOrNull()

                    if (name.isBlank()) { errorMsg = "Product name is required"; return@VyaparButton }
                    if (pPrice == null || pPrice < 0) { errorMsg = "Valid purchase price required"; return@VyaparButton }
                    if (sPrice == null || sPrice < 0) { errorMsg = "Valid selling price required"; return@VyaparButton }
                    if (!isEditing && (cStock == null || cStock < 0)) { errorMsg = "Valid current stock required"; return@VyaparButton }
                    if (mStock == null || mStock < 0) { errorMsg = "Valid min stock required"; return@VyaparButton }
                    
                    val businessId = currentBusiness?.id ?: return@VyaparButton
                    
                    if (barcodeStr.isNotBlank()) {
                        val duplicateBarcode = allProducts.any { it.businessId == businessId && it.barcode == barcodeStr && it.id != productId }
                        if (duplicateBarcode) {
                            errorMsg = "Barcode already assigned to another product."
                            return@VyaparButton
                        }
                    }
                    
                    isLoading = true
                    scope.launch {
                        val product = Product(
                            id = existingProduct?.id ?: UUID.randomUUID().toString(),
                            businessId = businessId,
                            name = name,
                            sku = sku,
                            barcode = barcodeStr,
                            category = category,
                            purchasePrice = pPrice,
                            sellingPrice = sPrice,
                            currentStock = if (isEditing) existingProduct!!.currentStock else cStock!!,
                            minStock = mStock,
                            supplierId = existingProduct?.supplierId,
                            expiryDate = existingProduct?.expiryDate,
                            createdAt = existingProduct?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val result = MockDataStore.saveProduct(product)
                        isLoading = false
                        if (result.isSuccess) {
                            onNavigateBack()
                        } else {
                            errorMsg = "Failed to save product"
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
