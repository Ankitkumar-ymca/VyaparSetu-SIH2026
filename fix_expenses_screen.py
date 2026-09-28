import re

with open('app/src/main/java/com/example/presentation/expenses/ExpensesScreen.kt', 'r') as f:
    text = f.read()

old_exp_scr = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToNewExpense: () -> Unit
) {"""

new_exp_scr = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToNewExpense: () -> Unit,
    onEditExpense: (String) -> Unit = {}
) {"""

text = text.replace(old_exp_scr, new_exp_scr)

old_card = """@Composable
fun ExpenseCard(expense: Expense) {"""

new_card = """@Composable
fun ExpenseCard(expense: Expense, onEdit: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_delete_expense)) },
            text = { Text(stringResource(R.string.str_delete_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        if(isDeleting) return@Button
                        isDeleting = true
                        scope.launch {
                            MockDataStore.deleteExpense(expense.id)
                            isDeleting = false
                            showDeleteConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.str_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.str_cancel)) }
            }
        )
    }"""

text = text.replace(old_card, new_card)
text = text.replace("items(filteredExpenses) { expense ->\n                    ExpenseCard(expense)\n                }", "items(filteredExpenses) { expense ->\n                    ExpenseCard(expense, onEditExpense)\n                }")

# we also need the overflow menu in the card
old_card_layout = """        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.str_2f).format(expense.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AccentRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = expense.paymentMethod,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}"""

new_card_layout = """        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.str_2f).format(expense.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AccentRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = expense.paymentMethod,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_edit)) },
                            onClick = {
                                showMenu = false
                                onEdit(expense.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_delete)) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }
        }
    }
}"""

text = text.replace(old_card_layout, new_card_layout)

with open('app/src/main/java/com/example/presentation/expenses/ExpensesScreen.kt', 'w') as f:
    f.write(text)
