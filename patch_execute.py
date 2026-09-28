import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

old_execute = """                            when (action.actionType) {
                                "CREATE_CUSTOMER" -> {
                                    val name = action.customerName ?: "New Customer"
                                    val newCust = Customer(id = UUID.randomUUID().toString(), businessId = businessId, name = name, phone = "", email = "", address = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveCustomer(newCust)
                                    success = res.isSuccess
                                }
                                "CREATE_SUPPLIER" -> {
                                    val name = action.supplierName ?: "New Supplier"
                                    val newSupp = Supplier(id = UUID.randomUUID().toString(), businessId = businessId, name = name, phone = "", email = "", address = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveSupplier(newSupp)
                                    success = res.isSuccess
                                }
                                "CREATE_EXPENSE" -> {
                                    val newExp = Expense(id = UUID.randomUUID().toString(), businessId = businessId, category = "General", description = action.notes ?: "", amount = action.amount ?: 0.0, paymentMethod = "CASH", notes = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveExpense(newExp)
                                    success = res.isSuccess
                                }
                                "CREATE_SALE" -> {
                                    val newSale = Sale(id = UUID.randomUUID().toString(), businessId = businessId, invoiceNumber = "INV-${timestamp}", customerId = null, items = emptyList(), subtotal = action.amount ?: 0.0, discount = 0.0, tax = 0.0, grandTotal = action.amount ?: 0.0, amountPaid = action.amount ?: 0.0, outstanding = 0.0, paymentStatus = "PAID", paymentMethod = "CASH", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.createSale(newSale)
                                    success = res.isSuccess
                                }
                                "CREATE_PURCHASE" -> {
                                    val newPurch = Purchase(id = UUID.randomUUID().toString(), businessId = businessId, referenceNumber = "PUR-${timestamp}", supplierId = "unknown", items = emptyList(), subtotal = action.amount ?: 0.0, discount = 0.0, tax = 0.0, grandTotal = action.amount ?: 0.0, amountPaid = action.amount ?: 0.0, outstanding = 0.0, paymentStatus = "PAID", paymentMethod = "CASH", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.createPurchase(newPurch)
                                    success = res.isSuccess
                                }
                                else -> { success = false }
                            }"""

new_execute = """                            var errorMessage = ""
                            when (action.actionType) {
                                "CREATE_CUSTOMER" -> {
                                    val name = action.customerName ?: "New Customer"
                                    val newCust = com.example.domain.model.Customer(id = java.util.UUID.randomUUID().toString(), businessId = businessId, name = name, phone = "", email = "", address = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveCustomer(newCust)
                                    success = res.isSuccess
                                }
                                "CREATE_SUPPLIER" -> {
                                    val name = action.supplierName ?: "New Supplier"
                                    val newSupp = com.example.domain.model.Supplier(id = java.util.UUID.randomUUID().toString(), businessId = businessId, name = name, phone = "", email = "", address = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveSupplier(newSupp)
                                    success = res.isSuccess
                                }
                                "CREATE_EXPENSE" -> {
                                    val newExp = com.example.domain.model.Expense(id = java.util.UUID.randomUUID().toString(), businessId = businessId, category = action.expenseCategory ?: "Other", description = action.notes ?: "", amount = action.amount ?: 0.0, paymentMethod = "CASH", notes = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveExpense(newExp)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "CREATE_SALE", "CREATE_CREDIT_SALE" -> {
                                    val qty = action.quantity ?: 1
                                    val amount = action.amount ?: 0.0
                                    val isCredit = action.actionType == "CREATE_CREDIT_SALE" || action.paymentMethod == "CREDIT"
                                    
                                    val items = listOf(com.example.domain.model.SaleItem(
                                        productId = action.productName ?: "", // this is actually the ID
                                        productName = "Product",
                                        quantity = qty,
                                        unitPrice = amount / qty,
                                        subtotal = amount
                                    ))
                                    
                                    val newSale = com.example.domain.model.Sale(
                                        id = java.util.UUID.randomUUID().toString(), 
                                        businessId = businessId, 
                                        invoiceNumber = "INV-${timestamp}", 
                                        customerId = action.customerName, // this is the ID
                                        items = items, 
                                        subtotal = amount, 
                                        discount = 0.0, 
                                        tax = 0.0, 
                                        grandTotal = amount, 
                                        amountPaid = if(isCredit) 0.0 else amount, 
                                        outstanding = if(isCredit) amount else 0.0, 
                                        paymentStatus = if(isCredit) "UNPAID" else "PAID", 
                                        paymentMethod = if(isCredit) "CREDIT" else (action.paymentMethod ?: "CASH"), 
                                        createdAt = timestamp, 
                                        updatedAt = timestamp
                                    )
                                    val res = MockDataStore.createSale(newSale)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "CREATE_PURCHASE" -> {
                                    val qty = action.quantity ?: 1
                                    val amount = action.amount ?: 0.0
                                    val isCredit = action.paymentMethod == "CREDIT"
                                    
                                    val items = listOf(com.example.domain.model.PurchaseItem(
                                        productId = action.productName ?: "", // ID
                                        productName = "Product",
                                        quantity = qty,
                                        unitPrice = amount / qty,
                                        subtotal = amount
                                    ))
                                    
                                    val newPurch = com.example.domain.model.Purchase(
                                        id = java.util.UUID.randomUUID().toString(), 
                                        businessId = businessId, 
                                        referenceNumber = "PUR-${timestamp}", 
                                        supplierId = action.supplierName ?: "", // ID
                                        items = items, 
                                        subtotal = amount, 
                                        discount = 0.0, 
                                        tax = 0.0, 
                                        grandTotal = amount, 
                                        amountPaid = if(isCredit) 0.0 else amount, 
                                        outstanding = if(isCredit) amount else 0.0, 
                                        paymentStatus = if(isCredit) "UNPAID" else "PAID", 
                                        paymentMethod = if(isCredit) "CREDIT" else "CASH", 
                                        createdAt = timestamp, 
                                        updatedAt = timestamp
                                    )
                                    val res = MockDataStore.createPurchase(newPurch)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "RECORD_PAYMENT" -> {
                                    val amount = action.amount ?: 0.0
                                    val payment = com.example.domain.model.CustomerPayment(
                                        id = java.util.UUID.randomUUID().toString(),
                                        businessId = businessId,
                                        customerId = action.customerName ?: "", // ID
                                        amount = amount,
                                        paymentMethod = action.paymentMethod ?: "CASH",
                                        referenceNumber = "",
                                        notes = action.notes ?: "",
                                        timestamp = timestamp
                                    )
                                    val res = MockDataStore.recordCustomerPayment(payment)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "ADD_OPENING_BALANCE" -> {
                                    val amount = action.amount ?: 0.0
                                    val customers = MockDataStore.customers.value.toMutableList()
                                    val custId = action.customerName ?: ""
                                    val idx = customers.indexOfFirst { it.id == custId }
                                    if (idx != -1) {
                                        val cust = customers[idx]
                                        customers[idx] = cust.copy(outstandingBalance = cust.outstandingBalance + amount, updatedAt = timestamp)
                                        com.example.data.repository.MockDataStore._customers.value = customers
                                        success = true
                                    } else {
                                        success = false
                                        errorMessage = "Customer not found."
                                    }
                                }
                                else -> { success = false; errorMessage = "Unknown action type." }
                            }"""

content = content.replace(old_execute, new_execute)

# Fix the success message block
old_success = """                                if (success) {
                                    updatedMessages.add(ChatMessage("Successfully executed!", false))
                                } else {
                                    updatedMessages.add(ChatMessage("Failed to execute.", false))
                                }"""

new_success = """                                val language = java.util.Locale.getDefault().language
                                if (success) {
                                    val msg = if (language == "hi") "लेन-देन सफलतापूर्वक दर्ज किया गया।" else "Action executed successfully."
                                    updatedMessages.add(ChatMessage(msg, false))
                                } else {
                                    val msg = if (language == "hi") "निष्पादन में विफल: $errorMessage" else "Failed to execute: $errorMessage"
                                    updatedMessages.add(ChatMessage(msg, false))
                                }"""

content = content.replace(old_success, new_success)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
