package com.example.presentation.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.example.domain.model.Expense
import com.example.presentation.components.VyaparButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    expenseId: String? = null,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allExpenses by MockDataStore.expenses.collectAsState()
    val existingExpense = remember(expenseId, allExpenses) { allExpenses.find { it.id == expenseId } }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rent") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    
        
    LaunchedEffect(existingExpense) {
        if(existingExpense != null) {
            amount = existingExpense.amount.toString()
            description = existingExpense.description
            category = existingExpense.category
            paymentMethod = existingExpense.paymentMethod
            notes = existingExpense.notes
        }
    }


    
    var showCategoryMenu by remember { mutableStateOf(false) }
    
    val categories = listOf(
        "Rent", "Electricity", "Internet", "Salary", "Transport",
        "Maintenance", "Marketing", "Packaging", "Office Supplies",
        "Bank Charges", "Taxes", "Other"
    )

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_add_expense), fontWeight = FontWeight.Bold, color = BlueNavy) },
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
                VyaparButton(
                    text = "Save Expense",
                    onClick = {
                        val amountVal = amount.toDoubleOrNull() ?: 0.0
                        if (amountVal <= 0) {
                            errorMessage = "Amount must be greater than zero."
                            return@VyaparButton
                        }
                        
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = if (existingExpense != null) {
                                val updated = existingExpense.copy(
                                    category = category,
                                    description = description,
                                    amount = amountVal,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    updatedAt = System.currentTimeMillis()
                                )
                                MockDataStore.updateExpense(updated)
                            } else {
                                val newExpense = Expense(
                                    id = UUID.randomUUID().toString(),
                                    businessId = currentBusiness!!.id,
                                    category = category,
                                    description = description,
                                    amount = amountVal,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                MockDataStore.saveExpense(newExpense)
                            }
                            if (result.isSuccess) {
                                onNavigateBack()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save expense"
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
                contentPadding = PaddingValues(20.dp),
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

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text(stringResource(R.string.str_amount_1)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.str_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.str_eg_november_rent_travel_to_sup)) }
                    )
                    
                    var isSuggesting by remember { mutableStateOf(false) }
                    
                    TextButton(
                        onClick = {
                            if (description.isNotBlank()) {
                                isSuggesting = true
                                coroutineScope.launch {
                                    val result = com.example.data.repository.AiService.suggestExpenseCategory(description)
                                    isSuggesting = false
                                    if (result.isSuccess) {
                                        val suggestion = result.getOrNull()
                                        if (categories.contains(suggestion)) {
                                            category = suggestion!!
                                        } else {
                                            category = "Other"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (isSuggesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.str_suggest_category), color = BlueAccent, fontSize = 12.sp)
                    }
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = showCategoryMenu,
                        onExpandedChange = { showCategoryMenu = !showCategoryMenu }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.str_category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categories.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        category = selectionOption
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Text(stringResource(R.string.str_payment_method), fontWeight = FontWeight.Bold, color = BlueNavy, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CASH", "UPI", "CARD", "BANK").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.str_notes_optional)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 3
                    )
                }
            }
        }
    }
}
