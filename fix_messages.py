import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\nimport androidx.compose.runtime.collectAsState")

text = text.replace('var messages by remember { mutableStateOf(listOf(ChatMessage("Hi! I\'m VyaparSetu AI. Tap the Mic to speak a command (e.g. \'Add customer Rahul\').", false))) }',
                    'val messages by com.example.data.repository.MockDataStore.chatMessages.collectAsState()')

# replace `messages = messages + ` with `com.example.data.repository.MockDataStore.updateChatMessages(messages + ` and add closing `)`
# Wait, this is tricky. Let's do it cleanly by creating a function `fun addMessage(msg: ChatMessage) { com.example.data.repository.MockDataStore.updateChatMessages(messages + msg) }`
text = text.replace("    var isLoading by remember { mutableStateOf(false) }", "    var isLoading by remember { mutableStateOf(false) }\n    fun addMessage(msg: ChatMessage) { com.example.data.repository.MockDataStore.updateChatMessages(messages + msg) }\n    fun updateMessage(index: Int, msg: ChatMessage) { val newList = messages.toMutableList(); newList[index] = msg; com.example.data.repository.MockDataStore.updateChatMessages(newList) }")

text = text.replace("messages = messages + ChatMessage(userText, true)", "addMessage(ChatMessage(userText, true))")
text = text.replace('messages = messages + ChatMessage("Navigating as requested...", false)', 'addMessage(ChatMessage("Navigating as requested...", false))')
text = text.replace("messages = messages + ChatMessage(\n                            \"I can help you execute this action. Please confirm:\",\n                            false,\n                            action = action\n                        )", "addMessage(ChatMessage(\n                            \"I can help you execute this action. Please confirm:\",\n                            false,\n                            action = action\n                        ))")
text = text.replace("messages = messages + ChatMessage(result.getOrElse { it.message ?: \"I couldn't process that.\" }, false)", "addMessage(ChatMessage(result.getOrElse { it.message ?: \"I couldn't process that.\" }, false))")
text = text.replace("messages = messages + ChatMessage(result.getOrElse { it.message ?: \"Sorry, I couldn't reach the AI service right now.\" }, false)", "addMessage(ChatMessage(result.getOrElse { it.message ?: \"Sorry, I couldn't reach the AI service right now.\" }, false))")

# In the onExecute part:
# val updatedMessages = messages.toMutableList()
# updatedMessages[messageIndex] = message.copy(isExecuted = true)
# if (result.isSuccess) { updatedMessages.add(ChatMessage("Successfully executed!", false)) } else { updatedMessages.add(ChatMessage("Failed to execute.", false)) }
# messages = updatedMessages
def replacer_execute(m):
    return """
                                val updatedMessages = messages.toMutableList()
                                updatedMessages[messageIndex] = message.copy(isExecuted = true)
                                if (result.isSuccess) {
                                    updatedMessages.add(ChatMessage("Successfully executed!", false))
                                } else {
                                    updatedMessages.add(ChatMessage("Failed to execute.", false))
                                }
                                com.example.data.repository.MockDataStore.updateChatMessages(updatedMessages)
    """

pattern_execute = r"val updatedMessages = messages\.toMutableList\(\)[\s\S]*?messages = updatedMessages"
text = re.sub(pattern_execute, replacer_execute, text)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)

