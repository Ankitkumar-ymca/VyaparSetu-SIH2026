package com.example.presentation.scenario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.MockDataStore
import com.example.domain.intelligence.BusinessScenarioEngine
import com.example.domain.intelligence.ScenarioRequest
import com.example.domain.intelligence.ScenarioResult
import com.example.domain.intelligence.ScenarioType
import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.ui.theme.*

/**
 * Dedicated What-If Scenario Simulator Screen.
 *
 * Runs deterministic simulations via BusinessScenarioEngine without modifying real business data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessScenarioScreen(
    onNavigateBack: () -> Unit
) {
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allProducts by MockDataStore.products.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val allExpenses by MockDataStore.expenses.collectAsState()

    val myProducts = remember(allProducts, currentBusiness) {
        allProducts.filter { it.businessId == currentBusiness?.id && it.isActive }
    }
    val myCustomers = remember(allCustomers, currentBusiness) {
        allCustomers.filter { it.businessId == currentBusiness?.id && it.isActive }
    }
    val mySales = remember(allSales, currentBusiness) {
        allSales.filter { it.businessId == currentBusiness?.id }
    }
    val myExpenses = remember(allExpenses, currentBusiness) {
        allExpenses.filter { it.businessId == currentBusiness?.id }
    }

    // Tabs: 0 -> Extra Sales, 1 -> Price Change, 2 -> Expense Change, 3 -> Collection, 4 -> Cost Change
    var selectedTab by remember { mutableIntStateOf(0) }

    // Common Simulation Result & Validation Error
    var simulationResult by remember { mutableStateOf<ScenarioResult?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // 1. Extra Sales State
    var salesProduct by remember { mutableStateOf<Product?>(null) }
    var additionalUnitsText by remember { mutableStateOf("10") }

    // 2. Price Change State
    var priceProduct by remember { mutableStateOf<Product?>(null) }
    var pricePctText by remember { mutableStateOf("10") }
    var priceUnitsText by remember { mutableStateOf("") }

    // 3. Expense Change State
    var expenseDeltaText by remember { mutableStateOf("5000") }
    var isExpenseIncrease by remember { mutableStateOf(true) }

    // 4. Collection State
    var collectionCustomer by remember { mutableStateOf<Customer?>(null) }
    var collectionAmountText by remember { mutableStateOf("") }

    // 5. Cost Change State
    var costProduct by remember { mutableStateOf<Product?>(null) }
    var costPctText by remember { mutableStateOf("10") }
    var costUnitsText by remember { mutableStateOf("") }

    // Auto-select first product/customer if available and not yet set
    LaunchedEffect(myProducts) {
        if (salesProduct == null && myProducts.isNotEmpty()) salesProduct = myProducts.first()
        if (priceProduct == null && myProducts.isNotEmpty()) priceProduct = myProducts.first()
        if (costProduct == null && myProducts.isNotEmpty()) costProduct = myProducts.first()
    }
    LaunchedEffect(myCustomers) {
        if (collectionCustomer == null && myCustomers.isNotEmpty()) {
            collectionCustomer = myCustomers.firstOrNull { it.outstandingAmount > 0 } ?: myCustomers.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.str_what_if_simulator),
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BlueNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BlueBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prominent disclaimer banner
            item {
                ScenarioDisclaimerBanner()
            }

            // Scenario Type Selector Chips
            item {
                Text(
                    text = stringResource(R.string.str_scenario_disclaimer).let { "Select Scenario" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scenarioTabs = listOf(
                        0 to R.string.str_scenario_additional_sales,
                        1 to R.string.str_scenario_price_change,
                        2 to R.string.str_scenario_expense_change,
                        3 to R.string.str_scenario_collection,
                        4 to R.string.str_scenario_cost_change
                    )
                    items(scenarioTabs) { (tabIndex, stringResId) ->
                        FilterChip(
                            selected = selectedTab == tabIndex,
                            onClick = {
                                selectedTab = tabIndex
                                validationError = null
                                simulationResult = null
                            },
                            label = {
                                Text(
                                    text = stringResource(stringResId),
                                    fontWeight = if (selectedTab == tabIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueNavy,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("scenario_tab_$tabIndex")
                        )
                    }
                }
            }

            // Inputs Card depending on active scenario type
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scenario_input_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            // 0: Additional Sales
                            0 -> {
                                if (myProducts.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.str_no_products_for_scenario),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    ProductDropdownSelector(
                                        products = myProducts,
                                        selectedProduct = salesProduct,
                                        onProductSelected = { salesProduct = it }
                                    )

                                    OutlinedTextField(
                                        value = additionalUnitsText,
                                        onValueChange = { additionalUnitsText = it },
                                        label = { Text(stringResource(R.string.str_additional_units)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("additional_units_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Quick units adjustment chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(5, 10, 25, 50).forEach { units ->
                                            SuggestionChip(
                                                onClick = { additionalUnitsText = units.toString() },
                                                label = { Text("+$units") }
                                            )
                                        }
                                    }
                                }
                            }

                            // 1: Price Change
                            1 -> {
                                if (myProducts.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.str_no_products_for_scenario),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    ProductDropdownSelector(
                                        products = myProducts,
                                        selectedProduct = priceProduct,
                                        onProductSelected = { priceProduct = it }
                                    )

                                    OutlinedTextField(
                                        value = pricePctText,
                                        onValueChange = { pricePctText = it },
                                        label = { Text(stringResource(R.string.str_price_change_pct)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        placeholder = { Text("e.g. 5.0 or -5.0") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("price_pct_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Quick percentage presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("-10", "-5", "+5", "+10", "+15").forEach { pct ->
                                            SuggestionChip(
                                                onClick = { pricePctText = pct.replace("+", "") },
                                                label = { Text("$pct%") }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = priceUnitsText,
                                        onValueChange = { priceUnitsText = it },
                                        label = { Text("Reference Quantity (optional, default: past sales)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // 2: Expense Change
                            2 -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = isExpenseIncrease,
                                        onClick = { isExpenseIncrease = true },
                                        label = { Text("+ Increase") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = !isExpenseIncrease,
                                        onClick = { isExpenseIncrease = false },
                                        label = { Text("- Decrease") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = expenseDeltaText,
                                    onValueChange = { expenseDeltaText = it },
                                    label = { Text(stringResource(R.string.str_expense_change_amount)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("expense_delta_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(1000, 2000, 5000, 10000).forEach { amt ->
                                        SuggestionChip(
                                            onClick = { expenseDeltaText = amt.toString() },
                                            label = { Text("₹$amt") }
                                        )
                                    }
                                }
                            }

                            // 3: Collection
                            3 -> {
                                if (myCustomers.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.str_no_customers_for_scenario),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    CustomerDropdownSelector(
                                        customers = myCustomers,
                                        selectedCustomer = collectionCustomer,
                                        onCustomerSelected = { collectionCustomer = it }
                                    )

                                    OutlinedTextField(
                                        value = collectionAmountText,
                                        onValueChange = { collectionAmountText = it },
                                        label = { Text(stringResource(R.string.str_collection_amount)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        placeholder = {
                                            val due = collectionCustomer?.outstandingAmount ?: 0.0
                                            Text("Max due: ₹${String.format("%,.0f", due)}")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("collection_amount_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    val customerDue = collectionCustomer?.outstandingAmount ?: 0.0
                                    if (customerDue > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "100%").forEach { (ratio, label) ->
                                                SuggestionChip(
                                                    onClick = {
                                                        collectionAmountText = String.format("%.0f", customerDue * ratio)
                                                    },
                                                    label = { Text(label) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4: Cost Change
                            4 -> {
                                if (myProducts.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.str_no_products_for_scenario),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    ProductDropdownSelector(
                                        products = myProducts,
                                        selectedProduct = costProduct,
                                        onProductSelected = { costProduct = it }
                                    )

                                    OutlinedTextField(
                                        value = costPctText,
                                        onValueChange = { costPctText = it },
                                        label = { Text(stringResource(R.string.str_cost_change_pct)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        placeholder = { Text("e.g. 10.0 or -5.0") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cost_pct_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("-10", "-5", "+5", "+10", "+15").forEach { pct ->
                                            SuggestionChip(
                                                onClick = { costPctText = pct.replace("+", "") },
                                                label = { Text("$pct%") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Validation Error Display
                        if (validationError != null) {
                            Text(
                                text = "⚠️ $validationError",
                                color = AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Calculate Scenario Button
                        Button(
                            onClick = {
                                validationError = null
                                when (selectedTab) {
                                    0 -> { // Extra sales
                                        val product = salesProduct
                                        val units = additionalUnitsText.trim().toIntOrNull()
                                        if (product == null) {
                                            validationError = "Please select a product."
                                        } else if (units == null || units <= 0) {
                                            validationError = "Additional units must be a positive number."
                                        } else {
                                            simulationResult = BusinessScenarioEngine.simulate(
                                                request = ScenarioRequest.AdditionalSales(
                                                    productId = product.id,
                                                    additionalUnits = units
                                                ),
                                                products = myProducts,
                                                sales = mySales
                                            )
                                        }
                                    }
                                    1 -> { // Price change
                                        val product = priceProduct
                                        val pct = pricePctText.trim().toDoubleOrNull()
                                        val units = priceUnitsText.trim().toIntOrNull()
                                        if (product == null) {
                                            validationError = "Please select a product."
                                        } else if (pct == null) {
                                            validationError = "Please enter a valid price change percentage."
                                        } else {
                                            simulationResult = BusinessScenarioEngine.simulate(
                                                request = ScenarioRequest.PriceChange(
                                                    productId = product.id,
                                                    percentageChange = pct,
                                                    referenceUnits = units
                                                ),
                                                products = myProducts,
                                                sales = mySales
                                            )
                                        }
                                    }
                                    2 -> { // Expense change
                                        val amt = expenseDeltaText.trim().toDoubleOrNull()
                                        if (amt == null || amt <= 0.0) {
                                            validationError = "Please enter a valid expense amount."
                                        } else {
                                            val signedAmt = if (isExpenseIncrease) amt else -amt
                                            simulationResult = BusinessScenarioEngine.simulate(
                                                request = ScenarioRequest.ExpenseChange(
                                                    amountDelta = signedAmt
                                                ),
                                                expenses = myExpenses,
                                                sales = mySales
                                            )
                                        }
                                    }
                                    3 -> { // Collection
                                        val cust = collectionCustomer
                                        val amt = collectionAmountText.trim().toDoubleOrNull()
                                        if (cust == null) {
                                            validationError = "Please select a customer."
                                        } else if (cust.outstandingAmount <= 0.0) {
                                            validationError = "Selected customer has no outstanding balance."
                                        } else if (amt == null || amt <= 0.0) {
                                            validationError = "Please enter a valid recovery amount."
                                        } else {
                                            simulationResult = BusinessScenarioEngine.simulate(
                                                request = ScenarioRequest.ReceivableRecovery(
                                                    customerId = cust.id,
                                                    recoveryAmount = amt
                                                ),
                                                customers = myCustomers
                                            )
                                        }
                                    }
                                    4 -> { // Cost change
                                        val product = costProduct
                                        val pct = costPctText.trim().toDoubleOrNull()
                                        if (product == null) {
                                            validationError = "Please select a product."
                                        } else if (pct == null) {
                                            validationError = "Please enter a valid cost change percentage."
                                        } else {
                                            simulationResult = BusinessScenarioEngine.simulate(
                                                request = ScenarioRequest.CostChange(
                                                    productId = product.id,
                                                    percentageChange = pct
                                                ),
                                                products = myProducts,
                                                sales = mySales
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("calculate_scenario_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueNavy)
                        ) {
                            Text(
                                text = stringResource(R.string.str_calculate_scenario),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Results Section
            simulationResult?.let { result ->
                if (result.isValid) {
                    item {
                        ScenarioResultCard(
                            result = result,
                            onReset = { simulationResult = null }
                        )
                    }
                } else if (result.validationErrors.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ " + result.validationErrors.joinToString("\n"),
                                color = AccentRed,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Clean Product selector dropdown menu.
 */
@Composable
private fun ProductDropdownSelector(
    products: List<Product>,
    selectedProduct: Product?,
    onProductSelected: (Product) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.str_select_product),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("product_dropdown_trigger")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedProduct?.name ?: "Select a product",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedProduct != null) TextPrimary else TextSecondary
                    )
                    if (selectedProduct != null) {
                        Text(
                            text = "Price: ₹${String.format("%,.2f", selectedProduct.sellingPrice)} | Stock: ${selectedProduct.currentStock}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = TextSecondary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                products.forEach { prod ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(prod.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Price: ₹${prod.sellingPrice} | Stock: ${prod.currentStock}", fontSize = 11.sp, color = TextSecondary)
                            }
                        },
                        onClick = {
                            onProductSelected(prod)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Clean Customer selector dropdown menu.
 */
@Composable
private fun CustomerDropdownSelector(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onCustomerSelected: (Customer) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.str_select_customer),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("customer_dropdown_trigger")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedCustomer?.name ?: "Select a customer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedCustomer != null) TextPrimary else TextSecondary
                    )
                    if (selectedCustomer != null) {
                        Text(
                            text = "Outstanding Due: ₹${String.format("%,.2f", selectedCustomer.outstandingAmount)}",
                            fontSize = 11.sp,
                            color = if (selectedCustomer.outstandingAmount > 0) AccentRed else AccentGreen
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = TextSecondary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                customers.forEach { cust ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(cust.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Outstanding: ₹${cust.outstandingAmount}", fontSize = 11.sp, color = if (cust.outstandingAmount > 0) AccentRed else AccentGreen)
                            }
                        },
                        onClick = {
                            onCustomerSelected(cust)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
