package com.example.presentation.alerts

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.intelligence.AlertCategory
import com.example.domain.intelligence.AlertSeverity
import com.example.domain.intelligence.BusinessAlert
import com.example.ui.theme.*

/**
 * Visual color mapping for Alert Severity.
 */
object AlertColors {
    val CriticalBg = Color(0xFFFEE2E2)
    val CriticalText = AccentRed
    val CriticalBorder = Color(0xFFFCA5A5)

    val WarningBg = Color(0xFFFFEDD5)
    val WarningText = Color(0xFFEA580C)
    val WarningBorder = Color(0xFFFDBA74)

    val InfoBg = Color(0xFFEFF6FF)
    val InfoText = BlueNavy
    val InfoBorder = Color(0xFFBFDBFE)
}

/**
 * Compact overview badge for an alert's severity.
 */
@Composable
fun AlertSeverityBadge(
    severity: AlertSeverity,
    modifier: Modifier = Modifier
) {
    val (bg, text, label) = when (severity) {
        AlertSeverity.CRITICAL -> Triple(AlertColors.CriticalBg, AlertColors.CriticalText, "CRITICAL")
        AlertSeverity.WARNING -> Triple(AlertColors.WarningBg, AlertColors.WarningText, "WARNING")
        AlertSeverity.INFO -> Triple(AlertColors.InfoBg, AlertColors.InfoText, "INFO")
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
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
 * Category badge for an alert.
 */
@Composable
fun AlertCategoryBadge(
    category: AlertCategory,
    modifier: Modifier = Modifier
) {
    val label = when (category) {
        AlertCategory.INVENTORY -> stringResource(R.string.str_category_inventory)
        AlertCategory.DEMAND -> stringResource(R.string.str_category_demand)
        AlertCategory.PROFITABILITY -> stringResource(R.string.str_category_profitability)
        AlertCategory.CASH_FLOW -> stringResource(R.string.str_category_cash_flow)
        AlertCategory.ATTENTION -> stringResource(R.string.str_category_attention)
    }

    Box(
        modifier = modifier
            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Compact Business Alert Summary Card for the Dashboard.
 *
 * Displays counts of Critical, Warning, and Info alerts and highlights
 * the top priority alert if any exist.
 */
@Composable
fun BusinessAlertSummaryCard(
    alerts: List<BusinessAlert>,
    onViewAllClick: () -> Unit,
    onAlertClick: (BusinessAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    val criticalCount = alerts.count { it.severity == AlertSeverity.CRITICAL }
    val warningCount = alerts.count { it.severity == AlertSeverity.WARNING }
    val infoCount = alerts.count { it.severity == AlertSeverity.INFO }
    val topPriorityAlert = alerts.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    criticalCount > 0 -> AlertColors.CriticalText
                                    warningCount > 0 -> AlertColors.WarningText
                                    infoCount > 0 -> AlertColors.InfoText
                                    else -> AccentGreen
                                }
                            )
                    )
                    Text(
                        text = stringResource(R.string.str_business_alerts),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                }

                if (alerts.isNotEmpty()) {
                    TextButton(
                        onClick = onViewAllClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.str_view_all_alerts, alerts.size),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = BlueNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Severity count chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountChip(
                    count = criticalCount,
                    label = stringResource(R.string.str_critical_count, criticalCount),
                    color = AlertColors.CriticalText,
                    bg = AlertColors.CriticalBg,
                    modifier = Modifier.weight(1f)
                )
                CountChip(
                    count = warningCount,
                    label = stringResource(R.string.str_warning_count, warningCount),
                    color = AlertColors.WarningText,
                    bg = AlertColors.WarningBg,
                    modifier = Modifier.weight(1f)
                )
                CountChip(
                    count = infoCount,
                    label = stringResource(R.string.str_info_count, infoCount),
                    color = AlertColors.InfoText,
                    bg = AlertColors.InfoBg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlighted Top Priority Alert or Empty State
            if (topPriorityAlert != null) {
                val alertBg = when (topPriorityAlert.severity) {
                    AlertSeverity.CRITICAL -> AlertColors.CriticalBg.copy(alpha = 0.4f)
                    AlertSeverity.WARNING -> AlertColors.WarningBg.copy(alpha = 0.4f)
                    AlertSeverity.INFO -> AlertColors.InfoBg.copy(alpha = 0.4f)
                }
                val alertBorder = when (topPriorityAlert.severity) {
                    AlertSeverity.CRITICAL -> AlertColors.CriticalBorder
                    AlertSeverity.WARNING -> AlertColors.WarningBorder
                    AlertSeverity.INFO -> AlertColors.InfoBorder
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(alertBg, RoundedCornerShape(12.dp))
                        .border(1.dp, alertBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = topPriorityAlert.productId != null) {
                            onAlertClick(topPriorityAlert)
                        }
                        .padding(12.dp)
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
                            AlertSeverityBadge(severity = topPriorityAlert.severity)
                            AlertCategoryBadge(category = topPriorityAlert.category)
                        }
                        if (topPriorityAlert.productId != null) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Product",
                                modifier = Modifier.size(14.dp),
                                tint = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = topPriorityAlert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    if (topPriorityAlert.productName != null) {
                        Text(
                            text = stringResource(R.string.str_alert_product_label, topPriorityAlert.productName),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BlueNavy
                        )
                    }
                    Text(
                        text = topPriorityAlert.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    if (topPriorityAlert.supportingMetric != null && topPriorityAlert.metricValue != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedVal = if (topPriorityAlert.unit == "₹") {
                            "₹%.2f".format(topPriorityAlert.metricValue)
                        } else if (topPriorityAlert.unit == "%") {
                            "%.1f%%".format(topPriorityAlert.metricValue)
                        } else {
                            "%.1f %s".format(topPriorityAlert.metricValue, topPriorityAlert.unit ?: "")
                        }
                        Text(
                            text = "${topPriorityAlert.supportingMetric}: $formattedVal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                // Neutral Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.str_no_business_alerts),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.str_no_business_alerts_desc),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountChip(
    count: Int,
    label: String,
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
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Detailed standalone Business Alert Card used in BusinessAlertsScreen.
 */
@Composable
fun BusinessAlertCard(
    alert: BusinessAlert,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val alertBg = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertColors.CriticalBg.copy(alpha = 0.3f)
        AlertSeverity.WARNING -> AlertColors.WarningBg.copy(alpha = 0.3f)
        AlertSeverity.INFO -> AlertColors.InfoBg.copy(alpha = 0.3f)
    }
    val alertBorder = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertColors.CriticalBorder
        AlertSeverity.WARNING -> AlertColors.WarningBorder
        AlertSeverity.INFO -> AlertColors.InfoBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, alertBorder, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(alertBg)
                .padding(16.dp)
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
                    AlertSeverityBadge(severity = alert.severity)
                    AlertCategoryBadge(category = alert.category)
                }
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigate to product",
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (alert.productName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.str_alert_product_label, alert.productName),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueNavy
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (alert.supportingMetric != null && alert.metricValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val formattedVal = if (alert.unit == "₹") {
                    "₹%.2f".format(alert.metricValue)
                } else if (alert.unit == "%") {
                    "%.1f%%".format(alert.metricValue)
                } else {
                    "%.1f %s".format(alert.metricValue, alert.unit ?: "")
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${alert.supportingMetric}: $formattedVal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
