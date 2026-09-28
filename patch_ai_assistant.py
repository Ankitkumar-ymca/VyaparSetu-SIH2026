import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

# Make sure to import SmartActionValidator
if "import com.example.domain.model.SmartActionValidator" not in content:
    content = content.replace("import com.example.domain.model.ProposedAction", "import com.example.domain.model.ProposedAction\nimport com.example.domain.model.SmartActionValidator\nimport com.example.domain.model.ValidationResult")

# Find the block where proposedAction is processed
old_action_block = """                    } else if (proposedAction.actionType != "UNKNOWN") {
                        addMessage(ChatMessage(
                            text = "I understood you want to do the following:\n\n${proposedAction.notes}\n\nDo you want to confirm this action?",
                            isUser = false,
                            action = proposedAction
                        ))
                        isLoading = false
                    } else {"""

new_action_block = """                    } else if (proposedAction.actionType != "UNKNOWN") {
                        val language = java.util.Locale.getDefault().language
                        
                        if (proposedAction.missingFields?.isNotEmpty() == true) {
                            addMessage(ChatMessage(proposedAction.notes ?: "Please provide more details.", false))
                        } else {
                            val validation = SmartActionValidator.validateAction(proposedAction, language)
                            if (validation is ValidationResult.Success) {
                                val confirmPrompt = if (language == "hi") "\n\nक्या आप इस कार्रवाई की पुष्टि करना चाहते हैं?" else "\n\nDo you want to confirm this action?"
                                addMessage(ChatMessage(
                                    text = validation.previewText + confirmPrompt,
                                    isUser = false,
                                    action = validation.resolvedAction
                                ))
                            } else if (validation is ValidationResult.Failure) {
                                addMessage(ChatMessage(validation.error, false))
                            }
                        }
                        isLoading = false
                    } else {"""

content = content.replace(old_action_block, new_action_block)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
