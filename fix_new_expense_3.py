import re

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'r') as f:
    text = f.read()

old_logic = """                        coroutineScope.launch {
                            val newExpense = Expense(
                                id = UUID.randomUUID().toString(),
                                businessId = currentBusiness!!.id,
                                category = category,
                                description = description,
                                amount = amountVal,
                                paymentMethod = paymentMethod,
                                notes = notes,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            val result = MockDataStore.saveExpense(newExpense)
                            if (result.isSuccess) {
                                onNavigateBack()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save expense"
                                isSaving = false
                            }
                        }"""

new_logic = """                        coroutineScope.launch {
                            val result = if (existingExpense != null) {
                                val updated = existingExpense.copy(
                                    category = category,
                                    description = description,
                                    amount = amountVal,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    updatedAt = System.currentTimeMillis()
                                )
                                MockDataStore.updateExpense(updated)
                            } else {
                                val newExpense = Expense(
                                    id = UUID.randomUUID().toString(),
                                    businessId = currentBusiness!!.id,
                                    category = category,
                                    description = description,
                                    amount = amountVal,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                MockDataStore.saveExpense(newExpense)
                            }
                            if (result.isSuccess) {
                                onNavigateBack()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save expense"
                                isSaving = false
                            }
                        }"""

text = text.replace(old_logic, new_logic)

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'w') as f:
    f.write(text)
