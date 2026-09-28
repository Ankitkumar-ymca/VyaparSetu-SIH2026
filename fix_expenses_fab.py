import re

with open('app/src/main/java/com/example/presentation/expenses/ExpensesScreen.kt', 'r') as f:
    text = f.read()

fab_code = """
            FloatingActionButton(
                onClick = onNavigateToNewExpense,
                containerColor = BlueNavy,                
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
"""
fab_code_alternative = """            FloatingActionButton(
                onClick = onNavigateToNewExpense,
                containerColor = BlueNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }"""

if fab_code in text:
    text = text.replace(fab_code, "")
elif fab_code_alternative in text:
    text = text.replace(fab_code_alternative, "")
else:
    # Use regex
    pattern = r'FloatingActionButton\(\s*onClick = onNavigateToNewExpense,[\s\S]*?\}\s*\}'
    text = re.sub(pattern, "", text)

with open('app/src/main/java/com/example/presentation/expenses/ExpensesScreen.kt', 'w') as f:
    f.write(text)

