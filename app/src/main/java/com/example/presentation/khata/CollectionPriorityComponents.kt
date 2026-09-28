package com.example.presentation.khata

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.domain.intelligence.CollectionPriorityLevel
import com.example.domain.intelligence.CollectionPriorityResult
import com.example.domain.intelligence.CollectionPrioritySummary
import com.example.ui.theme.*

/**
 * Styling colors for Collection Priority badges.
 */
object CollectionPriorityColors {
    val HighBg = Color(0xFFFEE2E2)
    val HighText = AccentRed

    val MediumBg = Color(0xFFFEF3C7)
    val MediumText = Color(0xFFD97706)

    val LowBg = Color(0xFFDEF7EC)
    val LowText = AccentGreen

    val NoneBg = Color(0xFFF3F4F6)
    val NoneText = TextSecondary

    val InsufficientBg = Color(0xFFF1F5F9)
    val InsufficientText = TextSecondary
}

/**
 * Compact chip for collection review priority.
 */
@Composable
fun CollectionPriorityBadge(
    priority: CollectionPriorityLevel,
    modifier: Modifier = Modifier
) {
    val (bg, text, labelRes) = when (priority) {
        CollectionPriorityLevel.HIGH -> Triple(CollectionPriorityColors.HighBg, CollectionPriorityColors.HighText, R.string.str_priority_high)
        CollectionPriorityLevel.MEDIUM -> Triple(CollectionPriorityColors.MediumBg, CollectionPriorityColors.MediumText, R.string.str_priority_medium)
        CollectionPriorityLevel.LOW -> Triple(CollectionPriorityColors.LowBg, CollectionPriorityColors.LowText, R.string.str_priority_low)
        CollectionPriorityLevel.NONE -> Triple(CollectionPriorityColors.NoneBg, CollectionPriorityColors.NoneText, R.string.str_priority_none)
        CollectionPriorityLevel.INSUFFICIENT_DATA -> Triple(CollectionPriorityColors.InsufficientBg, CollectionPriorityColors.InsufficientText, R.string.str_priority_insufficient_data)
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
 * Compact summary card of collection review priorities for the Khata experience.
 */
@Composable
fun CollectionPrioritySummaryCard(
    summary: CollectionPrioritySummary,
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
                    text = stringResource(R.string.str_collection_priority),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Text(
                    text = "₹%.2f %s".format(summary.totalOutstanding, stringResource(R.string.str_total_outstanding_amount)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (summary.totalOutstanding > 0) AccentRed else AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrioritySummaryChip(
                    label = stringResource(R.string.str_priority_high),
                    value = summary.highPriorityCount.toString(),
                    color = CollectionPriorityColors.HighText,
                    bg = CollectionPriorityColors.HighBg,
                    modifier = Modifier.weight(1f)
                )
                PrioritySummaryChip(
                    label = stringResource(R.string.str_priority_medium),
                    value = summary.mediumPriorityCount.toString(),
                    color = CollectionPriorityColors.MediumText,
                    bg = CollectionPriorityColors.MediumBg,
                    modifier = Modifier.weight(1f)
                )
                PrioritySummaryChip(
                    label = stringResource(R.string.str_priority_low),
                    value = summary.lowPriorityCount.toString(),
                    color = CollectionPriorityColors.LowText,
                    bg = CollectionPriorityColors.LowBg,
                    modifier = Modifier.weight(1f)
                )
                PrioritySummaryChip(
                    label = stringResource(R.string.str_with_outstanding),
                    value = summary.customersWithOutstanding.toString(),
                    color = BlueNavy,
                    bg = BlueLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrioritySummaryChip(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
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
 * Dedicated Collection Review Card for CustomerDetailScreen.
 */
@Composable
fun CustomerDetailCollectionCard(
    result: CollectionPriorityResult?,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_collection_review),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                CollectionPriorityBadge(priority = result.priority)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.str_amount_due),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹%.2f".format(result.outstandingAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.outstandingAmount > 0) AccentRed else AccentGreen
                    )
                }

                if (result.daysSinceLastPurchase != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.str_last_purchase),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.str_days_ago, result.daysSinceLastPurchase),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (result.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.str_collection_reasons),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    result.reasons.forEach { reason ->
                        Text(
                            text = "• $reason",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
