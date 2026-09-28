with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

text = text.replace("list.add(product)\n        }\n        kotlinx.coroutines", "list.add(product)\n        kotlinx.coroutines")
text = text.replace("storedUsers.add(newUser)\n        }\n        kotlinx.coroutines", "storedUsers.add(newUser)\n        kotlinx.coroutines")
text = text.replace("storedBusinesses.add(newBusiness)\n        }\n        kotlinx.coroutines", "storedBusinesses.add(newBusiness)\n        kotlinx.coroutines")

# Also fix the one in `saveProduct` which has an extra `}` right after `kotlinx.coroutines... insertProduct(product) }`
text = text.replace("dao?.insertProduct(product) }\n        }\n        _products.value = list", "dao?.insertProduct(product) }\n        _products.value = list")

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
