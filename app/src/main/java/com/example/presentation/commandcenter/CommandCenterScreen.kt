package com.example.presentation.commandcenter

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
import com.example.domain.intelligence.*
import com.example.presentation.dashboard.EmptyStateCard
import com.example.ui.theme.*

/**
 * Unified Business Command Center Screen.
 *
 * Provides a single, deterministic, prioritized executive view:
 * 1. NEEDS ATTENTION
 * 2. ACTIONS
 * 3. BUSINESS INSIGHTS
 * 4. OPPORTUNITIES
 *
 * Deterministic orchestration layer over existing intelligence engines.
 * Zero database writes, zero Gemini dependencies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit = {},
    onNavigateToCustomerDetail: (String) -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToActions: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToProfitExplainability: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {}
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

    // Deterministic Command Center generation from existing domain models
    val commandCenter = remember(businessProducts, businessSales, businessCustomers) {
        UnifiedBusinessCommandCenterEngine.generateFromRawModels(
            products = businessProducts,
            sales = businessSales,
            customers = businessCustomers
        )
    }

    var selectedSection by remember { mutableStateOf<CommandCenterSection?>(null) }

    fun handleItemAction(item: CommandCenterItem) {
        val route = item.destinationRoute
        if (route != null) {
            when {
                route.startsWith("product_detail/") -> {
                    val id = route.removePrefix("product_detail/")
                    onNavigateToProductDetail(id)
                }
                route.startsWith("customer_detail/") -> {
                    val id = route.removePrefix("customer_detail/")
                    onNavigateToCustomerDetail(id)
                }
                route == "business_alerts" -> onNavigateToAlerts()
                route == "business_actions" -> onNavigateToActions()
                route == "business_insights" -> onNavigateToInsights()
                route == "profit_explainability" -> onNavigateToProfitExplainability()
                else -> onNavigateToRoute(route)
            }
        }
    }

    Scaffold(
        modifier = Modifier.testTag("command_center_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.str_command_center_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BlueNavy
                        )
                        Text(
                            text = stringResource(R.string.str_command_center_subtitle),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_command_center_back")
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BlueBackground)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Summary Header
                item(key = "command_summary_header") {
                    CommandCenterSummaryHeader(
                        summary = commandCenter.summary,
                        profitSnapshot = commandCenter.profitSnapshot
                    )
                }

                // 2. Filter Bar
                item(key = "command_filter_bar") {
                    CommandCenterFilterBar(
                        selectedSection = selectedSection,
                        onSelectSection = { selectedSection = it },
                        attentionCount = commandCenter.attentionItems.size,
                        actionCount = commandCenter.actions.size,
                        insightCount = commandCenter.insights.size,
                        opportunityCount = commandCenter.opportunities.size
                    )
                }

                if (commandCenter.allItems.isEmpty()) {
                    item(key = "empty_command_center") {
                        EmptyStateCard(
                            title = stringResource(R.string.str_empty_command_center_title),
                            message = stringResource(R.string.str_empty_command_center_desc)
                        )
                    }
                } else {
                    // SECTION 1: NEEDS ATTENTION
                    if (selectedSection == null || selectedSection == CommandCenterSection.NEEDS_ATTENTION) {
                        if (commandCenter.attentionItems.isNotEmpty()) {
                            item(key = "header_needs_attention") {
                                SectionHeader(
                                    title = stringResource(R.string.str_needs_attention),
                                    count = commandCenter.attentionItems.size,
                                    color = PriorityCriticalColor
                                )
                            }
                            items(
                                items = commandCenter.attentionItems,
                                key = { "attention_${it.itemId}" }
                            ) { item ->
                                CommandCenterItemCard(
                                    item = item,
                                    onItemActionClick = { handleItemAction(it) }
                                )
                            }
                        }
                    }

                    // SECTION 2: ACTIONS
                    if (selectedSection == null || selectedSection == CommandCenterSection.ACTIONS) {
                        if (commandCenter.actions.isNotEmpty()) {
                            item(key = "header_actions") {
                                SectionHeader(
                                    title = stringResource(R.string.str_actions),
                                    count = commandCenter.actions.size,
                                    color = PriorityMediumColor
                                )
                            }
                            items(
                                items = commandCenter.actions,
                                key = { "action_${it.itemId}" }
                            ) { item ->
                                CommandCenterItemCard(
                                    item = item,
                                    onItemActionClick = { handleItemAction(it) }
                                )
                            }
                        }
                    }

                    // SECTION 3: BUSINESS INSIGHTS
                    if (selectedSection == null || selectedSection == CommandCenterSection.BUSINESS_INSIGHTS) {
                        if (commandCenter.insights.isNotEmpty()) {
                            item(key = "header_insights") {
                                SectionHeader(
                                    title = stringResource(R.string.str_business_insights),
                                    count = commandCenter.insights.size,
                                    color = SourceInsightColor
                                )
                            }
                            items(
                                items = commandCenter.insights,
                                key = { "insight_${it.itemId}" }
                            ) { item ->
                                CommandCenterItemCard(
                                    item = item,
                                    onItemActionClick = { handleItemAction(it) }
                                )
                            }
                        }
                    }

                    // SECTION 4: OPPORTUNITIES
                    if (selectedSection == null || selectedSection == CommandCenterSection.OPPORTUNITIES) {
                        if (commandCenter.opportunities.isNotEmpty()) {
                            item(key = "header_opportunities") {
                                SectionHeader(
                                    title = stringResource(R.string.str_opportunities),
                                    count = commandCenter.opportunities.size,
                                    color = PriorityPositiveColor
                                )
                            }
                            items(
                                items = commandCenter.opportunities,
                                key = { "opportunity_${it.itemId}" }
                            ) { item ->
                                CommandCenterItemCard(
                                    item = item,
                                    onItemActionClick = { handleItemAction(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, androidx.compose.foundation.shape.CircleShape)
            )
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = stringResource(R.string.str_command_items_count, count),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}
