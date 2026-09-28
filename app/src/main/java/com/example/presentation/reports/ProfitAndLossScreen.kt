package com.example.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitAndLossScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExplainability: (() -> Unit)? = null
) {
    val allSales by MockDataStore.sales.collectAsState()
    val allExpenses by MockDataStore.expenses.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val mySales = allSales.filter { it.businessId == currentBusiness?.id }
    val myExpenses = allExpenses.filter { it.businessId == currentBusiness?.id }

    var selectedRange by remember { mutableStateOf("This Month") }
    
    val currentTime = System.currentTimeMillis()
    val oneDay = 24 * 60 * 60 * 1000L
    
    val startTime = when (selectedRange) {
        "Today" -> currentTime - oneDay
        "Yesterday" -> currentTime - (2 * oneDay)
        "This Week" -> currentTime - (7 * oneDay)
        "This Month" -> currentTime - (30 * oneDay)
        "Last Month" -> currentTime - (60 * oneDay)
        "This Year" -> currentTime - (365 * oneDay)
        else -> 0L // All Time
    }
    
    val endTime = when (selectedRange) {
        "Yesterday" -> currentTime - oneDay
        "Last Month" -> currentTime - (30 * oneDay)
        else -> currentTime
    }

    val filteredSales = mySales.filter { it.createdAt in startTime..endTime }
    val filteredExpenses = myExpenses.filter { it.createdAt in startTime..endTime }
    
    val totalRevenue = filteredSales.sumOf { it.grandTotal }
    val totalCogs = filteredSales.sumOf { sale ->
        sale.items.sumOf { it.quantity * it.purchasePriceAtSale }
    }
    val grossProfit = totalRevenue - totalCogs
    
    val totalOperatingExpenses = filteredExpenses.sumOf { it.amount }
    
    val netProfit = grossProfit - totalOperatingExpenses
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_profit__loss), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ranges = listOf("Today", "Yesterday", "This Week", "This Month", "This Year", "All Time")
                    items(ranges) { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range) }
                        )
                    }
                }
            }
            
            if (filteredSales.isEmpty() && filteredExpenses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.str_not_enough_data_to_calculate_p), color = TextSecondary)
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(stringResource(R.string.str_revenue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ReportRow("Sales Revenue", totalRevenue)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.str_product_cost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ReportRow("Cost of items sold", totalCogs)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ReportRow("Gross Profit", grossProfit, isBold = true)
                    }
                }
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(stringResource(R.string.str_operating_expenses), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val categoryGroups = filteredExpenses.groupBy { it.category }
                        if (categoryGroups.isEmpty()) {
                            Text(stringResource(R.string.str_no_expenses_recorded), fontSize = 14.sp, color = TextSecondary)
                        } else {
                            categoryGroups.forEach { (cat, exps) ->
                                val sum = exps.sumOf { it.amount }
                                ReportRow(cat, sum)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ReportRow("Total Expenses", totalOperatingExpenses, isBold = true)
                    }
                }
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (netProfit >= 0) AccentGreen.copy(alpha = 0.1f) else AccentRed.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .border(1.dp, if (netProfit >= 0) AccentGreen else AccentRed, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Text(stringResource(R.string.str_net_profit), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (netProfit >= 0) AccentGreen else AccentRed)
                        Text(
                            text = stringResource(R.string.str_2f).format(netProfit), 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if (netProfit >= 0) AccentGreen else AccentRed
                        )
                    }
                }
                
                if (onNavigateToExplainability != null) {
                    item {
                        ProfitExplainabilityEntryCard(
                            onNavigateToExplainability = onNavigateToExplainability
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: Double, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = if (isBold) BlueNavy else TextSecondary)
        Text(stringResource(R.string.str_2f).format(value), fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = if (isBold) BlueNavy else TextPrimary)
    }
}
