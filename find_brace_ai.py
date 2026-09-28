with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    lines = f.readlines()
brace = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{': brace += 1
        elif char == '}': brace -= 1
    if "fun startListening" in line:
        print(f"startListening is at brace level {brace} (should be 0 or 1 depending on file level)")
