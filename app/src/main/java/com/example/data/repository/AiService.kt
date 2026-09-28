package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

object AiService {

    private suspend fun <T> executeWithTransientRetry(onRetry: (() -> Unit)? = null, block: suspend () -> T): T {
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
                onRetry?.invoke()
                kotlinx.coroutines.delay(delayMs)
                delayMs = (delayMs * 1.5).toLong()
            }
        }
    }

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



    private suspend fun generateContentWithRetry(
        model: String,
        systemPrompt: String,
        userText: String,
        isJson: Boolean = false,
        onRetry: (() -> Unit)? = null
    ): String? {
        val config = if (isJson) GenerationConfig(responseMimeType = "application/json") else null
        
        var request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userText)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = config
        )
        
        var response = executeWithTransientRetry(onRetry) {
            RetrofitClient.service.generateContent(
                model = model,
                apiKey = getApiKey(),
                request = request
            )
        }
        
        var text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        
        if (text != null && java.util.Locale.getDefault().language == "hi") {
            val latinCount = text.count { it in 'a'..'z' || it in 'A'..'Z' }
            val devanagariCount = text.count { it in 'ऀ'..'ॿ' }
            
            // If very little Devanagari is found but it's supposed to be Hindi
            if (devanagariCount < 10 && latinCount > 20) {
                val strongerPrompt = "$systemPrompt\n\nCRITICAL: Your previous response was in English or Roman Hindi. You MUST rewrite your response entirely in proper Devanagari Hindi (हिन्दी) script. DO NOT use English alphabets for Hindi words."
                
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = userText)))),
                    systemInstruction = Content(parts = listOf(Part(text = strongerPrompt))),
                    generationConfig = config
                )
                
                response = executeWithTransientRetry(onRetry) {
                    RetrofitClient.service.generateContent(
                        model = model,
                        apiKey = getApiKey(),
                        request = request
                    )
                }
                
                text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }
        }
        
        return text
    }
    
    private fun getLanguageInstruction(): String {
        val isHindi = java.util.Locale.getDefault().language == "hi"
        return if (isHindi) {
            "Respond ONLY in Hindi using Devanagari script (हिन्दी). Do not use Hinglish, Roman Hindi, or English alphabets for Hindi words. English technical/product names may remain in English only when necessary."
        } else {
            "Respond ONLY in English."
        }
    }
    
    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun getBusinessContext(): String {
        val summary = MockDataStore.getBusinessSummary() ?: return "No business data available."
        
        return """
            Business Summary:
            Period: ${summary.currentPeriod.periodName}
            Sales: ₹${summary.currentPeriod.totalSales}
            Purchases: ₹${summary.currentPeriod.totalPurchases}
            Expenses: ₹${summary.currentPeriod.totalExpenses}
            Profit: ₹${summary.currentPeriod.profit}
            Profit Margin: ${summary.currentPeriod.profitMargin}%
            
            Previous Period (${summary.previousPeriod?.periodName}):
            Sales: ₹${summary.previousPeriod?.totalSales ?: 0.0}
            Profit: ₹${summary.previousPeriod?.profit ?: 0.0}
            
            Customer Outstanding: ₹${summary.customerOutstanding}
            Supplier Balance: ₹${summary.supplierBalance}
            Inventory Value: ₹${summary.inventoryValue}
            
            Top Expense Categories: ${summary.topExpenseCategories.joinToString(", ")}
            Top Products: ${summary.topProducts.joinToString(", ")}
            Low Stock Products: ${summary.lowStockProducts.joinToString(", ")}
            
            Health Score: ${summary.healthScore}/100 (${summary.healthStatus})
            Health Reasons: ${summary.healthReasons.joinToString(", ")}
            
            Business Forecast (Next 30 days):
            ${MockDataStore.getBusinessForecast()?.let {
                "Overall Risk: ${it.overallRisk}, Cash Flow: ${it.cashFlowRisk}, Sales Trend: ${it.salesTrend}, Expense Trend: ${it.expenseTrend}, Inventory Risk: ${it.inventoryRisk}"
            } ?: "Not enough data for forecast."}
        """.trimIndent()
    }

    
    suspend fun generateForecastAdvice(forecast: com.example.domain.model.BusinessForecast): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are VyaparSetu AI, an expert business advisor for small businesses in India.
                Review the following deterministic business forecast and provide 2-3 short, highly actionable pieces of advice on 'What should I do?'.
                Keep it simple and practical for a micro-entrepreneur.
                ${getLanguageInstruction()}
                Use bullet points. Do NOT invent new numbers.
                
                FORECAST DATA:
                Overall Risk: ${forecast.overallRisk} (Confidence: ${forecast.confidence})
                Cash Flow: ${forecast.cashFlowRisk} - ${forecast.cashFlowMessage}
                Sales Trend: ${forecast.salesTrend} - ${forecast.salesMessage}
                Expenses: ${forecast.expenseTrend} - ${forecast.expenseMessage}
                Inventory Risk: ${forecast.inventoryRisk} - ${forecast.inventoryMessage}
                Outstanding Risk: ${forecast.outstandingRisk} - ${forecast.outstandingMessage}
            """.trimIndent()

            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, "What should I do?")?.trim()
                ?: "Monitor your cash flow and sales closely."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }

    suspend fun answerBusinessQuestion(question: String, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                
                You are VyaparSetu AI, an expert business assistant for small business owners in India.
                You are analyzing a small-business database. Use ONLY the supplied business context. 
                Never invent sales, expenses, customers, products, balances, dates or financial values. 
                If information is unavailable, explicitly say that sufficient data is not available.
                Always use a helpful, professional, and encouraging tone.
                ${getLanguageInstruction()}
CURRENT BUSINESS CONTEXT:
                ${getBusinessContext()}
            """.trimIndent()

            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, question)
                ?: "I couldn't generate an answer at this time."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }

    suspend fun suggestExpenseCategory(description: String, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = "You categorize expenses. Categories: Rent, Electricity, Internet, Salary, Transport, Maintenance, Marketing, Packaging, Office Supplies, Bank Charges, Taxes, Other. Return ONLY the category name. Do not add any extra text."
            
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = description)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = executeWithTransientRetry(onRetry) {
                RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = getApiKey(),
                    request = request
                )
            }

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "Other"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }

    suspend fun getProductInsights(productId: String, onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val product = MockDataStore.products.value.find { it.id == productId }
                ?: return@withContext Result.failure(Exception("Product not found"))
            
            val bid = product.businessId
            val sales = MockDataStore.sales.value.filter { it.businessId == bid }
            var timesSold = 0
            var totalRevenue = 0.0
            sales.forEach { sale ->
                sale.items.find { it.productId == productId }?.let { item ->
                    timesSold += item.quantity
                    totalRevenue += item.totalPrice
                }
            }

            val systemPrompt = """
                You are a smart inventory assistant.
                Analyze the following product data and provide a 1-sentence insight (e.g., 'Selling well, consider restocking soon' or 'Slow moving, maybe offer a discount').
                ${getLanguageInstruction()}
            """.trimIndent()

            val contentText = """
                Product: ${product.name}
                Current Stock: ${product.currentStock}
                Price: ₹${product.sellingPrice}
                Times Sold: $timesSold
                Total Revenue from Product: ₹$totalRevenue
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = contentText)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = executeWithTransientRetry(onRetry) {
                RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = getApiKey(),
                    request = request
                )
            }

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Stock levels look fine. Monitor sales velocity."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }
    
    suspend fun generateBusinessInsights(onRetry: (() -> Unit)? = null): Result<List<com.example.domain.model.BusinessInsight>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                
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
            """.trimIndent()

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

    suspend fun analyzeInvoice(bitmap: Bitmap, onRetry: (() -> Unit)? = null): Result<com.example.domain.model.InvoiceExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are an invoice extraction tool. Extract the details from the provided invoice image.
                Return ONLY a JSON object matching this schema. If a value is not found, return null.
                Schema:
                {
                  "supplierName": "String",
                  "invoiceNumber": "String",
                  "invoiceDate": "String (YYYY-MM-DD)",
                  "gstin": "String",
                  "items": [
                    {
                      "productName": "String",
                      "sku": "String",
                      "quantity": Int,
                      "unitPrice": Double,
                      "discount": Double,
                      "tax": Double,
                      "subtotal": Double
                    }
                  ],
                  "subtotal": Double,
                  "totalDiscount": Double,
                  "totalTax": Double,
                  "grandTotal": Double
                }
            """.trimIndent()

            val base64Image = bitmap.toBase64()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = "Extract invoice details"),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = executeWithTransientRetry(onRetry) {
                RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = getApiKey(),
                    request = request
                )
            }

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                val jsonObj = Json.parseToJsonElement(text).jsonObject
                
                val itemsArray = jsonObj["items"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
                val extractedItems = itemsArray.mapNotNull {
                    val itemObj = it.jsonObject
                    com.example.domain.model.ExtractedItem(
                        productName = itemObj["productName"]?.jsonPrimitive?.contentOrNull ?: "",
                        sku = itemObj["sku"]?.jsonPrimitive?.contentOrNull,
                        quantity = itemObj["quantity"]?.jsonPrimitive?.intOrNull ?: 1,
                        unitPrice = itemObj["unitPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        total = itemObj["subtotal"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    )
                }

                val result = com.example.domain.model.InvoiceExtractionResult(
                    supplierName = jsonObj["supplierName"]?.jsonPrimitive?.contentOrNull,
                    invoiceNumber = jsonObj["invoiceNumber"]?.jsonPrimitive?.contentOrNull,
                    invoiceDate = jsonObj["invoiceDate"]?.jsonPrimitive?.contentOrNull,
                    gstin = jsonObj["gstin"]?.jsonPrimitive?.contentOrNull,
                    items = extractedItems,
                    subtotal = jsonObj["subtotal"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    discount = jsonObj["totalDiscount"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    tax = jsonObj["totalTax"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    grandTotal = jsonObj["grandTotal"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
                Result.success(result)
            } else {
                Result.failure(Exception("Could not extract data."))
            }
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }
    
    suspend fun generateMonthlySummary(onRetry: (() -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                
                You are VyaparSetu AI, an expert business assistant for small business owners in India.
                You are analyzing a small-business database. Use ONLY the supplied business context. 
                Never invent sales, expenses, customers, products, balances, dates or financial values. 
                If information is unavailable, explicitly say that sufficient data is not available.
                Always use a helpful, professional, and encouraging tone.
                ${getLanguageInstruction()}
CURRENT BUSINESS CONTEXT:
                ${getBusinessContext()}
            """.trimIndent()

            val text = generateContentWithRetry("gemini-3.5-flash", systemPrompt, "Generate monthly summary")?.trim()
                ?: "Unable to generate summary at this time."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }

    suspend fun parseVoiceCommand(transcription: String, onRetry: (() -> Unit)? = null): Result<com.example.domain.model.ProposedAction> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are VyaparSetu AI, an expert business assistant. Parse the user's spoken or typed command (Hinglish, Hindi, or English) into a structured action.
                Return ONLY a JSON object with this exact schema:
                {
                  "type": "CREATE_CUSTOMER" | "CREATE_SUPPLIER" | "CREATE_SALE" | "CREATE_CREDIT_SALE" | "CREATE_PURCHASE" | "CREATE_EXPENSE" | "RECORD_PAYMENT" | "ADD_OPENING_BALANCE" | "NAVIGATE_REPORTS" | "NAVIGATE_CUSTOMERS" | "NAVIGATE_SUPPLIERS" | "UNKNOWN",
                  "customerName": "String (if applicable, else null)",
                  "supplierName": "String (if applicable, else null)",
                  "productName": "String (if applicable, else null)",
                  "amount": Double (if applicable, else null),
                  "quantity": Int (if applicable, else null),
                  "paymentMethod": "CASH" | "CREDIT" | "UPI" | "BANK" | null,
                  "expenseCategory": "String (e.g. Rent, Transport, Salary, etc, if applicable, else null)",
                  "missingFields": ["list", "of", "missing", "required", "fields", "like", "'amount'", "'customerName'"],
                  "summary": "String (a short human-readable summary of the action or a question asking for missing fields)"
                }
                
                IMPORTANT RULES:
                - The 'summary' field MUST be written in the same language the user spoke. (Hindi, Hinglish, or English).
                - For CREATE_SALE, if they mention 'udhaar' or 'credit', use type "CREATE_CREDIT_SALE" and paymentMethod "CREDIT".
                - For RECORD_PAYMENT (e.g. "Rahul ne 2000 jama kiye"), customerName and amount are required.
                - For CREATE_EXPENSE (e.g. "500 transport expense"), amount and expenseCategory are required.
                - For ADD_OPENING_BALANCE (e.g. "Rahul ka purana udhaar 3000 hai"), customerName and amount are required.
                - For CREATE_SALE / CREATE_CREDIT_SALE, productName, quantity, customerName, and amount are expected.
                - If a required field is missing, put its name in 'missingFields' and ask for it in the 'summary'.
                - NEVER invent financial numbers, IDs, or missing entities.
                - If the user asks a general business question (e.g. "How much did I sell?"), return "UNKNOWN".
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = transcription)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = executeWithTransientRetry(onRetry) {
                RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = getApiKey(),
                    request = request
                )
            }

            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                val jsonObj = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
                val type = jsonObj["type"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
                
                val action = com.example.domain.model.ProposedAction(
                    actionType = type,
                    customerName = jsonObj["customerName"]?.jsonPrimitive?.contentOrNull,
                    supplierName = jsonObj["supplierName"]?.jsonPrimitive?.contentOrNull,
                    productName = jsonObj["productName"]?.jsonPrimitive?.contentOrNull,
                    amount = jsonObj["amount"]?.jsonPrimitive?.doubleOrNull,
                    quantity = jsonObj["quantity"]?.jsonPrimitive?.intOrNull,
                    paymentMethod = jsonObj["paymentMethod"]?.jsonPrimitive?.contentOrNull,
                    notes = jsonObj["summary"]?.jsonPrimitive?.contentOrNull ?: "Unknown action",
                    expenseCategory = jsonObj["expenseCategory"]?.jsonPrimitive?.contentOrNull,
                    missingFields = jsonObj["missingFields"]?.jsonArray?.map { it.jsonPrimitive.content }
                )
                Result.success(action)
            } else {
                Result.failure(Exception("Could not parse command."))
            }
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }
}
