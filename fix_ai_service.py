import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

retry_function = """    private suspend fun <T> executeWithTransientRetry(block: suspend () -> T): T {
        val maxRetries = 3
        var currentAttempt = 0
        var delayMs = 1500L

        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                currentAttempt++
                val isTransient = when (e) {
                    is retrofit2.HttpException -> e.code() in 500..599 || e.code() == 429
                    is java.net.SocketTimeoutException -> true
                    is java.io.IOException -> true
                    else -> false
                }

                if (!isTransient || currentAttempt >= maxRetries) {
                    throw e
                }
                kotlinx.coroutines.delay(delayMs)
                delayMs = (delayMs * 1.5).toLong()
            }
        }
    }

    private fun handleGeminiException(e: Exception): Exception {"""

content = content.replace("    private fun handleGeminiException(e: Exception): Exception {", retry_function)

# Replace all RetrofitClient.service.generateContent calls with executeWithTransientRetry
old_call = """RetrofitClient.service.generateContent(
                    model = model,
                    apiKey = getApiKey(),
                    request = request
                )"""
new_call = """executeWithTransientRetry {
                    RetrofitClient.service.generateContent(
                        model = model,
                        apiKey = getApiKey(),
                        request = request
                    )
                }"""
content = content.replace(old_call, new_call)

old_call2 = """RetrofitClient.service.generateContent(
            model = model,
            apiKey = getApiKey(),
            request = request
        )"""
new_call2 = """executeWithTransientRetry {
            RetrofitClient.service.generateContent(
                model = model,
                apiKey = getApiKey(),
                request = request
            )
        }"""
content = content.replace(old_call2, new_call2)

old_call3 = """RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = getApiKey(),
                request = request
            )"""
new_call3 = """executeWithTransientRetry {
                RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = getApiKey(),
                    request = request
                )
            }"""
content = content.replace(old_call3, new_call3)


with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)

