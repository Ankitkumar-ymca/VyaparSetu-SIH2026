import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

# Add Expenses to Bottom Navigation Bar
text = text.replace(
    'Pair("Khata", "👥"),\n        Pair("Reports", "📊")',
    'Pair("Khata", "👥"),\n        Pair("Expenses", "💸"),\n        Pair("Reports", "📊")'
)

# Add handling for "Expenses" tab in DashboardScreen
expenses_case = """
        } else if (currentTab == "Expenses") {
            Box(modifier = Modifier.padding(innerPadding)) {
                com.example.presentation.expenses.ExpensesScreen(
                    onNavigateToNewExpense = { onNavigateToSubRoute("new_expense") }
                )
            }
"""
text = text.replace('} else if (currentTab == "Reports") {', expenses_case + '\n        } else if (currentTab == "Reports") {')

# Adjust FAB for Expenses tab to show "Add Expense"
fab_code = """
            } else if (currentTab == "Expenses") {
                FloatingActionButton(
                    onClick = { onNavigateToSubRoute("new_expense") },
                    containerColor = BlueLight,
                    contentColor = BlueNavy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
                }
"""
text = text.replace('} else if (currentTab == "Khata") {', fab_code + '\n            } else if (currentTab == "Khata") {')

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)

