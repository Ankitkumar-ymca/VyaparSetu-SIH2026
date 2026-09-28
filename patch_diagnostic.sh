sed -i 's/result.getOrElse { "I couldn'"'"'t process that." }/result.getOrElse { it.message ?: "I couldn'"'"'t process that." }/g' app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt
