import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

# Add loadingText state
content = content.replace(
    "var isLoading by remember { mutableStateOf(false) }",
    "var isLoading by remember { mutableStateOf(false) }\n    var loadingText by remember { mutableStateOf(\"\") }"
)

# Set loadingText on submit
content = content.replace(
    """        isLoading = true
        
        coroutineScope.launch {""",
    """        isLoading = true
        loadingText = context.getString(R.string.msg_ai_connecting)
        
        coroutineScope.launch {"""
)

# Helper function to get friendly error
friendly_err_func = """
    fun getFriendlyError(e: Throwable?): String {
        val msg = e?.message ?: "Unknown error"
        return if (msg.contains("503") || msg.contains("502") || msg.contains("500") || msg.contains("429") || msg.contains("Network Error")) {
            context.getString(R.string.err_ai_busy)
        } else {
            msg
        }
    }
"""

content = content.replace(
    "        fun handleSend(textToSend: String = input) {",
    friendly_err_func + "\n        fun handleSend(textToSend: String = input) {"
)

# Update AiService.parseVoiceCommand calls
old_parse = "val actionResult = AiService.parseVoiceCommand(userText)"
new_parse = """val actionResult = AiService.parseVoiceCommand(userText, onRetry = {
                    loadingText = context.getString(R.string.msg_ai_retrying)
                })"""
content = content.replace(old_parse, new_parse)

old_ans = "val result = AiService.answerBusinessQuestion(userText)"
new_ans = """val result = AiService.answerBusinessQuestion(userText, onRetry = {
                    loadingText = context.getString(R.string.msg_ai_retrying)
                })"""
content = content.replace(old_ans, new_ans)

# Replace the direct print of error with getFriendlyError
content = content.replace(
    """addMessage(ChatMessage(result.getOrElse { it.message ?: "I couldn't process that." }, false))""",
    """addMessage(ChatMessage(result.getOrNull() ?: getFriendlyError(result.exceptionOrNull()), false))"""
)

content = content.replace(
    """addMessage(ChatMessage(result.getOrElse { it.message ?: "Sorry, I couldn't reach the AI service right now." }, false))""",
    """addMessage(ChatMessage(result.getOrNull() ?: getFriendlyError(result.exceptionOrNull()), false))"""
)

# Finally, update the UI to show loadingText instead of just a CircularProgressIndicator
old_loading_ui = """                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BlueNavy)
                    }
                }"""
new_loading_ui = """                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = BlueNavy, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = loadingText, color = BlueNavy, style = MaterialTheme.typography.bodyMedium)
                    }
                }"""
content = content.replace(old_loading_ui, new_loading_ui)


with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)

