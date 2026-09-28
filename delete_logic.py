import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

text = text.replace("dao?.deleteProduct(productId) }", "dao?.deleteProduct(productId) }\n        return Result.success(true)")
text = text.replace("suspend fun deleteProduct(productId: String) {", "suspend fun deleteProduct(productId: String): Result<Boolean> {")

# Let's see if deleteProduct exists
