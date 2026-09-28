with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    lines = f.readlines()

start = -1
for i, line in enumerate(lines):
    if "fun handleSend" in line:
        start = i
        break

if start != -1:
    brace = 0
    for i in range(start, len(lines)):
        line = lines[i]
        for char in line:
            if char == '{': brace += 1
            elif char == '}': brace -= 1
        if brace == 0:
            print(f"handleSend closed at line {i+1}")
            break
