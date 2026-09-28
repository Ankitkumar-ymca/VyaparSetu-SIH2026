package com.example.presentation.khata

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.intelligence.CustomerActivityStatus
import com.example.domain.intelligence.CustomerIntelligenceSummary
import com.example.domain.intelligence.CustomerMetrics
import com.example.domain.intelligence.CustomerSegment
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Visual styling for Customer Segment badges.
 */
object SegmentColors {
    val FrequentBg = Color(0xFFEDE9FE)
    val FrequentText = Color(0xFF6D28D9)

    val HighValueBg = Color(0xFFECFDF5)
    val HighValueText = AccentGreen

    val NewBg = Color(0xFFEFF6FF)
    val NewText = BlueNavy

    val InactiveBg = Color(0xFFFEE2E2)
    val InactiveText = AccentRed

    val RegularBg = Color(0xFFF3F4F6)
    val RegularText = TextSecondary

    val InsufficientBg = Color(0xFFF1F5F9)
    val InsufficientText = TextSecondary
}

/**
 * Compact chip for displaying customer segmentation.
 */
@Composable
fun CustomerSegmentBadge(
    segment: CustomerSegment,
    modifier: Modifier = Modifier
) {
    val (bg, text, labelRes) = when (segment) {
        CustomerSegment.FREQUENT -> Triple(SegmentColors.FrequentBg, SegmentColors.FrequentText, R.string.str_frequent)
        CustomerSegment.HIGH_VALUE -> Triple(SegmentColors.HighValueBg, SegmentColors.HighValueText, R.string.str_high_value)
        CustomerSegment.NEW -> Triple(SegmentColors.NewBg, SegmentColors.NewText, R.string.str_new_customer)
        CustomerSegment.INACTIVE -> Triple(SegmentColors.InactiveBg, SegmentColors.InactiveText, R.string.str_inactive)
        CustomerSegment.REGULAR -> Triple(SegmentColors.RegularBg, SegmentColors.RegularText, R.string.str_regular)
        CustomerSegment.INSUFFICIENT_DATA -> Triple(SegmentColors.InsufficientBg, SegmentColors.InsufficientText, R.string.str_insufficient_data_label)
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(labelRes),
            color = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Compact chip for customer activity status.
 */
@Composable
fun CustomerActivityBadge(
    status: CustomerActivityStatus,
    modifier: Modifier = Modifier
) {
    val (bg, text, labelRes) = when (status) {
        CustomerActivityStatus.ACTIVE -> Triple(Color(0xFFDEF7EC), AccentGreen, R.string.str_active)
        CustomerActivityStatus.INACTIVE -> Triple(Color(0xFFFEE2E2), AccentRed, R.string.str_inactive)
        CustomerActivityStatus.NEW -> Triple(Color(0xFFE1EFFE), BlueNavy, R.string.str_new_customer)
        CustomerActivityStatus.INSUFFICIENT_DATA -> Triple(Color(0xFFF3F4F6), TextSecondary, R.string.str_insufficient_data_label)
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(labelRes),
            color = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Customer intelligence summary card displayed at the top of CustomersTab.
 */
@Composable
fun CustomerIntelligenceSummaryCard(
    summary: CustomerIntelligenceSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_customer_intelligence),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Text(
                    text = "${summary.totalCustomersAnalyzed} ${stringResource(R.string.str_customers).lowercase()}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Activity breakdown chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(
                    label = stringResource(R.string.str_active),
                    value = summary.activeCustomerCount.toString(),
                    color = AccentGreen,
                    bg = Color(0xFFDEF7EC),
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = stringResource(R.string.str_inactive),
                    value = summary.inactiveCustomerCount.toString(),
                    color = AccentRed,
                    bg = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = stringResource(R.string.str_new_customer),
                    value = summary.newCustomerCount.toString(),
                    color = BlueNavy,
                    bg = Color(0xFFE1EFFE),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Segments breakdown chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(
                    label = stringResource(R.string.str_frequent),
                    value = summary.frequentCustomerCount.toString(),
                    color = SegmentColors.FrequentText,
                    bg = SegmentColors.FrequentBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = stringResource(R.string.str_high_value),
                    value = summary.highValueCustomerCount.toString(),
                    color = AccentGreen,
                    bg = SegmentColors.HighValueBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = stringResource(R.string.str_has_outstanding),
                    value = summary.customersWithOutstandingCount.toString(),
                    color = if (summary.customersWithOutstandingCount > 0) AccentRed else TextSecondary,
                    bg = if (summary.customersWithOutstandingCount > 0) Color(0xFFFEE2E2) else Color(0xFFF3F4F6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Dedicated Customer Intelligence Card on the Customer Detail Screen.
 */
@Composable
fun CustomerIntelligenceDetailCard(
    metrics: CustomerMetrics?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_customer_intelligence),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )

                if (metrics != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CustomerSegmentBadge(segment = metrics.segment)
                        CustomerActivityBadge(status = metrics.activityStatus)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (metrics == null || !metrics.hasSufficientData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.str_customer_intelligence_no_data),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                // Section 1: Purchase Behaviour
                Text(
                    text = stringResource(R.string.str_purchase_behaviour),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = stringResource(R.string.str_period_purchases_30d),
                        value = "₹%.2f".format(metrics.periodPurchases)
                    )
                    MetricItem(
                        label = stringResource(R.string.str_lifetime_purchases),
                        value = "₹%.2f".format(metrics.lifetimePurchases),
                        alignment = Alignment.End
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = stringResource(R.string.str_period_orders),
                        value = metrics.periodOrders.toString()
                    )
                    MetricItem(
                        label = stringResource(R.string.str_lifetime_orders),
                        value = metrics.lifetimeOrders.toString(),
                        alignment = Alignment.CenterHorizontally
                    )
                    MetricItem(
                        label = stringResource(R.string.str_units_purchased),
                        value = metrics.lifetimeUnitsPurchased.toString(),
                        alignment = Alignment.End
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = stringResource(R.string.str_avg_order_value),
                        value = "₹%.2f".format(metrics.averageOrderValue)
                    )
                    MetricItem(
                        label = stringResource(R.string.str_purchase_frequency),
                        value = stringResource(R.string.str_orders_per_month, metrics.purchaseFrequencyPerMonth),
                        alignment = Alignment.End
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Activity
                Text(
                    text = stringResource(R.string.str_activity),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val firstDateStr = metrics.firstPurchaseDate?.let { dateFormatter.format(Date(it)) } ?: "—"
                val lastDateStr = metrics.lastPurchaseDate?.let { dateFormatter.format(Date(it)) } ?: "—"
                val recencyStr = metrics.daysSinceLastPurchase?.let {
                    stringResource(R.string.str_days_ago, it)
                } ?: "—"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = stringResource(R.string.str_first_purchase),
                        value = firstDateStr
                    )
                    MetricItem(
                        label = stringResource(R.string.str_last_purchase),
                        value = "$lastDateStr ($recencyStr)",
                        alignment = Alignment.End
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Financial Summary
                Text(
                    text = stringResource(R.string.str_financial_summary),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = stringResource(R.string.str_amount_due),
                        value = "₹%.2f".format(metrics.outstandingAmount),
                        valueColor = if (metrics.hasOutstanding) AccentRed else AccentGreen
                    )
                    MetricItem(
                        label = stringResource(R.string.str_total_paid),
                        value = "₹%.2f".format(metrics.totalPaid),
                        valueColor = AccentGreen,
                        alignment = Alignment.End
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
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
