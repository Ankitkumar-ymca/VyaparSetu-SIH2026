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
                Result.success(result)
