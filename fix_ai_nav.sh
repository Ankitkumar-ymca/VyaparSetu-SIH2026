cat << 'INNER_EOF' > patch_nav.txt
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
INNER_EOF
sed -i '83,93c\
                        val tab = when (proposedAction.actionType) {\
                            "NAVIGATE_REPORTS" -> "Reports"\
                            "NAVIGATE_CUSTOMERS" -> "Khata"\
                            "NAVIGATE_SUPPLIERS" -> "Khata"\
                            else -> null\
                        }\
                        val route = if (tab != null) "dashboard" else null\
                        \
                        messages = messages + ChatMessage("Navigating as requested...", false)\
                        isLoading = false\
                        if (tab != null) {\
                            com.example.data.repository.MockDataStore.targetDashboardTab.value = tab\
                        }\
                        if (route != null) {\
                            onNavigateToRoute(route)\
                        }\
' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt
