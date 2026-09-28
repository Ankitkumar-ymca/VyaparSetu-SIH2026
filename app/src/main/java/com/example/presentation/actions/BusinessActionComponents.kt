package com.example.presentation.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.domain.intelligence.BusinessAction
import com.example.domain.intelligence.BusinessActionCategory
import com.example.domain.intelligence.BusinessActionPriority
import com.example.domain.intelligence.BusinessActionSummary
import com.example.ui.theme.*

/**
 * Visual styling and color definitions for Business Action Priorities.
 */
object ActionColors {
    val CriticalBg = Color(0xFFFEE2E2)
    val CriticalText = AccentRed
    val CriticalBorder = Color(0xFFFCA5A5)

    val HighBg = Color(0xFFFFEDD5)
    val HighText = Color(0xFFEA580C)
    val HighBorder = Color(0xFFFDBA74)

    val MediumBg = Color(0xFFEFF6FF)
    val MediumText = BlueNavy
    val MediumBorder = Color(0xFFBFDBFE)

    val LowBg = Color(0xFFF3F4F6)
    val LowText = TextSecondary
    val LowBorder = Color(0xFFE5E7EB)
}

/**
 * Badge indicating priority severity of a business action.
 */
@Composable
fun BusinessActionPriorityBadge(
    priority: BusinessActionPriority,
    modifier: Modifier = Modifier
) {
    val (bg, text, label) = when (priority) {
        BusinessActionPriority.CRITICAL -> Triple(ActionColors.CriticalBg, ActionColors.CriticalText, "CRITICAL")
        BusinessActionPriority.HIGH -> Triple(ActionColors.HighBg, ActionColors.HighText, "HIGH")
        BusinessActionPriority.MEDIUM -> Triple(ActionColors.MediumBg, ActionColors.MediumText, "MEDIUM")
        BusinessActionPriority.LOW -> Triple(ActionColors.LowBg, ActionColors.LowText, "LOW")
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
 * Badge indicating functional category of a business action.
 */
@Composable
fun BusinessActionCategoryBadge(
    category: BusinessActionCategory,
    modifier: Modifier = Modifier
) {
    val label = when (category) {
        BusinessActionCategory.RESTOCK -> stringResource(R.string.str_category_restock)
        BusinessActionCategory.COLLECT_PAYMENT -> stringResource(R.string.str_category_collect_payment)
        BusinessActionCategory.CLEAR_STOCK -> stringResource(R.string.str_category_clear_stock)
        BusinessActionCategory.REVIEW_PRICING -> stringResource(R.string.str_category_review_pricing)
        BusinessActionCategory.REENGAGE_CUSTOMER -> stringResource(R.string.str_category_reengage_customer)
    }

    Box(
        modifier = modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
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
 * Compact Dashboard Summary Card displaying counts and top business actions.
 */
@Composable
fun BusinessActionSummaryCard(
    summary: BusinessActionSummary,
    onViewAllClick: () -> Unit,
    onActionClick: (BusinessAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val topAction = summary.topActions.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
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
                                    summary.criticalActionsCount > 0 -> ActionColors.CriticalText
                                    summary.highActionsCount > 0 -> ActionColors.HighText
                                    summary.mediumActionsCount > 0 -> ActionColors.MediumText
                                    else -> AccentGreen
                                }
                            )
                    )
                    Text(
                        text = stringResource(R.string.str_todays_business_actions),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueNavy
                    )
                }

                if (summary.totalActions > 0) {
                    TextButton(
                        onClick = onViewAllClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.str_view_all_actions, summary.totalActions),
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

            // Action count badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionCountChip(
                    label = stringResource(R.string.str_action_critical_count, summary.criticalActionsCount),
                    color = ActionColors.CriticalText,
                    bg = ActionColors.CriticalBg,
                    modifier = Modifier.weight(1f)
                )
                ActionCountChip(
                    label = stringResource(R.string.str_action_high_count, summary.highActionsCount),
                    color = ActionColors.HighText,
                    bg = ActionColors.HighBg,
                    modifier = Modifier.weight(1f)
                )
                ActionCountChip(
                    label = stringResource(R.string.str_action_medium_count, summary.mediumActionsCount),
                    color = ActionColors.MediumText,
                    bg = ActionColors.MediumBg,
                    modifier = Modifier.weight(1f)
                )
                ActionCountChip(
                    label = stringResource(R.string.str_action_low_count, summary.lowActionsCount),
                    color = ActionColors.LowText,
                    bg = ActionColors.LowBg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Top action preview or empty state
            if (topAction != null) {
                val actionBg = when (topAction.priority) {
                    BusinessActionPriority.CRITICAL -> ActionColors.CriticalBg.copy(alpha = 0.35f)
                    BusinessActionPriority.HIGH -> ActionColors.HighBg.copy(alpha = 0.35f)
                    BusinessActionPriority.MEDIUM -> ActionColors.MediumBg.copy(alpha = 0.35f)
                    BusinessActionPriority.LOW -> ActionColors.LowBg.copy(alpha = 0.5f)
                }
                val actionBorder = when (topAction.priority) {
                    BusinessActionPriority.CRITICAL -> ActionColors.CriticalBorder
                    BusinessActionPriority.HIGH -> ActionColors.HighBorder
                    BusinessActionPriority.MEDIUM -> ActionColors.MediumBorder
                    BusinessActionPriority.LOW -> ActionColors.LowBorder
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(actionBg, RoundedCornerShape(12.dp))
                        .border(1.dp, actionBorder, RoundedCornerShape(12.dp))
                        .clickable { onActionClick(topAction) }
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
                            BusinessActionPriorityBadge(priority = topAction.priority)
                            BusinessActionCategoryBadge(category = topAction.category)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = topAction.actionLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BlueNavy
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = BlueNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = topAction.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    if (!topAction.targetName.isNullOrBlank()) {
                        Text(
                            text = topAction.targetName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueNavy
                        )
                    }

                    Text(
                        text = topAction.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    if (!topAction.supportingMetric.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = topAction.supportingMetric,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                BusinessActionEmptyState(
                    title = stringResource(R.string.str_no_business_actions),
                    message = stringResource(R.string.str_no_business_actions_desc)
                )
            }
        }
    }
}

/**
 * Standalone individual action item card used in full list screen.
 */
@Composable
fun BusinessActionItemCard(
    action: BusinessAction,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when (action.priority) {
        BusinessActionPriority.CRITICAL -> ActionColors.CriticalBg.copy(alpha = 0.3f)
        BusinessActionPriority.HIGH -> ActionColors.HighBg.copy(alpha = 0.3f)
        BusinessActionPriority.MEDIUM -> ActionColors.MediumBg.copy(alpha = 0.3f)
        BusinessActionPriority.LOW -> ActionColors.LowBg.copy(alpha = 0.5f)
    }
    val border = when (action.priority) {
        BusinessActionPriority.CRITICAL -> ActionColors.CriticalBorder
        BusinessActionPriority.HIGH -> ActionColors.HighBorder
        BusinessActionPriority.MEDIUM -> ActionColors.MediumBorder
        BusinessActionPriority.LOW -> ActionColors.LowBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable { onActionClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
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
                    BusinessActionPriorityBadge(priority = action.priority)
                    BusinessActionCategoryBadge(category = action.category)
                }

                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Text(
                        text = action.actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = action.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (!action.targetName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.targetName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueNavy
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = action.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (!action.supportingMetric.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = action.supportingMetric,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Reusable empty state display for business actions.
 */
@Composable
fun BusinessActionEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActionCountChip(
    label: String,
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
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
