package com.example.presentation.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.MockDataStore
import com.example.domain.intelligence.BusinessActionEngine
import com.example.domain.intelligence.BusinessAlertEngine
import com.example.domain.intelligence.CollectionPriorityEngine
import com.example.domain.intelligence.CrossBusinessInsightsEngine
import com.example.domain.intelligence.CrossInsightSeverity
import com.example.domain.intelligence.CustomerIntelligenceEngine
import com.example.domain.intelligence.InventoryIntelligenceEngine
import com.example.domain.intelligence.ProductProfitabilityEngine
import com.example.presentation.dashboard.EmptyStateCard
import com.example.ui.theme.*

/**
 * Dedicated Business Insights Center presenting the deterministic output of
 * CrossBusinessInsightsEngine to the business owner.
 *
 * Rules:
 * - Deterministic, offline, zero Gemini/network calls, zero database writes.
 * - Does NOT recalculate financial truth or P&L.
 * - Shows connected signals from existing domain intelligence engines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessInsightsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit = {},
    onNavigateToCustomerDetail: (String) -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToActions: () -> Unit = {}
) {
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allProducts by MockDataStore.products.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()

    val businessProducts = remember(allProducts, currentBusiness?.id) {
        allProducts.filter { it.businessId == currentBusiness?.id }
    }
    val businessSales = remember(allSales, currentBusiness?.id) {
        allSales.filter { it.businessId == currentBusiness?.id }
    }
    val businessCustomers = remember(allCustomers, currentBusiness?.id) {
        allCustomers.filter { it.businessId == currentBusiness?.id }
    }

    // Existing intelligence outputs (evaluated reactively from real data)
    val inventorySummary = remember(businessProducts, businessSales) {
        if (businessProducts.isNotEmpty()) {
            InventoryIntelligenceEngine.analyze(
                products = businessProducts,
                sales = businessSales
            )
        } else null
    }

    val profitabilitySummary = remember(businessProducts, businessSales) {
        if (businessProducts.isNotEmpty()) {
            ProductProfitabilityEngine.analyze(
                products = businessProducts,
                sales = businessSales
            )
        } else null
    }

    val customerSummary = remember(businessCustomers, businessSales) {
        if (businessCustomers.isNotEmpty()) {
            CustomerIntelligenceEngine.analyze(
                customers = businessCustomers,
                sales = businessSales
            )
        } else null
    }

    val collectionSummary = remember(customerSummary, businessCustomers, businessSales) {
        if (customerSummary != null) {
            CollectionPriorityEngine.evaluateFromMetrics(customerSummary.customers)
        } else if (businessCustomers.isNotEmpty()) {
            CollectionPriorityEngine.evaluate(
                customers = businessCustomers,
                sales = businessSales
            )
        } else null
    }

    val businessAlerts = remember(inventorySummary, profitabilitySummary) {
        if (inventorySummary != null || profitabilitySummary != null) {
            BusinessAlertEngine.generateAlerts(
                inventorySummary = inventorySummary,
                inventoryMetrics = inventorySummary?.products ?: emptyList(),
                profitabilitySummary = profitabilitySummary,
                profitabilityMetrics = profitabilitySummary?.products ?: emptyList()
            )
        } else emptyList()
    }

    val businessActions = remember(inventorySummary, profitabilitySummary, customerSummary, collectionSummary, businessAlerts) {
        BusinessActionEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            businessAlerts = businessAlerts
        ).allActions
    }

    // Deterministic CrossBusinessInsightsEngine orchestration
    val insightsSummary = remember(
        inventorySummary,
        profitabilitySummary,
        customerSummary,
        collectionSummary,
        businessAlerts,
        businessActions
    ) {
        CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            businessAlerts = businessAlerts,
            businessActions = businessActions
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var expandedInsightIds by remember { mutableStateOf(setOf<String>()) }

    val filteredInsights = remember(insightsSummary.insights, selectedFilter) {
        when (selectedFilter) {
            "Critical" -> insightsSummary.insights.filter { it.severity == CrossInsightSeverity.CRITICAL }
            "High" -> insightsSummary.insights.filter { it.severity == CrossInsightSeverity.HIGH }
            "Medium" -> insightsSummary.insights.filter { it.severity == CrossInsightSeverity.MEDIUM }
            "Positive" -> insightsSummary.insights.filter { it.severity == CrossInsightSeverity.POSITIVE }
            else -> insightsSummary.insights
        }
    }

    Scaffold(
        modifier = Modifier.testTag("business_insights_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.str_business_insights),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BlueNavy
                        )
                        Text(
                            text = stringResource(R.string.str_business_insights_subtitle),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_insights_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BlueNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (insightsSummary.totalInsights == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = stringResource(R.string.str_empty_insights_title),
                    message = stringResource(R.string.str_empty_insights_desc)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Summary Header
                item {
                    BusinessInsightsSummaryHeader(summary = insightsSummary)
                }

                // Filter row
                item {
                    BusinessInsightsFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        summary = insightsSummary
                    )
                }

                if (filteredInsights.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateCard(
                                title = stringResource(R.string.str_empty_insights_title),
                                message = stringResource(R.string.str_empty_insights_desc)
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredInsights,
                        key = { it.id }
                    ) { insight ->
                        val isExpanded = expandedInsightIds.contains(insight.id)
                        BusinessInsightCard(
                            insight = insight,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedInsightIds = if (isExpanded) {
                                    expandedInsightIds - insight.id
                                } else {
                                    expandedInsightIds + insight.id
                                }
                            },
                            onNavigateToProduct = onNavigateToProductDetail,
                            onNavigateToCustomer = onNavigateToCustomerDetail,
                            onNavigateToAlerts = onNavigateToAlerts,
                            onNavigateToActions = onNavigateToActions
                        )
                    }
                }
            }
        }
    }
}
