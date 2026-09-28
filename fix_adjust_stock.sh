sed -i 's/kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }.copy/list[index] = product.copy/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
