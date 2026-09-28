package com.example.presentation.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.example.domain.model.Supplier
import com.example.domain.model.Product
import com.example.domain.model.Purchase
import com.example.domain.model.PurchaseItem
import com.example.presentation.components.VyaparButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.AiService
import com.example.domain.model.InvoiceExtractionResult

// ... skip to NewPurchaseScreen ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseScreen(
    onNavigateBack: () -> Unit,
    onPurchaseComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val allProducts by MockDataStore.products.collectAsState()
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    
    val myProducts = allProducts.filter { it.businessId == currentBusiness?.id }
    val mySuppliers = allSuppliers.filter { it.businessId == currentBusiness?.id }

    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showSupplierSheet by remember { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }
    
    var purchaseItems by remember { mutableStateOf<List<PurchaseItemState>>(emptyList()) }
    var discount by remember { mutableStateOf("") }
    var taxPercentage by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var ocrMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isAnalyzing = true
            coroutineScope.launch {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    val result = AiService.analyzeInvoice(bitmap)
                    isAnalyzing = false
                    if (result.isSuccess) {
                        val extraction = result.getOrNull()
                        if (extraction != null) {
                            // Try matching Supplier
                            val matchedSupplier = mySuppliers.find { s -> 
                                s.name.equals(extraction.supplierName, ignoreCase = true) 
                            }
                            if (matchedSupplier != null) {
                                selectedSupplier = matchedSupplier
                            }
                            
                            // Try matching Products
                            val newItems = mutableListOf<PurchaseItemState>()
                            extraction.items.forEach { extItem ->
                                val matchedProduct = myProducts.find { p ->
                                    p.name.equals(extItem.productName, ignoreCase = true) ||
                                    (extItem.sku != null && p.sku == extItem.sku)
                                }
                                if (matchedProduct != null) {
                                    newItems.add(PurchaseItemState(matchedProduct, extItem.quantity ?: 1))
                                }
                            }
                            if (newItems.isNotEmpty()) {
                                purchaseItems = purchaseItems + newItems
                            }
                            
                            // Try filling totals
                            if (extraction.discount != null) {
                                discount = extraction.discount.toString()
                            }
                            if (extraction.tax != null && extraction.subtotal != null && extraction.subtotal > 0) {
                                taxPercentage = ((extraction.tax / (extraction.subtotal - (extraction.discount ?: 0.0))) * 100).toString()
                            }
                            
                            ocrMessage = "AI Scan Complete. Found ${newItems.size} products. Please verify all details."
                        }
                    } else {
                        errorMessage = "AI Scan failed: ${result.exceptionOrNull()?.message}"
                    }
                } catch (e: Exception) {
                    isAnalyzing = false
                    errorMessage = "Error reading image."
                }
            }
        }
    }

    // Computations
    val subtotal = purchaseItems.sumOf { it.product.purchasePrice * it.quantity }
    val discountVal = discount.toDoubleOrNull() ?: 0.0
    val taxableAmount = maxOf(0.0, subtotal - discountVal)
    val taxVal = taxPercentage.toDoubleOrNull()?.let { taxableAmount * it / 100.0 } ?: 0.0
    val grandTotal = taxableAmount + taxVal
    val paidVal = amountPaid.toDoubleOrNull() ?: grandTotal // Default to full payment if blank
    val outstanding = maxOf(0.0, grandTotal - paidVal)
    
    val paymentStatus = when {
        paidVal >= grandTotal -> "PAID"
        paidVal > 0 -> "PARTIALLY PAID"
        else -> "CREDIT"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_new_purchase), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                actions = {
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.str_scan_ai), fontWeight = FontWeight.Bold, color = BlueAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, CardBorder)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.str_grand_total), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                    Text(stringResource(R.string.str_2f).format(grandTotal), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                }
                Spacer(modifier = Modifier.height(16.dp))
                VyaparButton(
                    text = "Confirm Purchase",
                    onClick = {
                        if (purchaseItems.isEmpty()) {
                            errorMessage = "Please add at least one product."
                            return@VyaparButton
                        }
                        if (selectedSupplier == null) {
                            errorMessage = "Please select a supplier."
                            return@VyaparButton
                        }
                        if (paidVal < 0) {
                            errorMessage = "Amount paid cannot be negative."
                            return@VyaparButton
                        }
                        
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val newPurchase = Purchase(
                                id = UUID.randomUUID().toString(),
                                businessId = currentBusiness!!.id,
                                referenceNumber = "PUR-${System.currentTimeMillis() % 100000}",
                                supplierId = selectedSupplier!!.id,
                                items = purchaseItems.map { 
                                    PurchaseItem(
                                        productId = it.product.id,
                                        productName = it.product.name,
                                        quantity = it.quantity,
                                        unitPrice = it.product.purchasePrice,
                                        totalPrice = it.product.purchasePrice * it.quantity
                                    )
                                },
                                subtotal = subtotal,
                                discount = discountVal,
                                tax = taxVal,
                                grandTotal = grandTotal,
                                amountPaid = paidVal,
                                outstanding = outstanding,
                                paymentStatus = paymentStatus,
                                paymentMethod = paymentMethod,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            val result = MockDataStore.createPurchase(newPurchase)
                            if (result.isSuccess) {
                                onPurchaseComplete(newPurchase.id)
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save purchase"
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isSaving
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BlueBackground)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage!!,
                            color = AccentRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                }
                
                if (isAnalyzing) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BlueAccent)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.str_analyzing_invoice_with_ai), color = BlueNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (ocrMessage != null) {
                    item {
                        Text(
                            text = ocrMessage!!,
                            color = BlueAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BlueLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(1.dp, BlueAccent, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                }

                // Supplier Selection
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { showSupplierSheet = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Supplier", tint = BlueNavy)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.str_supplier), fontSize = 12.sp, color = TextSecondary)
                            Text(selectedSupplier?.name ?: "Tap to Select Supplier", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                    }
                }

                // Add Product Button
                item {
                    OutlinedButton(
                        onClick = { showProductSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.str_add_products), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Items List
                items(purchaseItems) { item ->
                    PurchaseItemRow(
                        item = item,
                        onQuantityChange = { newQ ->
                            if (newQ > 0) {
                                purchaseItems = purchaseItems.map { if (it.product.id == item.product.id) it.copy(quantity = newQ) else it }
                                errorMessage = null
                            }
                        },
                        onRemove = {
                            purchaseItems = purchaseItems.filter { it.product.id != item.product.id }
                        }
                    )
                }

                if (purchaseItems.isNotEmpty()) {
                    // Calculations
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = discount,
                                onValueChange = { discount = it },
                                label = { Text(stringResource(R.string.str_discount)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = taxPercentage,
                                onValueChange = { taxPercentage = it },
                                label = { Text(stringResource(R.string.str_tax)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Payment
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(stringResource(R.string.str_payment), fontWeight = FontWeight.Bold, color = BlueNavy)
                            
                            OutlinedTextField(
                                value = amountPaid,
                                onValueChange = { amountPaid = it },
                                label = { Text(stringResource(R.string.str_amount_paid___leave_blank_for_)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("CASH", "UPI", "CARD").forEach { method ->
                                    FilterChip(
                                        selected = paymentMethod == method,
                                        onClick = { paymentMethod = method },
                                        label = { Text(method) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSupplierSheet) {
        ModalBottomSheet(onDismissRequest = { showSupplierSheet = false }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                Text(stringResource(R.string.str_select_supplier), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                Spacer(modifier = Modifier.height(16.dp))
                if (mySuppliers.isEmpty()) {
                     Text(stringResource(R.string.str_no_suppliers_found_please_add_), color = TextSecondary)
                } else {
                    LazyColumn {
                        items(mySuppliers) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedSupplier = s
                                        showSupplierSheet = false 
                                    }
                                    .padding(16.dp)
                            ) {
                                Text(s.name, fontSize = 16.sp)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showProductSheet) {
        ModalBottomSheet(onDismissRequest = { showProductSheet = false }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                Text(stringResource(R.string.str_select_product), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                Spacer(modifier = Modifier.height(16.dp))
                if (myProducts.isEmpty()) {
                    Text(stringResource(R.string.str_no_products_found), color = TextSecondary)
                } else {
                    LazyColumn {
                        items(myProducts) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (purchaseItems.none { it.product.id == p.id }) {
                                            purchaseItems = purchaseItems + PurchaseItemState(p, 1)
                                        }
                                        showProductSheet = false 
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(stringResource(R.string.str_stock_val, p.currentStock), fontSize = 12.sp, color = TextSecondary)
                                }
                                Text(stringResource(R.string.str_2f).format(p.purchasePrice), fontWeight = FontWeight.Bold, color = BlueNavy)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

data class PurchaseItemState(
    val product: Product,
    val quantity: Int
)

@Composable
fun PurchaseItemRow(item: PurchaseItemState, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
            Text(stringResource(R.string.str_2f).format(item.product.purchasePrice), color = TextSecondary, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                modifier = Modifier.size(32.dp).background(BlueLight, RoundedCornerShape(8.dp))
            ) { Text(stringResource(R.string.str_empty), fontWeight = FontWeight.Bold) }
            
            Text(item.quantity.toString(), modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
            
            IconButton(
                onClick = { onQuantityChange(item.quantity + 1) },
                modifier = Modifier.size(32.dp).background(BlueLight, RoundedCornerShape(8.dp))
            ) { Text(stringResource(R.string.str_empty_1), fontWeight = FontWeight.Bold) }
            
            IconButton(onClick = onRemove, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = AccentRed)
            }
        }
    }
}
