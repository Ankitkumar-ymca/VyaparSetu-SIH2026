package com.example.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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

/**
 * Top Summary Card showing Previous Profit vs Current Profit, Profit Change, and Direction Badge.
 */
@Composable
fun ProfitChangeSummaryCard(
    explanation: ProfitChangeExplanation,
    modifier: Modifier = Modifier
) {
    val isPositiveChange = explanation.netProfitChange >= 0.0
    val deltaColor = if (isPositiveChange) AccentGreen else AccentRed
    val deltaSign = if (explanation.netProfitChange >= 0.0) "+" else ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profit_change_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with period labels and direction badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_profit_change_summary),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )

                ProfitDirectionBadge(direction = explanation.changeDirection)
            }

            // Main Profit Change display
            Column {
                Text(
                    text = stringResource(R.string.str_profit_change),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$deltaSign₹${String.format("%,.2f", explanation.netProfitChange)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor
                )
            }

            HorizontalDivider(color = CardBorder.copy(alpha = 0.6f))

            // Previous vs Current Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${stringResource(R.string.str_previous_profit)} (${explanation.previousPeriodLabel})",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${String.format("%,.2f", explanation.previousNetProfit)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stringResource(R.string.str_current_profit)} (${explanation.currentPeriodLabel})",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${String.format("%,.2f", explanation.currentNetProfit)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                }
            }
        }
    }
}

/**
 * Direction badge (EXPANSION, CONTRACTION, LOSS, NO_CHANGE).
 */
@Composable
fun ProfitDirectionBadge(direction: ProfitChangeDirection) {
    val (labelRes, bgCol, textCol) = when (direction) {
        ProfitChangeDirection.EXPANSION -> Triple(R.string.str_direction_expansion, Color(0xFFDCFCE7), Color(0xFF15803D))
        ProfitChangeDirection.CONTRACTION -> Triple(R.string.str_direction_contraction, Color(0xFFFEF3C7), Color(0xFFB45309))
        ProfitChangeDirection.LOSS -> Triple(R.string.str_direction_loss, Color(0xFFFEE2E2), AccentRed)
        ProfitChangeDirection.NO_CHANGE -> Triple(R.string.str_direction_no_change, Color(0xFFF1F5F9), TextSecondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgCol)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("profit_direction_badge")
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}

/**
 * Profit Change Bridge Card: Revenue, COGS, and Operating Expenses contributions.
 */
@Composable
fun ProfitContributionCard(
    explanation: ProfitChangeExplanation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profit_contribution_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.str_whats_driving_change),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy
            )

            Text(
                text = "Positive contributions improved profit, while negative contributions reduced profit.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Revenue Contribution
            ContributionRow(
                label = stringResource(R.string.str_revenue_contribution),
                contribution = explanation.revenueContribution,
                explanationNote = if (explanation.revenueContribution >= 0) "Higher sales volume or prices" else "Lower sales volume or prices"
            )

            // 2. COGS Contribution
            ContributionRow(
                label = stringResource(R.string.str_cogs_contribution),
                contribution = explanation.cogsContribution,
                explanationNote = if (explanation.cogsContribution >= 0) "Procurement cost savings" else "Higher procurement or inventory cost"
            )

            // 3. Operating Expense Contribution
            ContributionRow(
                label = stringResource(R.string.str_expense_contribution),
                contribution = explanation.expenseContribution,
                explanationNote = if (explanation.expenseContribution >= 0) "Operational expense savings" else "Higher operational expenses"
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.6f))

            // Net Profit Change Reconciled Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.str_net_profit_change),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )

                val netColor = if (explanation.netProfitChange >= 0) AccentGreen else AccentRed
                val netSign = if (explanation.netProfitChange >= 0) "+" else ""
                Text(
                    text = "$netSign₹${String.format("%,.2f", explanation.netProfitChange)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }
        }
    }
}

@Composable
private fun ContributionRow(
    label: String,
    contribution: Double,
    explanationNote: String
) {
    val isFavorable = contribution >= 0.0
    val color = if (isFavorable) AccentGreen else AccentRed
    val sign = if (contribution >= 0.0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BlueNavy
            )
            Text(
                text = "$sign₹${String.format("%,.2f", contribution)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = explanationNote,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

/**
 * Top Business Drivers Section: Revenue, Cost, and Expense drivers.
 */
@Composable
fun ProfitDriversSection(
    attribution: ProfitDriverAttribution,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profit_drivers_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.str_top_business_drivers),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy
            )

            // A. Revenue Drivers
            DriverGroup(
                title = stringResource(R.string.str_revenue_drivers),
                positiveLabel = stringResource(R.string.str_top_pos_revenue),
                positiveName = attribution.topPositiveRevenueDriver?.productName,
                positiveDelta = attribution.topPositiveRevenueDriver?.revenueChange,
                negativeLabel = stringResource(R.string.str_top_neg_revenue),
                negativeName = attribution.topNegativeRevenueDriver?.productName,
                negativeDelta = attribution.topNegativeRevenueDriver?.revenueChange,
                isRevenue = true
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

            // B. Cost Drivers
            DriverGroup(
                title = stringResource(R.string.str_cost_drivers),
                positiveLabel = stringResource(R.string.str_top_cost_reduction),
                positiveName = attribution.topCostReductionDriver?.productName,
                positiveDelta = attribution.topCostReductionDriver?.cogsChange?.let { -it }, // Cost reduction is positive delta for profit
                negativeLabel = stringResource(R.string.str_top_cost_increase),
                negativeName = attribution.topCostIncreaseDriver?.productName,
                negativeDelta = attribution.topCostIncreaseDriver?.cogsChange?.let { -it }, // Cost increase is negative delta for profit
                isRevenue = false
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

            // C. Expense Drivers
            DriverGroup(
                title = stringResource(R.string.str_expense_drivers),
                positiveLabel = stringResource(R.string.str_top_exp_reduction),
                positiveName = attribution.topExpenseReductionDriver?.category,
                positiveDelta = attribution.topExpenseReductionDriver?.expenseChange?.let { -it }, // Expense reduction is positive
                negativeLabel = stringResource(R.string.str_top_exp_increase),
                negativeName = attribution.topExpenseIncreaseDriver?.category,
                negativeDelta = attribution.topExpenseIncreaseDriver?.expenseChange?.let { -it }, // Expense increase is negative
                isRevenue = false
            )
        }
    }
}

@Composable
private fun DriverGroup(
    title: String,
    positiveLabel: String,
    positiveName: String?,
    positiveDelta: Double?,
    negativeLabel: String?,
    negativeName: String?,
    negativeDelta: Double?,
    isRevenue: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = BlueNavy
        )

        if (positiveName == null && negativeName == null) {
            Text(
                text = stringResource(R.string.str_not_enough_driver_data),
                fontSize = 12.sp,
                color = TextSecondary
            )
        } else {
            if (positiveName != null && positiveDelta != null) {
                SingleDriverRow(
                    label = positiveLabel,
                    name = positiveName,
                    delta = positiveDelta,
                    isFavorable = true
                )
            }

            if (negativeName != null && negativeDelta != null && negativeLabel != null) {
                SingleDriverRow(
                    label = negativeLabel,
                    name = negativeName,
                    delta = negativeDelta,
                    isFavorable = false
                )
            }
        }
    }
}

@Composable
private fun SingleDriverRow(
    label: String,
    name: String,
    delta: Double,
    isFavorable: Boolean
) {
    val color = if (isFavorable) AccentGreen else AccentRed
    val sign = if (delta >= 0.0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Text(
            text = "$sign₹${String.format("%,.2f", delta)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Deterministic explanation narrative and consistency alerts.
 */
@Composable
fun ProfitExplanationCard(
    explanation: ProfitChangeExplanation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profit_explanation_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Explanation",
                        tint = BlueAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.str_explanation_reason),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
            }

            // Inconsistency warning if mathematical validation failed
            if (!explanation.isConsistent) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.str_inconsistency_warning),
                            fontSize = 12.sp,
                            color = AccentRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Primary narrative summary from deterministic engine
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = explanation.primaryDriverSummary,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

/**
 * Secondary Notes & Limitations card.
 */
@Composable
fun ProfitLimitationsCard(
    notes: List<String>,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profit_limitations_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "• " + stringResource(R.string.str_notes_and_limitations),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )

            notes.forEach { note ->
                Text(
                    text = "– $note",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Compact Entry Card to navigate to "Why did my profit change?".
 */
@Composable
fun ProfitExplainabilityEntryCard(
    onNavigateToExplainability: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToExplainability() }
            .testTag("profit_explainability_entry_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💡", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.str_why_did_profit_change),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.str_profit_explain_card_subtitle),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.str_explain_profit_change),
                tint = BlueAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
