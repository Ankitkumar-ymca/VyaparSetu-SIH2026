package com.example.presentation.khata

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.model.Customer
import com.example.domain.model.Supplier
import com.example.presentation.components.VyaparButton
import com.example.presentation.components.VyaparTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyFormScreen(
    partyId: String,
    isCustomer: Boolean,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = partyId != "new"
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val allCustomers by MockDataStore.customers.collectAsState()
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    
    val existingCustomer = if (isEditing && isCustomer) allCustomers.find { it.id == partyId } else null
    val existingSupplier = if (isEditing && !isCustomer) allSuppliers.find { it.id == partyId } else null

    var name by remember { mutableStateOf(existingCustomer?.name ?: existingSupplier?.name ?: "") }
    var phone by remember { mutableStateOf(existingCustomer?.phone ?: existingSupplier?.phone ?: "") }
    var email by remember { mutableStateOf(existingCustomer?.email ?: existingSupplier?.email ?: "") }
    var address by remember { mutableStateOf(existingCustomer?.address ?: existingSupplier?.address ?: "") }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val entityName = if (isCustomer) "Customer" else "Supplier"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) stringResource(R.string.str_edit_entity, entityName) else stringResource(R.string.str_add_entity, entityName), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
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

            VyaparTextField(value = name, onValueChange = { name = it }, label = "Name *")
            VyaparTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number *", keyboardType = KeyboardType.Phone)
            VyaparTextField(value = email, onValueChange = { email = it }, label = "Email (Optional)", keyboardType = KeyboardType.Email)
            VyaparTextField(value = address, onValueChange = { address = it }, label = "Address (Optional)")
            
            Spacer(modifier = Modifier.height(32.dp))

            VyaparButton(
                text = "Save $entityName",
                isLoading = isLoading,
                onClick = {
                    if (name.isBlank()) { errorMsg = "Name is required"; return@VyaparButton }
                    if (phone.isBlank()) { errorMsg = "Phone is required"; return@VyaparButton }
                    
                    val businessId = currentBusiness?.id ?: return@VyaparButton
                    isLoading = true
                    
                    scope.launch {
                        val result = if (isCustomer) {
                            val customer = Customer(
                                id = existingCustomer?.id ?: UUID.randomUUID().toString(),
                                businessId = businessId,
                                name = name, phone = phone, email = email, address = address,
                                totalPurchases = existingCustomer?.totalPurchases ?: 0.0,
                                totalPaid = existingCustomer?.totalPaid ?: 0.0,
                                outstandingAmount = existingCustomer?.outstandingAmount ?: 0.0,
                                createdAt = existingCustomer?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            MockDataStore.saveCustomer(customer).isSuccess
                        } else {
                            val supplier = Supplier(
                                id = existingSupplier?.id ?: UUID.randomUUID().toString(),
                                businessId = businessId,
                                name = name, phone = phone, email = email, address = address,
                                totalPurchases = existingSupplier?.totalPurchases ?: 0.0,
                                totalPaid = existingSupplier?.totalPaid ?: 0.0,
                                outstandingPayable = existingSupplier?.outstandingPayable ?: 0.0,
                                createdAt = existingSupplier?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            MockDataStore.saveSupplier(supplier).isSuccess
                        }
                        
                        isLoading = false
                        if (result) onNavigateBack() else errorMsg = "Failed to save"
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val allCustomers by MockDataStore.customers.collectAsState()
    val customer = allCustomers.find { it.id == customerId }
    var showPaymentDialog by remember { mutableStateOf(false) }
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
        },
        containerColor = BlueBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, CardBorder, RoundedCornerShape(24.dp)).padding(24.dp)
            ) {
                Text(customer.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                Spacer(modifier = Modifier.height(4.dp))
                Text(customer.phone, fontSize = 14.sp, color = TextSecondary)
            }

            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, CardBorder, RoundedCornerShape(24.dp)).padding(24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.str_amount_due), fontSize = 14.sp, color = TextSecondary)
                        Text("₹${customer.outstandingAmount}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if(customer.outstandingAmount > 0) AccentRed else AccentGreen)
                    }

                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueLight, contentColor = BlueNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.str_receive), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.str_add_previous_outstanding), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.str_total_purchases), fontSize = 12.sp, color = TextSecondary)
                        Text("₹${customer.totalPurchases}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.str_total_paid), fontSize = 12.sp, color = TextSecondary)
                        Text("₹${customer.totalPaid}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Customer Intelligence Section
            val allSales by MockDataStore.sales.collectAsState()
            val customerIntelligence = remember(customer, allSales) {
                com.example.domain.intelligence.CustomerIntelligenceEngine.analyze(
                    customers = listOf(customer),
                    sales = allSales
                ).customers.firstOrNull()
            }
            CustomerIntelligenceDetailCard(metrics = customerIntelligence)

            // Collection Priority Review Section
            val collectionPriorityResult = remember(customerIntelligence) {
                customerIntelligence?.let {
                    com.example.domain.intelligence.CollectionPriorityEngine.evaluateFromMetrics(listOf(it)).results.firstOrNull()
                }
            }
            CustomerDetailCollectionCard(result = collectionPriorityResult)

            Text(stringResource(R.string.str_transaction_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            val ledgerEntries = MockDataStore.ledgerEntries.collectAsState().value.filter { it.partyId == customerId }.sortedByDescending { it.timestamp }
            if (ledgerEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.str_no_transactions_yet), color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                ledgerEntries.forEach { entry ->
                    KhataEntryCard(entry)
                }
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(customer = customer, onDismiss = { showPaymentDialog = false })
    }
    if (showPreviousOutstandingDialog) {
        PreviousOutstandingDialog(customer = customer, onDismiss = { showPreviousOutstandingDialog = false })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
                        label = { Text(stringResource(R.string.str_select_customer)) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(customer: Customer, onDismiss: () -> Unit) {
    if (customer.outstandingAmount <= 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.str_no_amount_due)) },
            text = { Text(stringResource(R.string.str_this_customer_has_no_outstandi)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.str_ok)) } }
        )
        return
    }

    var amountStr by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.str_record_payment), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.str_outstanding_amt, customer.outstandingAmount), fontSize = 14.sp, color = AccentRed, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text(stringResource(R.string.str_amount_1)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                
                // Simple segmented control substitute for method
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Cash", "UPI", "Bank").forEach { opt ->
                        FilterChip(
                            selected = method == opt, onClick = { method = opt },
                            label = { Text(opt) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueLight)
                        )
                    }
                }
                
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.str_notes_optional)) }, modifier = Modifier.fillMaxWidth())
                if (errorMsg != null) Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) { errorMsg = "Enter valid amount > 0"; return@Button }
                    if (amount > customer.outstandingAmount) { errorMsg = "Amount cannot exceed outstanding due"; return@Button }
                    
                    isLoading = true
                    scope.launch {
                        val res = MockDataStore.recordCustomerPayment(customer.id, amount, method, notes)
                        isLoading = false
                        if (res.isSuccess) onDismiss() else errorMsg = "Failed to record payment"
                    }
                },
                enabled = !isLoading
            ) { Text(stringResource(R.string.str_save_payment)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.str_cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    supplierId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    val supplier = allSuppliers.find { it.id == supplierId }

    var showMenu by remember { mutableStateOf(false) }
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
        },
        containerColor = BlueBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, CardBorder, RoundedCornerShape(24.dp)).padding(24.dp)
            ) {
                Text(supplier.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                Spacer(modifier = Modifier.height(4.dp))
                Text(supplier.phone, fontSize = 14.sp, color = TextSecondary)
            }

            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, CardBorder, RoundedCornerShape(24.dp)).padding(24.dp)
            ) {
                Column {
                    Text(stringResource(R.string.str_amount_payable), fontSize = 14.sp, color = TextSecondary)
                    Text("₹${supplier.outstandingPayable}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if(supplier.outstandingPayable > 0) AccentRed else TextSecondary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.str_total_purchases), fontSize = 12.sp, color = TextSecondary)
                        Text("₹${supplier.totalPurchases}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.str_total_paid), fontSize = 12.sp, color = TextSecondary)
                        Text("₹${supplier.totalPaid}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(stringResource(R.string.str_purchase_history), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.str_no_purchases_yet), color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.str_purchases_coming_in_phase_3), color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun KhataEntryCard(entry: com.example.domain.model.LedgerEntry) {
    var showMenu by remember { mutableStateOf(false) }
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
                            MockDataStore.deleteKhataEntry(entry.id)
                            isDeleting = false
                            showDeleteConfirm = false
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(entry.type, fontSize = 12.sp, color = TextSecondary)
            Text(stringResource(R.string.str_balance_amt, entry.balanceAfter), fontSize = 12.sp, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "₹${entry.amount}",
                fontWeight = FontWeight.Bold,
                color = if (entry.type == "PAYMENT") AccentGreen else AccentRed
            )
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.str_delete)) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }
}
