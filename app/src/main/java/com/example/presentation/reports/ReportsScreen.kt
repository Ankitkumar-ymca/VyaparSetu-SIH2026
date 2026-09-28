package com.example.presentation.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToSubRoute: (String) -> Unit
) {
    val allSales by MockDataStore.sales.collectAsState()
    val allPurchases by MockDataStore.purchases.collectAsState()
    val allExpenses by MockDataStore.expenses.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    val allLedger by MockDataStore.ledgerEntries.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val bid = currentBusiness?.id ?: ""

    val mySales = allSales.filter { it.businessId == bid }
    val myPurchases = allPurchases.filter { it.businessId == bid }
    val myExpenses = allExpenses.filter { it.businessId == bid }
    val myCustomers = allCustomers.filter { it.businessId == bid }
    val mySuppliers = allSuppliers.filter { it.businessId == bid }
    val myLedger = allLedger.filter { it.businessId == bid }

    var selectedRange by remember { mutableStateOf("This Month") }
    
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    
    val startTime = when (selectedRange) {
        "Today" -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        "This Week" -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        "This Month" -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        "Last Month" -> {
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        "This Year" -> {
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        else -> 0L // All Time
    }
    
    val actualEndTime = if (selectedRange == "Last Month") {
        val endCal = Calendar.getInstance()
        endCal.set(Calendar.DAY_OF_MONTH, 1)
        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)
        endCal.timeInMillis - 1
    } else {
        endTime
    }

    val filteredSales = mySales.filter { it.createdAt in startTime..actualEndTime }
    val filteredPurchases = myPurchases.filter { it.createdAt in startTime..actualEndTime }
    val filteredExpenses = myExpenses.filter { it.createdAt in startTime..actualEndTime }
    val filteredLedger = myLedger.filter { it.timestamp in startTime..actualEndTime }

    // Metrics
    val totalSalesAmount = filteredSales.sumOf { it.grandTotal }
    val totalPurchasesAmount = filteredPurchases.sumOf { it.grandTotal }
    val totalExpensesAmount = filteredExpenses.sumOf { it.amount }
    
    val cogs = filteredSales.sumOf { sale ->
        sale.items.sumOf { it.quantity * it.purchasePriceAtSale }
    }
    val grossProfit = totalSalesAmount - cogs
    val netProfit = grossProfit - totalExpensesAmount
    
    val amountReceived = filteredLedger.filter { it.type == "PAYMENT" && myCustomers.any { c -> c.id == it.partyId } }.sumOf { it.amount }
    val amountPaidOut = filteredLedger.filter { it.type == "PAYMENT" && mySuppliers.any { s -> s.id == it.partyId } }.sumOf { it.amount }
    
    val amountPendingFromCustomers = myCustomers.sumOf { it.outstandingAmount }
    val amountPendingToSuppliers = mySuppliers.sumOf { it.outstandingPayable }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(stringResource(R.string.str_reports), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
            Text(stringResource(R.string.str_analytics__insights), fontSize = 14.sp, color = TextSecondary)
        }
        
        // Date Filter
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val ranges = listOf("Today", "This Week", "This Month", "Last Month", "This Year", "All Time")
            items(ranges) { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    label = { Text(range) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueLight,
                        selectedLabelColor = BlueNavy
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            
            // AI Summary Section
            item {
                var aiSummary by remember { mutableStateOf<String?>(null) }
                var isGenerating by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BlueLight.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, BlueAccent, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.str_ai_monthly_summary), fontWeight = FontWeight.Bold, color = BlueNavy)
                        if (aiSummary == null && !isGenerating) {
                            TextButton(onClick = {
                                isGenerating = true
                                coroutineScope.launch {
                                    val result = com.example.data.repository.AiService.generateMonthlySummary()
                                    isGenerating = false
                                    aiSummary = result.getOrNull() ?: "Unable to generate summary right now."
                                }
                            }) {
                                Text(stringResource(R.string.str_generate), color = BlueAccent)
                            }
                        }
                    }
                    
                    if (isGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.str_analyzing_your_financial_data), fontSize = 12.sp, color = TextSecondary)
                        }
                    } else if (aiSummary != null) {
                        Text(aiSummary!!, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Text(stringResource(R.string.str_get_a_smart_overview_of_your_b), fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
            
            // 1. Overview
            item {
                SectionHeader("Overview")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Total Revenue", totalSalesAmount, AccentGreen, modifier = Modifier.weight(1f))
                    MetricCard("Net Profit", netProfit, if (netProfit >= 0) AccentGreen else AccentRed, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Purchases", totalPurchasesAmount, Color(0xFFEA580C), modifier = Modifier.weight(1f))
                    MetricCard("Expenses", totalExpensesAmount, AccentRed, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Received", amountReceived, BlueAccent, modifier = Modifier.weight(1f))
                    MetricCard("To Receive", amountPendingFromCustomers, TextSecondary, modifier = Modifier.weight(1f))
                }
            }

            // Profit Explainability Entry Card
            item {
                ProfitExplainabilityEntryCard(
                    onNavigateToExplainability = { onNavigateToSubRoute("profit_explainability") }
                )
            }
            
            // 2. Sales Report
            item {
                SectionHeader("Sales Report")
                ReportCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.str_total_sales), fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%.2f", totalSalesAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.str_transactions), fontSize = 12.sp, color = TextSecondary)
                            Text("${filteredSales.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (filteredSales.isNotEmpty()) {
                        SimpleBarChart(
                            data = groupSalesByDate(filteredSales),
                            color = AccentGreen
                        )
                    } else {
                        EmptyChartState("No sales in this period")
                    }
                }
            }
            
            // 3. Purchase Report
            item {
                SectionHeader("Purchase Report")
                ReportCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.str_total_purchases), fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%.2f", totalPurchasesAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.str_transactions), fontSize = 12.sp, color = TextSecondary)
                            Text("${filteredPurchases.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (filteredPurchases.isNotEmpty()) {
                        SimpleBarChart(
                            data = groupPurchasesByDate(filteredPurchases),
                            color = Color(0xFFEA580C)
                        )
                    } else {
                        EmptyChartState("No purchases in this period")
                    }
                }
            }
            
            // 4. Expense Report
            item {
                SectionHeader("Expense Report")
                ReportCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.str_total_expenses), fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%.2f", totalExpensesAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (filteredExpenses.isNotEmpty()) {
                        ExpenseCategoryBreakdown(filteredExpenses)
                    } else {
                        EmptyChartState("No expenses in this period")
                    }
                }
            }
            
            // 5. Customer Report
            item {
                SectionHeader("Customer Report")
                ReportCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.str_total_customers), fontSize = 12.sp, color = TextSecondary)
                            Text("${myCustomers.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.str_outstanding), fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%.2f", amountPendingFromCustomers)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val customerSales = filteredSales.groupBy { it.customerId }
                        .mapNotNull { (id, sales) -> 
                            val c = myCustomers.find { it.id == id }
                            if (c != null) c.name to sales.sumOf { it.grandTotal } else null
                        }
                        .sortedByDescending { it.second }
                        .take(3)
                        
                    if (customerSales.isNotEmpty()) {
                        Text(stringResource(R.string.str_top_customers_sales), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BlueNavy)
                        Spacer(modifier = Modifier.height(8.dp))
                        customerSales.forEach { (name, amount) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, fontSize = 14.sp, color = TextPrimary)
                                Text("₹${String.format("%.2f", amount)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BlueNavy)
                            }
                        }
                    } else {
                        EmptyChartState("No customer transactions in this period")
                    }
                }
            }
            
            // 6. Supplier Report
            item {
                SectionHeader("Supplier Report")
                ReportCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.str_total_suppliers), fontSize = 12.sp, color = TextSecondary)
                            Text("${mySuppliers.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.str_to_pay), fontSize = 12.sp, color = TextSecondary)
                            Text("₹${String.format("%.2f", amountPendingToSuppliers)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val supplierPurchases = filteredPurchases.groupBy { it.supplierId }
                        .mapNotNull { (id, purchases) -> 
                            val s = mySuppliers.find { it.id == id }
                            if (s != null) s.name to purchases.sumOf { it.grandTotal } else null
                        }
                        .sortedByDescending { it.second }
                        .take(3)
                        
                    if (supplierPurchases.isNotEmpty()) {
                        Text(stringResource(R.string.str_top_suppliers_purchases), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BlueNavy)
                        Spacer(modifier = Modifier.height(8.dp))
                        supplierPurchases.forEach { (name, amount) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, fontSize = 14.sp, color = TextPrimary)
                                Text("₹${String.format("%.2f", amount)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BlueNavy)
                            }
                        }
                    } else {
                        EmptyChartState("No supplier transactions in this period")
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun MetricCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("₹${String.format("%.1f", amount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ReportCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        content = content
    )
}

@Composable
fun EmptyChartState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(BlueLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun SimpleBarChart(data: List<Pair<String, Double>>, color: Color) {
    if (data.isEmpty()) return
    
    val maxVal = max(data.maxOf { it.second }, 1.0)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.sortedBy { it.first }.takeLast(7).forEach { (label, value) ->
            val heightFraction = (value / maxVal).toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                        .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
fun ExpenseCategoryBreakdown(expenses: List<Expense>) {
    val categoryTotals = expenses.groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amount } }
        .sortedByDescending { it.second }
        
    val total = categoryTotals.sumOf { it.second }
    if (total == 0.0) return
    
    val colors = listOf(AccentRed, Color(0xFFEA580C), AccentGreen, BlueAccent, BlueNavy)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        categoryTotals.take(5).forEachIndexed { index, (cat, amount) ->
            val fraction = (amount / total).toFloat()
            val color = colors[index % colors.size]
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(cat, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("₹${String.format("%.1f", amount)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BlueNavy)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(CardBorder, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

fun groupSalesByDate(sales: List<Sale>): List<Pair<String, Double>> {
    val format = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sales.groupBy { 
        format.format(Date(it.createdAt))
    }.map { (date, list) -> date to list.sumOf { it.grandTotal } }
    .sortedBy { it.first } 
}

fun groupPurchasesByDate(purchases: List<Purchase>): List<Pair<String, Double>> {
    val format = SimpleDateFormat("dd MMM", Locale.getDefault())
    return purchases.groupBy { 
        format.format(Date(it.createdAt))
    }.map { (date, list) -> date to list.sumOf { it.grandTotal } }
    .sortedBy { it.first }
}
