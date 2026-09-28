package com.example.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.presentation.components.VyaparButton
import com.example.presentation.components.VyaparTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSetupComplete: () -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    
    var businessNameError by remember { mutableStateOf<String?>(null) }
    var ownerNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Retail", "Grocery", "Electronics", "Clothing", "Wholesale", "Services", "Other")
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_set_up_your_business), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },
        containerColor = BlueBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Progress Indicator (simple visual for step 1 of 1 for now)
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = BlueAccent,
                trackColor = CardBorder
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            VyaparTextField(
                value = businessName,
                onValueChange = { businessName = it; businessNameError = null },
                label = "Business Name*",
                isError = businessNameError != null,
                errorMessage = businessNameError
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            VyaparTextField(
                value = ownerName,
                onValueChange = { ownerName = it; ownerNameError = null },
                label = "Owner Name*",
                isError = ownerNameError != null,
                errorMessage = ownerNameError
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category.ifEmpty { "Select Category*" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if(categoryError != null) MaterialTheme.colorScheme.error else BlueAccent,
                        unfocusedBorderColor = if(categoryError != null) MaterialTheme.colorScheme.error else CardBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    isError = categoryError != null
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                categoryExpanded = false
                                categoryError = null
                            }
                        )
                    }
                }
            }
            if (categoryError != null) {
                Text(
                    text = categoryError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            VyaparTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = "Business Phone Number*",
                isError = phoneError != null,
                errorMessage = phoneError
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            VyaparTextField(
                value = address,
                onValueChange = { address = it },
                label = "Business Address (Optional)"
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            VyaparTextField(
                value = gstin,
                onValueChange = { gstin = it },
                label = "GSTIN (Optional)"
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            VyaparButton(
                text = "Complete Setup",
                isLoading = isLoading,
                onClick = {
                    var isValid = true
                    if (businessName.isBlank()) { businessNameError = "Business Name required"; isValid = false }
                    if (ownerName.isBlank()) { ownerNameError = "Owner Name required"; isValid = false }
                    if (phone.isBlank()) { phoneError = "Phone required"; isValid = false }
                    if (category.isBlank()) { categoryError = "Category required"; isValid = false }
                    
                    if (isValid) {
                        isLoading = true
                        scope.launch {
                            val result = MockDataStore.setupBusiness(
                                name = businessName,
                                ownerName = ownerName,
                                category = category,
                                phone = phone,
                                address = address,
                                gstin = gstin.takeIf { it.isNotBlank() }
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                onSetupComplete()
                            }
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
