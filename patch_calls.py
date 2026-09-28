import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

# answerBusinessQuestion
old_abq = """            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = question)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = getApiKey(),
                request = request
            )

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I couldn't generate an answer at this time."
            Result.success(text)"""

new_abq = """            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, question)
                ?: "I couldn't generate an answer at this time."
            Result.success(text)"""

content = content.replace(old_abq, new_abq)

# generateForecastAdvice
old_gfa = """            val request = GenerateContentRequest(
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
            Result.success(text)"""

new_gfa = """            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, "What should I do?")?.trim()
                ?: "Monitor your cash flow and sales closely."
            Result.success(text)"""

content = content.replace(old_gfa, new_gfa)

# generateBusinessInsights
old_gbi = """            val request = GenerateContentRequest(
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
                ?: "[]" """

new_gbi = """            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, "Generate insights", isJson = true)
                ?: "[]" """

content = content.replace(old_gbi, new_gbi)

# getProductInsights
old_pi = """            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = contentText)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = getApiKey(),
                request = request
            )

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Data looks good."
            Result.success(text)"""

new_pi = """            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, contentText)?.trim()
                ?: "Data looks good."
            Result.success(text)"""

content = content.replace(old_pi, new_pi)

# generateMonthlySummary
old_gms = """            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Generate monthly summary")))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = getApiKey(),
                request = request
            )

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Unable to generate summary at this time."
            Result.success(text)"""

new_gms = """            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, "Generate monthly summary")?.trim()
                ?: "Unable to generate summary at this time."
            Result.success(text)"""

content = content.replace(old_gms, new_gms)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
