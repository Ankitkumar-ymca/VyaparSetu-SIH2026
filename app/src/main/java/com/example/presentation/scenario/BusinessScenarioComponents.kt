package com.example.presentation.scenario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
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
import com.example.domain.intelligence.ScenarioMetricComparison
import com.example.domain.intelligence.ScenarioResult
import com.example.ui.theme.*

/**
 * Informational banner stating that simulations are strictly hypothetical and do not alter real data.
 */
@Composable
fun ScenarioDisclaimerBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scenario_disclaimer_banner"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFEFF6FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Scenario Info",
                    tint = BlueAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.str_hypothetical_badge),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.str_scenario_disclaimer),
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Single metric comparison block showing Baseline vs Scenario vs Estimated Impact.
 */
@Composable
fun ScenarioMetricComparisonRow(
    metric: ScenarioMetricComparison,
    modifier: Modifier = Modifier
) {
    val deltaColor = if (metric.isFavorable) AccentGreen else AccentRed
    val deltaSign = if (metric.delta >= 0.0) "+" else ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.metricName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BlueNavy
            )
            Text(
                text = "$deltaSign${metric.unit}${String.format("%,.2f", metric.delta)} (${String.format("%+.1f%%", metric.percentageDelta)})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = deltaColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.str_baseline),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${metric.unit}${String.format("%,.2f", metric.baselineValue)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.str_simulated),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${metric.unit}${String.format("%,.2f", metric.simulatedValue)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
            }
        }
    }
}

/**
 * Detailed result card displaying the hypothetical outcome computed by BusinessScenarioEngine.
 */
@Composable
fun ScenarioResultCard(
    result: ScenarioResult,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scenario_result_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BlueAccent.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with title and hypothetical badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                    if (!result.targetEntityName.isNullOrBlank()) {
                        Text(
                            text = result.targetEntityName,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.str_hypothetical_badge),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }

            // Summary description from engine
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = result.summary,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Metrics Breakdown
            Text(
                text = stringResource(R.string.str_estimated_impact),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.allMetrics.forEach { metric ->
                    ScenarioMetricComparisonRow(metric = metric)
                }
            }

            // Assumptions
            if (result.assumptions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "• " + stringResource(R.string.str_assumptions),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    result.assumptions.forEach { assumption ->
                        Text(
                            text = "– $assumption",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Limitations
            if (result.limitations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFBEB))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⚠️ " + stringResource(R.string.str_limitations),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    result.limitations.forEach { limitation ->
                        Text(
                            text = "– $limitation",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Reset button
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_scenario_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy)
            ) {
                Text(
                    text = stringResource(R.string.str_reset_scenario),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Compact Dashboard Entry Card for What-If Simulator.
 */
@Composable
fun ScenarioDashboardEntryCard(
    onNavigateToSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("what_if_simulator_card")
            .clickable { onNavigateToSimulator() },
        shape = RoundedCornerShape(20.dp),
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔮", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.str_what_if_card_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE0E7FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SIMULATOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueNavy
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.str_what_if_card_subtitle),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onNavigateToSimulator,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.str_what_if_simulator),
                    tint = BlueAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
