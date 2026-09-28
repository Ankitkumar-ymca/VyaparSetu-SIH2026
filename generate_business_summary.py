with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    content = f.read()

# We need to add `suspend fun getBusinessSummary(): BusinessSummary` to MockDataStore.
