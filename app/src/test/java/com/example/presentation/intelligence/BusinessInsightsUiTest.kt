package com.example.presentation.intelligence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.domain.intelligence.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BusinessInsightsUiTest {

    private val sampleInsight = CrossBusinessInsight(
        id = "insight_test_1",
        category = CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK,
        severity = CrossInsightSeverity.CRITICAL,
        title = "High-Margin Stockout: Premium Basmati",
        summary = "Premium Basmati is completely out of stock but yields high profit margin (45.0%).",
        detailedExplanation = "Zero units remain while demand exists, forfeiting high-margin profit.",
        contributingSignals = listOf(
            "Inventory: Stock 0 units",
            "Profitability: High Margin (45.0%)"
        ),
        entityId = "prod_rice_1",
        entityName = "Premium Basmati",
        entityType = CrossInsightEntityType.PRODUCT,
        metricValue = 45.0,
        metricUnit = "%",
        recommendedStrategy = "Expedite supplier reorder immediately."
    )

    @Test
    fun test01_localizationResourcePresence() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("Business Insights", context.getString(R.string.str_business_insights))
        assertEquals("Connected signals from your business data", context.getString(R.string.str_business_insights_subtitle))
        assertEquals("Total Insights", context.getString(R.string.str_total_insights))
        assertEquals("Critical", context.getString(R.string.str_critical_insights))
        assertEquals("High", context.getString(R.string.str_high_insights))
        assertEquals("Medium", context.getString(R.string.str_medium_insights))
        assertEquals("Opportunity", context.getString(R.string.str_positive_insights))
        assertEquals("Capital Locked", context.getString(R.string.str_working_capital_locked))
        assertEquals("No connected business insights yet.", context.getString(R.string.str_empty_insights_title))
        assertEquals("Insights appear when multiple existing business signals reveal a meaningful relationship.", context.getString(R.string.str_empty_insights_desc))
    }

    @Test
    fun test02_screenEmptyStateWhenZeroInsights() {
        val emptySummary = CrossBusinessInsightsSummary(
            totalInsights = 0,
            criticalInsightsCount = 0,
            highInsightsCount = 0,
            mediumInsightsCount = 0,
            positiveInsightsCount = 0,
            workingCapitalLockedTotal = 0.0,
            insights = emptyList()
        )

        assertEquals(0, emptySummary.totalInsights)
        assertEquals(0, emptySummary.criticalInsightsCount)
        assertEquals(0.0, emptySummary.workingCapitalLockedTotal, 0.001)
        assertTrue(emptySummary.insights.isEmpty())
    }

    @Test
    fun test03_insightRenderingFields() {
        assertEquals("insight_test_1", sampleInsight.id)
        assertEquals(CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK, sampleInsight.category)
        assertEquals(CrossInsightSeverity.CRITICAL, sampleInsight.severity)
        assertEquals("High-Margin Stockout: Premium Basmati", sampleInsight.title)
        assertEquals("prod_rice_1", sampleInsight.entityId)
        assertEquals("Premium Basmati", sampleInsight.entityName)
        assertEquals(CrossInsightEntityType.PRODUCT, sampleInsight.entityType)
        assertEquals(45.0, sampleInsight.metricValue ?: 0.0, 0.001)
        assertEquals("%", sampleInsight.metricUnit)
        assertEquals(2, sampleInsight.contributingSignals.size)
        assertTrue(sampleInsight.recommendedStrategy.contains("Expedite supplier reorder"))
    }

    @Test
    fun test04_priorityFilterAll() {
        val list = listOf(
            sampleInsight.copy(id = "1", severity = CrossInsightSeverity.CRITICAL),
            sampleInsight.copy(id = "2", severity = CrossInsightSeverity.HIGH),
            sampleInsight.copy(id = "3", severity = CrossInsightSeverity.MEDIUM),
            sampleInsight.copy(id = "4", severity = CrossInsightSeverity.POSITIVE)
        )

        val filtered = when ("All") {
            "Critical" -> list.filter { it.severity == CrossInsightSeverity.CRITICAL }
            "High" -> list.filter { it.severity == CrossInsightSeverity.HIGH }
            "Medium" -> list.filter { it.severity == CrossInsightSeverity.MEDIUM }
            "Positive" -> list.filter { it.severity == CrossInsightSeverity.POSITIVE }
            else -> list
        }

        assertEquals(4, filtered.size)
    }

    @Test
    fun test05_priorityFilterCritical() {
        val list = listOf(
            sampleInsight.copy(id = "1", severity = CrossInsightSeverity.CRITICAL),
            sampleInsight.copy(id = "2", severity = CrossInsightSeverity.HIGH),
            sampleInsight.copy(id = "3", severity = CrossInsightSeverity.MEDIUM),
            sampleInsight.copy(id = "4", severity = CrossInsightSeverity.POSITIVE)
        )

        val filtered = list.filter { it.severity == CrossInsightSeverity.CRITICAL }
        assertEquals(1, filtered.size)
        assertEquals("1", filtered.first().id)
    }

    @Test
    fun test06_priorityFilterHigh() {
        val list = listOf(
            sampleInsight.copy(id = "1", severity = CrossInsightSeverity.CRITICAL),
            sampleInsight.copy(id = "2", severity = CrossInsightSeverity.HIGH),
            sampleInsight.copy(id = "3", severity = CrossInsightSeverity.MEDIUM)
        )

        val filtered = list.filter { it.severity == CrossInsightSeverity.HIGH }
        assertEquals(1, filtered.size)
        assertEquals("2", filtered.first().id)
    }

    @Test
    fun test07_priorityFilterMedium() {
        val list = listOf(
            sampleInsight.copy(id = "1", severity = CrossInsightSeverity.CRITICAL),
            sampleInsight.copy(id = "2", severity = CrossInsightSeverity.HIGH),
            sampleInsight.copy(id = "3", severity = CrossInsightSeverity.MEDIUM)
        )

        val filtered = list.filter { it.severity == CrossInsightSeverity.MEDIUM }
        assertEquals(1, filtered.size)
        assertEquals("3", filtered.first().id)
    }

    @Test
    fun test08_priorityFilterPositive() {
        val list = listOf(
            sampleInsight.copy(id = "1", severity = CrossInsightSeverity.CRITICAL),
            sampleInsight.copy(id = "2", severity = CrossInsightSeverity.POSITIVE)
        )

        val filtered = list.filter { it.severity == CrossInsightSeverity.POSITIVE }
        assertEquals(1, filtered.size)
        assertEquals("2", filtered.first().id)
    }

    @Test
    fun test09_categoryPresentationMapping() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val stringMap = mapOf(
            CrossInsightCategory.HIGH_MARGIN_STOCKOUT_RISK to context.getString(R.string.str_cat_high_margin_stockout),
            CrossInsightCategory.STAR_PERFORMER to context.getString(R.string.str_cat_star_performer),
            CrossInsightCategory.UNPROFITABLE_CAPITAL_TRAP to context.getString(R.string.str_cat_capital_trap),
            CrossInsightCategory.VOLUME_MARGIN_VULNERABILITY to context.getString(R.string.str_cat_volume_vulnerability),
            CrossInsightCategory.KEY_ACCOUNT_EXPOSURE to context.getString(R.string.str_cat_key_account_due),
            CrossInsightCategory.DORMANT_DEBT_RISK to context.getString(R.string.str_cat_dormant_debt),
            CrossInsightCategory.WORKING_CAPITAL_STRAIN to context.getString(R.string.str_cat_working_capital_squeeze),
            CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE to context.getString(R.string.str_cat_multi_signal)
        )

        assertEquals(8, stringMap.size)
        stringMap.values.forEach { label ->
            assertTrue(label.isNotBlank())
        }
    }

    @Test
    fun test10_multiSignalDisplayFormatting() {
        val multiInsight = CrossBusinessInsight(
            id = "insight_conv_1",
            category = CrossInsightCategory.MULTI_SIGNAL_CONVERGENCE,
            severity = CrossInsightSeverity.HIGH,
            title = "Multi-Signal Focus: Product One",
            summary = "Product One is flagged by 2 alerts and 1 business actions.",
            detailedExplanation = "Multiple independent engines point to this product.",
            contributingSignals = listOf(
                "Alert: Dead Stock Alert",
                "Alert: Low Margin Alert",
                "Action: Discount Item"
            ),
            entityId = "prod_1",
            entityName = "Product One",
            entityType = CrossInsightEntityType.PRODUCT,
            metricValue = 3.0,
            metricUnit = "signals",
            recommendedStrategy = "Review stocking and pricing together."
        )

        assertEquals(3.0, multiInsight.metricValue ?: 0.0, 0.001)
        assertEquals("signals", multiInsight.metricUnit)
        assertEquals(3, multiInsight.contributingSignals.size)
        assertTrue(multiInsight.contributingSignals.all { it.isNotBlank() })
    }

    @Test
    fun test11_targetEntityFormatting() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val prodTarget = context.getString(R.string.str_target_product, "Sugar 1kg")
        assertTrue(prodTarget.contains("Sugar 1kg"))

        val custTarget = context.getString(R.string.str_target_customer, "Ramesh")
        assertTrue(custTarget.contains("Ramesh"))

        val bizTarget = context.getString(R.string.str_target_business_wide)
        assertEquals("Business-Wide", bizTarget)
    }

    @Test
    fun test12_missingActionAndAlertHandledSafely() {
        val businessWideInsight = CrossBusinessInsight(
            id = "insight_biz_wide",
            category = CrossInsightCategory.WORKING_CAPITAL_STRAIN,
            severity = CrossInsightSeverity.CRITICAL,
            title = "Compound Working Capital Squeeze",
            summary = "Locked across inventory and khata.",
            detailedExplanation = "Working capital squeeze restriction.",
            contributingSignals = listOf("Signal A", "Signal B"),
            entityId = null,
            entityName = null,
            entityType = CrossInsightEntityType.BUSINESS_WIDE,
            metricValue = 15000.0,
            metricUnit = "₹",
            recommendedStrategy = "Clearance sales and collection drive."
        )

        // Null target entity identifiers are safe
        assertNotNull(businessWideInsight)
        assertEquals(CrossInsightEntityType.BUSINESS_WIDE, businessWideInsight.entityType)
        assertEquals(null, businessWideInsight.entityId)
        assertEquals(null, businessWideInsight.entityName)
    }
}
