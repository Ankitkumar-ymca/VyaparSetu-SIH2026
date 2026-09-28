import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

target = "    fun updateChatMessages(messages: List<ChatMessage>) {"
replacement = """    fun clearChatHistory() {
        _chatMessages.value = listOf(ChatMessage("Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').", false))
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
    
    fun updateChatMessages(messages: List<ChatMessage>) {"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
