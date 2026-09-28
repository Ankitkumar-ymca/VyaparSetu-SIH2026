package com.example.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.intelligence.MarginCategory
import com.example.domain.intelligence.ProductProfitabilityMetrics
import com.example.domain.intelligence.ProductProfitabilitySummary
import com.example.ui.theme.*
import java.util.Locale

/**
 * Compact overview card displaying aggregated product profitability metrics.
 */
@Composable
fun ProductProfitabilitySummaryCard(
    summary: ProductProfitabilitySummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_product_profitability),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Text(
                    text = stringResource(
                        R.string.str_overall_margin,
                        String.format(Locale.getDefault(), "%.1f%%", summary.overallGrossMarginPercentage)
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.overallGrossMarginPercentage >= 20.0) Color(0xFF15803D) else BlueNavy
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial stats: Revenue, COGS, Gross Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialMiniCard(
                    title = stringResource(R.string.str_revenue),
                    amount = summary.totalRevenue,
                    modifier = Modifier.weight(1f)
                )
                FinancialMiniCard(
                    title = stringResource(R.string.str_cogs),
                    amount = summary.totalCostOfGoodsSold,
                    modifier = Modifier.weight(1f)
                )
                FinancialMiniCard(
                    title = stringResource(R.string.str_gross_profit),
                    amount = summary.totalGrossProfit,
                    highlightColor = if (summary.totalGrossProfit >= 0) Color(0xFF15803D) else AccentRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Classification counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfitabilityStatBox(
                    label = stringResource(R.string.str_margin_high),
                    count = summary.highMarginCount,
                    textColor = Color(0xFF15803D),
                    bgColor = Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f)
                )
                ProfitabilityStatBox(
                    label = stringResource(R.string.str_margin_med),
                    count = summary.mediumMarginCount,
                    textColor = BlueNavy,
                    bgColor = BlueLight,
                    modifier = Modifier.weight(1f)
                )
                ProfitabilityStatBox(
                    label = stringResource(R.string.str_margin_low),
                    count = summary.lowMarginCount,
                    textColor = Color(0xFFC2410C),
                    bgColor = Color(0xFFFFEDD5),
                    modifier = Modifier.weight(1f)
                )
                ProfitabilityStatBox(
                    label = stringResource(R.string.str_margin_no_profit),
                    count = summary.noProfitCount,
                    textColor = AccentRed,
                    bgColor = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinancialMiniCard(
    title: String,
    amount: Double,
    highlightColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "₹${formatCurrency(amount)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}

@Composable
private fun ProfitabilityStatBox(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Visual badge for product profitability classification.
 */
@Composable
fun ProfitabilityMarginBadge(category: MarginCategory) {
    val (label, textColor, bgColor) = when (category) {
        MarginCategory.HIGH_MARGIN -> Triple(stringResource(R.string.str_badge_high_margin), Color(0xFF15803D), Color(0xFFDCFCE7))
        MarginCategory.MEDIUM_MARGIN -> Triple(stringResource(R.string.str_badge_medium_margin), BlueNavy, BlueLight)
        MarginCategory.LOW_MARGIN -> Triple(stringResource(R.string.str_badge_low_margin), Color(0xFFC2410C), Color(0xFFFFEDD5))
        MarginCategory.NO_PROFIT -> Triple(stringResource(R.string.str_badge_no_profit), AccentRed, Color(0xFFFEE2E2))
        MarginCategory.INSUFFICIENT_DATA -> Triple(stringResource(R.string.str_intel_insufficient_data), TextSecondary, Color(0xFFF1F5F9))
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Detailed product-level profitability section for ProductDetailScreen.
 */
@Composable
fun ProductProfitabilityCard(
    metrics: ProductProfitabilityMetrics?,
    modifier: Modifier = Modifier
) {
    if (metrics == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.str_product_profitability),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy
            )
            ProfitabilityMarginBadge(category = metrics.marginCategory)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (metrics.marginCategory == MarginCategory.INSUFFICIENT_DATA || metrics.unitsSold <= 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_profitability_insufficient_data_desc),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        } else {
            // Row 1: Revenue, COGS, Gross Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = stringResource(R.string.str_revenue),
                    value = "₹${formatCurrency(metrics.revenue)}"
                )
                MetricColumn(
                    title = stringResource(R.string.str_cogs),
                    value = "₹${formatCurrency(metrics.costOfGoodsSold)}"
                )
                MetricColumn(
                    title = stringResource(R.string.str_gross_profit),
                    value = "₹${formatCurrency(metrics.grossProfit)}",
                    valueColor = if (metrics.grossProfit >= 0) Color(0xFF15803D) else AccentRed,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Gross Margin %, Units Sold, Profit Per Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = stringResource(R.string.str_gross_margin),
                    value = String.format(Locale.getDefault(), "%.1f%%", metrics.grossMarginPercentage),
                    valueColor = when (metrics.marginCategory) {
                        MarginCategory.HIGH_MARGIN -> Color(0xFF15803D)
                        MarginCategory.MEDIUM_MARGIN -> BlueNavy
                        MarginCategory.LOW_MARGIN -> Color(0xFFC2410C)
                        else -> AccentRed
                    }
                )
                MetricColumn(
                    title = stringResource(R.string.str_units_sold),
                    value = "${metrics.unitsSold}"
                )
                MetricColumn(
                    title = stringResource(R.string.str_profit_per_unit),
                    value = "₹${formatCurrency(metrics.profitPerUnit)}",
                    valueColor = if (metrics.profitPerUnit >= 0) Color(0xFF15803D) else AccentRed,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Avg Selling Price, Avg Cost Per Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = stringResource(R.string.str_avg_selling_price),
                    value = "₹${formatCurrency(metrics.averageSellingPrice)}"
                )
                MetricColumn(
                    title = stringResource(R.string.str_avg_cost_per_unit),
                    value = "₹${formatCurrency(metrics.averageCostPerUnit)}",
                    alignment = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    valueColor: Color = TextPrimary,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return if (amount.isNaN() || amount.isInfinite()) "0"
    else String.format(Locale.getDefault(), "%,.0f", amount)
}
