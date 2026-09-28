# Update Product
sed -i 's/list.add(product)/list.add(product)\n        }\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/list\[index\] = product/list[index] = product\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertProduct(product) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

# User and Business
sed -i 's/storedUsers.add(newUser)/storedUsers.add(newUser)\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertUser(newUser) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/storedBusinesses.add(newBusiness)/storedBusinesses.add(newBusiness)\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertBusiness(newBusiness) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/storedUsers\[userIndex\] = updatedUser/storedUsers[userIndex] = updatedUser\n            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertUser(updatedUser) }/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
