sed -i '/@androidx.room.Entity/ {
  N
  /data class SaleItem/ s/@androidx.room.Entity\n//
}' app/src/main/java/com/example/domain/model/Models.kt
sed -i '/@androidx.room.Entity/ {
  N
  /data class PurchaseItem/ s/@androidx.room.Entity\n//
}' app/src/main/java/com/example/domain/model/Models.kt
sed -i '/@androidx.room.Entity/ {
  N
  /data class ExtractedItem/ s/@androidx.room.Entity\n//
}' app/src/main/java/com/example/domain/model/Models.kt
sed -i '/@androidx.room.Entity/ {
  N
  /data class ProposedAction/ s/@androidx.room.Entity\n//
}' app/src/main/java/com/example/domain/model/Models.kt
sed -i '/@androidx.room.Entity/ {
  N
  /data class InvoiceExtractionResult/ s/@androidx.room.Entity\n//
}' app/src/main/java/com/example/domain/model/Models.kt
