import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

old_block = """                    } else if (proposedAction.actionType != "UNKNOWN") {
                        addMessage(ChatMessage(
                            text = "I understood you want to do the following:\\n\\n${proposedAction.notes}\\n\\nDo you want to confirm this action?",
                            isUser = false,
                            action = proposedAction
                        ))
                        isLoading = false"""

new_block = """                    } else if (proposedAction.actionType != "UNKNOWN") {
                        if (proposedAction.missingFields?.isNotEmpty() == true) {
                            addMessage(ChatMessage(proposedAction.notes ?: context.getString(R.string.msg_provide_more_details), false))
                        } else {
                            val validation = com.example.domain.model.SmartActionValidator.validateAction(proposedAction, context)
                            if (validation is com.example.domain.model.ValidationResult.Success) {
                                val confirmPrompt = "\\n\\n" + context.getString(R.string.msg_confirm_action)
                                addMessage(ChatMessage(
                                    text = validation.previewText + confirmPrompt,
                                    isUser = false,
                                    action = validation.resolvedAction
                                ))
                            } else if (validation is com.example.domain.model.ValidationResult.Failure) {
                                addMessage(ChatMessage(validation.error, false))
                            }
                        }
                        isLoading = false"""

if old_block in content:
    content = content.replace(old_block, new_block)
else:
    print("Could not find old_block")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)

