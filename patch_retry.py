import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

helper = """
    private suspend fun generateContentWithRetry(
        model: String,
        systemPrompt: String,
        userText: String,
        isJson: Boolean = false
    ): String? {
        val config = if (isJson) GenerationConfig(responseMimeType = "application/json") else null
        
        var request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userText)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = config
        )
        
        var response = RetrofitClient.service.generateContent(
            model = model,
            apiKey = getApiKey(),
            request = request
        )
        
        var text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        
        if (text != null && java.util.Locale.getDefault().language == "hi") {
            val latinCount = text.count { it in 'a'..'z' || it in 'A'..'Z' }
            val devanagariCount = text.count { it in '\u0900'..'\u097F' }
            
            // If very little Devanagari is found but it's supposed to be Hindi
            if (devanagariCount < 10 && latinCount > 20) {
                val strongerPrompt = "$systemPrompt\\n\\nCRITICAL: Your previous response was in English or Roman Hindi. You MUST rewrite your response entirely in proper Devanagari Hindi (हिन्दी) script. DO NOT use English alphabets for Hindi words."
                
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = userText)))),
                    systemInstruction = Content(parts = listOf(Part(text = strongerPrompt))),
                    generationConfig = config
                )
                
                response = RetrofitClient.service.generateContent(
                    model = model,
                    apiKey = getApiKey(),
                    request = request
                )
                
                text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }
        }
        
        return text
    }
    
    private fun getLanguageInstruction(): String {"""

content = content.replace("    private fun getLanguageInstruction(): String {", helper)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
