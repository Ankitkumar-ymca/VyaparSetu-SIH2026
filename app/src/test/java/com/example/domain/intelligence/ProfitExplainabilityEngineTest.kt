package com.example.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitExplainabilityEngineTest {

    private fun createSnapshot(
        label: String,
        revenue: Double,
        cogs: Double,
        expenses: Double,
        grossProfit: Double = revenue - cogs,
        netProfit: Double = revenue - cogs - expenses
    ) = ProfitPeriodSnapshot(
        periodLabel = label,
        revenue = revenue,
        cogs = cogs,
        operatingExpenses = expenses,
        grossProfit = grossProfit,
        netProfit = netProfit
    )

    // 1. Profit increased due to revenue increase
    @Test
    fun test01_profitIncreasedDueToRevenueIncrease() {
        val prev = createSnapshot("Last Month", revenue = 10000.0, cogs = 6000.0, expenses = 2000.0) // net 2000
        val curr = createSnapshot("This Month", revenue = 15000.0, cogs = 6000.0, expenses = 2000.0) // net 7000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.EXPANSION, result.changeDirection)
        assertEquals(5000.0, result.netProfitChange, 0.01)
        assertEquals(5000.0, result.revenueContribution, 0.01)
        assertEquals(0.0, result.cogsContribution, 0.01)
        assertEquals(0.0, result.expenseContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.REVENUE_DRIVEN_EXPANSION, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 2. Profit decreased due to revenue decrease
    @Test
    fun test02_profitDecreasedDueToRevenueDecrease() {
        val prev = createSnapshot("Last Month", revenue = 20000.0, cogs = 10000.0, expenses = 4000.0) // net 6000
        val curr = createSnapshot("This Month", revenue = 14000.0, cogs = 10000.0, expenses = 4000.0) // net 0

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.CONTRACTION, result.changeDirection)
        assertEquals(-6000.0, result.netProfitChange, 0.01)
        assertEquals(-6000.0, result.revenueContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.REVENUE_DECLINE_CONTRACTION, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 3. COGS increase reduces profit
    @Test
    fun test03_cogsIncreaseReducesProfit() {
        val prev = createSnapshot("Last Month", revenue = 20000.0, cogs = 8000.0, expenses = 3000.0) // net 9000
        val curr = createSnapshot("This Month", revenue = 20000.0, cogs = 12000.0, expenses = 3000.0) // net 5000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.CONTRACTION, result.changeDirection)
        assertEquals(-4000.0, result.netProfitChange, 0.01)
        assertEquals(-4000.0, result.cogsContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.COST_INCREASE_CONTRACTION, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 4. COGS reduction increases profit
    @Test
    fun test04_cogsReductionIncreasesProfit() {
        val prev = createSnapshot("Last Month", revenue = 20000.0, cogs = 12000.0, expenses = 3000.0) // net 5000
        val curr = createSnapshot("This Month", revenue = 20000.0, cogs = 8000.0, expenses = 3000.0) // net 9000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.EXPANSION, result.changeDirection)
        assertEquals(4000.0, result.netProfitChange, 0.01)
        assertEquals(4000.0, result.cogsContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.COST_REDUCTION_EXPANSION, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 5. Expense increase reduces profit
    @Test
    fun test05_expenseIncreaseReducesProfit() {
        val prev = createSnapshot("Last Month", revenue = 30000.0, cogs = 15000.0, expenses = 5000.0) // net 10000
        val curr = createSnapshot("This Month", revenue = 30000.0, cogs = 15000.0, expenses = 8000.0) // net 7000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.CONTRACTION, result.changeDirection)
        assertEquals(-3000.0, result.netProfitChange, 0.01)
        assertEquals(-3000.0, result.expenseContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.EXPENSE_INCREASE_CONTRACTION, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 6. Expense reduction increases profit
    @Test
    fun test06_expenseReductionIncreasesProfit() {
        val prev = createSnapshot("Last Month", revenue = 30000.0, cogs = 15000.0, expenses = 8000.0) // net 7000
        val curr = createSnapshot("This Month", revenue = 30000.0, cogs = 15000.0, expenses = 4000.0) // net 11000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.EXPANSION, result.changeDirection)
        assertEquals(4000.0, result.netProfitChange, 0.01)
        assertEquals(4000.0, result.expenseContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.EXPENSE_REDUCTION_EXPANSION, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 7. Combined revenue/COGS/expense change
    @Test
    fun test07_combinedRevenueCogsExpenseChange() {
        val prev = createSnapshot("Last Month", revenue = 20000.0, cogs = 10000.0, expenses = 4000.0) // net 6000
        val curr = createSnapshot("This Month", revenue = 25000.0, cogs = 13000.0, expenses = 5000.0) // net 7000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.EXPANSION, result.changeDirection)
        assertEquals(1000.0, result.netProfitChange, 0.01)
        assertEquals(5000.0, result.revenueContribution, 0.01)
        assertEquals(-3000.0, result.cogsContribution, 0.01)
        assertEquals(-1000.0, result.expenseContribution, 0.01)
        assertEquals(ProfitExplanationReasonCode.MIXED_DRIVERS, result.reasonCode)
        assertTrue(result.isConsistent)
    }

    // 8. Contribution reconciliation
    @Test
    fun test08_contributionReconciliation() {
        val prev = createSnapshot("P1", revenue = 50000.0, cogs = 25000.0, expenses = 10000.0) // net 15000
        val curr = createSnapshot("P2", revenue = 58000.0, cogs = 31000.0, expenses = 8000.0) // net 19000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        // rev: +8000, cogs: -6000, exp: +2000 -> total: +4000
        assertEquals(8000.0, result.revenueContribution, 0.01)
        assertEquals(-6000.0, result.cogsContribution, 0.01)
        assertEquals(2000.0, result.expenseContribution, 0.01)
        assertEquals(4000.0, result.contributionTotal, 0.01)
        assertEquals(result.netProfitChange, result.contributionTotal, 0.01)
        assertTrue(result.isConsistent)
    }

    // 9. Inconsistent input detection
    @Test
    fun test09_inconsistentInputDetection() {
        val prev = ProfitPeriodSnapshot("P1", revenue = 10000.0, cogs = 5000.0, operatingExpenses = 2000.0, grossProfit = 5000.0, netProfit = 3000.0)
        // Mismatched current net profit (claims 9000 instead of 3000)
        val curr = ProfitPeriodSnapshot("P2", revenue = 10000.0, cogs = 5000.0, operatingExpenses = 2000.0, grossProfit = 5000.0, netProfit = 9000.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertFalse(result.isConsistent)
        assertEquals(ProfitExplanationReasonCode.DATA_INCONSISTENT, result.reasonCode)
        assertTrue(result.notes.any { it.contains("do not reconcile") })
    }

    // 10. No-change scenario
    @Test
    fun test10_noChangeScenario() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 10000.0, cogs = 5000.0, expenses = 2000.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.NO_CHANGE, result.changeDirection)
        assertEquals(0.0, result.netProfitChange, 0.01)
        assertEquals(ProfitExplanationReasonCode.NO_CHANGE, result.reasonCode)
    }

    // 11. Loss-to-profit scenario
    @Test
    fun test11_lossToProfitScenario() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 8000.0, expenses = 4000.0) // net -2000 (loss)
        val curr = createSnapshot("P2", revenue = 20000.0, cogs = 10000.0, expenses = 4000.0) // net +6000

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.EXPANSION, result.changeDirection)
        assertEquals(8000.0, result.netProfitChange, 0.01)
        assertTrue(result.previousNetProfit < 0.0)
        assertTrue(result.currentNetProfit > 0.0)
    }

    // 12. Profit-to-loss scenario
    @Test
    fun test12_profitToLossScenario() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 10000.0, expenses = 4000.0) // net +6000
        val curr = createSnapshot("P2", revenue = 10000.0, cogs = 9000.0, expenses = 4000.0) // net -3000 (loss)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(ProfitChangeDirection.LOSS, result.changeDirection)
        assertEquals(-9000.0, result.netProfitChange, 0.01)
        assertTrue(result.currentNetProfit < 0.0)
    }

    // 13. Zero revenue
    @Test
    fun test13_zeroRevenue() {
        val prev = createSnapshot("P1", revenue = 0.0, cogs = 0.0, expenses = 1000.0)
        val curr = createSnapshot("P2", revenue = 5000.0, cogs = 2000.0, expenses = 1000.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(5000.0, result.revenueContribution, 0.01)
        assertEquals(3000.0, result.netProfitChange, 0.01)
        assertTrue(result.isConsistent)
    }

    // 14. Zero expenses
    @Test
    fun test14_zeroExpenses() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 5000.0, expenses = 0.0)
        val curr = createSnapshot("P2", revenue = 12000.0, cogs = 6000.0, expenses = 0.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(0.0, result.expenseContribution, 0.01)
        assertEquals(1000.0, result.netProfitChange, 0.01)
    }

    // 15. Empty driver lists
    @Test
    fun test15_emptyDriverLists() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 12000.0, cogs = 5000.0, expenses = 2000.0)

        val result = ProfitExplainabilityEngine.explain(
            previous = prev,
            current = curr,
            revenueDrivers = emptyList(),
            costDrivers = emptyList(),
            expenseDrivers = emptyList()
        )

        assertNull(result.driverAttribution.topPositiveRevenueDriver)
        assertNull(result.driverAttribution.topNegativeRevenueDriver)
        assertNull(result.driverAttribution.topCostIncreaseDriver)
        assertNull(result.driverAttribution.topCostReductionDriver)
        assertNull(result.driverAttribution.topExpenseIncreaseDriver)
        assertNull(result.driverAttribution.topExpenseReductionDriver)
    }

    // 16. Top positive revenue driver
    @Test
    fun test16_topPositiveRevenueDriver() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 15000.0, cogs = 5000.0, expenses = 2000.0)

        val drivers = listOf(
            ProductRevenueDriver("p1", "Rice", 5000.0, 7000.0), // +2000
            ProductRevenueDriver("p2", "Oil", 3000.0, 6000.0), // +3000 (highest)
            ProductRevenueDriver("p3", "Dal", 2000.0, 2000.0) // 0
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, revenueDrivers = drivers)

        val topPos = result.driverAttribution.topPositiveRevenueDriver
        assertNotNull(topPos)
        assertEquals("p2", topPos?.productId)
        assertEquals(3000.0, topPos?.revenueChange ?: 0.0, 0.01)
    }

    // 17. Top negative revenue driver
    @Test
    fun test17_topNegativeRevenueDriver() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 14000.0, cogs = 5000.0, expenses = 2000.0)

        val drivers = listOf(
            ProductRevenueDriver("p1", "Rice", 10000.0, 8000.0), // -2000
            ProductRevenueDriver("p2", "Sugar", 8000.0, 4000.0), // -4000 (largest decline)
            ProductRevenueDriver("p3", "Tea", 2000.0, 2000.0)
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, revenueDrivers = drivers)

        val topNeg = result.driverAttribution.topNegativeRevenueDriver
        assertNotNull(topNeg)
        assertEquals("p2", topNeg?.productId)
        assertEquals(-4000.0, topNeg?.revenueChange ?: 0.0, 0.01)
    }

    // 18. Top cost increase driver
    @Test
    fun test18_topCostIncreaseDriver() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 10000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 20000.0, cogs = 15000.0, expenses = 2000.0)

        val drivers = listOf(
            ProductCostDriver("p1", "Wheat", 4000.0, 5000.0), // +1000
            ProductCostDriver("p2", "Ghee", 6000.0, 10000.0) // +4000 (highest cost jump)
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, costDrivers = drivers)

        val topCost = result.driverAttribution.topCostIncreaseDriver
        assertNotNull(topCost)
        assertEquals("p2", topCost?.productId)
        assertEquals(4000.0, topCost?.cogsChange ?: 0.0, 0.01)
    }

    // 19. Top cost reduction driver
    @Test
    fun test19_topCostReductionDriver() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 15000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 20000.0, cogs = 10000.0, expenses = 2000.0)

        val drivers = listOf(
            ProductCostDriver("p1", "Wheat", 5000.0, 4000.0), // -1000
            ProductCostDriver("p2", "Ghee", 10000.0, 6000.0) // -4000 (largest reduction)
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, costDrivers = drivers)

        val topRed = result.driverAttribution.topCostReductionDriver
        assertNotNull(topRed)
        assertEquals("p2", topRed?.productId)
        assertEquals(-4000.0, topRed?.cogsChange ?: 0.0, 0.01)
    }

    // 20. Top expense increase driver
    @Test
    fun test20_topExpenseIncreaseDriver() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 10000.0, expenses = 5000.0)
        val curr = createSnapshot("P2", revenue = 20000.0, cogs = 10000.0, expenses = 8000.0)

        val drivers = listOf(
            ExpenseCategoryDriver("Electricity", 2000.0, 2500.0), // +500
            ExpenseCategoryDriver("Rent", 3000.0, 5500.0) // +2500 (highest increase)
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, expenseDrivers = drivers)

        val topExp = result.driverAttribution.topExpenseIncreaseDriver
        assertNotNull(topExp)
        assertEquals("Rent", topExp?.category)
        assertEquals(2500.0, topExp?.expenseChange ?: 0.0, 0.01)
    }

    // 21. Top expense reduction driver
    @Test
    fun test21_topExpenseReductionDriver() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 10000.0, expenses = 8000.0)
        val curr = createSnapshot("P2", revenue = 20000.0, cogs = 10000.0, expenses = 5000.0)

        val drivers = listOf(
            ExpenseCategoryDriver("Electricity", 2500.0, 2000.0), // -500
            ExpenseCategoryDriver("Logistics", 5500.0, 3000.0) // -2500 (highest decrease)
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, expenseDrivers = drivers)

        val topRed = result.driverAttribution.topExpenseReductionDriver
        assertNotNull(topRed)
        assertEquals("Logistics", topRed?.category)
        assertEquals(-2500.0, topRed?.expenseChange ?: 0.0, 0.01)
    }

    // 22. Deterministic driver ordering (tie-breaker)
    @Test
    fun test22_deterministicDriverOrdering() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 10000.0, expenses = 5000.0)
        val curr = createSnapshot("P2", revenue = 26000.0, cogs = 10000.0, expenses = 5000.0)

        // Both p_b and p_a have identical +3000 change. Tie-break should pick p_a (lexicographical)
        val drivers = listOf(
            ProductRevenueDriver("p_b", "Product B", 2000.0, 5000.0), // +3000
            ProductRevenueDriver("p_a", "Product A", 2000.0, 5000.0) // +3000
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, revenueDrivers = drivers)

        assertEquals("p_a", result.driverAttribution.topPositiveRevenueDriver?.productId)
    }

    // 23. Duplicate driver handling
    @Test
    fun test23_duplicateDriverHandling() {
        val prev = createSnapshot("P1", revenue = 20000.0, cogs = 10000.0, expenses = 5000.0)
        val curr = createSnapshot("P2", revenue = 25000.0, cogs = 10000.0, expenses = 5000.0)

        // Multiple entries for the same product ID should be aggregated deterministically
        val drivers = listOf(
            ProductRevenueDriver("p1", "Item 1", 1000.0, 2000.0), // +1000
            ProductRevenueDriver("p1", "Item 1", 1000.0, 3000.0) // +2000 -> Total p1 = +3000
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, revenueDrivers = drivers)

        val top = result.driverAttribution.topPositiveRevenueDriver
        assertNotNull(top)
        assertEquals("p1", top?.productId)
        assertEquals(3000.0, top?.revenueChange ?: 0.0, 0.01)
    }

    // 24. Missing product/category handling
    @Test
    fun test24_missingProductCategoryHandling() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 12000.0, cogs = 5000.0, expenses = 2000.0)

        val drivers = listOf(
            ProductRevenueDriver("", "", 1000.0, 2000.0) // Blank strings
        )

        val result = ProfitExplainabilityEngine.explain(prev, curr, revenueDrivers = drivers)

        assertNotNull(result.driverAttribution.topPositiveRevenueDriver)
        assertTrue(result.isConsistent)
    }

    // 25. NaN safety
    @Test
    fun test25_nanSafety() {
        val prev = createSnapshot("P1", revenue = Double.NaN, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 12000.0, cogs = 5000.0, expenses = 2000.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertFalse(result.netProfitChange.isNaN())
        assertFalse(result.revenueContribution.isNaN())
        assertTrue(result.revenueContribution.isFinite())
    }

    // 26. Infinity safety
    @Test
    fun test26_infinitySafety() {
        val prev = createSnapshot("P1", revenue = Double.POSITIVE_INFINITY, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 12000.0, cogs = 5000.0, expenses = 2000.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertTrue(result.netProfitChange.isFinite())
        assertTrue(result.revenueContribution.isFinite())
    }

    // 27. Deterministic repeated calculation
    @Test
    fun test27_deterministicRepeatedCalculation() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 6000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 15000.0, cogs = 7000.0, expenses = 3000.0)

        val r1 = ProfitExplainabilityEngine.explain(prev, curr)
        val r2 = ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(r1.netProfitChange, r2.netProfitChange, 0.0001)
        assertEquals(r1.contributionTotal, r2.contributionTotal, 0.0001)
        assertEquals(r1.reasonCode, r2.reasonCode)
        assertEquals(r1.primaryDriverSummary, r2.primaryDriverSummary)
    }

    // 28. Input immutability
    @Test
    fun test28_inputImmutability() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 6000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 15000.0, cogs = 7000.0, expenses = 3000.0)

        val origPrevRev = prev.revenue
        val origCurrRev = curr.revenue

        ProfitExplainabilityEngine.explain(prev, curr)

        assertEquals(origPrevRev, prev.revenue, 0.0001)
        assertEquals(origCurrRev, curr.revenue, 0.0001)
    }

    // 29. No refund attribution documented
    @Test
    fun test29_noRefundAttributionDocumented() {
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 6000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 15000.0, cogs = 7000.0, expenses = 3000.0)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        assertTrue(result.notes.any { it.contains("Financial returns and refunds are outside the current ledger scope") })
    }

    // 30. Mathematical contribution consistency
    @Test
    fun test30_mathematicalContributionConsistency() {
        // Delta Net = Delta Rev - Delta COGS - Delta Exp
        // 5432.10 - 2345.67 - 1234.56 = 1851.87
        val prev = createSnapshot("P1", revenue = 10000.0, cogs = 5000.0, expenses = 2000.0)
        val curr = createSnapshot("P2", revenue = 15432.10, cogs = 7345.67, expenses = 3234.56)

        val result = ProfitExplainabilityEngine.explain(prev, curr)

        val expectedRevContr = 5432.10
        val expectedCogsContr = -2345.67
        val expectedExpContr = -1234.56
        val expectedNetChange = expectedRevContr + expectedCogsContr + expectedExpContr

        assertEquals(expectedRevContr, result.revenueContribution, 0.05)
        assertEquals(expectedCogsContr, result.cogsContribution, 0.05)
        assertEquals(expectedExpContr, result.expenseContribution, 0.05)
        assertEquals(expectedNetChange, result.contributionTotal, 0.05)
        assertEquals(result.netProfitChange, result.contributionTotal, 0.05)
        assertTrue(result.isConsistent)
    }
}
