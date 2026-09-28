import re

with open('app/src/main/java/com/example/data/repository/AppDao.kt', 'r') as f:
    text = f.read()

new_methods = """
    @Query("DELETE FROM Customer WHERE id = :id")
    suspend fun deleteCustomer(id: String)
    @Query("DELETE FROM Supplier WHERE id = :id")
    suspend fun deleteSupplier(id: String)
    @Query("DELETE FROM Sale WHERE id = :id")
    suspend fun deleteSale(id: String)
    @Query("DELETE FROM Purchase WHERE id = :id")
    suspend fun deletePurchase(id: String)
    @Query("DELETE FROM Expense WHERE id = :id")
    suspend fun deleteExpense(id: String)
    @Query("DELETE FROM LedgerEntry WHERE id = :id")
    suspend fun deleteLedgerEntry(id: String)
    @Query("DELETE FROM CustomerPayment WHERE id = :id")
    suspend fun deleteCustomerPayment(id: String)
}"""

text = text.replace('}', new_methods)

with open('app/src/main/java/com/example/data/repository/AppDao.kt', 'w') as f:
    f.write(text)
