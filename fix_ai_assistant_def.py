import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

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
    "    fun handleSend(textToSend: String = input) {",
    friendly_err_func + "\n    fun handleSend(textToSend: String = input) {"
)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)

