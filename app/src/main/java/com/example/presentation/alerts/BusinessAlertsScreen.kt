package com.example.presentation.alerts

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
import com.example.domain.intelligence.AlertSeverity
import com.example.domain.intelligence.BusinessAlertEngine
import com.example.domain.intelligence.InventoryIntelligenceEngine
import com.example.domain.intelligence.ProductProfitabilityEngine
import com.example.presentation.dashboard.EmptyStateCard
import com.example.ui.theme.*

/**
 * Detailed screen showing all current business alerts.
 * Supports filtering by severity (All, Critical, Warnings, Info)
 * and tapping on product-specific alerts to navigate to ProductDetailScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAlertsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit
) {
    val allProducts by MockDataStore.products.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()

    val businessProducts = remember(allProducts, currentBusiness?.id) {
        allProducts.filter { it.businessId == currentBusiness?.id }
    }
    val businessSales = remember(allSales, currentBusiness?.id) {
        allSales.filter { it.businessId == currentBusiness?.id }
    }

    // Memoized intelligence engines outputs
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

    // Deterministic BusinessAlertEngine output
    val allAlerts = remember(inventorySummary, profitabilitySummary) {
        BusinessAlertEngine.generateAlerts(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary.products,
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary.products
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredAlerts = remember(allAlerts, selectedFilter) {
        when (selectedFilter) {
            "Critical" -> allAlerts.filter { it.severity == AlertSeverity.CRITICAL }
            "Warnings" -> allAlerts.filter { it.severity == AlertSeverity.WARNING }
            "Info" -> allAlerts.filter { it.severity == AlertSeverity.INFO }
            else -> allAlerts
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.str_business_alerts),
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
                    "Critical" to stringResource(R.string.str_filter_critical),
                    "Warnings" to stringResource(R.string.str_filter_warnings),
                    "Info" to stringResource(R.string.str_filter_info)
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

            if (allAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = stringResource(R.string.str_no_business_alerts),
                        message = stringResource(R.string.str_no_business_alerts_desc)
                    )
                }
            } else if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.str_no_alerts_found_filter),
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
                    items(filteredAlerts, key = { it.alertId }) { alert ->
                        BusinessAlertCard(
                            alert = alert,
                            onClick = alert.productId?.let { pId ->
                                { onNavigateToProductDetail(pId) }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
