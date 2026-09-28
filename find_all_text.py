import os
import re

directory = "app/src/main/java/com/example"

# This will find Text(text = "...") or Text("...")
text_pattern1 = re.compile(r'Text\(\s*"([^"]+)"')
text_pattern2 = re.compile(r'Text\(\s*text\s*=\s*"([^"]+)"')

found = set()

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
                for m in text_pattern1.finditer(content):
                    found.add((path, m.group(1)))
                for m in text_pattern2.finditer(content):
                    found.add((path, m.group(1)))

for p, s in found:
    print(f'{p}: "{s}"')

