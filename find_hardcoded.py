import os
import re

directory = "app/src/main/java/com/example"

text_pattern = re.compile(r'Text\(\s*"([^"]+)"')
label_pattern = re.compile(r'label\s*=\s*{\s*Text\(\s*"([^"]+)"\s*\)\s*}')
placeholder_pattern = re.compile(r'placeholder\s*=\s*{\s*Text\(\s*"([^"]+)"\s*\)\s*}')
button_pattern = re.compile(r'Button\([^)]*\)\s*{\s*Text\(\s*"([^"]+)"\s*\)\s*}')

found = set()

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
                for m in text_pattern.finditer(content):
                    found.add(m.group(1))

print("Found hardcoded strings:")
for s in found:
    print(f'"{s}"')

