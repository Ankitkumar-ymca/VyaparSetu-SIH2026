package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Product
import com.example.domain.model.Sale
import java.util.Locale

/**
 * Priority level across all command-center items.
 */
enum class CommandCenterPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    POSITIVE
}

/**
 * Core sections of the Unified Business Command Center.
 */
enum class CommandCenterSection {
    NEEDS_ATTENTION,
    ACTIONS,
    BUSINESS_INSIGHTS,
    OPPORTUNITIES
}

/**
 * Source domain intelligence origin for surfaced items.
 */
enum class CommandCenterSourceType {
    BUSINESS_ALERT,
    BUSINESS_ACTION,
    CROSS_BUSINESS_INSIGHT,
    COLLECTION_PRIORITY,
    PROFIT_EXPLANATION,
    MULTI_SIGNAL_CONVERGENCE,
    PROFIT_INTELLIGENCE
}

/**
 * Business entity scope for a command center item.
 */
enum class CommandCenterTargetType {
    PRODUCT,
    CUSTOMER,
    FINANCIAL,
    BUSINESS_WIDE
}

/**
 * Domain issue classification used for deterministic multi-signal consolidation.
 */
enum class CommandIssueDomain {
    STOCKOUT,
    DEAD_STOCK,
    PRICING,
    COLLECTION,
    CUSTOMER_ENGAGEMENT,
    WORKING_CAPITAL,
    MULTI_SIGNAL,
    GENERAL
}

/**
 * An actionable, unified command-center item synthesizing business intelligence signals.
 */
data class CommandCenterItem(
    val itemId: String,
    val priority: CommandCenterPriority,
    val section: CommandCenterSection,
    val sourceType: CommandCenterSourceType,
    val sourceId: String,
    val title: String,
    val description: String,
    val targetId: String? = null,
    val targetName: String? = null,
    val targetType: CommandCenterTargetType = CommandCenterTargetType.BUSINESS_WIDE,
    val supportingMetric: String? = null,
    val metricValue: Double? = null,
    val metricUnit: String? = null,
    val supportingSignals: List<String> = emptyList(),
    val destinationRoute: String? = null,
    val actionLabel: String? = null,
    val relatedAlertId: String? = null,
    val relatedActionId: String? = null,
    val relatedInsightId: String? = null,
    val contributingSources: List<CommandCenterSourceType> = emptyList()
)

/**
 * High-level counts and totals across the Unified Business Command Center.
 */
data class CommandCenterSummary(
    val totalItems: Int,
    val attentionCount: Int,
    val actionCount: Int,
    val insightCount: Int,
    val opportunityCount: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val positiveCount: Int,
    val workingCapitalLocked: Double = 0.0
)

/**
 * Data availability status for profit metrics in the Command Center.
 */
enum class ProfitDataAvailability {
    FULL,
    PARTIAL,
    NO_DATA
}

/**
 * Immutable profit intelligence snapshot for the Unified Business Command Center.
 *
 * Exposes authoritative financial numbers without duplicating calculations:
 * - Revenue, COGS, Gross Profit, Operating Expenses, Net Profit (from P&L / ProfitPeriodSnapshot)
 * - Gross Margin % and Net Margin % (derived with division-by-zero protection)
 * - Product / Stock Profit (from ProductProfitabilityEngine / ProductProfitabilitySummary)
 * - Data availability status and explicit period boundaries
 */
data class CommandCenterProfitSnapshot(
    val periodLabel: String,
    val revenue: Double,
    val cogs: Double,
    val grossProfit: Double,
    val operatingExpenses: Double,
    val netProfit: Double,
    val grossMarginPercent: Double,
    val netMarginPercent: Double,
    val productProfit: Double? = null,
    val dataAvailability: ProfitDataAvailability = ProfitDataAvailability.FULL
) {
    /**
     * Alias for productProfit representing Product / Stock Gross Profit (Sales Revenue - COGS of sold items),
     * derived from the authoritative [ProductProfitabilityEngine].
     */
    val stockProfit: Double?
        get() = productProfit

    /**
     * Whether stock/product gross profit metric is supported and available in this snapshot.
     */
    val isStockProfitSupported: Boolean
        get() = productProfit != null

    /**
     * Whether the business operated at a net loss in this period.
     */
    val isOperatingAtLoss: Boolean
        get() = netProfit < 0.0

    /**
     * Whether cost of goods sold exceeded revenue (negative gross margin).
     */
    val hasNegativeGrossMargin: Boolean
        get() = grossProfit < 0.0

    /**
     * Whether the business was profitable overall in this period.
     */
    val isProfitable: Boolean
        get() = netProfit > 0.0

    companion object {
        /**
         * Safe factory calculating margin percentages deterministically with zero-division
         * and NaN/Infinity guards.
         */
        fun create(
            periodLabel: String,
            revenue: Double,
            cogs: Double,
            operatingExpenses: Double = 0.0,
            grossProfit: Double? = null,
            netProfit: Double? = null,
            productProfit: Double? = null,
            dataAvailability: ProfitDataAvailability? = null
        ): CommandCenterProfitSnapshot {
            val safeRev = if (revenue.isFinite()) revenue else 0.0
            val safeCogs = if (cogs.isFinite()) cogs else 0.0
            val safeExp = if (operatingExpenses.isFinite()) operatingExpenses else 0.0

            val safeGross = grossProfit?.let { if (it.isFinite()) it else (safeRev - safeCogs) } ?: (safeRev - safeCogs)
            val safeNet = netProfit?.let { if (it.isFinite()) it else (safeGross - safeExp) } ?: (safeGross - safeExp)
            val safeProdProfit = productProfit?.let { if (it.isFinite()) it else null }

            val grossMargin = if (safeRev > 0.0) {
                val gm = (safeGross / safeRev) * 100.0
                if (gm.isFinite()) gm else 0.0
            } else 0.0

            val netMargin = if (safeRev > 0.0) {
                val nm = (safeNet / safeRev) * 100.0
                if (nm.isFinite()) nm else 0.0
            } else 0.0

            val availability = dataAvailability ?: when {
                safeRev == 0.0 && safeCogs == 0.0 && safeExp == 0.0 && (safeProdProfit == null || safeProdProfit == 0.0) -> ProfitDataAvailability.NO_DATA
                safeExp == 0.0 && safeProdProfit != null -> ProfitDataAvailability.PARTIAL
                else -> ProfitDataAvailability.FULL
            }

            return CommandCenterProfitSnapshot(
                periodLabel = periodLabel,
                revenue = safeRev,
                cogs = safeCogs,
                grossProfit = safeGross,
                operatingExpenses = safeExp,
                netProfit = safeNet,
                grossMarginPercent = grossMargin,
                netMarginPercent = netMargin,
                productProfit = safeProdProfit,
                dataAvailability = availability
            )
        }

        /**
         * Adapts authoritative ProfitPeriodSnapshot from ProfitExplainabilityEngine.
         */
        fun fromProfitPeriodSnapshot(
            snapshot: ProfitPeriodSnapshot,
            productProfit: Double? = null,
            includeProductProfitFromGross: Boolean = (productProfit == null)
        ): CommandCenterProfitSnapshot {
            val prodProfit = productProfit ?: if (includeProductProfitFromGross) snapshot.grossProfit else null
            return create(
                periodLabel = snapshot.periodLabel,
                revenue = snapshot.revenue,
                cogs = snapshot.cogs,
                grossProfit = snapshot.grossProfit,
                operatingExpenses = snapshot.operatingExpenses,
                netProfit = snapshot.netProfit,
                productProfit = prodProfit
            )
        }

        /**
         * Adapts authoritative ProductProfitabilitySummary from ProductProfitabilityEngine.
         */
        fun fromProductProfitabilitySummary(
            summary: ProductProfitabilitySummary,
            periodLabel: String = "Last 30 Days",
            operatingExpenses: Double = 0.0
        ): CommandCenterProfitSnapshot {
            val availability = if (summary.productCount == 0 || (summary.totalRevenue == 0.0 && summary.totalCostOfGoodsSold == 0.0)) {
                ProfitDataAvailability.NO_DATA
            } else if (operatingExpenses == 0.0) {
                ProfitDataAvailability.PARTIAL
            } else {
                ProfitDataAvailability.FULL
            }

            return create(
                periodLabel = periodLabel,
                revenue = summary.totalRevenue,
                cogs = summary.totalCostOfGoodsSold,
                grossProfit = summary.totalGrossProfit,
                operatingExpenses = operatingExpenses,
                netProfit = summary.totalGrossProfit - operatingExpenses,
                productProfit = summary.totalGrossProfit,
                dataAvailability = availability
            )
        }
    }
}

/**
 * Complete immutable output of the Unified Business Command Center Engine.
 */
data class UnifiedBusinessCommandCenter(
    val attentionItems: List<CommandCenterItem>,
    val actions: List<CommandCenterItem>,
    val insights: List<CommandCenterItem>,
    val opportunities: List<CommandCenterItem>,
    val summary: CommandCenterSummary,
    val workingCapitalLockedTotal: Double? = null,
    val profitExplanationSummary: String? = null,
    val profitSnapshot: CommandCenterProfitSnapshot? = null,
    val allItems: List<CommandCenterItem>
)

/**
 * Pure, deterministic Unified Business Command Center Engine for VyaparSetu.
 *
 * Orchestrates existing intelligence outputs across:
 * - Business Alerts (BusinessAlertEngine)
 * - Business Actions (BusinessActionEngine)
 * - Cross-Business Insights (CrossBusinessInsightsEngine)
 * - Collection Priorities (CollectionPriorityEngine)
 * - Profit Driver Explanations (ProfitExplainabilityEngine)
 *
 * Rules:
 * - Pure Kotlin domain orchestration layer.
 * - Deterministic, offline, zero Gemini/network calls, zero database writes.
 * - Does NOT recalculate financial truth, margins, stock velocity, or credit risk.
 * - Reuses existing intelligence results.
 * - Safe against NaN/Infinity and empty/null data.
 * - Provides intelligent deterministic deduplication and consolidation of multi-engine signals.
 */
object UnifiedBusinessCommandCenterEngine {

    /**
     * Primary orchestration API accepting pre-computed intelligence models.
     */
    fun generate(
        businessAlerts: List<BusinessAlert> = emptyList(),
        businessActions: List<BusinessAction> = emptyList(),
        crossBusinessInsights: List<CrossBusinessInsight> = emptyList(),
        collectionResults: List<CollectionPriorityResult> = emptyList(),
        profitExplanation: ProfitChangeExplanation? = null,
        workingCapitalLockedTotal: Double? = null,
        profitSnapshot: CommandCenterProfitSnapshot? = null
    ): UnifiedBusinessCommandCenter {

        // 1. Deduplicate identical source records by ID
        val distinctAlerts = businessAlerts.distinctBy { it.alertId }
        val distinctActions = businessActions.distinctBy { it.actionId }
        val distinctInsights = crossBusinessInsights.distinctBy { it.id }
        val distinctCollections = collectionResults.distinctBy { it.customerId }

        // 2. Build complete ACTIONS section directly from existing BusinessActions
        val actionsSectionItems = distinctActions.map { action ->
            val priority = mapActionPriority(action.priority)
            CommandCenterItem(
                itemId = "action_${action.actionId}",
                priority = priority,
                section = CommandCenterSection.ACTIONS,
                sourceType = CommandCenterSourceType.BUSINESS_ACTION,
                sourceId = action.actionId,
                title = action.title,
                description = action.description,
                targetId = action.targetId,
                targetName = action.targetName,
                targetType = resolveActionTargetType(action),
                supportingMetric = action.supportingMetric,
                metricValue = safeValue(action.metricValue),
                metricUnit = action.metricUnit,
                supportingSignals = listOfNotNull(action.supportingMetric?.let { "$it: ${formatMetric(action.metricValue, action.metricUnit)}" }),
                destinationRoute = action.destinationRoute,
                actionLabel = action.actionLabel,
                relatedActionId = action.actionId,
                contributingSources = listOf(CommandCenterSourceType.BUSINESS_ACTION)
            )
        }.sortedWith(itemComparator)

        // 3. Build BUSINESS_INSIGHTS and OPPORTUNITIES sections from CrossBusinessInsights
        val insightsSectionItems = mutableListOf<CommandCenterItem>()
        val opportunitiesSectionItems = mutableListOf<CommandCenterItem>()

        for (insight in distinctInsights) {
            val isOpportunity = insight.category == CrossInsightCategory.STAR_PERFORMER ||
                    insight.severity == CrossInsightSeverity.POSITIVE
            val priority = mapInsightSeverity(insight.severity)
            val section = if (isOpportunity) CommandCenterSection.OPPORTUNITIES else CommandCenterSection.BUSINESS_INSIGHTS

            val item = CommandCenterItem(
                itemId = "insight_${insight.id}",
                priority = priority,
                section = section,
                sourceType = if (insight.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE) {
                    CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE
                } else {
                    CommandCenterSourceType.CROSS_BUSINESS_INSIGHT
                },
                sourceId = insight.id,
                title = insight.title,
                description = insight.summary,
                targetId = insight.entityId,
                targetName = insight.entityName,
                targetType = mapInsightEntityType(insight.entityType),
                supportingMetric = insight.metricUnit,
                metricValue = safeValue(insight.metricValue),
                metricUnit = insight.metricUnit,
                supportingSignals = insight.contributingSignals,
                destinationRoute = resolveInsightRoute(insight),
                actionLabel = if (isOpportunity) "View Performer" else "View Insight",
                relatedInsightId = insight.id,
                contributingSources = listOf(CommandCenterSourceType.CROSS_BUSINESS_INSIGHT)
            )

            if (isOpportunity) {
                opportunitiesSectionItems.add(item)
            } else {
                insightsSectionItems.add(item)
            }
        }

        // Add positive profit driver explanation to OPPORTUNITIES if expansion occurred
        if (profitExplanation != null && profitExplanation.changeDirection == ProfitChangeDirection.EXPANSION && isValidNumber(profitExplanation.netProfitChange) && profitExplanation.netProfitChange > 0.0) {
            opportunitiesSectionItems.add(
                CommandCenterItem(
                    itemId = "profit_expansion_${sanitizeId(profitExplanation.currentPeriodLabel)}",
                    priority = CommandCenterPriority.POSITIVE,
                    section = CommandCenterSection.OPPORTUNITIES,
                    sourceType = CommandCenterSourceType.PROFIT_EXPLANATION,
                    sourceId = profitExplanation.currentPeriodLabel,
                    title = "Profit Growth: ${profitExplanation.currentPeriodLabel}",
                    description = profitExplanation.primaryDriverSummary,
                    targetType = CommandCenterTargetType.FINANCIAL,
                    supportingMetric = "Net Profit Change",
                    metricValue = safeValue(profitExplanation.netProfitChange),
                    metricUnit = "₹",
                    supportingSignals = profitExplanation.notes,
                    destinationRoute = "profit_explainability",
                    actionLabel = "View P&L Analysis",
                    contributingSources = listOf(CommandCenterSourceType.PROFIT_EXPLANATION)
                )
            )
        }

        val sortedInsights = insightsSectionItems.sortedWith(itemComparator)
        val sortedOpportunities = opportunitiesSectionItems.sortedWith(itemComparator)

        // 4. Build NEEDS_ATTENTION section:
        // Identify signals that genuinely require immediate owner attention:
        // - All alerts (CRITICAL and WARNING)
        // - High collection review accounts
        // - Multi-signal consolidated clusters (e.g. alert + action on same entity and issue)
        val (attentionItems, consumedActionIds, consumedInsightIds) = buildAttentionSection(
            distinctAlerts = distinctAlerts,
            distinctActions = distinctActions,
            distinctInsights = distinctInsights,
            distinctCollections = distinctCollections
        )

        // 5. Construct unified allItems list with zero redundant duplication:
        // - Consolidated/Alert items in NEEDS_ATTENTION
        // - Non-consolidated actions in ACTIONS
        // - Non-consolidated insights in BUSINESS_INSIGHTS
        // - Opportunities in OPPORTUNITIES
        val unabsorbedActions = actionsSectionItems.filterNot { it.sourceId in consumedActionIds }
        val unabsorbedInsights = sortedInsights.filterNot { it.sourceId in consumedInsightIds }

        val allItems = (attentionItems + unabsorbedActions + unabsorbedInsights + sortedOpportunities).sortedWith(itemComparator)

        // 6. Resolve working capital locked total
        val resolvedWorkingCapital = when {
            isValidNumber(workingCapitalLockedTotal) && workingCapitalLockedTotal!! > 0.0 -> workingCapitalLockedTotal
            else -> {
                val insightVal = distinctInsights.find { it.category == CrossInsightCategory.WORKING_CAPITAL_STRAIN }?.metricValue
                if (isValidNumber(insightVal)) insightVal else 0.0
            }
        }

        val summary = CommandCenterSummary(
            totalItems = allItems.size,
            attentionCount = attentionItems.size,
            actionCount = actionsSectionItems.size,
            insightCount = sortedInsights.size,
            opportunityCount = sortedOpportunities.size,
            criticalCount = allItems.count { it.priority == CommandCenterPriority.CRITICAL },
            highCount = allItems.count { it.priority == CommandCenterPriority.HIGH },
            mediumCount = allItems.count { it.priority == CommandCenterPriority.MEDIUM },
            lowCount = allItems.count { it.priority == CommandCenterPriority.LOW },
            positiveCount = allItems.count { it.priority == CommandCenterPriority.POSITIVE },
            workingCapitalLocked = resolvedWorkingCapital ?: 0.0
        )

        return UnifiedBusinessCommandCenter(
            attentionItems = attentionItems,
            actions = actionsSectionItems,
            insights = sortedInsights,
            opportunities = sortedOpportunities,
            summary = summary,
            workingCapitalLockedTotal = if (resolvedWorkingCapital != null && resolvedWorkingCapital > 0.0) resolvedWorkingCapital else null,
            profitExplanationSummary = profitExplanation?.primaryDriverSummary,
            profitSnapshot = profitSnapshot,
            allItems = allItems
        )
    }

    /**
     * Convenience delegation API accepting raw domain records and executing pre-existing engines.
     * Guaranteed zero recalculation of financial truth.
     */
    fun generateFromRawModels(
        products: List<Product> = emptyList(),
        sales: List<Sale> = emptyList(),
        customers: List<Customer> = emptyList(),
        currentTime: Long = System.currentTimeMillis(),
        profitSnapshot: CommandCenterProfitSnapshot? = null
    ): UnifiedBusinessCommandCenter {
        val inventorySummary = if (products.isNotEmpty()) {
            InventoryIntelligenceEngine.analyze(products, sales, currentTime)
        } else null

        val profitabilitySummary = if (products.isNotEmpty()) {
            ProductProfitabilityEngine.analyze(products, sales, referenceTimeMs = currentTime)
        } else null

        val customerSummary = if (customers.isNotEmpty()) {
            CustomerIntelligenceEngine.analyze(customers = customers, sales = sales, currentTime = currentTime)
        } else null

        val collectionSummary = if (customerSummary != null) {
            CollectionPriorityEngine.evaluateFromMetrics(customerSummary.customers)
        } else if (customers.isNotEmpty()) {
            CollectionPriorityEngine.evaluate(customers, sales, currentTime)
        } else null

        val businessAlerts = if (inventorySummary != null || profitabilitySummary != null) {
            BusinessAlertEngine.generateAlerts(
                inventorySummary = inventorySummary,
                profitabilitySummary = profitabilitySummary
            )
        } else emptyList()

        val businessActions = BusinessActionEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            businessAlerts = businessAlerts
        ).allActions

        val crossBusinessSummary = CrossBusinessInsightsEngine.generateFromIntelligence(
            inventorySummary = inventorySummary,
            inventoryMetrics = inventorySummary?.products ?: emptyList(),
            profitabilitySummary = profitabilitySummary,
            profitabilityMetrics = profitabilitySummary?.products ?: emptyList(),
            customerSummary = customerSummary,
            customerMetrics = customerSummary?.customers ?: emptyList(),
            collectionSummary = collectionSummary,
            collectionResults = collectionSummary?.results ?: emptyList(),
            businessAlerts = businessAlerts,
            businessActions = businessActions
        )

        val resolvedProfitSnapshot = profitSnapshot ?: if (profitabilitySummary != null) {
            CommandCenterProfitSnapshot.fromProductProfitabilitySummary(profitabilitySummary)
        } else null

        return generate(
            businessAlerts = businessAlerts,
            businessActions = businessActions,
            crossBusinessInsights = crossBusinessSummary.insights,
            collectionResults = collectionSummary?.results ?: emptyList(),
            workingCapitalLockedTotal = crossBusinessSummary.workingCapitalLockedTotal,
            profitSnapshot = resolvedProfitSnapshot
        )
    }

    // ==========================================
    // ATTENTION SECTION BUILDER & DEDUPLICATION
    // ==========================================

    private data class AttentionResult(
        val items: List<CommandCenterItem>,
        val consumedActionIds: Set<String>,
        val consumedInsightIds: Set<String>
    )

    private data class CandidateAttentionItem(
        val priority: CommandCenterPriority,
        val sourceType: CommandCenterSourceType,
        val sourceId: String,
        val title: String,
        val description: String,
        val targetId: String?,
        val targetName: String?,
        val targetType: CommandCenterTargetType,
        val issueDomain: CommandIssueDomain,
        val supportingMetric: String? = null,
        val metricValue: Double? = null,
        val metricUnit: String? = null,
        val signals: List<String> = emptyList(),
        val destinationRoute: String? = null,
        val actionLabel: String? = null,
        val relatedAlertId: String? = null,
        val relatedActionId: String? = null,
        val relatedInsightId: String? = null
    )

    private fun buildAttentionSection(
        distinctAlerts: List<BusinessAlert>,
        distinctActions: List<BusinessAction>,
        distinctInsights: List<CrossBusinessInsight>,
        distinctCollections: List<CollectionPriorityResult>
    ): AttentionResult {
        val candidates = mutableListOf<CandidateAttentionItem>()

        // 1. All alerts belong to attention (CRITICAL -> CRITICAL, WARNING -> HIGH, INFO -> LOW)
        for (alert in distinctAlerts) {
            val priority = when (alert.severity) {
                AlertSeverity.CRITICAL -> CommandCenterPriority.CRITICAL
                AlertSeverity.WARNING -> CommandCenterPriority.HIGH
                AlertSeverity.INFO -> CommandCenterPriority.LOW
            }
            candidates.add(
                CandidateAttentionItem(
                    priority = priority,
                    sourceType = CommandCenterSourceType.BUSINESS_ALERT,
                    sourceId = alert.alertId,
                    title = alert.title,
                    description = alert.description,
                    targetId = alert.productId,
                    targetName = alert.productName,
                    targetType = if (alert.productId != null) CommandCenterTargetType.PRODUCT else CommandCenterTargetType.BUSINESS_WIDE,
                    issueDomain = classifyAlertIssueDomain(alert),
                    supportingMetric = alert.supportingMetric,
                    metricValue = safeValue(alert.metricValue),
                    metricUnit = alert.unit,
                    signals = listOf("Alert: ${alert.title}"),
                    destinationRoute = if (alert.productId != null) "product_detail/${alert.productId}" else "business_alerts",
                    actionLabel = "View Alert",
                    relatedAlertId = alert.alertId
                )
            )
        }

        // 2. High collection review accounts belong to attention
        for (coll in distinctCollections) {
            if (coll.priority == CollectionPriorityLevel.HIGH) {
                candidates.add(
                    CandidateAttentionItem(
                        priority = CommandCenterPriority.HIGH,
                        sourceType = CommandCenterSourceType.COLLECTION_PRIORITY,
                        sourceId = "coll_${coll.customerId}",
                        title = "High Collection Priority: ${coll.customerName}",
                        description = coll.reasons.firstOrNull() ?: "Substantial outstanding balance requires review.",
                        targetId = coll.customerId,
                        targetName = coll.customerName,
                        targetType = CommandCenterTargetType.CUSTOMER,
                        issueDomain = CommandIssueDomain.COLLECTION,
                        supportingMetric = "Outstanding",
                        metricValue = safeValue(coll.outstandingAmount),
                        metricUnit = "₹",
                        signals = coll.reasons,
                        destinationRoute = "customer_detail/${coll.customerId}",
                        actionLabel = "View Khata"
                    )
                )
            }
        }

        // 3. Actions that are CRITICAL or match an existing alert/collection domain on the same target
        // For consolidation: find if any action matches a target and issue domain of an alert or collection
        val alertTargetDomains = candidates.filter { it.targetId != null }.map { Pair(it.targetId!!, it.issueDomain) }.toSet()

        val matchingActions = distinctActions.filter { action ->
            action.priority == BusinessActionPriority.CRITICAL ||
                    action.priority == BusinessActionPriority.HIGH ||
                    (action.targetId != null && Pair(action.targetId, classifyActionIssueDomain(action)) in alertTargetDomains)
        }

        for (action in matchingActions) {
            val priority = mapActionPriority(action.priority)
            candidates.add(
                CandidateAttentionItem(
                    priority = priority,
                    sourceType = CommandCenterSourceType.BUSINESS_ACTION,
                    sourceId = action.actionId,
                    title = action.title,
                    description = action.description,
                    targetId = action.targetId,
                    targetName = action.targetName,
                    targetType = resolveActionTargetType(action),
                    issueDomain = classifyActionIssueDomain(action),
                    supportingMetric = action.supportingMetric,
                    metricValue = safeValue(action.metricValue),
                    metricUnit = action.metricUnit,
                    signals = listOf("Action: ${action.title}"),
                    destinationRoute = action.destinationRoute,
                    actionLabel = action.actionLabel,
                    relatedActionId = action.actionId
                )
            )
        }

        // 4. Insights that are CRITICAL or match an existing alert/action domain on the same target
        val attentionTargetDomains = candidates.filter { it.targetId != null }.map { Pair(it.targetId!!, it.issueDomain) }.toSet()

        val matchingInsights = distinctInsights.filter { insight ->
            val isOpportunity = insight.category == CrossInsightCategory.STAR_PERFORMER || insight.severity == CrossInsightSeverity.POSITIVE
            !isOpportunity && (insight.severity == CrossInsightSeverity.CRITICAL ||
                    insight.severity == CrossInsightSeverity.HIGH ||
                    (insight.entityId != null && Pair(insight.entityId, classifyInsightIssueDomain(insight)) in attentionTargetDomains))
        }

        for (insight in matchingInsights) {
            candidates.add(
                CandidateAttentionItem(
                    priority = mapInsightSeverity(insight.severity),
                    sourceType = if (insight.category == CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE) {
                        CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE
                    } else {
                        CommandCenterSourceType.CROSS_BUSINESS_INSIGHT
                    },
                    sourceId = insight.id,
                    title = insight.title,
                    description = insight.summary,
                    targetId = insight.entityId,
                    targetName = insight.entityName,
                    targetType = mapInsightEntityType(insight.entityType),
                    issueDomain = classifyInsightIssueDomain(insight),
                    supportingMetric = insight.metricUnit,
                    metricValue = safeValue(insight.metricValue),
                    metricUnit = insight.metricUnit,
                    signals = insight.contributingSignals,
                    destinationRoute = resolveInsightRoute(insight),
                    actionLabel = "View Insight",
                    relatedInsightId = insight.id
                )
            )
        }

        // 5. Group by (targetType, targetId, issueDomain) for consolidation
        val groupedWithTarget = candidates.filter { it.targetId != null }.groupBy { Triple(it.targetType, it.targetId!!, it.issueDomain) }
        val itemsWithoutTarget = candidates.filter { it.targetId == null }

        val consolidatedItems = mutableListOf<CommandCenterItem>()
        val consumedActionIds = mutableSetOf<String>()
        val consumedInsightIds = mutableSetOf<String>()

        for ((triple, group) in groupedWithTarget) {
            val (targetType, targetId, issueDomain) = triple

            if (group.size == 1) {
                val single = group.first()
                if (single.sourceType == CommandCenterSourceType.BUSINESS_ACTION) {
                    consumedActionIds.add(single.sourceId)
                }
                if (single.sourceType == CommandCenterSourceType.CROSS_BUSINESS_INSIGHT || single.sourceType == CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE) {
                    consumedInsightIds.add(single.sourceId)
                }
                consolidatedItems.add(
                    CommandCenterItem(
                        itemId = "${single.sourceType.name.lowercase()}_${sanitizeId(single.sourceId)}",
                        priority = single.priority,
                        section = CommandCenterSection.NEEDS_ATTENTION,
                        sourceType = single.sourceType,
                        sourceId = single.sourceId,
                        title = single.title,
                        description = single.description,
                        targetId = single.targetId,
                        targetName = single.targetName,
                        targetType = single.targetType,
                        supportingMetric = single.supportingMetric,
                        metricValue = single.metricValue,
                        metricUnit = single.metricUnit,
                        supportingSignals = single.signals,
                        destinationRoute = single.destinationRoute,
                        actionLabel = single.actionLabel,
                        relatedAlertId = single.relatedAlertId,
                        relatedActionId = single.relatedActionId,
                        relatedInsightId = single.relatedInsightId,
                        contributingSources = listOf(single.sourceType)
                    )
                )
            } else {
                // Multi-signal consolidation on the same target and issue domain
                val highestPriority = if (group.any { it.priority == CommandCenterPriority.CRITICAL }) {
                    CommandCenterPriority.CRITICAL
                } else {
                    CommandCenterPriority.HIGH
                }

                val preferredPrimary = group.find { it.sourceType == CommandCenterSourceType.CROSS_BUSINESS_INSIGHT || it.sourceType == CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE }
                    ?: group.find { it.sourceType == CommandCenterSourceType.BUSINESS_ACTION }
                    ?: group.first()

                val actionItem = group.find { it.sourceType == CommandCenterSourceType.BUSINESS_ACTION }
                val alertItem = group.find { it.sourceType == CommandCenterSourceType.BUSINESS_ALERT }
                val insightItem = group.find { it.sourceType == CommandCenterSourceType.CROSS_BUSINESS_INSIGHT || it.sourceType == CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE }

                if (actionItem != null) consumedActionIds.add(actionItem.sourceId)
                if (insightItem != null) consumedInsightIds.add(insightItem.sourceId)

                val allSignals = group.flatMap { it.signals }.distinct()
                val contributingSources = group.map { it.sourceType }.distinct()

                consolidatedItems.add(
                    CommandCenterItem(
                        itemId = "unified_${targetType.name.lowercase()}_${sanitizeId(targetId)}_${issueDomain.name.lowercase()}",
                        priority = highestPriority,
                        section = CommandCenterSection.NEEDS_ATTENTION,
                        sourceType = if (insightItem != null) {
                            insightItem.sourceType
                        } else {
                            CommandCenterSourceType.MULTI_SIGNAL_CONVERGENCE
                        },
                        sourceId = preferredPrimary.sourceId,
                        title = preferredPrimary.title,
                        description = preferredPrimary.description,
                        targetId = targetId,
                        targetName = preferredPrimary.targetName,
                        targetType = targetType,
                        supportingMetric = preferredPrimary.supportingMetric ?: actionItem?.supportingMetric ?: alertItem?.supportingMetric,
                        metricValue = preferredPrimary.metricValue ?: actionItem?.metricValue ?: alertItem?.metricValue,
                        metricUnit = preferredPrimary.metricUnit ?: actionItem?.metricUnit ?: alertItem?.metricUnit,
                        supportingSignals = allSignals,
                        destinationRoute = actionItem?.destinationRoute ?: preferredPrimary.destinationRoute,
                        actionLabel = actionItem?.actionLabel ?: preferredPrimary.actionLabel,
                        relatedAlertId = alertItem?.relatedAlertId,
                        relatedActionId = actionItem?.relatedActionId,
                        relatedInsightId = insightItem?.relatedInsightId,
                        contributingSources = contributingSources
                    )
                )
            }
        }

        // Add items without targetId directly
        for (item in itemsWithoutTarget) {
            if (item.sourceType == CommandCenterSourceType.CROSS_BUSINESS_INSIGHT) {
                consumedInsightIds.add(item.sourceId)
            }
            consolidatedItems.add(
                CommandCenterItem(
                    itemId = "${item.sourceType.name.lowercase()}_${sanitizeId(item.sourceId)}",
                    priority = item.priority,
                    section = CommandCenterSection.NEEDS_ATTENTION,
                    sourceType = item.sourceType,
                    sourceId = item.sourceId,
                    title = item.title,
                    description = item.description,
                    targetId = null,
                    targetName = null,
                    targetType = item.targetType,
                    supportingMetric = item.supportingMetric,
                    metricValue = item.metricValue,
                    metricUnit = item.metricUnit,
                    supportingSignals = item.signals,
                    destinationRoute = item.destinationRoute,
                    actionLabel = item.actionLabel,
                    relatedAlertId = item.relatedAlertId,
                    relatedActionId = item.relatedActionId,
                    relatedInsightId = item.relatedInsightId,
                    contributingSources = listOf(item.sourceType)
                )
            )
        }

        return AttentionResult(
            items = consolidatedItems.sortedWith(itemComparator),
            consumedActionIds = consumedActionIds,
            consumedInsightIds = consumedInsightIds
        )
    }

    // ==========================================
    // HELPERS & MAPPINGS
    // ==========================================

    private fun mapActionPriority(p: BusinessActionPriority): CommandCenterPriority {
        return when (p) {
            BusinessActionPriority.CRITICAL -> CommandCenterPriority.CRITICAL
            BusinessActionPriority.HIGH -> CommandCenterPriority.HIGH
            BusinessActionPriority.MEDIUM -> CommandCenterPriority.MEDIUM
            BusinessActionPriority.LOW -> CommandCenterPriority.LOW
        }
    }

    private fun mapInsightSeverity(s: CrossInsightSeverity): CommandCenterPriority {
        return when (s) {
            CrossInsightSeverity.CRITICAL -> CommandCenterPriority.CRITICAL
            CrossInsightSeverity.HIGH -> CommandCenterPriority.HIGH
            CrossInsightSeverity.MEDIUM -> CommandCenterPriority.MEDIUM
            CrossInsightSeverity.POSITIVE -> CommandCenterPriority.POSITIVE
        }
    }

    private fun mapInsightEntityType(t: CrossInsightEntityType): CommandCenterTargetType {
        return when (t) {
            CrossInsightEntityType.PRODUCT -> CommandCenterTargetType.PRODUCT
            CrossInsightEntityType.CUSTOMER -> CommandCenterTargetType.CUSTOMER
            CrossInsightEntityType.BUSINESS_WIDE -> CommandCenterTargetType.BUSINESS_WIDE
        }
    }

    private fun resolveActionTargetType(action: BusinessAction): CommandCenterTargetType {
        return when (action.category) {
            BusinessActionCategory.RESTOCK,
            BusinessActionCategory.CLEAR_STOCK,
            BusinessActionCategory.REVIEW_PRICING -> CommandCenterTargetType.PRODUCT
            BusinessActionCategory.COLLECT_PAYMENT,
            BusinessActionCategory.REENGAGE_CUSTOMER -> CommandCenterTargetType.CUSTOMER
        }
    }

    private fun resolveInsightRoute(insight: CrossBusinessInsight): String {
        return when (insight.entityType) {
            CrossInsightEntityType.PRODUCT -> if (insight.entityId != null) "product_detail/${insight.entityId}" else "business_insights"
            CrossInsightEntityType.CUSTOMER -> if (insight.entityId != null) "customer_detail/${insight.entityId}" else "business_insights"
            CrossInsightEntityType.BUSINESS_WIDE -> "business_insights"
        }
    }

    private fun classifyAlertIssueDomain(alert: BusinessAlert): CommandIssueDomain {
        val lowerTitle = alert.title.lowercase(Locale.ROOT)
        return when {
            alert.category == AlertCategory.INVENTORY && (lowerTitle.contains("stockout") || lowerTitle.contains("low stock") || lowerTitle.contains("zero")) -> CommandIssueDomain.STOCKOUT
            lowerTitle.contains("dead") -> CommandIssueDomain.DEAD_STOCK
            alert.category == AlertCategory.PROFITABILITY -> CommandIssueDomain.PRICING
            alert.category == AlertCategory.CASH_FLOW -> CommandIssueDomain.COLLECTION
            else -> CommandIssueDomain.GENERAL
        }
    }

    private fun classifyActionIssueDomain(action: BusinessAction): CommandIssueDomain {
        return when (action.category) {
            BusinessActionCategory.RESTOCK -> CommandIssueDomain.STOCKOUT
            BusinessActionCategory.CLEAR_STOCK -> CommandIssueDomain.DEAD_STOCK
            BusinessActionCategory.REVIEW_PRICING -> CommandIssueDomain.PRICING
            BusinessActionCategory.COLLECT_PAYMENT -> CommandIssueDomain.COLLECTION
            BusinessActionCategory.REENGAGE_CUSTOMER -> CommandIssueDomain.CUSTOMER_ENGAGEMENT
        }
    }

    private fun classifyInsightIssueDomain(insight: CrossBusinessInsight): CommandIssueDomain {
        return when (insight.category) {
            CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK -> CommandIssueDomain.STOCKOUT
            CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP -> CommandIssueDomain.DEAD_STOCK
            CrossInsightCategory.VOLUME_MARGIN_VULNERABILITY -> CommandIssueDomain.PRICING
            CrossInsightCategory.KEY_ACCOUNT_EXPOSURE,
            CrossInsightCategory.DORMANT_DEBT_RISK -> CommandIssueDomain.COLLECTION
            CrossInsightCategory.WORKING_CAPITAL_STRAIN -> CommandIssueDomain.WORKING_CAPITAL
            CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE -> CommandIssueDomain.MULTI_SIGNAL
            CrossInsightCategory.STAR_PERFORMER -> CommandIssueDomain.GENERAL
        }
    }

    private fun sanitizeId(id: String): String {
        return id.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }

    private fun isValidNumber(v: Double?): Boolean {
        return v != null && !v.isNaN() && !v.isInfinite()
    }

    private fun safeValue(v: Double?): Double? {
        return if (isValidNumber(v)) v else null
    }

    private fun formatMetric(value: Double?, unit: String?): String {
        if (!isValidNumber(value)) return "N/A"
        val formatted = String.format(Locale.US, "%.1f", value)
        return if (!unit.isNullOrBlank()) "$formatted $unit" else formatted
    }

    /**
     * Deterministic tie-breaking comparator for CommandCenterItem:
     * 1. Priority: CRITICAL (0), HIGH (1), MEDIUM (2), LOW (3), POSITIVE (4)
     * 2. Section: NEEDS_ATTENTION (0), ACTIONS (1), BUSINESS_INSIGHTS (2), OPPORTUNITIES (3)
     * 3. SourceType name
     * 4. Target Name or Target ID
     * 5. Item ID
     */
    private val itemComparator = compareBy<CommandCenterItem> { item ->
        when (item.priority) {
            CommandCenterPriority.CRITICAL -> 0
            CommandCenterPriority.HIGH -> 1
            CommandCenterPriority.MEDIUM -> 2
            CommandCenterPriority.LOW -> 3
            CommandCenterPriority.POSITIVE -> 4
        }
    }.thenBy { item ->
        when (item.section) {
            CommandCenterSection.NEEDS_ATTENTION -> 0
            CommandCenterSection.ACTIONS -> 1
            CommandCenterSection.BUSINESS_INSIGHTS -> 2
            CommandCenterSection.OPPORTUNITIES -> 3
        }
    }.thenBy { item ->
        item.sourceType.name
    }.thenBy { item ->
        item.targetName ?: item.targetId ?: ""
    }.thenBy { item ->
        item.itemId
    }
}
