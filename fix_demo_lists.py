with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

text = text.replace("_products.value = list\n        listOf(", "_products.value = listOf(")
text = text.replace("_customers.value = list\n        listOf(", "_customers.value = listOf(")
text = text.replace("_suppliers.value = list\n        listOf(", "_suppliers.value = listOf(")
text = text.replace("_expenses.value = list\n        listOf(", "_expenses.value = listOf(")

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
