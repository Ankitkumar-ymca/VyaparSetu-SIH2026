import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "private suspend fun <T> executeWithTransientRetry(block: suspend () -> T): T {",
    "private suspend fun <T> executeWithTransientRetry(onRetry: (() -> Unit)? = null, block: suspend () -> T): T {"
)

content = content.replace(
    "                kotlinx.coroutines.delay(delayMs)",
    "                onRetry?.invoke()\n                kotlinx.coroutines.delay(delayMs)"
)

content = content.replace(
    "executeWithTransientRetry {",
    "executeWithTransientRetry(onRetry) {"
)

content = content.replace(
    "suspend fun answerBusinessQuestion(question: String): Result<String> = withContext(Dispatchers.IO) {",
    "suspend fun answerBusinessQuestion(question: String, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun generateBusinessForecast(product: com.example.domain.model.Product, timesSold: Int, totalRevenue: Double): Result<String> = withContext(Dispatchers.IO) {",
    "suspend fun generateBusinessForecast(product: com.example.domain.model.Product, timesSold: Int, totalRevenue: Double, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun generateBusinessInsights(): Result<List<com.example.domain.model.BusinessInsight>> = withContext(Dispatchers.IO) {",
    "suspend fun generateBusinessInsights(onRetry: (() -> Unit)? = null): Result<List<com.example.domain.model.BusinessInsight>> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun extractInvoiceData(base64Image: String): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {",
    "suspend fun extractInvoiceData(base64Image: String, onRetry: (() -> Unit)? = null): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun generateMonthlySummary(): Result<String> = withContext(Dispatchers.IO) {",
    "suspend fun generateMonthlySummary(onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {"
)

content = content.replace(
    "suspend fun parseVoiceCommand(transcription: String): Result<com.example.domain.model.ProposedAction> = withContext(Dispatchers.IO) {",
    "suspend fun parseVoiceCommand(transcription: String, onRetry: (() -> Unit)? = null): Result<com.example.domain.model.ProposedAction> = withContext(Dispatchers.IO) {"
)

# And in generateContentWithRetry
content = content.replace(
    """    private suspend fun generateContentWithRetry(
        model: String,
        systemPrompt: String,
        userText: String,
        isJson: Boolean = false
    ): String? {""",
    """    private suspend fun generateContentWithRetry(
        model: String,
        systemPrompt: String,
        userText: String,
        isJson: Boolean = false,
        onRetry: (() -> Unit)? = null
    ): String? {"""
)


with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
