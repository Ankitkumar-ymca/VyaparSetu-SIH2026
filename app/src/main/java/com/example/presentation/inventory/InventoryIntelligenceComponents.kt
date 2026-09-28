package com.example.presentation.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.example.domain.intelligence.DemandTrend
import com.example.domain.intelligence.InventoryIntelligenceSummary
import com.example.domain.intelligence.InventoryProductMetrics
import com.example.domain.intelligence.VelocityCategory
import com.example.ui.theme.*
import java.util.Locale

/**
 * Compact overview card displaying high-level inventory intelligence metrics:
 * Total Inventory Value, Fast/Normal/Slow/Dead stock counts, and Capital Locked.
 */
@Composable
fun InventoryIntelligenceSummaryCard(
    summary: InventoryIntelligenceSummary,
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
                    text = stringResource(R.string.str_inventory_intelligence),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Text(
                    text = stringResource(R.string.str_intel_total_val, formatCurrency(summary.totalInventoryValue)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-column metric counters for Fast, Normal, Slow, Dead
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VelocityStatBox(
                    label = stringResource(R.string.str_fast),
                    count = summary.fastMovingCount,
                    textColor = Color(0xFF15803D), // Green
                    bgColor = Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f)
                )
                VelocityStatBox(
                    label = stringResource(R.string.str_normal),
                    count = summary.normalCount,
                    textColor = BlueNavy,
                    bgColor = BlueLight,
                    modifier = Modifier.weight(1f)
                )
                VelocityStatBox(
                    label = stringResource(R.string.str_slow),
                    count = summary.slowMovingCount,
                    textColor = Color(0xFFC2410C), // Orange
                    bgColor = Color(0xFFFFEDD5),
                    modifier = Modifier.weight(1f)
                )
                VelocityStatBox(
                    label = stringResource(R.string.str_dead),
                    count = summary.deadStockCount,
                    textColor = AccentRed,
                    bgColor = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.capitalLockedInSlowAndDeadStock > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.str_capital_locked_slow_dead),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = "₹${formatCurrency(summary.capitalLockedInSlowAndDeadStock)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }
    }
}

@Composable
private fun VelocityStatBox(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Visual badge for product movement classification.
 */
@Composable
fun MovementCategoryBadge(category: VelocityCategory) {
    val (label, textColor, bgColor) = when (category) {
        VelocityCategory.FAST_MOVING -> Triple(stringResource(R.string.str_fast_moving), Color(0xFF15803D), Color(0xFFDCFCE7))
        VelocityCategory.NORMAL -> Triple(stringResource(R.string.str_normal_moving), BlueNavy, BlueLight)
        VelocityCategory.SLOW_MOVING -> Triple(stringResource(R.string.str_slow_moving), Color(0xFFC2410C), Color(0xFFFFEDD5))
        VelocityCategory.DEAD_STOCK -> Triple(stringResource(R.string.str_dead_stock), AccentRed, Color(0xFFFEE2E2))
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
 * Visual badge for demand trend.
 */
@Composable
fun DemandTrendBadge(trend: DemandTrend) {
    val (label, color) = when (trend) {
        DemandTrend.RISING -> "↗ " + stringResource(R.string.str_demand_rising) to Color(0xFF15803D)
        DemandTrend.FALLING -> "↘ " + stringResource(R.string.str_demand_falling) to AccentRed
        DemandTrend.STABLE -> "→ " + stringResource(R.string.str_demand_stable) to TextSecondary
        DemandTrend.INSUFFICIENT_DATA -> "– " + stringResource(R.string.str_intel_insufficient_data) to TextSecondary
    }

    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

/**
 * Collapsible or expandable intelligence details inside ProductCard.
 */
@Composable
fun ProductIntelligenceDetails(
    metrics: InventoryProductMetrics?,
    modifier: Modifier = Modifier
) {
    if (metrics == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MovementCategoryBadge(category = metrics.velocityCategory)
            DemandTrendBadge(trend = metrics.demandTrend)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.str_sales_velocity),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.2f units/day", metrics.salesVelocityPerDay),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.str_stock_remaining),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                val remainingText = when {
                    metrics.isDeadStock -> stringResource(R.string.str_no_recent_sales)
                    metrics.daysOfStockRemaining == null -> stringResource(R.string.str_no_recent_sales)
                    metrics.daysOfStockRemaining < 1.0 -> stringResource(R.string.str_less_than_day)
                    else -> String.format(Locale.getDefault(), "~%d days", metrics.daysOfStockRemaining.toInt())
                }
                Text(
                    text = remainingText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (metrics.daysOfStockRemaining != null && metrics.daysOfStockRemaining <= 3.0) AccentRed else TextPrimary
                )
            }
        }

        if (metrics.isDeadStock || metrics.velocityCategory == VelocityCategory.SLOW_MOVING) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (metrics.isDeadStock) stringResource(R.string.str_no_sales_45d) else stringResource(R.string.str_capital_locked),
                    fontSize = 10.sp,
                    color = Color(0xFFC2410C),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${formatCurrency(metrics.capitalLocked)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC2410C)
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return if (amount.isNaN() || amount.isInfinite()) "0"
    else String.format(Locale.getDefault(), "%,.0f", amount)
}
