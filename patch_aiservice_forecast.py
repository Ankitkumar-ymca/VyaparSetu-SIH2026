import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

forecast_advice_function = """
    suspend fun generateForecastAdvice(forecast: com.example.domain.model.BusinessForecast): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = \"\"\"
                You are VyaparSetu AI, an expert business advisor for small businesses in India.
                Review the following deterministic business forecast and provide 2-3 short, highly actionable pieces of advice on 'What should I do?'.
                Keep it simple and practical for a micro-entrepreneur.
                IMPORTANT: You MUST reply in the EXACT SAME LANGUAGE as requested, typically English or Hindi, depending on the context. But for this endpoint, assume English unless specified. Actually, just provide it in English, the app will handle translation or the user will read it. Wait, the prompt says "Generate practical recommendations based on actual detected conditions."
                Use bullet points. Do NOT invent new numbers.
                
                FORECAST DATA:
                Overall Risk: ${forecast.overallRisk} (Confidence: ${forecast.confidence})
                Cash Flow: ${forecast.cashFlowRisk} - ${forecast.cashFlowMessage}
                Sales Trend: ${forecast.salesTrend} - ${forecast.salesMessage}
                Expenses: ${forecast.expenseTrend} - ${forecast.expenseMessage}
                Inventory Risk: ${forecast.inventoryRisk} - ${forecast.inventoryMessage}
                Outstanding Risk: ${forecast.outstandingRisk} - ${forecast.outstandingMessage}
            \"\"\".trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "What should I do?")))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = getApiKey(),
                request = request
            )

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Monitor your cash flow and sales closely."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }
"""

if 'fun generateForecastAdvice' not in content:
    content = content.replace("suspend fun answerBusinessQuestion", forecast_advice_function + "\n    suspend fun answerBusinessQuestion")
    with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
        f.write(content)
    print("Added generateForecastAdvice")
