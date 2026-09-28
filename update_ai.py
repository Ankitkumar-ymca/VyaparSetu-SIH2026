import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

# Add _chatMessages to MockDataStore
chat_state = """    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun updateChatMessages(messages: List<ChatMessage>) {
        _chatMessages.value = messages
        saveChatToPrefs()
    }
    
    private fun saveChatToPrefs() {
        val prefs = prefs ?: return
        try {
            val jsonString = Json.encodeToString(_chatMessages.value)
            prefs.edit().putString("chat_history", jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
"""

text = text.replace("    val isOnline = MutableStateFlow(true)", chat_state + "\n    val isOnline = MutableStateFlow(true)")

load_chat = """
        try {
            val chatJson = prefs.getString("chat_history", null)
            if (chatJson != null) {
                _chatMessages.value = Json.decodeFromString<List<ChatMessage>>(chatJson)
            } else {
                _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
            }
        } catch (e: Exception) {
            _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
        }
"""
text = text.replace("        val currentUserId = prefs.getString(\"currentUserId\", null)", load_chat + "\n        val currentUserId = prefs.getString(\"currentUserId\", null)")

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
