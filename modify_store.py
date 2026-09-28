import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

# Replace deleteProduct
old_delete_product = """    suspend fun deleteProduct(productId: String) {
        delay(NETWORK_DELAY)
        _products.value = _products.value.filter { it.id != productId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteProduct(productId) }
    }"""

new_delete_product = """    suspend fun deleteProduct(productId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        // Check for transactions
        val hasSales = _sales.value.any { s -> s.items.any { it.productId == productId } }
        val hasPurchases = _purchases.value.any { p -> p.items.any { it.productId == productId } }
        val hasStockTx = _stockTransactions.value.any { it.productId == productId }
        
        if (hasSales || hasPurchases || hasStockTx) {
            // Cannot delete permanently, deactivate instead
            val product = _products.value.find { it.id == productId } ?: return Result.failure(Exception("Product not found"))
            val updated = product.copy(isActive = false, updatedAt = System.currentTimeMillis())
            val list = _products.value.toMutableList()
            val idx = list.indexOfFirst { it.id == productId }
            list[idx] = updated
            _products.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(updated) }
            return Result.success(true)
        }
        
        _products.value = _products.value.filter { it.id != productId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteProduct(productId) }
        return Result.success(true)
    }
    
    suspend fun deactivateCustomer(customerId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val customer = _customers.value.find { it.id == customerId } ?: return Result.failure(Exception("Customer not found"))
        
        val hasSales = _sales.value.any { it.customerId == customerId }
        val hasLedger = _ledgerEntries.value.any { it.partyId == customerId }
        
        if (hasSales || hasLedger) {
            val updated = customer.copy(isActive = false, updatedAt = System.currentTimeMillis())
            val list = _customers.value.toMutableList()
            list[list.indexOfFirst { it.id == customerId }] = updated
            _customers.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(updated) }
        } else {
            _customers.value = _customers.value.filter { it.id != customerId }
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteCustomer(customerId) }
        }
        return Result.success(true)
    }
    
    suspend fun deactivateSupplier(supplierId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val supplier = _suppliers.value.find { it.id == supplierId } ?: return Result.failure(Exception("Supplier not found"))
        
        val hasPurchases = _purchases.value.any { it.supplierId == supplierId }
        val hasLedger = _ledgerEntries.value.any { it.partyId == supplierId }
        
        if (hasPurchases || hasLedger) {
            val updated = supplier.copy(isActive = false, updatedAt = System.currentTimeMillis())
            val list = _suppliers.value.toMutableList()
            list[list.indexOfFirst { it.id == supplierId }] = updated
            _suppliers.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(updated) }
        } else {
            _suppliers.value = _suppliers.value.filter { it.id != supplierId }
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteSupplier(supplierId) }
        }
        return Result.success(true)
    }
    
    suspend fun deleteExpense(expenseId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        _expenses.value = _expenses.value.filter { it.id != expenseId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteExpense(expenseId) }
        return Result.success(true)
    }

    suspend fun updateExpense(expense: Expense): Result<Boolean> {
        delay(NETWORK_DELAY)
        val list = _expenses.value.toMutableList()
        val index = list.indexOfFirst { it.id == expense.id }
        if(index != -1) {
            list[index] = expense
            _expenses.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(expense) }
        }
        return Result.success(true)
    }
"""

text = text.replace(old_delete_product, new_delete_product)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
