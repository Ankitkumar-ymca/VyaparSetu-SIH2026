import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

delete_tx = """
    suspend fun reverseSale(saleId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val sale = _sales.value.find { it.id == saleId } ?: return Result.failure(Exception("Sale not found"))
        
        // Reverse inventory
        val products = _products.value.toMutableList()
        sale.items.forEach { item ->
            val pIdx = products.indexOfFirst { it.id == item.productId }
            if (pIdx != -1) {
                products[pIdx] = products[pIdx].copy(currentStock = products[pIdx].currentStock + item.quantity)
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(products[pIdx]) }
            }
        }
        _products.value = products
        
        // Reverse customer outstanding if any
        if (sale.customerId != null) {
            val customers = _customers.value.toMutableList()
            val cIdx = customers.indexOfFirst { it.id == sale.customerId }
            if (cIdx != -1) {
                val outstandingChange = sale.grandTotal - sale.amountPaid
                customers[cIdx] = customers[cIdx].copy(
                    totalPurchases = customers[cIdx].totalPurchases - sale.grandTotal,
                    outstandingAmount = customers[cIdx].outstandingAmount - outstandingChange
                )
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(customers[cIdx]) }
                _customers.value = customers
                
                // Remove ledger entries associated with this sale
                // We'd ideally need a reference, but we can try to find ledger entries with this amount/timestamp or description.
                // Assuming description contains invoiceNumber
                val ledgers = _ledgerEntries.value.filter { !(it.partyId == sale.customerId && it.description.contains(sale.invoiceNumber)) }
                _ledgerEntries.value = ledgers
                // We don't have a direct delete by query for ledger, so we might need a dao method or just ignore DB sync for now if not strictly checked by ID.
                // Actually, let's just delete the specific ledger entries
                _ledgerEntries.value.filter { it.partyId == sale.customerId && it.description.contains(sale.invoiceNumber) }.forEach {
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteLedgerEntry(it.id) }
                }
            }
        }
        
        // Remove sale
        _sales.value = _sales.value.filter { it.id != saleId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteSale(saleId) }
        return Result.success(true)
    }
    
    suspend fun reversePurchase(purchaseId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val purchase = _purchases.value.find { it.id == purchaseId } ?: return Result.failure(Exception("Purchase not found"))
        
        // Reverse inventory
        val products = _products.value.toMutableList()
        purchase.items.forEach { item ->
            val pIdx = products.indexOfFirst { it.id == item.productId }
            if (pIdx != -1) {
                products[pIdx] = products[pIdx].copy(currentStock = products[pIdx].currentStock - item.quantity)
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(products[pIdx]) }
            }
        }
        _products.value = products
        
        // Reverse supplier outstanding
        val suppliers = _suppliers.value.toMutableList()
        val sIdx = suppliers.indexOfFirst { it.id == purchase.supplierId }
        if (sIdx != -1) {
            val outstandingChange = purchase.grandTotal - purchase.amountPaid
            suppliers[sIdx] = suppliers[sIdx].copy(
                totalPurchases = suppliers[sIdx].totalPurchases - purchase.grandTotal,
                outstandingPayable = suppliers[sIdx].outstandingPayable - outstandingChange
            )
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(suppliers[sIdx]) }
            _suppliers.value = suppliers
            
            // Remove ledger entries
            val entriesToDelete = _ledgerEntries.value.filter { it.partyId == purchase.supplierId && it.description.contains(purchase.referenceNumber) }
            _ledgerEntries.value = _ledgerEntries.value.filter { !(it.partyId == purchase.supplierId && it.description.contains(purchase.referenceNumber)) }
            entriesToDelete.forEach {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteLedgerEntry(it.id) }
            }
        }
        
        // Remove purchase
        _purchases.value = _purchases.value.filter { it.id != purchaseId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deletePurchase(purchaseId) }
        return Result.success(true)
    }

    suspend fun deleteKhataEntry(entryId: String): Result<Boolean> {
        delay(NETWORK_DELAY)
        val entry = _ledgerEntries.value.find { it.id == entryId } ?: return Result.failure(Exception("Entry not found"))
        
        if (entry.type == "OPENING_BALANCE" || entry.type == "PAYMENT") {
            // Reverse from customer or supplier
            val customers = _customers.value.toMutableList()
            val cIdx = customers.indexOfFirst { it.id == entry.partyId }
            if (cIdx != -1) {
                if (entry.type == "OPENING_BALANCE") {
                    customers[cIdx] = customers[cIdx].copy(outstandingAmount = customers[cIdx].outstandingAmount - entry.amount)
                } else if (entry.type == "PAYMENT") {
                    customers[cIdx] = customers[cIdx].copy(
                        outstandingAmount = customers[cIdx].outstandingAmount + entry.amount,
                        totalPaid = customers[cIdx].totalPaid - entry.amount
                    )
                }
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(customers[cIdx]) }
                _customers.value = customers
            } else {
                val suppliers = _suppliers.value.toMutableList()
                val sIdx = suppliers.indexOfFirst { it.id == entry.partyId }
                if (sIdx != -1) {
                    if (entry.type == "OPENING_BALANCE") {
                        suppliers[sIdx] = suppliers[sIdx].copy(outstandingPayable = suppliers[sIdx].outstandingPayable - entry.amount)
                    } else if (entry.type == "PAYMENT") {
                        suppliers[sIdx] = suppliers[sIdx].copy(
                            outstandingPayable = suppliers[sIdx].outstandingPayable + entry.amount,
                            totalPaid = suppliers[sIdx].totalPaid - entry.amount
                        )
                    }
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(suppliers[sIdx]) }
                    _suppliers.value = suppliers
                }
            }
        }
        
        _ledgerEntries.value = _ledgerEntries.value.filter { it.id != entryId }
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.deleteLedgerEntry(entryId) }
        return Result.success(true)
    }
"""

text = text.replace("    fun getSyncStatus() = SyncStatus.SYNCED", "    fun getSyncStatus() = SyncStatus.SYNCED\n" + delete_tx)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
