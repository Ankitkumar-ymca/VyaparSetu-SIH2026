package com.example.domain.model

@kotlinx.serialization.Serializable
enum class SyncStatus {
    SYNCED, PENDING, FAILED
}

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class AuditLog(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val timestamp: Long,
    val details: String = ""
)

@kotlinx.serialization.Serializable
data class ProposedAction(
    val actionType: String, // CREATE_SALE, RECORD_CUSTOMER_PAYMENT, CREATE_EXPENSE, ADJUST_STOCK, CREATE_PURCHASE
    val customerName: String? = null,
    val supplierName: String? = null,
    val productName: String? = null,
    val quantity: Int? = null,
    val amount: Double? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val expenseCategory: String? = null,
    val missingFields: List<String>? = null
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class User(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val businessId: String? = null
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Business(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val ownerName: String,
    val category: String,
    val phone: String,
    val address: String,
    val gstin: String?,
    val createdAt: Long
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Product(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val sku: String,
    val barcode: String,
    val category: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val currentStock: Int,
    val minStock: Int,
    val supplierId: String?,
    val expiryDate: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Customer(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Supplier(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstandingPayable: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class StockAdjustment(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val productId: String,
    val quantityChange: Int,
    val reason: String,
    val timestamp: Long
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class CustomerPayment(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val amount: Double,
    val paymentMethod: String,
    val notes: String,
    val timestamp: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Sale(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val invoiceNumber: String,
    val customerId: String?, // null for walk-in
    val items: List<SaleItem>,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val grandTotal: Double,
    val amountPaid: Double,
    val outstanding: Double,
    val paymentStatus: String, // PAID, PARTIALLY PAID, CREDIT
    val paymentMethod: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@kotlinx.serialization.Serializable
data class SaleItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val purchasePriceAtSale: Double = 0.0 // Added for COGS calculation
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Expense(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class Purchase(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val referenceNumber: String,
    val supplierId: String,
    val items: List<PurchaseItem>,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val grandTotal: Double,
    val amountPaid: Double,
    val outstanding: Double,
    val paymentStatus: String, // PAID, PARTIALLY PAID, CREDIT
    val paymentMethod: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@kotlinx.serialization.Serializable
data class PurchaseItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class StockTransaction(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val productId: String,
    val quantityChange: Int,
    val previousStock: Int,
    val newStock: Int,
    val transactionType: String, // SALE, PURCHASE, ADJUSTMENT, RETURN
    val referenceId: String?, // Invoice/Purchase ID
    val reason: String?,
    val timestamp: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@kotlinx.serialization.Serializable
data class InvoiceExtractionResult(
    val supplierName: String?,
    val invoiceNumber: String?,
    val invoiceDate: String?,
    val gstin: String?,
    val items: List<ExtractedItem>,
    val subtotal: Double?,
    val discount: Double?,
    val tax: Double?,
    val grandTotal: Double?
)

@kotlinx.serialization.Serializable
data class ExtractedItem(
    val productName: String?,
    val sku: String?,
    val quantity: Int?,
    val unitPrice: Double?,
    val total: Double?
)

@kotlinx.serialization.Serializable
@androidx.room.Entity
data class LedgerEntry(
    @androidx.room.PrimaryKey val id: String,
    val businessId: String,
    val partyId: String, // Customer ID or Supplier ID
    val type: String, // SALE, PURCHASE, PAYMENT
    val amount: Double,
    val balanceAfter: Double,
    val description: String,
    val timestamp: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@kotlinx.serialization.Serializable
data class ChatMessage(val text: String, val isUser: Boolean, val action: ProposedAction? = null, var isExecuted: Boolean = false)

data class PeriodMetrics(
    val periodName: String,
    val totalSales: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val profit: Double,
    val profitMargin: Double, // (profit / sales) * 100
    val salesTransactionCount: Int,
    val purchaseTransactionCount: Int,
    val expenseTransactionCount: Int
)

data class BusinessSummary(
    val currentPeriod: PeriodMetrics,
    val previousPeriod: PeriodMetrics?,
    
    val customerOutstanding: Double,
    val supplierBalance: Double,
    val inventoryValue: Double,
    
    val numberOfCustomers: Int,
    val numberOfSuppliers: Int,
    val numberOfProducts: Int,
    
    val healthScore: Int,
    val healthStatus: String, // Excellent, Good, Needs Attention, Critical
    val healthReasons: List<String>,
    
    val topExpenseCategories: List<String>,
    val topProducts: List<String>,
    val lowStockProducts: List<String>
)

data class BusinessForecast(
    val cashFlowRisk: String,
    val cashFlowMessage: String,
    
    val salesTrend: String,
    val salesMessage: String,
    
    val expenseTrend: String,
    val expenseMessage: String,
    
    val inventoryRisk: String,
    val inventoryMessage: String,
    
    val outstandingRisk: String,
    val outstandingMessage: String,
    
    val overallRisk: String,
    val confidence: String
)

@kotlinx.serialization.Serializable
data class BusinessInsight(
    val observation: String,
    val reason: String,
    val recommendation: String
)
