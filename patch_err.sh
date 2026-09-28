cat << 'INNEREOF' > patch_err.kt
    private fun handleGeminiException(e: Exception): Exception {
        return when (e) {
            is retrofit2.HttpException -> {
                val errorBody = try { e.response()?.errorBody()?.string() } catch(t:Throwable){null}
                when (e.code()) {
                    400 -> Exception("API Error (400): Bad Request or Invalid API Key. Body: $errorBody")
                    401 -> Exception("API Error (401): Unauthorized. Please check your Gemini API key. Body: $errorBody")
                    403 -> Exception("API Error (403): Forbidden. API key may be restricted. Body: $errorBody")
                    404 -> Exception("API Error (404): Model not found. Body: $errorBody")
                    429 -> Exception("API Error (429): Quota exceeded. Too many requests. Body: $errorBody")
                    in 500..599 -> Exception("API Error (${e.code()}): Gemini server error. Body: $errorBody")
                    else -> Exception("API Error ${e.code()}: ${e.message}. Body: $errorBody")
                }
            }
            is java.net.SocketTimeoutException -> Exception("Network Error: Connection timed out.")
            is java.io.IOException -> Exception("Network Error: Please check your internet connection. ${e.message}")
            else -> Exception("Error: ${e.message}")
        }
    }
INNEREOF
sed -i -e '/private fun handleGeminiException/,/    }/c\' -e 'r patch_err.kt' -e 'd' app/src/main/java/com/example/data/repository/AiService.kt
