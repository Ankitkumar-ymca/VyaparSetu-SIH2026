import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

bad_pattern = r'if \(proposedAction\.actionType\.startsWith\("NAVIGATE_"\)\) \{.*?\} else if \(proposedAction\.actionType != "UNKNOWN"\) \{'
good_replacement = '''if (proposedAction.actionType.startsWith("NAVIGATE_")) {
                        val tab = when (proposedAction.actionType) {
                            "NAVIGATE_REPORTS" -> "Reports"
                            "NAVIGATE_CUSTOMERS" -> "Khata"
                            "NAVIGATE_SUPPLIERS" -> "Khata"
                            else -> null
                        }
                        val route = if (tab != null) "dashboard" else null
                        
                        messages = messages + ChatMessage("Navigating as requested...", false)
                        isLoading = false
                        if (tab != null) {
                            com.example.data.repository.MockDataStore.targetDashboardTab.value = tab
                        }
                        if (route != null) {
                            onNavigateToRoute(route)
                        }
                    } else if (proposedAction.actionType != "UNKNOWN") {'''

text = re.sub(bad_pattern, good_replacement, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)
