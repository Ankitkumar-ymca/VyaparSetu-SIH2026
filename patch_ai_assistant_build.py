import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

# Fix subtotal to totalPrice
content = content.replace("subtotal = amount", "totalPrice = amount")

# Fix recordCustomerPayment
old_payment = """                                "RECORD_PAYMENT" -> {
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
                                }"""

new_payment = """                                "RECORD_PAYMENT" -> {
                                    val amount = action.amount ?: 0.0
                                    val res = MockDataStore.recordCustomerPayment(
                                        customerId = action.customerName ?: "", 
                                        amount = amount, 
                                        method = action.paymentMethod ?: "CASH", 
                                        notes = action.notes ?: ""
                                    )
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }"""

content = content.replace(old_payment, new_payment)

# Fix ADD_OPENING_BALANCE outstandingBalance to outstandingAmount
content = content.replace("outstandingBalance = cust.outstandingBalance", "outstandingAmount = cust.outstandingAmount")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
