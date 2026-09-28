package com.example.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import com.example.domain.intelligence.*
import com.example.ui.theme.*
import java.util.Calendar

/**
 * Profit Explainability Screen answering "Why did my profit change?".
 *
 * Consumes existing authoritative business data via ProfitExplainabilityEngine without modifying real records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitExplainabilityScreen(
    onNavigateBack: () -> Unit
) {
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val allExpenses by MockDataStore.expenses.collectAsState()
    val allProducts by MockDataStore.products.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val mySales = remember(allSales, businessId) { allSales.filter { it.businessId == businessId } }
    val myExpenses = remember(allExpenses, businessId) { allExpenses.filter { it.businessId == businessId } }
    val myProducts = remember(allProducts, businessId) { allProducts.filter { it.businessId == businessId } }

    val productCostMap = remember(myProducts) { myProducts.associate { it.id to it.purchasePrice } }
    val productNameMap = remember(myProducts) { myProducts.associate { it.id to it.name } }

    // 0 -> "This Month vs Last Month" (Calendar Month)
    // 1 -> "Last 30 Days vs Prior 30 Days" (Rolling 30 Days)
    var selectedComparisonMode by remember { mutableIntStateOf(0) }

    val explanation = remember(mySales, myExpenses, productCostMap, productNameMap, selectedComparisonMode) {
        if (mySales.isEmpty() && myExpenses.isEmpty()) {
            null
        } else {
            val now = System.currentTimeMillis()

            val (prevStart, prevEnd, prevLabel, currStart, currEnd, currLabel) = if (selectedComparisonMode == 0) {
                // Calendar Months
                val cal = Calendar.getInstance()
                cal.timeInMillis = now

                // Current month start
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val cStart = cal.timeInMillis
                val cEnd = now
                val cLabel = "This Month"

                // Previous month start and end
                cal.add(Calendar.MONTH, -1)
                val pStart = cal.timeInMillis
                val pEnd = cStart - 1
                val pLabel = "Last Month"

                ListOfPeriods(pStart, pEnd, pLabel, cStart, cEnd, cLabel)
            } else {
                // Rolling 30 Days
                val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
                val cStart = now - thirtyDaysMs
                val cEnd = now
                val cLabel = "Last 30 Days"

                val pStart = now - (2 * thirtyDaysMs)
                val pEnd = cStart - 1
                val pLabel = "Prior 30 Days"

                ListOfPeriods(pStart, pEnd, pLabel, cStart, cEnd, cLabel)
            }

            // Filter sales & expenses for both periods
            val prevSales = mySales.filter { it.createdAt in prevStart..prevEnd }
            val currSales = mySales.filter { it.createdAt in currStart..currEnd }
            val prevExpenses = myExpenses.filter { it.createdAt in prevStart..prevEnd }
            val currExpenses = myExpenses.filter { it.createdAt in currStart..currEnd }

            // Period Snapshot: Previous
            val prevRev = prevSales.sumOf { it.grandTotal }
            val prevCogs = prevSales.sumOf { sale ->
                sale.items.sumOf { it.quantity * (if (it.purchasePriceAtSale > 0.0) it.purchasePriceAtSale else (productCostMap[it.productId] ?: 0.0)) }
            }
            val prevExp = prevExpenses.sumOf { it.amount }
            val prevGross = prevRev - prevCogs
            val prevNet = prevGross - prevExp

            val prevSnapshot = ProfitPeriodSnapshot(
                periodLabel = prevLabel,
                revenue = prevRev,
                cogs = prevCogs,
                operatingExpenses = prevExp,
                grossProfit = prevGross,
                netProfit = prevNet
            )

            // Period Snapshot: Current
            val currRev = currSales.sumOf { it.grandTotal }
            val currCogs = currSales.sumOf { sale ->
                sale.items.sumOf { it.quantity * (if (it.purchasePriceAtSale > 0.0) it.purchasePriceAtSale else (productCostMap[it.productId] ?: 0.0)) }
            }
            val currExp = currExpenses.sumOf { it.amount }
            val currGross = currRev - currCogs
            val currNet = currGross - currExp

            val currSnapshot = ProfitPeriodSnapshot(
                periodLabel = currLabel,
                revenue = currRev,
                cogs = currCogs,
                operatingExpenses = currExp,
                grossProfit = currGross,
                netProfit = currNet
            )

            // Calculate Product Revenue Drivers
            val allProductIds = (prevSales.flatMap { it.items.map { i -> i.productId } } +
                    currSales.flatMap { it.items.map { i -> i.productId } }).distinct()

            val revDrivers = allProductIds.map { pid ->
                val pName = productNameMap[pid] ?: "Product $pid"
                val pRev = prevSales.sumOf { s -> s.items.filter { it.productId == pid }.sumOf { it.totalPrice } }
                val cRev = currSales.sumOf { s -> s.items.filter { it.productId == pid }.sumOf { it.totalPrice } }
                ProductRevenueDriver(pid, pName, pRev, cRev, cRev - pRev)
            }

            // Calculate Product Cost Drivers
            val costDrivers = allProductIds.map { pid ->
                val pName = productNameMap[pid] ?: "Product $pid"
                val pCogs = prevSales.sumOf { s ->
                    s.items.filter { it.productId == pid }.sumOf {
                        it.quantity * (if (it.purchasePriceAtSale > 0.0) it.purchasePriceAtSale else (productCostMap[pid] ?: 0.0))
                    }
                }
                val cCogs = currSales.sumOf { s ->
                    s.items.filter { it.productId == pid }.sumOf {
                        it.quantity * (if (it.purchasePriceAtSale > 0.0) it.purchasePriceAtSale else (productCostMap[pid] ?: 0.0))
                    }
                }
                ProductCostDriver(pid, pName, pCogs, cCogs, cCogs - pCogs)
            }

            // Calculate Expense Category Drivers
            val allCategories = (prevExpenses.map { it.category } + currExpenses.map { it.category }).distinct()
            val expDrivers = allCategories.map { cat ->
                val pAmt = prevExpenses.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
                val cAmt = currExpenses.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
                ExpenseCategoryDriver(cat, pAmt, cAmt, cAmt - pAmt)
            }

            // Call pure deterministic engine
            ProfitExplainabilityEngine.explain(
                previous = prevSnapshot,
                current = currSnapshot,
                revenueDrivers = revDrivers,
                costDrivers = costDrivers,
                expenseDrivers = expDrivers
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.str_explain_profit_change),
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
            // Period Comparison Mode Toggle Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_comparison_selector"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedComparisonMode == 0,
                        onClick = { selectedComparisonMode = 0 },
                        label = {
                            Text(
                                text = stringResource(R.string.str_period_this_vs_last_month),
                                fontSize = 12.sp,
                                fontWeight = if (selectedComparisonMode == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueNavy,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedComparisonMode == 1,
                        onClick = { selectedComparisonMode = 1 },
                        label = {
                            Text(
                                text = stringResource(R.string.str_period_last_30_days),
                                fontSize = 12.sp,
                                fontWeight = if (selectedComparisonMode == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueNavy,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (explanation == null) {
                // Empty / Low data state
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profit_explain_empty_state"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "📊", fontSize = 36.sp)
                            Text(
                                text = stringResource(R.string.str_no_financial_data_explain),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 1. Profit Change Summary Card
                item {
                    ProfitChangeSummaryCard(explanation = explanation)
                }

                // 2. Profit Change Bridge / Contribution Breakdown Card
                item {
                    ProfitContributionCard(explanation = explanation)
                }

                // 3. Top Business Drivers Section
                item {
                    ProfitDriversSection(attribution = explanation.driverAttribution)
                }

                // 4. Explanation & Reason Narrative Card
                item {
                    ProfitExplanationCard(explanation = explanation)
                }

                // 5. Notes & Limitations Card
                item {
                    ProfitLimitationsCard(notes = explanation.notes)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class ListOfPeriods(
    val prevStart: Long,
    val prevEnd: Long,
    val prevLabel: String,
    val currStart: Long,
    val currEnd: Long,
    val currLabel: String
)
