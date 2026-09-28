import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

# Replace generateBusinessInsights definition and parsing
old_gbi = """    suspend fun generateBusinessInsights(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = \"\"\"
                
                You are an AI business analyst. Based on the business context, generate exactly 3 short, actionable insights.
                Structure recommendations as:
                Observation: [What happened]
                Reason: [Why it happened]
                Recommended Action: [What to do]
                
                Focus on sales trends, low stock, or outstanding balances.
                Do NOT invent numbers. Use ONLY the provided context. If there is not enough data, say "More business data is required before making a reliable recommendation."
                ${getLanguageInstruction()}
                
                Format as a JSON array of strings: ["Observation: ... Reason: ... Recommended Action: ...", "...", "..."]
CURRENT BUSINESS CONTEXT:
                ${getBusinessContext()}
            \"\"\".trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Generate insights")))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = getApiKey(),
                request = request
            )

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                val jsonArray = Json.parseToJsonElement(text).jsonArray
                val insights = jsonArray.map { it.jsonPrimitive.content }
                Result.success(insights)
            } else {
                Result.failure(Exception("Empty response"))
            }
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }"""

new_gbi = """    suspend fun generateBusinessInsights(): Result<List<com.example.domain.model.BusinessInsight>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = \"\"\"
                
                You are an AI business analyst. Based on the business context, generate exactly 3 short, actionable insights.
                Focus on sales trends, low stock, or outstanding balances.
                Do NOT invent numbers. Use ONLY the provided context.
                ${getLanguageInstruction()}
                
                CRITICAL INSTRUCTION:
                You MUST return a JSON array of objects with EXACTLY these English keys: "Observation", "Reason", "Recommendation".
                However, the VALUES for these keys must be in the requested language (e.g. Devanagari Hindi or English).
                Example format:
                [
                  {
                    "Observation": "...",
                    "Reason": "...",
                    "Recommendation": "..."
                  }
                ]
                
CURRENT BUSINESS CONTEXT:
                ${getBusinessContext()}
            \"\"\".trimIndent()

            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, "Generate insights", isJson = true)
            if (text != null) {
                val jsonArray = Json.parseToJsonElement(text).jsonArray
                val insights = jsonArray.map { 
                    val obj = it.jsonObject
                    com.example.domain.model.BusinessInsight(
                        observation = obj["Observation"]?.jsonPrimitive?.content ?: "",
                        reason = obj["Reason"]?.jsonPrimitive?.content ?: "",
                        recommendation = obj["Recommendation"]?.jsonPrimitive?.content ?: ""
                    )
                }
                Result.success(insights)
            } else {
                Result.failure(Exception("Empty response"))
            }
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }"""

content = content.replace(old_gbi, new_gbi)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)

