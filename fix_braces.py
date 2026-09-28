with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

text = text.replace("        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }\n        _products.value = list", "        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }\n        }\n        _products.value = list")
text = text.replace("        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(customer) }\n        _customers.value = list", "        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(customer) }\n        }\n        _customers.value = list")
text = text.replace("        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(supplier) }\n        _suppliers.value = list", "        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(supplier) }\n        }\n        _suppliers.value = list")
text = text.replace("        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(expense) }\n        _expenses.value = list", "        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(expense) }\n        }\n        _expenses.value = list")

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
