import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

# Extract the body of generateBusinessInsights
start_idx = content.find("suspend fun generateBusinessInsights")
end_idx = content.find("suspend fun analyzeInvoice", start_idx)

if start_idx != -1 and end_idx != -1:
    new_method = """suspend fun generateBusinessInsights(): Result<List<com.example.domain.model.BusinessInsight>> = withContext(Dispatchers.IO) {
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
                val jsonArray = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonArray
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
    }

    """
    content = content[:start_idx] + new_method + content[end_idx:]
    with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Could not find start or end index")
