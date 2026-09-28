with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "list.forEach { kotlinx.coroutines.GlobalScope.launch" in line:
        print(f"Line {i+1}: {line.strip()}")
