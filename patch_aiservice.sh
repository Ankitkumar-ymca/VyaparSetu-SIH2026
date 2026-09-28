sed -i 's/item.subtotal/item.totalPrice/g' app/src/main/java/com/example/data/repository/AiService.kt
sed -i 's/product.stockQuantity/product.currentStock/g' app/src/main/java/com/example/data/repository/AiService.kt

cat << 'INNEREOF' > patch_invoice.kt
                val extractedItems = itemsArray.mapNotNull {
                    val itemObj = it.jsonObject
                    com.example.domain.model.ExtractedItem(
                        productName = itemObj["productName"]?.jsonPrimitive?.contentOrNull ?: "",
                        sku = itemObj["sku"]?.jsonPrimitive?.contentOrNull,
                        quantity = itemObj["quantity"]?.jsonPrimitive?.intOrNull ?: 1,
                        unitPrice = itemObj["unitPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        total = itemObj["subtotal"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    )
                }

                val result = com.example.domain.model.InvoiceExtractionResult(
                    supplierName = jsonObj["supplierName"]?.jsonPrimitive?.contentOrNull,
                    invoiceNumber = jsonObj["invoiceNumber"]?.jsonPrimitive?.contentOrNull,
                    invoiceDate = jsonObj["invoiceDate"]?.jsonPrimitive?.contentOrNull,
                    gstin = jsonObj["gstin"]?.jsonPrimitive?.contentOrNull,
                    items = extractedItems,
                    subtotal = jsonObj["subtotal"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    discount = jsonObj["totalDiscount"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    tax = jsonObj["totalTax"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    grandTotal = jsonObj["grandTotal"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
INNEREOF
sed -i -e '/val extractedItems = itemsArray.mapNotNull {/,/grandTotal = jsonObj\["grandTotal"\]?.jsonPrimitive?.doubleOrNull ?: 0.0\n                )/c\' -e 'r patch_invoice.kt' app/src/main/java/com/example/data/repository/AiService.kt

sed -i 's/summary = jsonObj\["summary"\]?.jsonPrimitive?.contentOrNull ?: "Unknown action"/notes = jsonObj\["summary"\]?.jsonPrimitive?.contentOrNull ?: "Unknown action"/g' app/src/main/java/com/example/data/repository/AiService.kt

