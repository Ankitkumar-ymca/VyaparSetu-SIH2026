package com.example.data.repository

import androidx.room.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM User")
    fun getUsers(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM Business")
    fun getBusinesses(): List<Business>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: Business)

    @Query("SELECT * FROM Product")
    fun getProducts(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("DELETE FROM Product WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("SELECT * FROM Customer")
    fun getCustomers(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Query("SELECT * FROM Supplier")
    fun getSuppliers(): List<Supplier>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)

    @Query("SELECT * FROM Sale")
    fun getSales(): List<Sale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Query("SELECT * FROM Purchase")
    fun getPurchases(): List<Purchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase)

    @Query("SELECT * FROM Expense")
    fun getExpenses(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM StockAdjustment")
    fun getStockAdjustments(): List<StockAdjustment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustment(stockAdjustment: StockAdjustment)

    @Query("SELECT * FROM CustomerPayment")
    fun getCustomerPayments(): List<CustomerPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerPayment(customerPayment: CustomerPayment)

    @Query("SELECT * FROM StockTransaction")
    fun getStockTransactions(): List<StockTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTransaction(stockTransaction: StockTransaction)

    @Query("SELECT * FROM LedgerEntry")
    fun getLedgerEntries(): List<LedgerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(ledgerEntry: LedgerEntry)

    @Query("SELECT * FROM AuditLog")
    fun getAuditLogs(): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(auditLog: AuditLog)

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
}
