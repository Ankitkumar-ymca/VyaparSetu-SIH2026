package com.example.presentation.khata

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.domain.intelligence.CustomerActivityStatus
import com.example.domain.intelligence.CustomerIntelligenceEngine
import com.example.domain.intelligence.CustomerMetrics
import com.example.domain.intelligence.CustomerSegment
import com.example.domain.model.Customer
import com.example.domain.model.Supplier
import com.example.presentation.dashboard.EmptyStateCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToCustomerForm: (String) -> Unit,
    onNavigateToCustomerDetail: (String) -> Unit,
    onNavigateToSupplierForm: (String) -> Unit,
    onNavigateToSupplierDetail: (String) -> Unit
) {
    val tabs = listOf("Customers", "Suppliers")

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.str_khata), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BlueBackground,
            contentColor = BlueNavy,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = BlueAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        if (selectedTab == 0) {
            CustomersTab(
                onCustomerClick = onNavigateToCustomerDetail
            )
        } else {
            SuppliersTab(
                onSupplierClick = onNavigateToSupplierDetail
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersTab(onCustomerClick: (String) -> Unit) {
    val allCustomers by MockDataStore.customers.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val businessCustomers = remember(allCustomers, currentBusiness) {
        allCustomers.filter { it.businessId == currentBusiness?.id }
    }
    val businessSales = remember(allSales, currentBusiness) {
        allSales.filter { it.businessId == currentBusiness?.id }
    }

    // Deterministic Customer Intelligence analysis using CustomerIntelligenceEngine
    val intelligenceSummary = remember(businessCustomers, businessSales) {
        CustomerIntelligenceEngine.analyze(customers = businessCustomers, sales = businessSales)
    }

    val metricsMap = remember(intelligenceSummary) {
        intelligenceSummary.customers.associateBy { it.customerId }
    }

    // Deterministic Collection Priority analysis using CollectionPriorityEngine
    val collectionSummary = remember(intelligenceSummary) {
        com.example.domain.intelligence.CollectionPriorityEngine.evaluateFromMetrics(intelligenceSummary.customers)
    }

    val priorityMap = remember(collectionSummary) {
        collectionSummary.results.associateBy { it.customerId }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filtered = remember(businessCustomers, metricsMap, priorityMap, searchQuery, selectedFilter) {
        businessCustomers.filter { c ->
            val matchesSearch = c.name.contains(searchQuery, true) || c.phone.contains(searchQuery, true)
            val metric = metricsMap[c.id]
            val priorityResult = priorityMap[c.id]
            val matchesFilter = when (selectedFilter) {
                "Amount Due" -> c.outstandingAmount > 0
                "No Due" -> c.outstandingAmount <= 0
                "High Priority" -> priorityResult?.priority == com.example.domain.intelligence.CollectionPriorityLevel.HIGH
                "Medium Priority" -> priorityResult?.priority == com.example.domain.intelligence.CollectionPriorityLevel.MEDIUM
                "Low Priority" -> priorityResult?.priority == com.example.domain.intelligence.CollectionPriorityLevel.LOW
                "Active" -> metric?.activityStatus == CustomerActivityStatus.ACTIVE
                "Inactive" -> metric?.activityStatus == CustomerActivityStatus.INACTIVE
                "New" -> metric?.segment == CustomerSegment.NEW || metric?.activityStatus == CustomerActivityStatus.NEW
                "Frequent" -> metric?.segment == CustomerSegment.FREQUENT
                "High Value" -> metric?.segment == CustomerSegment.HIGH_VALUE
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text(stringResource(R.string.str_search_customers)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedBorderColor = BlueAccent, unfocusedBorderColor = CardBorder
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "All" to R.string.str_filter_all,
                "High Priority" to R.string.str_priority_high,
                "Medium Priority" to R.string.str_priority_medium,
                "Low Priority" to R.string.str_priority_low,
                "Active" to R.string.str_filter_active,
                "Frequent" to R.string.str_filter_frequent,
                "High Value" to R.string.str_filter_high_value,
                "New" to R.string.str_filter_new,
                "Inactive" to R.string.str_filter_inactive,
                "Amount Due" to R.string.str_filter_has_outstanding,
                "No Due" to R.string.str_no_due
            ).forEach { (filterKey, labelRes) ->
                FilterChip(
                    selected = selectedFilter == filterKey,
                    onClick = { selectedFilter = filterKey },
                    label = { Text(stringResource(labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueLight)
                )
            }
        }

        if (businessCustomers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp)) {
                EmptyStateCard("No customers yet", "Add customers to start managing Khata.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Intelligence Summary Card at the top
                item {
                    CustomerIntelligenceSummaryCard(summary = intelligenceSummary)
                }

                // Collection Priority Summary Card
                item {
                    CollectionPrioritySummaryCard(summary = collectionSummary)
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.str_no_matching_customers_found),
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(filtered) { c ->
                        CustomerCard(
                            customer = c,
                            metrics = metricsMap[c.id],
                            collectionPriority = priorityMap[c.id],
                            onClick = { onCustomerClick(c.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersTab(onSupplierClick: (String) -> Unit) {
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val businessSuppliers = allSuppliers.filter { it.businessId == currentBusiness?.id }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filtered = businessSuppliers.filter {
        val matchesSearch = it.name.contains(searchQuery, true) || it.phone.contains(searchQuery, true)
        val matchesFilter = when (selectedFilter) {
            "Amount Payable" -> it.outstandingPayable > 0
            "No Payable" -> it.outstandingPayable <= 0
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text(stringResource(R.string.str_search_suppliers)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedBorderColor = BlueAccent, unfocusedBorderColor = CardBorder
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Amount Payable", "No Payable").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueLight)
                )
            }
        }

        if (businessSuppliers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp)) {
                EmptyStateCard("No suppliers yet", "Add your suppliers to manage purchases.")
            }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.str_no_matching_suppliers_found), color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { s ->
                    SupplierCard(s, onClick = { onSupplierClick(s.id) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    metrics: CustomerMetrics? = null,
    collectionPriority: com.example.domain.intelligence.CollectionPriorityResult? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(BlueLight, CircleShape), contentAlignment = Alignment.Center) {
            Text(customer.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = BlueNavy)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(customer.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (collectionPriority != null && collectionPriority.priority != com.example.domain.intelligence.CollectionPriorityLevel.NONE) {
                    CollectionPriorityBadge(priority = collectionPriority.priority)
                } else if (metrics != null && metrics.segment != CustomerSegment.INSUFFICIENT_DATA && metrics.segment != CustomerSegment.REGULAR) {
                    CustomerSegmentBadge(segment = metrics.segment)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(customer.phone, fontSize = 12.sp, color = TextSecondary)
            if (metrics != null && metrics.lifetimeOrders > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.str_orders_count, metrics.lifetimeOrders),
                        fontSize = 11.sp,
                        color = BlueNavy,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹%.0f".format(metrics.lifetimePurchases),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (customer.outstandingAmount > 0) {
                Text("₹${customer.outstandingAmount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                Text(stringResource(R.string.str_amount_due), fontSize = 10.sp, color = AccentRed)
            } else {
                Text(stringResource(R.string.str_0), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                Text(stringResource(R.string.str_no_due), fontSize = 10.sp, color = AccentGreen)
            }
        }
    }
}

@Composable
fun SupplierCard(supplier: Supplier, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFFFFEDD5), CircleShape), contentAlignment = Alignment.Center) {
            Text(supplier.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(supplier.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(supplier.phone, fontSize = 12.sp, color = TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (supplier.outstandingPayable > 0) {
                Text("₹${supplier.outstandingPayable}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                Text(stringResource(R.string.str_to_pay), fontSize = 10.sp, color = AccentRed)
            } else {
                Text(stringResource(R.string.str_0), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
        }
    }
}
