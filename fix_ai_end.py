with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    lines = f.readlines()

if lines[-1].strip() == "}":
    lines.pop()

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.writelines(lines)
