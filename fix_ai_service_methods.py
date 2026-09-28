import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "suspend fun suggestExpenseCategory(description: String): Result<String> = withContext(Dispatchers.IO) {",
    "suspend fun suggestExpenseCategory(description: String, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun getProductInsights(productId: String): Result<String> = withContext(Dispatchers.IO) {",
    "suspend fun getProductInsights(productId: String, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun extractInvoiceData(base64Image: String): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {",
    "suspend fun extractInvoiceData(base64Image: String, onRetry: (() -> Unit)? = null): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {"
)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)

