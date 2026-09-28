package com.example.presentation.sales

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import com.example.presentation.components.VyaparButton
import com.example.ui.theme.*
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    onNavigateBack: () -> Unit,
    onSaleComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val allProducts by MockDataStore.products.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()
    
    val myProducts = allProducts.filter { it.businessId == currentBusiness?.id && it.currentStock > 0 }
    val myCustomers = allCustomers.filter { it.businessId == currentBusiness?.id }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerSheet by remember { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }
    
    var saleItems by remember { mutableStateOf<List<SaleItemState>>(emptyList()) }
    var discount by remember { mutableStateOf("") }
    var taxPercentage by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Computations
    val subtotal = saleItems.sumOf { it.product.sellingPrice * it.quantity }
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
                title = { Text(stringResource(R.string.str_new_sale), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
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
                    text = "Complete Sale",
                    onClick = {
                        if (saleItems.isEmpty()) {
                            errorMessage = "Please add at least one product."
                            return@VyaparButton
                        }
                        if (paidVal < grandTotal && selectedCustomer == null) {
                            errorMessage = "Customer must be selected for credit or partial payment."
                            return@VyaparButton
                        }
                        if (paidVal < 0) {
                            errorMessage = "Amount paid cannot be negative."
                            return@VyaparButton
                        }
                        
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val newSale = Sale(
                                id = UUID.randomUUID().toString(),
                                businessId = currentBusiness!!.id,
                                invoiceNumber = "INV-${System.currentTimeMillis() % 100000}",
                                customerId = selectedCustomer?.id,
                                items = saleItems.map { 
                                    SaleItem(
                                        productId = it.product.id,
                                        productName = it.product.name,
                                        quantity = it.quantity,
                                        unitPrice = it.product.sellingPrice,
                                        totalPrice = it.product.sellingPrice * it.quantity
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
                            val result = MockDataStore.createSale(newSale)
                            if (result.isSuccess) {
                                onSaleComplete(newSale.id)
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save sale"
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

                // Customer Selection
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { showCustomerSheet = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Customer", tint = BlueNavy)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.str_customer), fontSize = 12.sp, color = TextSecondary)
                            Text(selectedCustomer?.name ?: "Walk-in Customer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                    }
                }

                // Add Product Button
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showProductSheet = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.str_add_products), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Button(
                            onClick = {
                                val options = GmsBarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                    .build()
                                val scanner = GmsBarcodeScanning.getClient(context, options)
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        val rawValue = barcode.rawValue
                                        if (rawValue != null) {
                                            val matchedProduct = myProducts.find { it.barcode == rawValue || it.sku == rawValue }
                                            if (matchedProduct != null) {
                                                val existing = saleItems.find { it.product.id == matchedProduct.id }
                                                if (existing != null) {
                                                    saleItems = saleItems.map { if (it.product.id == matchedProduct.id) it.copy(quantity = it.quantity + 1) else it }
                                                } else {
                                                    saleItems = saleItems + SaleItemState(matchedProduct, 1)
                                                }
                                                errorMessage = null
                                            } else {
                                                errorMessage = "Product with barcode $rawValue not found."
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Scan failed: ${e.message}"
                                    }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueNavy, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.str_scan_barcode), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Items List
                items(saleItems) { item ->
                    SaleItemRow(
                        item = item,
                        onQuantityChange = { newQ ->
                            if (newQ > item.product.currentStock) {
                                errorMessage = "Only ${item.product.currentStock} units available."
                            } else if (newQ > 0) {
                                saleItems = saleItems.map { if (it.product.id == item.product.id) it.copy(quantity = newQ) else it }
                                errorMessage = null
                            }
                        },
                        onRemove = {
                            saleItems = saleItems.filter { it.product.id != item.product.id }
                        }
                    )
                }

                if (saleItems.isNotEmpty()) {
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

    if (showCustomerSheet) {
        ModalBottomSheet(onDismissRequest = { showCustomerSheet = false }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                Text(stringResource(R.string.str_select_customer), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedCustomer = null
                                    showCustomerSheet = false 
                                }
                                .padding(16.dp)
                        ) {
                            Text(stringResource(R.string.str_walkin_customer), fontSize = 16.sp)
                        }
                        HorizontalDivider()
                    }
                    items(myCustomers) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedCustomer = c
                                    showCustomerSheet = false 
                                }
                                .padding(16.dp)
                        ) {
                            Text(c.name, fontSize = 16.sp)
                        }
                        HorizontalDivider()
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
                    Text(stringResource(R.string.str_no_products_with_available_sto), color = TextSecondary)
                } else {
                    LazyColumn {
                        items(myProducts) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (saleItems.none { it.product.id == p.id }) {
                                            saleItems = saleItems + SaleItemState(p, 1)
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
                                Text(stringResource(R.string.str_2f).format(p.sellingPrice), fontWeight = FontWeight.Bold, color = BlueNavy)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

data class SaleItemState(
    val product: Product,
    val quantity: Int
)

@Composable
fun SaleItemRow(item: SaleItemState, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
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
            Text(stringResource(R.string.str_2f).format(item.product.sellingPrice), color = TextSecondary, fontSize = 14.sp)
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
