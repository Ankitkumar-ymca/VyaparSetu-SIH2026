import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

old_action_block = """                    } else if (proposedAction.actionType != "UNKNOWN") {
                        val language = java.util.Locale.getDefault().language
                        
                        if (proposedAction.missingFields?.isNotEmpty() == true) {
                            addMessage(ChatMessage(proposedAction.notes ?: "Please provide more details.", false))
                        } else {
                            val validation = SmartActionValidator.validateAction(proposedAction, language)
                            if (validation is ValidationResult.Success) {
                                val confirmPrompt = if (language == "hi") "\\n\\nक्या आप इस कार्रवाई की पुष्टि करना चाहते हैं?" else "\\n\\nDo you want to confirm this action?"
                                addMessage(ChatMessage(
                                    text = validation.previewText + confirmPrompt,
                                    isUser = false,
                                    action = validation.resolvedAction
                                ))
                            } else if (validation is ValidationResult.Failure) {
                                addMessage(ChatMessage(validation.error, false))
                            }
                        }
                        isLoading = false"""

new_action_block = """                    } else if (proposedAction.actionType != "UNKNOWN") {
                        if (proposedAction.missingFields?.isNotEmpty() == true) {
                            addMessage(ChatMessage(proposedAction.notes ?: context.getString(R.string.msg_provide_more_details), false))
                        } else {
                            val validation = SmartActionValidator.validateAction(proposedAction, context)
                            if (validation is ValidationResult.Success) {
                                val confirmPrompt = "\n\n" + context.getString(R.string.msg_confirm_action)
                                addMessage(ChatMessage(
                                    text = validation.previewText + confirmPrompt,
                                    isUser = false,
                                    action = validation.resolvedAction
                                ))
                            } else if (validation is ValidationResult.Failure) {
                                addMessage(ChatMessage(validation.error, false))
                            }
                        }
                        isLoading = false"""

content = content.replace(old_action_block, new_action_block)

old_success_msg = """                                val language = java.util.Locale.getDefault().language
                                if (success) {
                                    val msg = if (language == "hi") "लेन-देन सफलतापूर्वक दर्ज किया गया।" else "Action executed successfully."
                                    updatedMessages.add(ChatMessage(msg, false))
                                } else {
                                    val msg = if (language == "hi") "निष्पादन में विफल: $errorMessage" else "Failed to execute: $errorMessage"
                                    updatedMessages.add(ChatMessage(msg, false))
                                }"""

new_success_msg = """                                if (success) {
                                    val msg = context.getString(R.string.msg_action_executed)
                                    updatedMessages.add(ChatMessage(msg, false))
                                } else {
                                    val msg = context.getString(R.string.msg_action_failed, errorMessage)
                                    updatedMessages.add(ChatMessage(msg, false))
                                }"""

content = content.replace(old_success_msg, new_success_msg)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
