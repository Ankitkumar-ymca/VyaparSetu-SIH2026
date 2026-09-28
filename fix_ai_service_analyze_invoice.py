import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "suspend fun analyzeInvoice(bitmap: Bitmap): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {",
    "suspend fun analyzeInvoice(bitmap: Bitmap, onRetry: (() -> Unit)? = null): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {"
)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)

