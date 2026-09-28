import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

# Remove ChatMessage from AiAssistantScreen.kt
text = re.sub(r"data class ChatMessage\([^\)]+\)", "", text)
with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/domain/model/Models.kt', 'r') as f:
    models_text = f.read()

# Add ChatMessage to Models.kt
chat_msg = """
@kotlinx.serialization.Serializable
data class ChatMessage(val text: String, val isUser: Boolean, val action: ProposedAction? = null, var isExecuted: Boolean = false)
"""
models_text = models_text + "\n" + chat_msg
with open('app/src/main/java/com/example/domain/model/Models.kt', 'w') as f:
    f.write(models_text)
