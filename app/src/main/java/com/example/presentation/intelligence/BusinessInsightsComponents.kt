package com.example.presentation.intelligence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.domain.intelligence.CrossBusinessInsight
import com.example.domain.intelligence.CrossBusinessInsightsSummary
import com.example.domain.intelligence.CrossInsightCategory
import com.example.domain.intelligence.CrossInsightEntityType
import com.example.domain.intelligence.CrossInsightSeverity
import com.example.ui.theme.*
import java.util.Locale

/**
 * Semantic color definitions for Cross-Business Insight Severities.
 */
object InsightColors {
    val CriticalBg = Color(0xFFFEE2E2)
    val CriticalText = AccentRed
    val CriticalBorder = Color(0xFFFCA5A5)

    val HighBg = Color(0xFFFFEDD5)
    val HighText = Color(0xFFEA580C)
    val HighBorder = Color(0xFFFDBA74)

    val MediumBg = Color(0xFFEFF6FF)
    val MediumText = BlueNavy
    val MediumBorder = Color(0xFFBFDBFE)

    val PositiveBg = Color(0xFFECFDF5)
    val PositiveText = AccentGreen
    val PositiveBorder = Color(0xFFA7F3D0)
}

/**
 * Compact severity badge for an insight.
 */
@Composable
fun InsightSeverityBadge(
    severity: CrossInsightSeverity,
    modifier: Modifier = Modifier
) {
    val (bg, text, label) = when (severity) {
        CrossInsightSeverity.CRITICAL -> Triple(InsightColors.CriticalBg, InsightColors.CriticalText, stringResource(R.string.str_critical_insights).uppercase(Locale.US))
        CrossInsightSeverity.HIGH -> Triple(InsightColors.HighBg, InsightColors.HighText, stringResource(R.string.str_high_insights).uppercase(Locale.US))
        CrossInsightSeverity.MEDIUM -> Triple(InsightColors.MediumBg, InsightColors.MediumText, stringResource(R.string.str_medium_insights).uppercase(Locale.US))
        CrossInsightSeverity.POSITIVE -> Triple(InsightColors.PositiveBg, InsightColors.PositiveText, stringResource(R.string.str_positive_insights).uppercase(Locale.US))
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(0.5.dp, bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = label,
            color = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Localized human-readable category badge for an insight.
 */
@Composable
fun InsightCategoryBadge(
    category: CrossInsightCategory,
    modifier: Modifier = Modifier
) {
    val label = when (category) {
        CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK -> stringResource(R.string.str_cat_high_margin_stockout)
        CrossInsightCategory.STAR_PERFORMER -> stringResource(R.string.str_cat_star_performer)
        CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP -> stringResource(R.string.str_cat_capital_trap)
        CrossInsightCategory.VOLUME_MARGIN_VULNERABILITY -> stringResource(R.string.str_cat_volume_vulnerability)
        CrossInsightCategory.KEY_ACCOUNT_EXPOSURE -> stringResource(R.string.str_cat_key_account_due)
        CrossInsightCategory.DORMANT_DEBT_RISK -> stringResource(R.string.str_cat_dormant_debt)
        CrossInsightCategory.WORKING_CAPITAL_STRAIN -> stringResource(R.string.str_cat_working_capital_squeeze)
        CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE -> stringResource(R.string.str_cat_multi_signal)
    }

    Box(
        modifier = modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Summary header card displaying counts of insights by severity and working capital locked.
 */
@Composable
fun BusinessInsightsSummaryHeader(
    summary: CrossBusinessInsightsSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insights_summary_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_total_insights),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Text(
                    text = "${summary.totalInsights}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Severity counts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricPill(
                    label = stringResource(R.string.str_critical_insights),
                    count = summary.criticalInsightsCount,
                    color = AccentRed,
                    bgColor = InsightColors.CriticalBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricPill(
                    label = stringResource(R.string.str_high_insights),
                    count = summary.highInsightsCount,
                    color = Color(0xFFEA580C),
                    bgColor = InsightColors.HighBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricPill(
                    label = stringResource(R.string.str_medium_insights),
                    count = summary.mediumInsightsCount,
                    color = BlueNavy,
                    bgColor = InsightColors.MediumBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricPill(
                    label = stringResource(R.string.str_positive_insights),
                    count = summary.positiveInsightsCount,
                    color = AccentGreen,
                    bgColor = InsightColors.PositiveBg,
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.workingCapitalLockedTotal > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.str_working_capital_locked),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.1f", summary.workingCapitalLockedTotal)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricPill(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/**
 * Filter row for filtering insights by severity.
 */
@Composable
fun BusinessInsightsFilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    summary: CrossBusinessInsightsSummary,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        "All" to stringResource(R.string.str_filter_all),
        "Critical" to stringResource(R.string.str_critical_insights),
        "High" to stringResource(R.string.str_high_insights),
        "Medium" to stringResource(R.string.str_medium_insights),
        "Positive" to stringResource(R.string.str_positive_insights)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, localizedLabel) ->
            val count = when (key) {
                "Critical" -> summary.criticalInsightsCount
                "High" -> summary.highInsightsCount
                "Medium" -> summary.mediumInsightsCount
                "Positive" -> summary.positiveInsightsCount
                else -> summary.totalInsights
            }
            val isSelected = selectedFilter == key

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(key) },
                label = {
                    Text(
                        text = "$localizedLabel ($count)",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("filter_chip_$key")
            )
        }
    }
}

/**
 * Card displaying an individual CrossBusinessInsight with expand/collapse detail view.
 */
@Composable
fun BusinessInsightCard(
    insight: CrossBusinessInsight,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToProduct: ((String) -> Unit)? = null,
    onNavigateToCustomer: ((String) -> Unit)? = null,
    onNavigateToAlerts: (() -> Unit)? = null,
    onNavigateToActions: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insight_card_${insight.id}")
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InsightSeverityBadge(severity = insight.severity)
                    InsightCategoryBadge(category = insight.category)
                }

                if (insight.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.str_multiple_signals_badge),
                            color = Color(0xFF6D28D9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = insight.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy
            )

            // Target information
            if (insight.entityName != null || insight.entityId != null) {
                val targetText = when (insight.entityType) {
                    CrossInsightEntityType.PRODUCT -> stringResource(R.string.str_target_product, insight.entityName ?: insight.entityId ?: "")
                    CrossInsightEntityType.CUSTOMER -> stringResource(R.string.str_target_customer, insight.entityName ?: insight.entityId ?: "")
                    CrossInsightEntityType.BUSINESS_WIDE -> stringResource(R.string.str_target_business_wide)
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (insight.entityType == CrossInsightEntityType.PRODUCT) Icons.Filled.ShoppingCart else Icons.Filled.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = targetText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Summary text
            Text(
                text = insight.summary,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            // Metric Value display if available
            if (insight.metricValue != null && insight.metricUnit != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val formattedMetric = when (insight.metricUnit) {
                    "₹" -> "₹${String.format(Locale.US, "%.1f", insight.metricValue)}"
                    "%" -> "${String.format(Locale.US, "%.1f%%", insight.metricValue)}"
                    else -> "${insight.metricValue.toInt()} ${insight.metricUnit}"
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formattedMetric,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                }
            }

            // Expand / Collapse details indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) stringResource(R.string.str_hide_details) else stringResource(R.string.str_view_details),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueAccent
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BlueAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expandable details section
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Divider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detailed explanation
                    Text(
                        text = insight.detailedExplanation,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    // Supporting signals
                    if (insight.contributingSignals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.str_supporting_signals),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        insight.contributingSignals.forEach { signal ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(4.dp)
                                        .background(BlueAccent, CircleShape)
                                )
                                Text(
                                    text = signal,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Recommended Strategy
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.str_recommended_strategy),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = insight.recommendedStrategy,
                                fontSize = 11.sp,
                                color = Color(0xFF166534),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Action buttons
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (insight.entityType == CrossInsightEntityType.PRODUCT && insight.entityId != null && onNavigateToProduct != null) {
                            OutlinedButton(
                                onClick = { onNavigateToProduct(insight.entityId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_view_product_${insight.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.str_view_product), fontSize = 12.sp)
                            }
                        }

                        if (insight.entityType == CrossInsightEntityType.CUSTOMER && insight.entityId != null && onNavigateToCustomer != null) {
                            OutlinedButton(
                                onClick = { onNavigateToCustomer(insight.entityId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_view_customer_${insight.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.str_view_customer), fontSize = 12.sp)
                            }
                        }

                        if (onNavigateToAlerts != null && (insight.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE || insight.category == CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK)) {
                            OutlinedButton(
                                onClick = onNavigateToAlerts,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_view_alert_${insight.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.str_view_alert), fontSize = 12.sp)
                            }
                        }

                        if (onNavigateToActions != null && (insight.category == CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP || insight.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN)) {
                            OutlinedButton(
                                onClick = onNavigateToActions,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_view_action_${insight.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.str_view_action), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Dashboard entry card for Cross-Business Insights.
 */
@Composable
fun BusinessInsightsDashboardCard(
    totalInsights: Int,
    criticalCount: Int,
    highCount: Int,
    capitalLocked: Double,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_insights_card")
            .clickable { onViewAllClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BlueLight, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = BlueNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.str_dashboard_insights_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueNavy
                        )
                        Text(
                            text = stringResource(R.string.str_dashboard_insights_subtitle),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BlueNavy,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalInsights ${stringResource(R.string.str_total_insights)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BlueNavy
                    )
                }

                if (criticalCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(InsightColors.CriticalBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$criticalCount ${stringResource(R.string.str_critical_insights)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRed
                        )
                    }
                }

                if (highCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(InsightColors.HighBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$highCount ${stringResource(R.string.str_high_insights)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                    }
                }
            }
        }
    }
}
