cat << 'INNEREOF' > patch_service.kt
    private fun handleGeminiException(e: Exception): Exception {
        return when (e) {
            is retrofit2.HttpException -> {
                when (e.code()) {
                    400 -> Exception("API Error (400): Bad Request or Invalid API Key.")
                    401 -> Exception("API Error (401): Unauthorized. Please check your Gemini API key.")
                    403 -> Exception("API Error (403): Forbidden. API key may be restricted.")
                    404 -> Exception("API Error (404): Model not found.")
                    429 -> Exception("API Error (429): Quota exceeded. Too many requests.")
                    in 500..599 -> Exception("API Error (${e.code()}): Gemini server error.")
                    else -> Exception("API Error ${e.code()}: ${e.message()}")
                }
            }
            is java.net.SocketTimeoutException -> Exception("Network Error: Connection timed out.")
            is java.io.IOException -> Exception("Network Error: Please check your internet connection.")
            else -> Exception("Error: ${e.message}")
        }
    }
INNEREOF
sed -i '/object AiService {/r patch_service.kt' app/src/main/java/com/example/data/repository/AiService.kt
sed -i 's/Result.failure(e)/Result.failure(handleGeminiException(e))/g' app/src/main/java/com/example/data/repository/AiService.kt
