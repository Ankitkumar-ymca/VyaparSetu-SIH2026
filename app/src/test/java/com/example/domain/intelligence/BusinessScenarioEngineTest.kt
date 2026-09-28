package com.example.domain.intelligence

import com.example.domain.model.Customer
import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessScenarioEngineTest {

    private fun createProduct(
        id: String = "p1",
        name: String = "Basmati Rice 5kg",
        sellingPrice: Double = 500.0,
        purchasePrice: Double = 350.0,
        currentStock: Int = 20,
        minStock: Int = 5
    ) = Product(
        id = id,
        businessId = "biz1",
        name = name,
        sku = "SKU-$id",
        barcode = "BAR-$id",
        category = "Grocery",
        purchasePrice = purchasePrice,
        sellingPrice = sellingPrice,
        currentStock = currentStock,
        minStock = minStock,
        supplierId = "s1",
        expiryDate = null,
        createdAt = 1000L,
        updatedAt = 1000L,
        isActive = true
    )

    private fun createCustomer(
        id: String = "c1",
        name: String = "Sharma Stores",
        outstandingAmount: Double = 15000.0
    ) = Customer(
        id = id,
        businessId = "biz1",
        name = name,
        phone = "9876543210",
        email = "sharma@example.com",
        address = "Delhi",
        totalPurchases = 50000.0,
        totalPaid = 35000.0,
        outstandingAmount = outstandingAmount,
        createdAt = 1000L,
        updatedAt = 1000L,
        isActive = true
    )

    private fun createSale(
        id: String = "s1",
        items: List<SaleItem>,
        grandTotal: Double,
        createdAt: Long = 1000L
    ) = Sale(
        id = id,
        businessId = "biz1",
        invoiceNumber = "INV-$id",
        customerId = "c1",
        items = items,
        subtotal = grandTotal,
        discount = 0.0,
        tax = 0.0,
        grandTotal = grandTotal,
        amountPaid = grandTotal,
        outstanding = 0.0,
        paymentStatus = "PAID",
        paymentMethod = "CASH",
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun createExpense(
        id: String = "e1",
        category: String = "Rent",
        amount: Double = 10000.0
    ) = Expense(
        id = id,
        businessId = "biz1",
        category = category,
        description = "Monthly shop rent",
        amount = amount,
        paymentMethod = "BANK_TRANSFER",
        notes = "",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    // ========================================================
    // SCENARIO A: ADDITIONAL SALES TESTS
    // ========================================================

    @Test
    fun additionalSales_basicIncrementalCalculation() {
        val product = createProduct(sellingPrice = 500.0, purchasePrice = 350.0, currentStock = 50)
        val result = BusinessScenarioEngine.simulateAdditionalSales(
            product = product,
            additionalUnits = 10
        )

        assertTrue(result.isValid)
        assertEquals(ScenarioType.ADDITIONAL_SALES, result.scenarioType)
        assertTrue(result.isSimulated)
        assertEquals("Basmati Rice 5kg", result.targetEntityName)

        // Delta revenue = 10 * 500 = 5000
        val rev = result.revenueImpact
        assertNotNull(rev)
        assertEquals(5000.0, rev!!.delta, 0.01)
        assertTrue(rev.isFavorable)

        // Delta COGS = 10 * 350 = 3500
        val cogs = result.cogsImpact
        assertNotNull(cogs)
        assertEquals(3500.0, cogs!!.delta, 0.01)

        // Delta Gross profit = 5000 - 3500 = 1500
        val profit = result.grossProfitImpact
        assertNotNull(profit)
        assertEquals(1500.0, profit!!.delta, 0.01)
        assertTrue(profit.isFavorable)

        // Gross margin = (1500 / 5000) * 100 = 30.0%
        val margin = result.marginImpact
        assertNotNull(margin)
        assertEquals(30.0, margin!!.simulatedValue, 0.01)
    }

    @Test
    fun additionalSales_withHistoricalSalesBaseline() {
        val product = createProduct(id = "p1", sellingPrice = 500.0, purchasePrice = 350.0)
        val sales = listOf(
            createSale(
                id = "s1",
                items = listOf(
                    SaleItem("p1", "Basmati Rice", quantity = 5, unitPrice = 500.0, totalPrice = 2500.0, purchasePriceAtSale = 350.0)
                ),
                grandTotal = 2500.0
            )
        )

        val result = BusinessScenarioEngine.simulateAdditionalSales(
            product = product,
            additionalUnits = 5,
            recentSales = sales
        )

        assertTrue(result.isValid)
        assertEquals(2500.0, result.revenueImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(5000.0, result.revenueImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(2500.0, result.revenueImpact?.delta ?: 0.0, 0.01)
        assertEquals(750.0, result.grossProfitImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(1500.0, result.grossProfitImpact?.simulatedValue ?: 0.0, 0.01)
    }

    @Test
    fun additionalSales_customPriceAndCostOverrides() {
        val product = createProduct(sellingPrice = 500.0, purchasePrice = 350.0)
        val result = BusinessScenarioEngine.simulateAdditionalSales(
            product = product,
            additionalUnits = 4,
            customUnitPrice = 600.0, // Selling higher in scenario
            customCostPerUnit = 400.0 // Higher cost in scenario
        )

        assertTrue(result.isValid)
        assertEquals(2400.0, result.revenueImpact?.delta ?: 0.0, 0.01) // 4 * 600
        assertEquals(1600.0, result.cogsImpact?.delta ?: 0.0, 0.01) // 4 * 400
        assertEquals(800.0, result.grossProfitImpact?.delta ?: 0.0, 0.01) // 2400 - 1600
    }

    @Test
    fun additionalSales_invalidUnits_returnsValidationFailure() {
        val product = createProduct()
        val resultZero = BusinessScenarioEngine.simulateAdditionalSales(product, additionalUnits = 0)
        assertFalse(resultZero.isValid)
        assertTrue(resultZero.validationErrors.isNotEmpty())

        val resultNegative = BusinessScenarioEngine.simulateAdditionalSales(product, additionalUnits = -5)
        assertFalse(resultNegative.isValid)
    }

    @Test
    fun additionalSales_stockDeficit_includesLimitationWarning() {
        val product = createProduct(currentStock = 5) // Only 5 in stock
        val result = BusinessScenarioEngine.simulateAdditionalSales(product, additionalUnits = 20)

        assertTrue(result.isValid)
        assertTrue(result.limitations.any { it.contains("Current stock (5 units) is less than hypothetical demand") })
    }

    // ========================================================
    // SCENARIO B: PRICE CHANGE TESTS
    // ========================================================

    @Test
    fun priceChange_percentageIncrease_calculatesMarginExpansion() {
        val product = createProduct(sellingPrice = 100.0, purchasePrice = 60.0) // 40% margin
        val result = BusinessScenarioEngine.simulatePriceChange(
            product = product,
            percentageChange = 10.0, // +10% -> 110.0
            referenceUnits = 10
        )

        assertTrue(result.isValid)
        assertEquals(ScenarioType.PRICE_CHANGE, result.scenarioType)
        // Baseline: 10 * 100 = 1000 rev, 600 cogs, 400 profit (40% margin)
        // Simulated: 10 * 110 = 1100 rev, 600 cogs, 500 profit (45.45% margin)
        assertEquals(1000.0, result.revenueImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(1100.0, result.revenueImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(100.0, result.revenueImpact?.delta ?: 0.0, 0.01)
        assertEquals(100.0, result.grossProfitImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.marginImpact!!.delta > 5.0) // 45.45 - 40.0 = +5.45%
        assertTrue(result.marginImpact!!.isFavorable)
    }

    @Test
    fun priceChange_percentageDecrease_calculatesMarginCompression() {
        val product = createProduct(sellingPrice = 100.0, purchasePrice = 60.0)
        val result = BusinessScenarioEngine.simulatePriceChange(
            product = product,
            percentageChange = -10.0, // -10% -> 90.0
            referenceUnits = 10
        )

        assertTrue(result.isValid)
        // Simulated: 10 * 90 = 900 rev, 600 cogs, 300 profit (33.33% margin)
        assertEquals(-100.0, result.revenueImpact?.delta ?: 0.0, 0.01)
        assertEquals(-100.0, result.grossProfitImpact?.delta ?: 0.0, 0.01)
        assertFalse(result.grossProfitImpact!!.isFavorable)
    }

    @Test
    fun priceChange_explicitNewPrice() {
        val product = createProduct(sellingPrice = 200.0, purchasePrice = 150.0)
        val result = BusinessScenarioEngine.simulatePriceChange(
            product = product,
            newPrice = 250.0,
            referenceUnits = 5
        )

        assertTrue(result.isValid)
        assertEquals(1000.0, result.revenueImpact?.baselineValue ?: 0.0, 0.01) // 5 * 200
        assertEquals(1250.0, result.revenueImpact?.simulatedValue ?: 0.0, 0.01) // 5 * 250
        assertEquals(250.0, result.grossProfitImpact?.delta ?: 0.0, 0.01)
    }

    @Test
    fun priceChange_usesPastSalesForReferenceUnitsWhenOmitted() {
        val product = createProduct(id = "p1", sellingPrice = 100.0, purchasePrice = 70.0)
        val sales = listOf(
            createSale(
                id = "s1",
                items = listOf(SaleItem("p1", "Product P1", quantity = 8, unitPrice = 100.0, totalPrice = 800.0)),
                grandTotal = 800.0
            )
        )

        val result = BusinessScenarioEngine.simulatePriceChange(
            product = product,
            percentageChange = 5.0,
            recentSales = sales
        )

        assertTrue(result.isValid)
        // 8 units evaluated
        assertEquals(800.0, result.revenueImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(840.0, result.revenueImpact?.simulatedValue ?: 0.0, 0.01)
    }

    @Test
    fun priceChange_invalidPriceOrPercentage_returnsValidationFailure() {
        val product = createProduct()
        // Below -100% price
        val resultUnder = BusinessScenarioEngine.simulatePriceChange(product, percentageChange = -110.0)
        assertFalse(resultUnder.isValid)

        // Negative new price
        val resultNegative = BusinessScenarioEngine.simulatePriceChange(product, newPrice = -50.0)
        assertFalse(resultNegative.isValid)

        // Missing both
        val resultEmpty = BusinessScenarioEngine.simulatePriceChange(product)
        assertFalse(resultEmpty.isValid)
    }

    // ========================================================
    // SCENARIO C: EXPENSE CHANGE TESTS
    // ========================================================

    @Test
    fun expenseChange_amountIncrease_reducesNetProfit() {
        val expenses = listOf(createExpense(amount = 10000.0))
        val sales = listOf(
            createSale(
                id = "s1",
                items = listOf(SaleItem("p1", "Item", quantity = 1, unitPrice = 50000.0, totalPrice = 50000.0, purchasePriceAtSale = 30000.0)),
                grandTotal = 50000.0
            )
        )

        val result = BusinessScenarioEngine.simulateExpenseChange(
            recentExpenses = expenses,
            amountDelta = 5000.0, // Expenses increase by 5000
            recentSales = sales
        )

        assertTrue(result.isValid)
        assertEquals(ScenarioType.EXPENSE_CHANGE, result.scenarioType)
        // Baseline expenses: 10000 -> Simulated: 15000
        assertEquals(10000.0, result.expenseImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(15000.0, result.expenseImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(5000.0, result.expenseImpact?.delta ?: 0.0, 0.01)
        assertFalse(result.expenseImpact!!.isFavorable)

        // Gross profit = 50000 - 30000 = 20000
        // Baseline net profit = 20000 - 10000 = 10000
        // Simulated net profit = 20000 - 15000 = 5000
        assertEquals(10000.0, result.netProfitImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(5000.0, result.netProfitImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(-5000.0, result.netProfitImpact?.delta ?: 0.0, 0.01)
        assertFalse(result.netProfitImpact!!.isFavorable)
    }

    @Test
    fun expenseChange_amountDecrease_increasesNetProfit() {
        val expenses = listOf(createExpense(amount = 10000.0))
        val result = BusinessScenarioEngine.simulateExpenseChange(
            recentExpenses = expenses,
            amountDelta = -3000.0
        )

        assertTrue(result.isValid)
        assertEquals(7000.0, result.expenseImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(-3000.0, result.expenseImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.expenseImpact!!.isFavorable)
        assertEquals(3000.0, result.netProfitImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.netProfitImpact!!.isFavorable)
    }

    @Test
    fun expenseChange_percentageChange_appliedCorrectly() {
        val expenses = listOf(createExpense(amount = 20000.0))
        val result = BusinessScenarioEngine.simulateExpenseChange(
            recentExpenses = expenses,
            percentageDelta = 25.0 // +25% of 20000 = +5000
        )

        assertTrue(result.isValid)
        assertEquals(25000.0, result.expenseImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(5000.0, result.expenseImpact?.delta ?: 0.0, 0.01)
    }

    @Test
    fun expenseChange_categoryFilter_onlyAppliesToCategory() {
        val expenses = listOf(
            createExpense(id = "e1", category = "Rent", amount = 15000.0),
            createExpense(id = "e2", category = "Electricity", amount = 5000.0)
        )

        val result = BusinessScenarioEngine.simulateExpenseChange(
            recentExpenses = expenses,
            category = "Rent",
            amountDelta = 2000.0
        )

        assertTrue(result.isValid)
        // Baseline should only be Rent (15000)
        assertEquals(15000.0, result.expenseImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(17000.0, result.expenseImpact?.simulatedValue ?: 0.0, 0.01)
    }

    @Test
    fun expenseChange_missingDeltas_failsValidation() {
        val result = BusinessScenarioEngine.simulateExpenseChange(recentExpenses = emptyList())
        assertFalse(result.isValid)
        assertTrue(result.validationErrors.isNotEmpty())
    }

    // ========================================================
    // SCENARIO D: RECEIVABLE RECOVERY TESTS
    // ========================================================

    @Test
    fun receivableRecovery_singleCustomerAmount() {
        val customer = createCustomer(outstandingAmount = 20000.0)
        val result = BusinessScenarioEngine.simulateReceivableRecovery(
            customer = customer,
            recoveryAmount = 8000.0
        )

        assertTrue(result.isValid)
        assertEquals(ScenarioType.RECEIVABLE_RECOVERY, result.scenarioType)
        assertEquals("Sharma Stores", result.targetEntityName)

        // Outstanding drops from 20000 to 12000
        assertEquals(20000.0, result.outstandingImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(12000.0, result.outstandingImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(-8000.0, result.outstandingImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.outstandingImpact!!.isFavorable)

        // Cash flow +8000
        assertEquals(8000.0, result.cashFlowImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.cashFlowImpact!!.isFavorable)
    }

    @Test
    fun receivableRecovery_percentageOfTotalCustomerBase() {
        val customers = listOf(
            createCustomer(id = "c1", outstandingAmount = 10000.0),
            createCustomer(id = "c2", outstandingAmount = 30000.0)
        )

        val result = BusinessScenarioEngine.simulateReceivableRecovery(
            allCustomers = customers,
            recoveryPercentage = 50.0 // 50% of 40000 = 20000
        )

        assertTrue(result.isValid)
        assertEquals(40000.0, result.outstandingImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(20000.0, result.outstandingImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(20000.0, result.cashFlowImpact?.delta ?: 0.0, 0.01)
    }

    @Test
    fun receivableRecovery_capsAtMaxOutstanding() {
        val customer = createCustomer(outstandingAmount = 5000.0)
        val result = BusinessScenarioEngine.simulateReceivableRecovery(
            customer = customer,
            recoveryAmount = 10000.0 // Requested more than balance
        )

        assertTrue(result.isValid)
        assertEquals(0.0, result.outstandingImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(5000.0, result.cashFlowImpact?.simulatedValue ?: 0.0, 0.01)
        assertTrue(result.limitations.any { it.contains("Capped at full settlement") })
    }

    @Test
    fun receivableRecovery_zeroOutstanding_failsValidation() {
        val customer = createCustomer(outstandingAmount = 0.0)
        val result = BusinessScenarioEngine.simulateReceivableRecovery(
            customer = customer,
            recoveryAmount = 1000.0
        )

        assertFalse(result.isValid)
        assertTrue(result.validationErrors.any { it.contains("no outstanding balance") })
    }

    // ========================================================
    // SCENARIO E: COST BASIS CHANGE TESTS
    // ========================================================

    @Test
    fun costChange_percentageIncrease_reducesGrossProfit() {
        val product = createProduct(sellingPrice = 100.0, purchasePrice = 60.0)
        val result = BusinessScenarioEngine.simulateCostChange(
            product = product,
            percentageChange = 10.0, // Cost goes from 60 to 66
            referenceUnits = 10
        )

        assertTrue(result.isValid)
        assertEquals(ScenarioType.COST_CHANGE, result.scenarioType)
        // Baseline COGS: 600 -> Simulated COGS: 660 (+60)
        assertEquals(600.0, result.cogsImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(660.0, result.cogsImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(60.0, result.cogsImpact?.delta ?: 0.0, 0.01)

        // Gross Profit: was 400, becomes 340 (-60)
        assertEquals(400.0, result.grossProfitImpact?.baselineValue ?: 0.0, 0.01)
        assertEquals(340.0, result.grossProfitImpact?.simulatedValue ?: 0.0, 0.01)
        assertEquals(-60.0, result.grossProfitImpact?.delta ?: 0.0, 0.01)
        assertFalse(result.grossProfitImpact!!.isFavorable)
    }

    @Test
    fun costChange_explicitNewUnitCost() {
        val product = createProduct(sellingPrice = 200.0, purchasePrice = 120.0)
        val result = BusinessScenarioEngine.simulateCostChange(
            product = product,
            newUnitCost = 100.0, // Supplier discount in scenario
            referenceUnits = 5
        )

        assertTrue(result.isValid)
        // COGS drops from 600 to 500
        assertEquals(-100.0, result.cogsImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.cogsImpact!!.isFavorable)
        assertEquals(100.0, result.grossProfitImpact?.delta ?: 0.0, 0.01)
        assertTrue(result.grossProfitImpact!!.isFavorable)
    }

    // ========================================================
    // HIGH-LEVEL DISPATCHER & SAFETY TESTS
    // ========================================================

    @Test
    fun simulate_dispatchesAllSupportedRequests() {
        val product = createProduct(id = "p1")
        val customer = createCustomer(id = "c1")
        val expense = createExpense()

        val reqSales = ScenarioRequest.AdditionalSales("p1", 5)
        val resSales = BusinessScenarioEngine.simulate(
            request = reqSales,
            products = listOf(product)
        )
        assertTrue(resSales.isValid)
        assertEquals(ScenarioType.ADDITIONAL_SALES, resSales.scenarioType)

        val reqPrice = ScenarioRequest.PriceChange("p1", percentageChange = 5.0)
        val resPrice = BusinessScenarioEngine.simulate(
            request = reqPrice,
            products = listOf(product)
        )
        assertTrue(resPrice.isValid)
        assertEquals(ScenarioType.PRICE_CHANGE, resPrice.scenarioType)

        val reqExpense = ScenarioRequest.ExpenseChange(amountDelta = 1000.0)
        val resExpense = BusinessScenarioEngine.simulate(
            request = reqExpense,
            expenses = listOf(expense)
        )
        assertTrue(resExpense.isValid)
        assertEquals(ScenarioType.EXPENSE_CHANGE, resExpense.scenarioType)

        val reqRecovery = ScenarioRequest.ReceivableRecovery(customerId = "c1", recoveryAmount = 5000.0)
        val resRecovery = BusinessScenarioEngine.simulate(
            request = reqRecovery,
            customers = listOf(customer)
        )
        assertTrue(resRecovery.isValid)
        assertEquals(ScenarioType.RECEIVABLE_RECOVERY, resRecovery.scenarioType)

        val reqCost = ScenarioRequest.CostChange("p1", percentageChange = 10.0)
        val resCost = BusinessScenarioEngine.simulate(
            request = reqCost,
            products = listOf(product)
        )
        assertTrue(resCost.isValid)
        assertEquals(ScenarioType.COST_CHANGE, resCost.scenarioType)
    }

    @Test
    fun simulate_productNotFound_returnsGracefulFailure() {
        val res = BusinessScenarioEngine.simulate(
            request = ScenarioRequest.AdditionalSales("unknown_product", 5),
            products = emptyList()
        )
        assertFalse(res.isValid)
        assertTrue(res.validationErrors.any { it.contains("unknown_product") })
    }

    @Test
    fun simulate_unsupportedScenario_returnsDocumentedExplanation() {
        val req = ScenarioRequest.Unsupported(
            scenarioName = "Inflation Spike 8%",
            explanation = "Macroeconomic inflation cannot be modeled from offline store data."
        )
        val result = BusinessScenarioEngine.simulate(req)

        assertFalse(result.isValid)
        assertEquals(ScenarioType.UNSUPPORTED, result.scenarioType)
        assertTrue(result.validationErrors.isNotEmpty())
    }

    @Test
    fun getUnsupportedScenarios_returnsCatalogOfUncomputableScenarios() {
        val unsupported = BusinessScenarioEngine.getUnsupportedScenarios()
        assertTrue(unsupported.size >= 4)
        assertTrue(unsupported.any { it.type == "MACRO_INFLATION" })
        assertTrue(unsupported.any { it.type == "COMPETITOR_PRICING" })
        assertTrue(unsupported.any { it.type == "FOOT_TRAFFIC_SHIFTS" })
        assertTrue(unsupported.any { it.type == "ADVERTISING_ROI" })
    }

    @Test
    fun simulation_doesNotMutateInputData() {
        val product = createProduct(currentStock = 25, sellingPrice = 200.0, purchasePrice = 140.0)
        val originalStock = product.currentStock
        val originalPrice = product.sellingPrice

        BusinessScenarioEngine.simulateAdditionalSales(product, additionalUnits = 100)
        BusinessScenarioEngine.simulatePriceChange(product, newPrice = 999.0)
        BusinessScenarioEngine.simulateCostChange(product, newUnitCost = 50.0)

        assertEquals(originalStock, product.currentStock)
        assertEquals(originalPrice, product.sellingPrice, 0.001)
    }
}
