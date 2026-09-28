import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

old_balance = """                                "ADD_OPENING_BALANCE" -> {
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
                                }"""

new_balance = """                                "ADD_OPENING_BALANCE" -> {
                                    val amount = action.amount ?: 0.0
                                    val customers = MockDataStore.customers.value
                                    val custId = action.customerName ?: ""
                                    val cust = customers.find { it.id == custId }
                                    if (cust != null) {
                                        val updatedCust = cust.copy(outstandingBalance = cust.outstandingBalance + amount, updatedAt = timestamp)
                                        val res = MockDataStore.saveCustomer(updatedCust)
                                        success = res.isSuccess
                                        if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                    } else {
                                        success = false
                                        errorMessage = "Customer not found."
                                    }
                                }"""

content = content.replace(old_balance, new_balance)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
