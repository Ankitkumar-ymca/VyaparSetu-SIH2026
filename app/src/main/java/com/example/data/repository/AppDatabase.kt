package com.example.data.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.domain.model.*

@Database(entities = [
    User::class, Business::class, Product::class, Customer::class, 
    Supplier::class, Sale::class, Purchase::class, Expense::class, 
    StockAdjustment::class, CustomerPayment::class, StockTransaction::class, 
    LedgerEntry::class, AuditLog::class
], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
