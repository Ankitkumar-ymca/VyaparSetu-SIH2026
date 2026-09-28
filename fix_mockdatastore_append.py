import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

methods = """
    suspend fun updateExpense(expense: Expense): Result<Boolean> {
        delay(NETWORK_DELAY)
        val list = _expenses.value.toMutableList()
        val index = list.indexOfFirst { it.id == expense.id }
        if (index != -1) {
            list[index] = expense
            _expenses.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(expense) }
            return Result.success(true)
        }
        return Result.failure(Exception("Expense not found"))
    }
    
    suspend fun deleteKhataEntry(entryId: String) {
        delay(NETWORK_DELAY)
        val list = _ledgerEntries.value.toMutableList()
        val index = list.indexOfFirst { it.id == entryId }
        if (index != -1) {
            list.removeAt(index)
            _ledgerEntries.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                val d = dao ?: return@launch
                d.deleteLedgerEntry(entryId)
            }
        }
    }
    
    suspend fun deactivateCustomer(customerId: String) {
        delay(NETWORK_DELAY)
        val list = _customers.value.toMutableList()
        val index = list.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val c = list[index]
            val updated = c.copy(isActive = false)
            list[index] = updated
            _customers.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(updated) }
        }
    }
    
    suspend fun deactivateSupplier(supplierId: String) {
        delay(NETWORK_DELAY)
        val list = _suppliers.value.toMutableList()
        val index = list.indexOfFirst { it.id == supplierId }
        if (index != -1) {
            val s = list[index]
            val updated = s.copy(isActive = false)
            list[index] = updated
            _suppliers.value = list
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(updated) }
        }
    }
    
    suspend fun reverseSale(saleId: String) {
        delay(NETWORK_DELAY)
        val sale = _sales.value.find { it.id == saleId } ?: return
        
        _sales.value = _sales.value.filter { it.id != saleId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val d = dao ?: return@launch
            d.deleteSale(saleId)
        }
        
        val prodList = _products.value.toMutableList()
        sale.items.forEach { item ->
            val pIdx = prodList.indexOfFirst { it.id == item.productId }
            if (pIdx != -1) {
                val p = prodList[pIdx]
                val updated = p.copy(currentStock = p.currentStock + item.quantity)
                prodList[pIdx] = updated
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(updated) }
            }
        }
        _products.value = prodList
        
        val cList = _customers.value.toMutableList()
        val cIdx = cList.indexOfFirst { it.id == sale.customerId }
        if (cIdx != -1) {
            val c = cList[cIdx]
            val updated = c.copy(
                totalPurchases = c.totalPurchases - sale.totalAmount,
                outstandingAmount = c.outstandingAmount - (sale.totalAmount - sale.amountPaid)
            )
            cList[cIdx] = updated
            _customers.value = cList
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(updated) }
        }
    }
    
    suspend fun reversePurchase(purchaseId: String) {
        delay(NETWORK_DELAY)
        val purchase = _purchases.value.find { it.id == purchaseId } ?: return
        
        _purchases.value = _purchases.value.filter { it.id != purchaseId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val d = dao ?: return@launch
            d.deletePurchase(purchaseId)
        }
        
        val prodList = _products.value.toMutableList()
        purchase.items.forEach { item ->
            val pIdx = prodList.indexOfFirst { it.id == item.productId }
            if (pIdx != -1) {
                val p = prodList[pIdx]
                val updated = p.copy(currentStock = p.currentStock - item.quantity)
                prodList[pIdx] = updated
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(updated) }
            }
        }
        _products.value = prodList
        
        val sList = _suppliers.value.toMutableList()
        val sIdx = sList.indexOfFirst { it.id == purchase.supplierId }
        if (sIdx != -1) {
            val s = sList[sIdx]
            val updated = s.copy(
                totalPurchases = s.totalPurchases - purchase.totalAmount,
                outstandingPayable = s.outstandingPayable - (purchase.totalAmount - purchase.amountPaid)
            )
            sList[sIdx] = updated
            _suppliers.value = sList
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(updated) }
        }
    }
}
"""

text = text.rstrip()
if text.endswith("}"):
    text = text[:-1] + methods

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
