import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

# Replace local mutable state with collecting from MockDataStore
text = text.replace("var messages by remember { mutableStateOf(listOf(ChatMessage(\"Hi! I'm VyaparSetu AI. Tap the Mic to speak a command (e.g. 'Add customer Rahul').\", false))) }", 
                    "val messages by com.example.data.repository.MockDataStore.chatMessages.collectAsState()\n    var localMessages by remember(messages) { mutableStateOf(messages) }")

text = text.replace("messages = messages + ", "localMessages = localMessages + ")
text = text.replace("updatedMessages.add(", "localMessages = localMessages + ")
# Actually it's better to make a helper function in MockDataStore `addChatMessage` OR just update the state here and call MockDataStore.updateChatMessages(localMessages)
