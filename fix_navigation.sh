# Add targetDashboardTab to MockDataStore
sed -i 's/val isDemoMode = MutableStateFlow(false)/val isDemoMode = MutableStateFlow(false)\n    val targetDashboardTab = MutableStateFlow<String?>(null)/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

# Update AiAssistantScreen navigation logic
sed -i 's/val route = when (proposedAction.actionType) {/val tab = when (proposedAction.actionType) {\n                            "NAVIGATE_REPORTS" -> "Reports"\n                            "NAVIGATE_CUSTOMERS" -> "Khata"\n                            "NAVIGATE_SUPPLIERS" -> "Khata"\n                            else -> null\n                        }\n                        val route = if (tab != null) "dashboard" else null/g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt

sed -i 's/val route = when/val _old_route = when/g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt
sed -i 's/"NAVIGATE_REPORTS" -> "reports"//g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt
sed -i 's/"NAVIGATE_CUSTOMERS" -> "dashboard"//g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt
sed -i 's/"NAVIGATE_SUPPLIERS" -> "dashboard"//g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt
sed -i 's/else -> null//g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt

