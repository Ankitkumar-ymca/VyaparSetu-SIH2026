import re

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'r') as f:
    text = f.read()

old_header = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }"""

new_header = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    expenseId: String? = null,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allExpenses by MockDataStore.expenses.collectAsState()
    val existingExpense = remember(expenseId, allExpenses) { allExpenses.find { it.id == expenseId } }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    LaunchedEffect(existingExpense) {
        if(existingExpense != null) {
            amount = existingExpense.amount.toString()
            description = existingExpense.description
            // Need to set category, method, notes if we can access them here
        }
    }"""

text = text.replace(old_header, new_header)

old_save = """                        val expense = Expense(
                            id = UUID.randomUUID().toString(),
                            businessId = currentBusiness?.id ?: "",
                            category = category,
                            description = description,
                            amount = parsedAmount,
                            paymentMethod = paymentMethod,
                            notes = notes,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        coroutineScope.launch {
                            MockDataStore.addExpense(expense)
                            onNavigateBack()
                        }"""

new_save = """                        coroutineScope.launch {
                            if (existingExpense != null) {
                                val updated = existingExpense.copy(
                                    category = category,
                                    description = description,
                                    amount = parsedAmount,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    updatedAt = System.currentTimeMillis()
                                )
                                MockDataStore.updateExpense(updated)
                            } else {
                                val expense = Expense(
                                    id = UUID.randomUUID().toString(),
                                    businessId = currentBusiness?.id ?: "",
                                    category = category,
                                    description = description,
                                    amount = parsedAmount,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                MockDataStore.addExpense(expense)
                            }
                            onNavigateBack()
                        }"""

text = text.replace(old_save, new_save)

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'w') as f:
    f.write(text)
