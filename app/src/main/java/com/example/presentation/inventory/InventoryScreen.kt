package com.example.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.intelligence.InventoryIntelligenceEngine
import com.example.domain.intelligence.InventoryProductMetrics
import com.example.domain.intelligence.MarginCategory
import com.example.domain.intelligence.ProductProfitabilityEngine
import com.example.domain.intelligence.ProductProfitabilityMetrics
import com.example.domain.intelligence.VelocityCategory
import com.example.domain.model.Product
import com.example.presentation.dashboard.EmptyStateCard
import com.example.presentation.purchases.PurchasesListScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToProductForm: (String) -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToPurchaseDetail: (String) -> Unit,
    onNavigateToNewPurchase: () -> Unit
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

    // Deterministic intelligence analysis computed only when products or sales change
    val intelligenceSummary = remember(businessProducts, businessSales) {
        InventoryIntelligenceEngine.analyze(
            products = businessProducts,
            sales = businessSales
        )
    }

    // Deterministic product profitability analysis computed only when products or sales change
    val profitabilitySummary = remember(businessProducts, businessSales) {
        ProductProfitabilityEngine.analyze(
            products = businessProducts,
            sales = businessSales
        )
    }

    // Map metrics for fast O(1) lookup
    val metricsMap = remember(intelligenceSummary) {
        intelligenceSummary.products.associateBy { it.productId }
    }

    val profitabilityMap = remember(profitabilitySummary) {
        profitabilitySummary.products.associateBy { it.productId }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    var currentTab by remember { mutableStateOf("Products") }
    
    val filteredProducts = businessProducts.filter { product ->
        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) || 
                            product.sku.contains(searchQuery, ignoreCase = true)
        val isOutOfStock = product.currentStock <= 0
        val isLowStock = !isOutOfStock && product.currentStock <= product.minStock
        val productMetrics = metricsMap[product.id]
        val productProfitability = profitabilityMap[product.id]
        
        val matchesFilter = when (selectedFilter) {
            "Low Stock" -> isLowStock || isOutOfStock
            "Out of Stock" -> isOutOfStock
            "Fast Moving" -> productMetrics?.velocityCategory == VelocityCategory.FAST_MOVING
            "Slow Moving" -> productMetrics?.velocityCategory == VelocityCategory.SLOW_MOVING
            "Dead Stock" -> productMetrics?.isDeadStock == true
            "High Margin" -> productProfitability?.marginCategory == MarginCategory.HIGH_MARGIN
            "Med Margin" -> productProfitability?.marginCategory == MarginCategory.MEDIUM_MARGIN
            "Low Margin" -> productProfitability?.marginCategory == MarginCategory.LOW_MARGIN
            "No Profit" -> productProfitability?.marginCategory == MarginCategory.NO_PROFIT
            else -> true
        }
        
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.str_inventory), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
        }
        
        // Tabs
        TabRow(
            selectedTabIndex = if (currentTab == "Products") 0 else 1,
            containerColor = Color.Transparent,
            contentColor = BlueNavy,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (currentTab == "Products") 0 else 1]),
                    color = BlueNavy
                )
            }
        ) {
            Tab(
                selected = currentTab == "Products",
                onClick = { currentTab = "Products" },
                text = { Text(stringResource(R.string.str_products), fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = currentTab == "Purchases",
                onClick = { currentTab = "Purchases" },
                text = { Text(stringResource(R.string.str_purchases), fontWeight = FontWeight.SemiBold) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (currentTab == "Products") {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text(stringResource(R.string.str_search_products_by_name_or_sku)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = BlueAccent,
                    unfocusedBorderColor = CardBorder
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            // Compact Inventory Intelligence Summary Card
            if (businessProducts.isNotEmpty()) {
                InventoryIntelligenceSummaryCard(
                    summary = intelligenceSummary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                ProductProfitabilitySummaryCard(
                    summary = profitabilitySummary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "All" to stringResource(R.string.str_filter_all),
                    "High Margin" to stringResource(R.string.str_filter_high_margin),
                    "Med Margin" to stringResource(R.string.str_filter_medium_margin),
                    "Low Margin" to stringResource(R.string.str_filter_low_margin),
                    "No Profit" to stringResource(R.string.str_filter_no_profit),
                    "Fast Moving" to stringResource(R.string.str_filter_fast_moving),
                    "Slow Moving" to stringResource(R.string.str_filter_slow_moving),
                    "Dead Stock" to stringResource(R.string.str_filter_dead_stock),
                    "Low Stock" to "Low Stock",
                    "Out of Stock" to "Out of Stock"
                ).forEach { (filterKey, filterLabel) ->
                    FilterChip(
                        selected = selectedFilter == filterKey,
                        onClick = { selectedFilter = filterKey },
                        label = { Text(filterLabel) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueLight)
                    )
                }
            }
            
            if (businessProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp)) {
                    EmptyStateCard(
                        title = "No products yet",
                        message = "Add your first product to start tracking inventory."
                    )
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.str_no_matching_products_found), color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            metrics = metricsMap[product.id],
                            profitability = profitabilityMap[product.id],
                            onClick = { onNavigateToProductDetail(product.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        } else {
            PurchasesListScreen(
                onNavigateToPurchaseDetail = onNavigateToPurchaseDetail,
                onNavigateToNewPurchase = onNavigateToNewPurchase
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    metrics: InventoryProductMetrics? = null,
    profitability: ProductProfitabilityMetrics? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(product.category, fontSize = 12.sp, color = TextSecondary)
                    if (profitability != null) {
                        ProfitabilityMarginBadge(category = profitability.marginCategory)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${product.sellingPrice}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BlueNavy)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val isOutOfStock = product.currentStock <= 0
                val isLowStock = !isOutOfStock && product.currentStock <= product.minStock
                
                val statusColor = when {
                    isOutOfStock -> AccentRed
                    isLowStock -> Color(0xFFEA580C) // Orange
                    else -> AccentGreen
                }
                
                val statusBgColor = when {
                    isOutOfStock -> Color(0xFFFEE2E2) // Light Red
                    isLowStock -> Color(0xFFFFEDD5) // Light Orange
                    else -> Color(0xFFD1FAE5) // Light Green
                }
                
                val statusText = when {
                    isOutOfStock -> "OUT OF STOCK"
                    isLowStock -> "LOW STOCK"
                    else -> "IN STOCK"
                }
                
                Box(
                    modifier = Modifier
                        .background(statusBgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.str_in_stock, product.currentStock), fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }

        // Integrated compact product intelligence card
        if (metrics != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ProductIntelligenceDetails(metrics = metrics)
        }
    }
}
