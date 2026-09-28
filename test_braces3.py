with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    lines = f.readlines()
brace = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{': brace += 1
        elif char == '}': brace -= 1
    if brace == 0 and i > 46:
        print(f"Brace went to 0 at line {i+1}:\n{line}")
        break
