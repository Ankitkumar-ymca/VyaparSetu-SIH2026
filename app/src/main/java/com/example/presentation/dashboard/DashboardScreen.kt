package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.intelligence.BusinessActionCategory
import com.example.domain.intelligence.BusinessActionEngine
import com.example.domain.intelligence.BusinessAlertEngine
import com.example.domain.intelligence.CollectionPriorityEngine
import com.example.domain.intelligence.CrossBusinessInsightsEngine
import com.example.domain.intelligence.CustomerIntelligenceEngine
import com.example.domain.intelligence.InventoryIntelligenceEngine
import com.example.domain.intelligence.ProductProfitabilityEngine
import com.example.domain.model.Business
import com.example.domain.model.User
import com.example.presentation.actions.BusinessActionSummaryCard
import com.example.presentation.alerts.BusinessAlertSummaryCard
import com.example.presentation.components.VyaparButton
import com.example.presentation.intelligence.BusinessInsightsDashboardCard
import com.example.presentation.inventory.InventoryScreen
import com.example.presentation.khata.KhataScreen
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.scenario.ScenarioDashboardEntryCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToSubRoute: (String) -> Unit
) {
    val currentUser by MockDataStore.currentUser.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val allProducts by MockDataStore.products.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val allPurchases by MockDataStore.purchases.collectAsState()
    
    val myProducts = allProducts.filter { it.businessId == currentBusiness?.id }
    val myCustomers = allCustomers.filter { it.businessId == currentBusiness?.id }
    val mySuppliers = allSuppliers.filter { it.businessId == currentBusiness?.id }
    val mySales = allSales.filter { it.businessId == currentBusiness?.id }
    val myPurchases = allPurchases.filter { it.businessId == currentBusiness?.id }
    
    val allExpenses by MockDataStore.expenses.collectAsState()
    val myExpenses = allExpenses.filter { it.businessId == currentBusiness?.id }
    
    val lowStockCount = myProducts.count { it.currentStock <= it.minStock }
    val outstandingCount = myCustomers.count { it.outstandingAmount > 0 }
    
    // Quick today filter (simple midnight check can be complex in local time, using a rough 24h approximation for MVP or just summing all)
    // For Phase 4 real values, calculating past 24 hours.
    val oneDayMs = 24 * 60 * 60 * 1000L
    val todayMs = System.currentTimeMillis() - oneDayMs
    val todaysSalesList = mySales.filter { it.createdAt >= todayMs }
    val todaysSales = todaysSalesList.sumOf { it.grandTotal }
    
    val todaysExpenses = myExpenses.filter { it.createdAt >= todayMs }.sumOf { it.amount }
    
    // Calculate COGS for today's sales
    val todaysCogs = todaysSalesList.sumOf { sale ->
        sale.items.sumOf { it.quantity * it.purchasePriceAtSale }
    }
    
    val todaysProfit = todaysSales - todaysCogs - todaysExpenses
    
    val inventoryValue = myProducts.sumOf { it.currentStock * it.purchasePrice }
    val totalOutstanding = myCustomers.sumOf { it.outstandingAmount }

    // Derive deterministic business alerts via domain intelligence engines
    val inventorySummary = remember(myProducts, mySales) {
        InventoryIntelligenceEngine.analyze(
            products = myProducts,
            sales = mySales
        )
    }

    val profitabilitySummary = remember(myProducts, mySales) {
        ProductProfitabilityEngine.analyze(
            products = myProducts,
            sales = mySales
        )
    }

    val businessAlerts = remember(inventorySummary, profitabilitySummary) {
        BusinessAlertEngine.generateAlerts(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary.products,
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary.products
        )
    }

    val customerSummary = remember(myCustomers, mySales) {
        CustomerIntelligenceEngine.analyze(
            customers = myCustomers,
            sales = mySales
        )
    }

    val collectionSummary = remember(customerSummary) {
        CollectionPriorityEngine.evaluateFromMetrics(customerSummary.customers)
    }

    val businessActionSummary = remember(inventorySummary, profitabilitySummary, customerSummary, collectionSummary) {
        BusinessActionEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            profitabilitySummary = profitabilitySummary,
            customerSummary = customerSummary,
            collectionSummary = collectionSummary
        )
    }

    val crossInsightsSummary = remember(
        inventorySummary,
        profitabilitySummary,
        customerSummary,
        collectionSummary,
        businessAlerts,
        businessActionSummary
    ) {
        CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary.products,
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary.products,
            customerSummary = customerSummary,
            customerMetrics = customerSummary.customers,
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary.results,
            businessAlerts = businessAlerts,
            businessActions = businessActionSummary.allActions
        )
    }

    val commandCenterProfitSnapshot = remember(profitabilitySummary) {
        com.example.domain.intelligence.CommandCenterProfitSnapshot.fromProductProfitabilitySummary(profitabilitySummary)
    }

    val commandCenterSummary = remember(
        businessAlerts,
        businessActionSummary,
        crossInsightsSummary,
        collectionSummary,
        commandCenterProfitSnapshot
    ) {
        com.example.domain.intelligence.UnifiedBusinessCommandCenterEngine.generate(
            businessAlerts = businessAlerts,
            businessActions = businessActionSummary.allActions,
            crossBusinessInsights = crossInsightsSummary.insights,
            collectionResults = collectionSummary.results,
            workingCapitalLockedTotal = crossInsightsSummary.workingCapitalLockedTotal,
            profitSnapshot = commandCenterProfitSnapshot
        ).summary
    }

    
    var showProfileSheet by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("Home") }
    var khataSelectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(BlueBackground),
        containerColor = BlueBackground,
        floatingActionButton = {
            if (currentTab == "Home" || currentTab == "Sales") {
                FloatingActionButton(
                    onClick = { onNavigateToSubRoute("new_sale") },
                    containerColor = BlueLight,
                    contentColor = BlueNavy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                }
            } else if (currentTab == "Inventory") {
                FloatingActionButton(
                    onClick = { onNavigateToSubRoute("product_form/new") },
                    containerColor = BlueLight,
                    contentColor = BlueNavy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.str_add_products), modifier = Modifier.size(32.dp))
                }
            
            } else if (currentTab == "Expenses") {
                FloatingActionButton(
                    onClick = { onNavigateToSubRoute("new_expense") },
                    containerColor = BlueLight,
                    contentColor = BlueNavy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.str_add_expense_btn), modifier = Modifier.size(32.dp))
                }

        } else if (currentTab == "Khata") {
                FloatingActionButton(
                    onClick = { 
                        if (khataSelectedTab == 0) {
                            onNavigateToSubRoute("customer_form/new")
                        } else {
                            onNavigateToSubRoute("supplier_form/new")
                        }
                    },
                    containerColor = BlueLight,
                    contentColor = BlueNavy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = if (khataSelectedTab == 0) stringResource(R.string.str_add_customer) else stringResource(R.string.str_add_supplier),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(currentTab = currentTab, onTabSelected = { currentTab = it })
        }
    ) { innerPadding ->
        if (currentTab == "Home") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection(
                    user = currentUser, 
                    business = currentBusiness,
                    onProfileClick = { showProfileSheet = true }
                )
                
                SalesCard(todaysSales, todaysExpenses, todaysProfit)

                // Today's Business Actions Card
                BusinessActionSummaryCard(
                    summary = businessActionSummary,
                    onViewAllClick = { onNavigateToSubRoute("business_actions") },
                    onActionClick = { action ->
                        when {
                            action.targetId != null && (action.category == BusinessActionCategory.RESTOCK ||
                                    action.category == BusinessActionCategory.CLEAR_STOCK ||
                                    action.category == BusinessActionCategory.REVIEW_PRICING) -> {
                                onNavigateToSubRoute("product_detail/${action.targetId}")
                            }
                            action.targetId != null && (action.category == BusinessActionCategory.COLLECT_PAYMENT ||
                                    action.category == BusinessActionCategory.REENGAGE_CUSTOMER) -> {
                                onNavigateToSubRoute("customer_detail/${action.targetId}")
                            }
                            else -> {
                                onNavigateToSubRoute("business_actions")
                            }
                        }
                    }
                )
 
                // Unified Business Command Center Entry Card
                com.example.presentation.commandcenter.CommandCenterDashboardCard(
                    attentionCount = commandCenterSummary.attentionCount,
                    actionCount = commandCenterSummary.actionCount,
                    insightCount = commandCenterSummary.insightCount,
                    opportunityCount = commandCenterSummary.opportunityCount,
                    onOpenCommandCenter = { onNavigateToSubRoute("command_center") }
                )

                // Smart Business Alert Summary Card
                BusinessAlertSummaryCard(
                    alerts = businessAlerts,
                    onViewAllClick = { onNavigateToSubRoute("business_alerts") },
                    onAlertClick = { alert ->
                        if (alert.productId != null) {
                            onNavigateToSubRoute("product_detail/${alert.productId}")
                        } else {
                            onNavigateToSubRoute("business_alerts")
                        }
                    }
                )
                
                // What-If Scenario Simulator Card
                ScenarioDashboardEntryCard(
                    onNavigateToSimulator = { onNavigateToSubRoute("business_scenario") }
                )

                // Cross-Business Insights Card
                if (crossInsightsSummary.totalInsights > 0) {
                    BusinessInsightsDashboardCard(
                        totalInsights = crossInsightsSummary.totalInsights,
                        criticalCount = crossInsightsSummary.criticalInsightsCount,
                        highCount = crossInsightsSummary.highInsightsCount,
                        capitalLocked = crossInsightsSummary.workingCapitalLockedTotal,
                        onViewAllClick = { onNavigateToSubRoute("business_insights") }
                    )
                }

AiAdvisorCard(
                    onAskBusiness = { onNavigateToSubRoute("ai_assistant") },
                    onNavigateToSubRoute = onNavigateToSubRoute
                )
                
                // Real DB Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCardSimple(stringResource(R.string.str_products), myProducts.size.toString(), Modifier.weight(1f)) { currentTab = "Inventory" }
                    StatCardSimple(stringResource(R.string.str_customers), myCustomers.size.toString(), Modifier.weight(1f)) { khataSelectedTab = 0; currentTab = "Khata" }
                    StatCardSimple(stringResource(R.string.str_suppliers), mySuppliers.size.toString(), Modifier.weight(1f)) { khataSelectedTab = 1; currentTab = "Khata" }
                }
                
                if (lowStockCount > 0) {
                    EmptyStateCard(stringResource(R.string.str_inventory_alert), stringResource(R.string.str_products_low_stock, lowStockCount))
                } else if (myProducts.isEmpty()) {
                    EmptyStateCard("Inventory", stringResource(R.string.str_add_first_product))
                }
                
                if (outstandingCount > 0) {
                    EmptyStateCard(stringResource(R.string.str_khata_alert), stringResource(R.string.str_customers_outstanding, outstandingCount))
                } else if (myCustomers.isEmpty()) {
                    EmptyStateCard("Khata / Outstanding", stringResource(R.string.str_customer_outstanding_appear))
                } else {
                    EmptyStateCard("Khata", stringResource(R.string.str_no_outstanding))
                }
                
                // Recent Transactions
                Text(stringResource(R.string.str_recent_transactions), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy, modifier = Modifier.padding(top = 8.dp))
                
                val salesTx = mySales.map { sale -> 
                    val cName = myCustomers.find { it.id == sale.customerId }?.name ?: stringResource(R.string.str_cash)
                    Triple(stringResource(R.string.str_tx_sale), stringResource(R.string.str_sale_to, cName), Pair(sale.grandTotal, sale.createdAt)) 
                }
                val purchasesTx = myPurchases.map { purchase -> 
                    val sName = mySuppliers.find { it.id == purchase.supplierId }?.name ?: stringResource(R.string.str_unknown)
                    Triple(stringResource(R.string.str_tx_purchase), stringResource(R.string.str_purchase_from, sName), Pair(purchase.grandTotal, purchase.createdAt)) 
                }
                val expensesTx = MockDataStore.expenses.collectAsState().value.filter { it.businessId == currentBusiness?.id }.map { 
                    Triple(stringResource(R.string.str_tx_expense), it.category, Pair(it.amount, it.createdAt)) 
                }
                val paymentsTx = MockDataStore.ledgerEntries.collectAsState().value.filter { it.businessId == currentBusiness?.id && (it.type == "PAYMENT" || it.type == "OPENING_BALANCE") }.map { entry ->
                    val cName = myCustomers.find { it.id == entry.partyId }?.name
                    val sName = mySuppliers.find { it.id == entry.partyId }?.name
                    val partyName = cName ?: sName ?: ""
                    val desc = if (partyName.isNotEmpty()) "$partyName - ${entry.description}" else entry.description
                    Triple(if (entry.type == "OPENING_BALANCE") stringResource(R.string.str_tx_balance) else stringResource(R.string.str_tx_payment), desc, Pair(entry.amount, entry.timestamp))
                }
                
                val recentTransactions = (salesTx + purchasesTx + expensesTx + paymentsTx)
                    .sortedByDescending { it.third.second }
                    .take(5)
                    
                if (recentTransactions.isEmpty()) {
                    EmptyStateCard(stringResource(R.string.str_transactions), stringResource(R.string.str_no_recent_tx))
                } else {
                    recentTransactions.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueNavy)
                                Text(entry.second, fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(
                                text = stringResource(R.string.str_2f).format(entry.third.first),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (entry.first == "PAYMENT" || entry.first == "SALE") AccentGreen else TextPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        } else if (currentTab == "Inventory") {
            Box(modifier = Modifier.padding(innerPadding)) {
                InventoryScreen(
                    onNavigateToProductForm = { id -> onNavigateToSubRoute("product_form/$id") },
                    onNavigateToProductDetail = { id -> onNavigateToSubRoute("product_detail/$id") },
                    onNavigateToPurchaseDetail = { id -> onNavigateToSubRoute("purchase_detail/$id") },
                    onNavigateToNewPurchase = { onNavigateToSubRoute("new_purchase") }
                )
            }
        } else if (currentTab == "Sales") {
            Box(modifier = Modifier.padding(innerPadding)) {
                com.example.presentation.sales.SalesListScreen(
                    onNavigateToSaleDetail = { id -> onNavigateToSubRoute("sale_detail/$id") }
                )
            }
        
        } else if (currentTab == "Khata") {
            Box(modifier = Modifier.padding(innerPadding)) {
                KhataScreen(
                    selectedTab = khataSelectedTab,
                    onTabSelected = { khataSelectedTab = it },
                    onNavigateToCustomerForm = { id -> onNavigateToSubRoute("customer_form/$id") },
                    onNavigateToCustomerDetail = { id -> onNavigateToSubRoute("customer_detail/$id") },
                    onNavigateToSupplierForm = { id -> onNavigateToSubRoute("supplier_form/$id") },
                    onNavigateToSupplierDetail = { id -> onNavigateToSubRoute("supplier_detail/$id") }
                )
            }
        
        } else if (currentTab == "Expenses") {
            Box(modifier = Modifier.padding(innerPadding)) {
                com.example.presentation.expenses.ExpensesScreen(
                    onNavigateToNewExpense = { onNavigateToSubRoute("new_expense") }
                )
            }

        } else if (currentTab == "Reports") {
            Box(modifier = Modifier.padding(innerPadding)) {
                ReportsScreen(
                    onNavigateToSubRoute = onNavigateToSubRoute
                )
            }
        }
    }

    if (showProfileSheet) {
        ModalBottomSheet(onDismissRequest = { showProfileSheet = false }, containerColor = Color.White) {
            ProfileSettingsContent(
                user = currentUser, business = currentBusiness,
                onLogout = {
                    showProfileSheet = false
                    kotlinx.coroutines.GlobalScope.launch {
                        MockDataStore.logout()
                        onLogout()
                    }
                }
            )
        }
    }
}

@Composable
fun StatCardSimple(title: String, count: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(count, color = BlueNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// ... the rest of the functions (HeaderSection, ProfileSettingsContent, etc) remain exactly the same.
@Composable
fun ProfileSettingsContent(user: User?, business: Business?, onLogout: () -> Unit) {
    val isDemoMode by MockDataStore.isDemoMode.collectAsState()
    val isOnline by MockDataStore.isOnline.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.str_profile__settings), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
        Spacer(modifier = Modifier.height(24.dp))
        
        // User Info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(BlueLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = BlueNavy)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user?.name ?: stringResource(R.string.str_user), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user?.email ?: "", color = TextSecondary, fontSize = 14.sp)
                Text(user?.phone ?: "", color = TextSecondary, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = CardBorder)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Business Info
        Text(stringResource(R.string.str_business_details), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(business?.name ?: stringResource(R.string.str_no_business_setup), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(business?.category ?: "", color = TextSecondary, fontSize = 14.sp)
        Text(business?.address ?: "", color = TextSecondary, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(stringResource(R.string.str_app_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        
        val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val isHindi = currentLocale == "hi"
        
        VyaparButton(
            text = if (isHindi) stringResource(R.string.str_switch_english) else stringResource(R.string.str_switch_hindi),
            onClick = {
                val newLocale = if (isHindi) "en" else "hi"
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(newLocale)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        VyaparButton(
            text = if (isOnline) stringResource(R.string.str_simulate_offline) else stringResource(R.string.str_simulate_online),
            onClick = { MockDataStore.toggleNetwork() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        VyaparButton(
            text = if (isDemoMode) stringResource(R.string.str_exit_demo) else stringResource(R.string.str_enter_demo),
            onClick = { if (isDemoMode) MockDataStore.exitDemoMode() else MockDataStore.enterDemoMode() },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (isDemoMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { MockDataStore.resetDemoData() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.str_reset_demo_data), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(stringResource(R.string.str_about_vyaparsetu), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.str_smart_business_management_for_), color = TextPrimary, fontSize = 14.sp)
        Text(stringResource(R.string.str_version_100_build_42), color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.str_privacy_policy__terms_of_servi), color = BlueAccent, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.str_logout), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HeaderSection(user: User?, business: Business?, onProfileClick: () -> Unit) {
    var greetingId by remember { mutableIntStateOf(getGreetingMessageId()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            greetingId = getGreetingMessageId()
            delay(60000L) // Check every minute
        }
    }
    val greeting = stringResource(id = greetingId)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = business?.name?.uppercase() ?: "VYAPARSETU",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            val firstName = user?.name?.substringBefore(" ") ?: stringResource(R.string.str_user)
            Text(
                text = "$greeting, $firstName 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(BlueLight, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .shadow(1.dp, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            val initials = user?.name?.split(" ")?.joinToString("") { it.take(1) }?.take(2)?.uppercase() ?: "US"
            Text(
                text = initials,
                color = BlueAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

fun getGreetingMessageId(): Int {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (currentHour) {
        in 5..11 -> R.string.str_good_morning
        in 12..16 -> R.string.str_good_afternoon
        in 17..20 -> R.string.str_good_evening
        else -> R.string.str_good_night
    }
}

@Composable
fun SalesCard(todaysSales: Double, todaysExpenses: Double, todaysProfit: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlueNavy, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.str_todays_sales),
            color = BlueLight.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.str_2f).format(todaysSales),
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.str_expenses), color = BlueLight.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(stringResource(R.string.str_2f).format(todaysExpenses), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.str_profit), color = BlueLight.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(stringResource(R.string.str_2f).format(todaysProfit), color = if (todaysProfit >= 0) AccentGreenLight else AccentRed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        if (todaysSales == 0.0 && todaysExpenses == 0.0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.str_no_transactions_recorded_yet_t),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StatsGrid(inventoryValue: Double, totalOutstanding: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.str_stock_value), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.str_2f).format(inventoryValue), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.weight(1f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.str_customer_outstanding), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.str_2f).format(totalOutstanding), color = AccentRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyStateCard(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = BlueNavy,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomNavigationBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val items = listOf(
        Triple("Home", "🏠", R.string.str_nav_home),
        Triple("Sales", "🏷️", R.string.str_nav_sales),
        Triple("Inventory", "📦", R.string.str_nav_inventory),
        Triple("Khata", "👥", R.string.str_nav_khata),
        Triple("Expenses", "💸", R.string.str_nav_expenses),
        Triple("Reports", "📊", R.string.str_nav_reports)
    )
    
    Surface(
        color = Color.White,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        modifier = Modifier.border(width = 1.dp, color = CardBorder).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, stringRes) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(label) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .weight(1f, fill = false) // Allows it to take needed space but not force equal weight
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (currentTab == label) BlueLight else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = stringRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentTab == label) BlueNavy else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AiAdvisorCard(onAskBusiness: () -> Unit, onNavigateToSubRoute: (String) -> Unit) {
    val summary = com.example.data.repository.MockDataStore.getBusinessSummary()
    var aiInsights by remember { mutableStateOf<List<com.example.domain.model.BusinessInsight>?>(null) }
    var isGeneratingInsights by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val defaultErrorMsg = stringResource(R.string.str_not_enough_data)
    
    val bgGradient = Brush.verticalGradient(
        colors = listOf(BlueLight.copy(alpha = 0.3f), Color.White)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgGradient, RoundedCornerShape(24.dp))
            .border(1.dp, BlueAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧠", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.str_ai_business_advisor), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                Text(stringResource(R.string.str_understand_your_business_and_g), fontSize = 12.sp, color = TextSecondary)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (summary != null && summary.numberOfProducts == 0 && summary.currentPeriod.salesTransactionCount == 0) {
            Text(stringResource(R.string.str_start_adding_sales_purchases), fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
        } else if (summary != null) {
            val healthColor = when(summary.healthScore) {
                in 80..100 -> AccentGreen
                in 60..79 -> BlueAccent
                in 40..59 -> AccentOrange
                else -> AccentRed
            }
            
            // Health Score
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(stringResource(R.string.str_business_health), fontSize = 12.sp, color = TextSecondary)
                    Text("${summary.healthScore}/100", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                }
                Text(summary.healthStatus, fontWeight = FontWeight.Bold, color = healthColor, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            summary.healthReasons.forEach { reason ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("•", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                    Text(reason, color = TextSecondary, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = CardBorder)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(R.string.str_nav_sales), summary.currentPeriod.totalSales, summary.previousPeriod?.totalSales, modifier = Modifier.weight(1f))
                MetricItem(stringResource(R.string.str_profit), summary.currentPeriod.profit, summary.previousPeriod?.profit, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(R.string.str_expenses), summary.currentPeriod.totalExpenses, summary.previousPeriod?.totalExpenses, modifier = Modifier.weight(1f), reverseColor = true)
                MetricItem(stringResource(R.string.str_outstanding), summary.customerOutstanding, null, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = CardBorder)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // AI Insights & Recommendations
        if (isGeneratingInsights) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueAccent, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.str_analyzing_your_business_data), fontSize = 12.sp, color = TextSecondary)
            }
        } else if (aiInsights != null) {
            Text(stringResource(R.string.str_insights_and_recommendations), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueNavy)
            Spacer(modifier = Modifier.height(8.dp))
            aiInsights?.forEach { insight ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp, top = 2.dp))
                        Text(insight.observation ?: "", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    if (!insight.reason.isNullOrBlank()) {
                        Text(insight.reason ?: "", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                    if (!insight.recommendation.isNullOrBlank()) {
                        Text(insight.recommendation ?: "", fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    isGeneratingInsights = true
                    coroutineScope.launch {
                        val result = com.example.data.repository.AiService.generateBusinessInsights()
                        isGeneratingInsights = false
                        aiInsights = result.getOrNull() ?: listOf(com.example.domain.model.BusinessInsight(defaultErrorMsg, "", ""))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                border = BorderStroke(1.dp, BlueNavy)
            ) {
                Text(stringResource(R.string.str_analyze_my_business), fontSize = 14.sp)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onNavigateToSubRoute("business_forecast") },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.str_business_forecast), fontSize = 12.sp)
                }
                
                VyaparButton(
                    text = stringResource(R.string.str_ask_my_business),
                    onClick = onAskBusiness,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: Double, prevValue: Double?, modifier: Modifier = Modifier, reverseColor: Boolean = false) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text("₹${String.format("%,.2f", value)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (prevValue != null) {
            if (prevValue == 0.0) {
                Text(stringResource(R.string.str_insufficient_data), fontSize = 10.sp, color = TextSecondary)
            } else {
                val change = ((value - prevValue) / prevValue) * 100.0
                val formattedChange = String.format("%.1f%%", kotlin.math.abs(change))
                val isPositive = change >= 0
                val color = if (reverseColor) {
                    if (isPositive) AccentRed else AccentGreen
                } else {
                    if (isPositive) AccentGreen else AccentRed
                }
                val icon = if (isPositive) "↑" else "↓"
                Text("$icon $formattedChange", fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            }
        }
    }
}
