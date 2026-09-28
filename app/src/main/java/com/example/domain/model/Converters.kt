package com.example.domain.model

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSaleItemList(value: List<SaleItem>): String = json.encodeToString(value)

    @TypeConverter
    fun toSaleItemList(value: String): List<SaleItem> = json.decodeFromString(value)

    @TypeConverter
    fun fromPurchaseItemList(value: List<PurchaseItem>): String = json.encodeToString(value)

    @TypeConverter
    fun toPurchaseItemList(value: String): List<PurchaseItem> = json.decodeFromString(value)
}
