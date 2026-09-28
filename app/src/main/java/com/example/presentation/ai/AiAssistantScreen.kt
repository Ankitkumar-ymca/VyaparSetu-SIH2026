package com.example.presentation.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.repository.AiService
import com.example.data.repository.MockDataStore
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.util.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var input by remember { mutableStateOf("") }
    val messages by com.example.data.repository.MockDataStore.chatMessages.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("") }
    fun addMessage(msg: ChatMessage) { com.example.data.repository.MockDataStore.updateChatMessages(messages + msg) }
    fun updateMessage(index: Int, msg: ChatMessage) { val newList = messages.toMutableList(); newList[index] = msg; com.example.data.repository.MockDataStore.updateChatMessages(newList) }
    
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var selectedLanguageCode by remember { mutableStateOf("en-IN") } // default Hinglish/Indian English
    
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }
    

    fun getFriendlyError(e: Throwable?): String {
        val msg = e?.message ?: "Unknown error"
        return if (msg.contains("503") || msg.contains("502") || msg.contains("500") || msg.contains("429") || msg.contains("Network Error")) {
            context.getString(R.string.err_ai_busy)
        } else {
            msg
        }
    }

    fun handleSend(textToSend: String = input) {
        if (textToSend.isBlank() || isLoading) return
        val userText = textToSend
        addMessage(ChatMessage(userText, true))
        input = ""
        isLoading = true
        loadingText = context.getString(R.string.msg_ai_connecting)
        
        coroutineScope.launch {
            val isLikelyCommand = listOf("add", "record", "create", "show", "open", "view", "navigate", "becha", "diye", "karo", "sale", "payment", "reports", "customer", "supplier", "expense", "नया", "जोड़ो", "बनाओ", "खोलो", "दिखाओ", "ग्राहक", "सप्लायर", "खर्चा", "बिक्री", "kharid", "kharcha", "list").any { userText.lowercase().contains(it) }
            
            if (isLikelyCommand) {
                val actionResult = AiService.parseVoiceCommand(userText, onRetry = {
                    loadingText = context.getString(R.string.msg_ai_retrying)
                })
                if (actionResult.isSuccess) {
                    val proposedAction = actionResult.getOrThrow()
                    if (proposedAction.actionType.startsWith("NAVIGATE_")) {
                        val tab = when (proposedAction.actionType) {
                            "NAVIGATE_REPORTS" -> "Reports"
                            "NAVIGATE_CUSTOMERS" -> "Khata"
                            "NAVIGATE_SUPPLIERS" -> "Khata"
                            else -> null
                        }
                        val route = if (tab != null) "dashboard" else null
                        
                        addMessage(ChatMessage("Navigating as requested...", false))
                        isLoading = false
                        if (tab != null) {
                            com.example.data.repository.MockDataStore.targetDashboardTab.value = tab
                        }
                        if (route != null) {
                            onNavigateToRoute(route)
                        }
                    } else if (proposedAction.actionType != "UNKNOWN") {
                        if (proposedAction.missingFields?.isNotEmpty() == true) {
                            addMessage(ChatMessage(proposedAction.notes ?: context.getString(R.string.msg_provide_more_details), false))
                        } else {
                            val validation = com.example.domain.model.SmartActionValidator.validateAction(proposedAction, context)
                            if (validation is com.example.domain.model.ValidationResult.Success) {
                                val confirmPrompt = "\n\n" + context.getString(R.string.msg_confirm_action)
                                addMessage(ChatMessage(
                                    text = validation.previewText + confirmPrompt,
                                    isUser = false,
                                    action = validation.resolvedAction
                                ))
                            } else if (validation is com.example.domain.model.ValidationResult.Failure) {
                                addMessage(ChatMessage(validation.error, false))
                            }
                        }
                        isLoading = false
                    } else {
                        val result = AiService.answerBusinessQuestion(userText, onRetry = {
                    loadingText = context.getString(R.string.msg_ai_retrying)
                })
                        addMessage(ChatMessage(result.getOrNull() ?: getFriendlyError(result.exceptionOrNull()), false))
                        isLoading = false
                    }
                } else {
                    val err = actionResult.exceptionOrNull()
                    val msg = err?.message ?: ""
                    if (msg.contains("503") || msg.contains("502") || msg.contains("500") || msg.contains("429") || msg.contains("Network Error")) {
                        addMessage(ChatMessage(getFriendlyError(err), false))
                        isLoading = false
                    } else {
                        val result = AiService.answerBusinessQuestion(userText, onRetry = {
                            loadingText = context.getString(R.string.msg_ai_retrying)
                        })
                        addMessage(ChatMessage(result.getOrNull() ?: getFriendlyError(result.exceptionOrNull()), false))
                        isLoading = false
                    }
                }
            } else {
                val result = AiService.answerBusinessQuestion(userText, onRetry = {
                    loadingText = context.getString(R.string.msg_ai_retrying)
                })
                addMessage(ChatMessage(result.getOrNull() ?: getFriendlyError(result.exceptionOrNull()), false))
                isLoading = false
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening(
                context = context,
                onListeningStateChanged = { isListening = it },
                onResult = { resultText -> 
                    input = resultText
                    isListening = false
                    handleSend(resultText)
                },
                onError = { errorText -> Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show(); isListening = false },
                setRecognizer = { speechRecognizer = it },
                languageCode = selectedLanguageCode
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_clear_chat_history)) },
            text = { Text(stringResource(R.string.str_delete_all_chat)) },
            confirmButton = {
                Button(
                    onClick = {
                        MockDataStore.clearChatHistory()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.str_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.str_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_vyaparsetu_ai), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_clear_chat_history)) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BlueBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages.size) { idx ->
                    val msg = messages[idx]
                    ChatBubble(msg, index = idx, onDelete = { MockDataStore.deleteChatMessage(idx) }, onExecute = { action, msgObj ->
                        coroutineScope.launch {
                            val businessId = MockDataStore.currentBusiness.value?.id ?: return@launch
                            val timestamp = System.currentTimeMillis()
                            var success = false
                            
                            var errorMessage = ""
                            when (action.actionType) {
                                "CREATE_CUSTOMER" -> {
                                    val name = action.customerName ?: "New Customer"
                                    val newCust = com.example.domain.model.Customer(id = java.util.UUID.randomUUID().toString(), businessId = businessId, name = name, phone = "", email = "", address = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveCustomer(newCust)
                                    success = res.isSuccess
                                }
                                "CREATE_SUPPLIER" -> {
                                    val name = action.supplierName ?: "New Supplier"
                                    val newSupp = com.example.domain.model.Supplier(id = java.util.UUID.randomUUID().toString(), businessId = businessId, name = name, phone = "", email = "", address = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveSupplier(newSupp)
                                    success = res.isSuccess
                                }
                                "CREATE_EXPENSE" -> {
                                    val newExp = com.example.domain.model.Expense(id = java.util.UUID.randomUUID().toString(), businessId = businessId, category = action.expenseCategory ?: "Other", description = action.notes ?: "", amount = action.amount ?: 0.0, paymentMethod = "CASH", notes = "", createdAt = timestamp, updatedAt = timestamp)
                                    val res = MockDataStore.saveExpense(newExp)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "CREATE_SALE", "CREATE_CREDIT_SALE" -> {
                                    val qty = action.quantity ?: 1
                                    val amount = action.amount ?: 0.0
                                    val isCredit = action.actionType == "CREATE_CREDIT_SALE" || action.paymentMethod == "CREDIT"
                                    
                                    val items = listOf(com.example.domain.model.SaleItem(
                                        productId = action.productName ?: "", // this is actually the ID
                                        productName = "Product",
                                        quantity = qty,
                                        unitPrice = amount / qty,
                                        totalPrice = amount
                                    ))
                                    
                                    val newSale = com.example.domain.model.Sale(
                                        id = java.util.UUID.randomUUID().toString(), 
                                        businessId = businessId, 
                                        invoiceNumber = "INV-${timestamp}", 
                                        customerId = action.customerName, // this is the ID
                                        items = items, 
                                        subtotal = amount, 
                                        discount = 0.0, 
                                        tax = 0.0, 
                                        grandTotal = amount, 
                                        amountPaid = if(isCredit) 0.0 else amount, 
                                        outstanding = if(isCredit) amount else 0.0, 
                                        paymentStatus = if(isCredit) "UNPAID" else "PAID", 
                                        paymentMethod = if(isCredit) "CREDIT" else (action.paymentMethod ?: "CASH"), 
                                        createdAt = timestamp, 
                                        updatedAt = timestamp
                                    )
                                    val res = MockDataStore.createSale(newSale)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "CREATE_PURCHASE" -> {
                                    val qty = action.quantity ?: 1
                                    val amount = action.amount ?: 0.0
                                    val isCredit = action.paymentMethod == "CREDIT"
                                    
                                    val items = listOf(com.example.domain.model.PurchaseItem(
                                        productId = action.productName ?: "", // ID
                                        productName = "Product",
                                        quantity = qty,
                                        unitPrice = amount / qty,
                                        totalPrice = amount
                                    ))
                                    
                                    val newPurch = com.example.domain.model.Purchase(
                                        id = java.util.UUID.randomUUID().toString(), 
                                        businessId = businessId, 
                                        referenceNumber = "PUR-${timestamp}", 
                                        supplierId = action.supplierName ?: "", // ID
                                        items = items, 
                                        subtotal = amount, 
                                        discount = 0.0, 
                                        tax = 0.0, 
                                        grandTotal = amount, 
                                        amountPaid = if(isCredit) 0.0 else amount, 
                                        outstanding = if(isCredit) amount else 0.0, 
                                        paymentStatus = if(isCredit) "UNPAID" else "PAID", 
                                        paymentMethod = if(isCredit) "CREDIT" else "CASH", 
                                        createdAt = timestamp, 
                                        updatedAt = timestamp
                                    )
                                    val res = MockDataStore.createPurchase(newPurch)
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "RECORD_PAYMENT" -> {
                                    val amount = action.amount ?: 0.0
                                    val res = MockDataStore.recordCustomerPayment(
                                        customerId = action.customerName ?: "", 
                                        amount = amount, 
                                        method = action.paymentMethod ?: "CASH", 
                                        notes = action.notes ?: ""
                                    )
                                    success = res.isSuccess
                                    if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                }
                                "ADD_OPENING_BALANCE" -> {
                                    val amount = action.amount ?: 0.0
                                    val customers = MockDataStore.customers.value
                                    val custId = action.customerName ?: ""
                                    val cust = customers.find { it.id == custId }
                                    if (cust != null) {
                                        val updatedCust = cust.copy(outstandingAmount = cust.outstandingAmount + amount, updatedAt = timestamp)
                                        val res = MockDataStore.saveCustomer(updatedCust)
                                        success = res.isSuccess
                                        if (res.isFailure) errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                                    } else {
                                        success = false
                                        errorMessage = "Customer not found."
                                    }
                                }
                                else -> { success = false; errorMessage = "Unknown action type." }
                            }
                            
                            val index = messages.indexOf(msgObj)
                            if (index != -1) {
                                
                                val updatedMessages = messages.toMutableList()
                                updatedMessages[index] = msgObj.copy(isExecuted = true)
                                if (success) {
                                    val msg = context.getString(R.string.msg_action_executed)
                                    updatedMessages.add(ChatMessage(msg, false))
                                } else {
                                    val msg = context.getString(R.string.msg_action_failed, errorMessage)
                                    updatedMessages.add(ChatMessage(msg, false))
                                }
                                com.example.data.repository.MockDataStore.updateChatMessages(updatedMessages)
    
                            }
                        }
                    })
                }
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BlueAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.str_processing), fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
            
            if (isListening) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.str_listening), color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.str_voice_language), fontSize = 12.sp, color = TextSecondary)
                
                FilterChip(
                    selected = selectedLanguageCode == "en-US",
                    onClick = { selectedLanguageCode = "en-US" },
                    label = { Text(stringResource(R.string.str_english), fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedLanguageCode == "en-IN",
                    onClick = { selectedLanguageCode = "en-IN" },
                    label = { Text(stringResource(R.string.str_hinglish), fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedLanguageCode == "hi-IN",
                    onClick = { selectedLanguageCode = "hi-IN" },
                    label = { Text(stringResource(R.string.str_empty_1), fontSize = 12.sp) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, CardBorder)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.str_ask_about_your_business)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = BlueLight.copy(alpha = 0.3f),
                        focusedContainerColor = BlueLight.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = BlueAccent
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    startListening(
                                        context = context,
                                        onListeningStateChanged = { isListening = it },
                                        onResult = { resultText -> 
                                            input = resultText
                                            isListening = false
                                            handleSend(resultText)
                                        },
                                        onError = { errorText -> Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show(); isListening = false },
                                        setRecognizer = { speechRecognizer = it },
                                        languageCode = selectedLanguageCode
                                    )
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic, 
                                contentDescription = if (isListening) "Stop Listening" else "Speak", 
                                tint = if (isListening) AccentRed else BlueNavy
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { handleSend() },
                    modifier = Modifier.background(BlueNavy, RoundedCornerShape(50)).size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }


}

private fun startListening(
    context: android.content.Context,
    onListeningStateChanged: (Boolean) -> Unit,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    setRecognizer: (SpeechRecognizer) -> Unit,
    languageCode: String = "en-IN"
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onError("Speech recognition not available on this device.")
        return
    }
    
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    setRecognizer(recognizer)
    
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onListeningStateChanged(true) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { onListeningStateChanged(false) }
        override fun onError(error: Int) {
            val message = when(error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error. Ensure Google App is installed/updated."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand the speech. Please try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition is busy. Please try again."
                SpeechRecognizer.ERROR_SERVER -> "Error from server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Didn't understand, please try again. Code: $error"
            }
            onError(message)
            onListeningStateChanged(false)
            recognizer.destroy()
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
            recognizer.destroy()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            // Just for demonstration, normally you'd update input incrementally
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    
    recognizer.startListening(intent)
}


@Composable
fun ChatBubble(message: ChatMessage, index: Int, onExecute: (ProposedAction, ChatMessage) -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) BlueNavy else Color.White
    val textColor = if (message.isUser) Color.White else TextPrimary
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .border(1.dp, if (message.isUser) Color.Transparent else CardBorder, shape)
                .padding(16.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = textColor
            )
            if (message.action != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (message.isExecuted) {
                    Text(stringResource(R.string.str_executed), color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Button(
                        onClick = { onExecute(message.action, message) },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                    ) {
                        Text(stringResource(R.string.str_confirm__execute), fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }

}
