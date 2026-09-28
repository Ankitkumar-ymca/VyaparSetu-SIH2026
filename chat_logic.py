import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

chat_logic = """    fun updateChatMessages(messages: List<ChatMessage>) {
        _chatMessages.value = messages
        saveToPrefs()
    }
    
    fun deleteChatMessage(index: Int) {
        val list = _chatMessages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _chatMessages.value = list
            saveToPrefs()
        }
    }
    
    fun clearChatHistory() {
        _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
        saveToPrefs()
    }"""

text = text.replace("    fun updateChatMessages(messages: List<ChatMessage>) {\n        _chatMessages.value = messages\n        saveToPrefs()\n    }", chat_logic)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
