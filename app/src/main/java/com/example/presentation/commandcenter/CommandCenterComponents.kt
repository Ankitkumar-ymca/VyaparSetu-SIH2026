package com.example.presentation.commandcenter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.intelligence.*
import com.example.ui.theme.*
import java.util.Locale

// Unified Command Center visual color palette
val PriorityCriticalColor = Color(0xFFDC2626)
val PriorityCriticalBg = Color(0xFFFEE2E2)
val PriorityHighColor = Color(0xFFD97706)
val PriorityHighBg = Color(0xFFFEF3C7)
val PriorityMediumColor = Color(0xFF2563EB)
val PriorityMediumBg = Color(0xFFDBEAFE)
val PriorityLowColor = Color(0xFF4B5563)
val PriorityLowBg = Color(0xFFF3F4F6)
val PriorityPositiveColor = Color(0xFF16A34A)
val PriorityPositiveBg = Color(0xFFDCFCE7)

val SourceAlertColor = Color(0xFFDC2626)
val SourceActionColor = Color(0xFF2563EB)
val SourceInsightColor = Color(0xFF7C3AED)
val SourceProfitColor = Color(0xFF059669)

/**
 * Dashboard entry card for Unified Business Command Center.
 */
@Composable
fun CommandCenterDashboardCard(
    attentionCount: Int,
    actionCount: Int,
    insightCount: Int,
    opportunityCount: Int,
    onOpenCommandCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenCommandCenter() }
            .testTag("dashboard_command_center_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = BlueNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.str_dashboard_command_center_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BlueNavy
                        )
                        Text(
                            text = stringResource(R.string.str_dashboard_command_center_subtitle),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = BlueNavy,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Metric pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommandPill(
                    count = attentionCount,
                    label = stringResource(R.string.str_needs_attention),
                    textColor = PriorityCriticalColor,
                    bgColor = PriorityCriticalBg,
                    modifier = Modifier.weight(1f)
                )
                CommandPill(
                    count = actionCount,
                    label = stringResource(R.string.str_actions),
                    textColor = PriorityMediumColor,
                    bgColor = PriorityMediumBg,
                    modifier = Modifier.weight(1f)
                )
                CommandPill(
                    count = insightCount,
                    label = stringResource(R.string.str_business_insights),
                    textColor = SourceInsightColor,
                    bgColor = Color(0xFFF3E8FF),
                    modifier = Modifier.weight(1f)
                )
                CommandPill(
                    count = opportunityCount,
                    label = stringResource(R.string.str_opportunities),
                    textColor = PriorityPositiveColor,
                    bgColor = PriorityPositiveBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CommandPill(
    count: Int,
    label: String,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Header summary showing high-level operational counts and profit snapshot if available.
 */
@Composable
fun CommandCenterSummaryHeader(
    summary: CommandCenterSummary,
    profitSnapshot: CommandCenterProfitSnapshot?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profit Snapshot banner (if available)
            if (profitSnapshot != null) {
                ProfitSnapshotBanner(profitSnapshot)
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
            }

            // Summary counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_command_operational_signals, summary.totalItems),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BlueNavy
                )
                if (summary.workingCapitalLocked > 0.0) {
                    Text(
                        text = stringResource(R.string.str_command_locked_capital, String.format(Locale.US, "%.0f", summary.workingCapitalLocked)),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = PriorityHighColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricItem(
                    count = summary.attentionCount,
                    label = stringResource(R.string.str_needs_attention),
                    color = PriorityCriticalColor,
                    bgColor = PriorityCriticalBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    count = summary.actionCount,
                    label = stringResource(R.string.str_actions),
                    color = PriorityMediumColor,
                    bgColor = PriorityMediumBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    count = summary.insightCount,
                    label = stringResource(R.string.str_business_insights),
                    color = SourceInsightColor,
                    bgColor = Color(0xFFF3E8FF),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    count = summary.opportunityCount,
                    label = stringResource(R.string.str_opportunities),
                    color = PriorityPositiveColor,
                    bgColor = PriorityPositiveBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfitSnapshotBanner(snapshot: CommandCenterProfitSnapshot) {
    val netProfitColor = if (snapshot.isProfitable) PriorityPositiveColor else if (snapshot.isOperatingAtLoss) PriorityCriticalColor else TextSecondary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = SourceProfitColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.str_command_pnl_intelligence, snapshot.periodLabel),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BlueNavy
                )
            }
            if (snapshot.hasNegativeGrossMargin) {
                Text(
                    text = stringResource(R.string.str_command_margin_risk),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriorityCriticalColor
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.str_revenue), fontSize = 10.sp, color = TextSecondary)
                Text("₹${String.format(Locale.US, "%.0f", snapshot.revenue)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BlueNavy)
            }
            Column {
                Text(stringResource(R.string.str_gross_margin), fontSize = 10.sp, color = TextSecondary)
                Text("${String.format(Locale.US, "%.1f", snapshot.grossMarginPercent)}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BlueNavy)
            }
            Column {
                Text(stringResource(R.string.str_net_profit), fontSize = 10.sp, color = TextSecondary)
                Text(
                    "₹${String.format(Locale.US, "%.0f", snapshot.netProfit)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = netProfitColor
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    count: Int,
    label: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

/**
 * Filter chip selection bar for Command Center tabs.
 */
@Composable
fun CommandCenterFilterBar(
    selectedSection: CommandCenterSection?,
    onSelectSection: (CommandCenterSection?) -> Unit,
    attentionCount: Int,
    actionCount: Int,
    insightCount: Int,
    opportunityCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedSection == null,
            onClick = { onSelectSection(null) },
            label = { Text(stringResource(R.string.str_all_items), fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_all")
        )
        FilterChip(
            selected = selectedSection == CommandCenterSection.NEEDS_ATTENTION,
            onClick = { onSelectSection(CommandCenterSection.NEEDS_ATTENTION) },
            label = { Text(stringResource(R.string.str_command_filter_attention, attentionCount), fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_attention")
        )
        FilterChip(
            selected = selectedSection == CommandCenterSection.ACTIONS,
            onClick = { onSelectSection(CommandCenterSection.ACTIONS) },
            label = { Text(stringResource(R.string.str_command_filter_actions, actionCount), fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_actions")
        )
        FilterChip(
            selected = selectedSection == CommandCenterSection.BUSINESS_INSIGHTS,
            onClick = { onSelectSection(CommandCenterSection.BUSINESS_INSIGHTS) },
            label = { Text(stringResource(R.string.str_command_filter_insights, insightCount), fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_insights")
        )
        FilterChip(
            selected = selectedSection == CommandCenterSection.OPPORTUNITIES,
            onClick = { onSelectSection(CommandCenterSection.OPPORTUNITIES) },
            label = { Text(stringResource(R.string.str_command_filter_opportunities, opportunityCount), fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_opportunities")
        )
    }
}

/**
 * Unified Card displaying an individual Command Center item.
 */
@Composable
fun CommandCenterItemCard(
    item: CommandCenterItem,
    onItemActionClick: (CommandCenterItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val (priorityColor, priorityBg) = when (item.priority) {
        CommandCenterPriority.CRITICAL -> Pair(PriorityCriticalColor, PriorityCriticalBg)
        CommandCenterPriority.HIGH -> Pair(PriorityHighColor, PriorityHighBg)
        CommandCenterPriority.MEDIUM -> Pair(PriorityMediumColor, PriorityMediumBg)
        CommandCenterPriority.LOW -> Pair(PriorityLowColor, PriorityLowBg)
        CommandCenterPriority.POSITIVE -> Pair(PriorityPositiveColor, PriorityPositiveBg)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("command_item_${item.itemId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badges row: Priority, Target Type / Name, and Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.priority.name,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Contributing sources badge if multi-source
                    if (item.contributingSources.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEDE9FE))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.str_command_consolidated),
                                color = SourceInsightColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Target identifier if available
                if (!item.targetName.isNullOrBlank()) {
                    Text(
                        text = item.targetName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Title
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BlueNavy
            )

            // Short Description
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            // Primary Metric Pill (if available)
            if (item.metricValue != null || !item.supportingMetric.isNullOrBlank()) {
                val metricText = buildString {
                    if (!item.supportingMetric.isNullOrBlank()) append("${item.supportingMetric}: ")
                    if (item.metricValue != null) append(String.format(Locale.US, "%.1f", item.metricValue))
                    if (!item.metricUnit.isNullOrBlank()) append(" ${item.metricUnit}")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = metricText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BlueNavy
                    )
                }
            }

            // Expandable details (signals)
            if (item.supportingSignals.isNotEmpty()) {
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.str_command_connected_signals),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueNavy
                        )
                        item.supportingSignals.forEach { sig ->
                            Text(
                                text = "• $sig",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Footer action & expand toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.supportingSignals.isNotEmpty()) {
                    Text(
                        text = if (expanded) stringResource(R.string.str_command_less_details) else stringResource(R.string.str_command_signals_count, item.supportingSignals.size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = BlueAccent,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (!item.actionLabel.isNullOrBlank() && !item.destinationRoute.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = { onItemActionClick(item) },
                        modifier = Modifier.testTag("btn_action_${item.itemId}"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
