import re

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'r') as f:
    text = f.read()

# I will just write a regex to replace the LaunchedEffect body
pattern = r'LaunchedEffect\(existingExpense\) \{.*?\n    \}'
new_body = """    var category by remember { mutableStateOf("Rent") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }
    
    LaunchedEffect(existingExpense) {
        if(existingExpense != null) {
            amount = existingExpense.amount.toString()
            description = existingExpense.description
            category = existingExpense.category
            paymentMethod = existingExpense.paymentMethod
            notes = existingExpense.notes
        }
    }"""

text = re.sub(r'LaunchedEffect\(existingExpense\) \{.*?\n    \}', new_body, text, flags=re.DOTALL)

# Also need to remove the old declarations of category, paymentMethod, notes
text = re.sub(r'    var category by remember \{ mutableStateOf\("Rent"\) \}\n', '', text, count=1)
text = re.sub(r'    var paymentMethod by remember \{ mutableStateOf\("Cash"\) \}\n', '', text, count=1)
text = re.sub(r'    var notes by remember \{ mutableStateOf\(""\) \}\n', '', text, count=1)


with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'w') as f:
    f.write(text)
