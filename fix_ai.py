with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("private fun startListening", "}\n\nprivate fun startListening")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)

# Also remove the last } in the file to keep it balanced
with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    lines = f.readlines()
for i in range(len(lines)-1, -1, -1):
    if "}" in lines[i]:
        lines[i] = lines[i].replace("}", "", 1)
        break

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.writelines(lines)
