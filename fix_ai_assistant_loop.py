import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

# Replace the else block that calls answerBusinessQuestion unconditionally
old_block = """                } else {
                    val result = AiService.answerBusinessQuestion(userText, onRetry = {
                    loadingText = context.getString(R.string.msg_ai_retrying)
                })
                    addMessage(ChatMessage(result.getOrNull() ?: getFriendlyError(result.exceptionOrNull()), false))
                    isLoading = false
                }"""

new_block = """                } else {
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
                }"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)

