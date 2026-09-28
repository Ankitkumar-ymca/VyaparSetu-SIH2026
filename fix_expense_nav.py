import re

with open('app/src/main/java/com/example/presentation/navigation/AppNavigation.kt', 'r') as f:
    text = f.read()

old_exp = """        composable("expenses") {
            com.example.presentation.expenses.ExpensesScreen(
                onNavigateToNewExpense = { navController.navigate("new_expense") }
            )
        }
        
        composable("new_expense") {
            com.example.presentation.expenses.NewExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }"""

new_exp = """        composable("expenses") {
            com.example.presentation.expenses.ExpensesScreen(
                onNavigateToNewExpense = { navController.navigate("new_expense") },
                onEditExpense = { id -> navController.navigate("new_expense?expenseId=$id") }
            )
        }
        
        composable(
            route = "new_expense?expenseId={expenseId}",
            arguments = listOf(androidx.navigation.navArgument("expenseId") { 
                nullable = true; defaultValue = null 
            })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            com.example.presentation.expenses.NewExpenseScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }"""

if old_exp in text:
    text = text.replace(old_exp, new_exp)
else:
    # try regex
    pattern = r'composable\("expenses"\) \{.*?composable\("new_expense"\) \{.*?\n\s+\}'
    text = re.sub(pattern, new_exp, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/navigation/AppNavigation.kt', 'w') as f:
    f.write(text)
