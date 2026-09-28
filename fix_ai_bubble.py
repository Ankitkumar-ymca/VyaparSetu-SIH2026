with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("@Composable\nfun ChatBubble", "}\n\n@Composable\nfun ChatBubble")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)
