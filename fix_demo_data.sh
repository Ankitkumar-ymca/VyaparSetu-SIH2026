sed -i 's/list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(it) } }Of/listOf/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertCustomer(it) } }Of/listOf/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertSupplier(it) } }Of/listOf/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
sed -i 's/list.forEach { kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertExpense(it) } }Of/listOf/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
