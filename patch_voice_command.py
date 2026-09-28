import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

# Replace the parseVoiceCommand
old_method = """    suspend fun parseVoiceCommand(transcription: String): Result<com.example.domain.model.ProposedAction> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = \"\"\"
                You are VyaparSetu Voice AI. Parse the user's spoken command (which may be in Hinglish, Hindi, or English) into a structured action.
                Return ONLY a JSON object with this exact schema:
                {
                  "type": "CREATE_CUSTOMER" | "CREATE_SUPPLIER" | "CREATE_SALE" | "CREATE_PURCHASE" | "CREATE_EXPENSE" | "RECORD_PAYMENT" | "NAVIGATE_REPORTS" | "NAVIGATE_CUSTOMERS" | "NAVIGATE_SUPPLIERS" | "UNKNOWN",
                  "entityName": "String (e.g., customer name, supplier name, product name)",
                  "amount": Double (if applicable, else null),
                  "quantity": Int (if applicable, else null),
                  "summary": "String (a short human-readable summary of what you are proposing to do, e.g. 'Record a sale of ₹500 for Ramesh' or 'Add supplier ABC Traders')"
                }
                
                IMPORTANT RULES:
                - The 'summary' field MUST be written in the same language the user spoke.
                  If they speak Hindi (e.g. 'नया ग्राहक राहुल जोड़ो'), reply in Hindi ('राहुल को ग्राहक के रूप में जोड़ें?').
                  If Hinglish ('Rahul ko customer add karo'), reply in Hinglish.
                  If English, reply in English.
                - "Add customer Rahul" / "नया ग्राहक राहुल जोड़ो" / "Rahul ko grahak banao" -> type: "CREATE_CUSTOMER", entityName: "Rahul", summary: "Add Rahul as a customer" (or translated)
                - "Add supplier ABC Traders" / "एबीसी ट्रेडर्स को सप्लायर के रूप में जोड़ो" -> type: "CREATE_SUPPLIER", entityName: "ABC Traders", summary: "Add ABC Traders as a supplier" (or translated)
                - "Open reports" / "रिपोर्ट खोलो" / "Report kholo" -> type: "NAVIGATE_REPORTS", summary: "Open reports" (or translated)
                - "Show customers" / "ग्राहक दिखाओ" -> type: "NAVIGATE_CUSTOMERS", summary: "Open customers" (or translated)
                - "Show suppliers" / "सप्लायर दिखाओ" -> type: "NAVIGATE_SUPPLIERS", summary: "Open suppliers" (or translated)
                - "Add an expense of 500 for chai" / "500 ka kharcha jodo" -> type: "CREATE_EXPENSE", amount: 500, summary: "Add an expense of ₹500 for chai" (or translated)
                - DO NOT confuse Customer (ग्राहक) and Supplier (सप्लायर/माल देने वाला). If the user says 'Supplier', it MUST be CREATE_SUPPLIER.
                - If the intent is unclear between customer and supplier, ASK FOR CLARIFICATION by returning type: "UNKNOWN" and putting your question in the 'summary'.
            \"\"\".trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = transcription)))),
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
                val jsonObj = Json.parseToJsonElement(text).jsonObject
                val type = jsonObj["type"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
                val entityName = jsonObj["entityName"]?.jsonPrimitive?.contentOrNull
                
                val action = com.example.domain.model.ProposedAction(
                    actionType = type,
                    customerName = if (type == "CREATE_CUSTOMER" || type == "RECORD_PAYMENT" || type == "CREATE_SALE") entityName else null,
                    supplierName = if (type == "CREATE_SUPPLIER" || type == "CREATE_PURCHASE") entityName else null,
                    productName = if (type == "ADD_PRODUCT") entityName else null,
                    amount = jsonObj["amount"]?.jsonPrimitive?.doubleOrNull,
                    quantity = jsonObj["quantity"]?.jsonPrimitive?.intOrNull,
                    notes = jsonObj["summary"]?.jsonPrimitive?.contentOrNull ?: "Unknown action"
                )
                Result.success(action)
            } else {
                Result.failure(Exception("Could not parse command."))
            }
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }"""

new_method = """    suspend fun parseVoiceCommand(transcription: String): Result<com.example.domain.model.ProposedAction> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = \"\"\"
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
            \"\"\".trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = transcription)))),
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
                    expenseCategory = jsonObj["expenseCategory"]?.jsonPrimitive?.contentOrNull
                )
                Result.success(action)
            } else {
                Result.failure(Exception("Could not parse command."))
            }
        } catch (e: Exception) {
            Result.failure(handleGeminiException(e))
        }
    }"""

content = content.replace(old_method, new_method)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)

