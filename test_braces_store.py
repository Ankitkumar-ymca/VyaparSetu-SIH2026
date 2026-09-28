with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    lines = f.readlines()

brace = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{': brace += 1
        elif char == '}': brace -= 1
    if i == len(lines)-1:
        print(f"End of file brace level: {brace}")
