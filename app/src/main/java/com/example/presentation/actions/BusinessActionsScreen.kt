package com.example.presentation.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.MockDataStore
import com.example.domain.intelligence.BusinessAction
import com.example.domain.intelligence.BusinessActionCategory
import com.example.domain.intelligence.BusinessActionEngine
import com.example.domain.intelligence.BusinessActionPriority
import com.example.domain.intelligence.CollectionPriorityEngine
import com.example.domain.intelligence.CustomerIntelligenceEngine
import com.example.domain.intelligence.InventoryIntelligenceEngine
import com.example.domain.intelligence.ProductProfitabilityEngine
import com.example.presentation.dashboard.EmptyStateCard
import com.example.ui.theme.*

/**
 * Dedicated screen showing all business actions for the shopkeeper.
 * Supports filtering by priority severity and action category,
 * with deterministic navigation to the appropriate product, customer, or inventory screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessActionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAction: (BusinessAction) -> Unit
) {
    val allProducts by MockDataStore.products.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()

    val businessProducts = remember(allProducts, currentBusiness?.id) {
        allProducts.filter { it.businessId == currentBusiness?.id }
    }
    val businessSales = remember(allSales, currentBusiness?.id) {
        allSales.filter { it.businessId == currentBusiness?.id }
    }
    val businessCustomers = remember(allCustomers, currentBusiness?.id) {
        allCustomers.filter { it.businessId == currentBusiness?.id }
    }

    // Memoize engine calculations matching DashboardScreen
    val inventorySummary = remember(businessProducts, businessSales) {
        InventoryIntelligenceEngine.analyze(
            products = businessProducts,
            sales = businessSales
        )
    }

    val profitabilitySummary = remember(businessProducts, businessSales) {
        ProductProfitabilityEngine.analyze(
            products = businessProducts,
            sales = businessSales
        )
    }

    val customerSummary = remember(businessCustomers, businessSales) {
        CustomerIntelligenceEngine.analyze(
            customers = businessCustomers,
            sales = businessSales
        )
    }

    val collectionSummary = remember(customerSummary) {
        CollectionPriorityEngine.evaluateFromMetrics(customerSummary.customers)
    }

    val actionSummary = remember(inventorySummary, profitabilitySummary, customerSummary, collectionSummary) {
        BusinessActionEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            profitabilitySummary = profitabilitySummary,
            customerSummary = customerSummary,
            collectionSummary = collectionSummary,
            maxActions = 50
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredActions = remember(actionSummary.allActions, selectedFilter) {
        when (selectedFilter) {
            "Critical" -> actionSummary.allActions.filter { it.priority == BusinessActionPriority.CRITICAL }
            "High" -> actionSummary.allActions.filter { it.priority == BusinessActionPriority.HIGH }
            "Medium" -> actionSummary.allActions.filter { it.priority == BusinessActionPriority.MEDIUM }
            "Low" -> actionSummary.allActions.filter { it.priority == BusinessActionPriority.LOW }
            "Restock" -> actionSummary.allActions.filter { it.category == BusinessActionCategory.RESTOCK }
            "Collection" -> actionSummary.allActions.filter { it.category == BusinessActionCategory.COLLECT_PAYMENT }
            "Dead Stock" -> actionSummary.allActions.filter { it.category == BusinessActionCategory.CLEAR_STOCK }
            "Pricing" -> actionSummary.allActions.filter { it.category == BusinessActionCategory.REVIEW_PRICING }
            "Re-engage" -> actionSummary.allActions.filter { it.category == BusinessActionCategory.REENGAGE_CUSTOMER }
            else -> actionSummary.allActions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.str_business_actions_title),
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BlueNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BlueBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "All" to stringResource(R.string.str_filter_all),
                    "Critical" to stringResource(R.string.str_filter_critical_actions),
                    "High" to stringResource(R.string.str_filter_high_actions),
                    "Medium" to stringResource(R.string.str_filter_medium_actions),
                    "Low" to stringResource(R.string.str_filter_low_actions),
                    "Restock" to stringResource(R.string.str_category_restock),
                    "Collection" to stringResource(R.string.str_category_collect_payment),
                    "Dead Stock" to stringResource(R.string.str_category_clear_stock),
                    "Pricing" to stringResource(R.string.str_category_review_pricing),
                    "Re-engage" to stringResource(R.string.str_category_reengage_customer)
                ).forEach { (filterKey, filterLabel) ->
                    FilterChip(
                        selected = selectedFilter == filterKey,
                        onClick = { selectedFilter = filterKey },
                        label = { Text(filterLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueLight,
                            selectedLabelColor = BlueNavy
                        )
                    )
                }
            }

            if (actionSummary.allActions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = stringResource(R.string.str_no_business_actions),
                        message = stringResource(R.string.str_no_business_actions_desc)
                    )
                }
            } else if (filteredActions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.str_no_actions_filter),
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredActions, key = { it.actionId }) { action ->
                        BusinessActionItemCard(
                            action = action,
                            onActionClick = { onNavigateToAction(action) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
