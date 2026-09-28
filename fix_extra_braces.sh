sed -i 's/list.add(product)\n        }\n        kotlinx.coroutines/list.add(product)\n        kotlinx.coroutines/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/storedUsers.add(newUser)\n        }\n        kotlinx.coroutines/storedUsers.add(newUser)\n        kotlinx.coroutines/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

sed -i 's/storedBusinesses.add(newBusiness)\n        }\n        kotlinx.coroutines/storedBusinesses.add(newBusiness)\n        kotlinx.coroutines/g' app/src/main/java/com/example/data/repository/MockDataStore.kt
