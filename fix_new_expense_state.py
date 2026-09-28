import re

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("    var category by remember { mutableStateOf(\"Rent\") }", "") # clean if exists
text = text.replace("    var paymentMethod by remember { mutableStateOf(\"Cash\") }", "")
text = text.replace("    var notes by remember { mutableStateOf(\"\") }", "")

insert_pos = text.find("    var description by remember { mutableStateOf(\"\") }")
if insert_pos != -1:
    addition = """    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rent") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }"""
    text = text.replace('    var description by remember { mutableStateOf("") }', addition, 1)

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'w') as f:
    f.write(text)
