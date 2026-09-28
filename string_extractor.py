import os
import re

kt_files = []
for root, dirs, files in os.walk('app/src/main/java/com/example/presentation'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root, file))

strings_dict = {}

def slugify(text):
    text = re.sub(r'[^a-zA-Z0-9 ]', '', text)
    text = text.strip().lower().replace(' ', '_')
    if not text: return "empty"
    return text[:30]

for filepath in kt_files:
    with open(filepath, 'r') as f:
        content = f.read()
    
    # We want to replace Text("Some String") or Text(text = "Some String")
    # Also placeholder = { Text("Search") }
    # Let's find all occurrences of "..." inside UI components
    # Specifically Text("...",
    pattern = r'Text\(\s*"([^"]+)"'
    
    matches = re.finditer(pattern, content)
    for match in matches:
        s = match.group(1)
        if "{" in s or "$" in s or len(s) == 0: continue
        slug = "str_" + slugify(s)
        
        # Deduplicate
        if slug in strings_dict and strings_dict[slug] != s:
            slug = slug + "_1"
        strings_dict[slug] = s

# Generate strings.xml
english_strings = "<resources>\n"
english_strings += '    <string name="app_name">My Application</string>\n'
for slug, s in strings_dict.items():
    safe_s = s.replace('&', '&amp;').replace('<', '&lt;').replace("'", "\\'")
    english_strings += f'    <string name="{slug}">{safe_s}</string>\n'
english_strings += "</resources>"

print(f"Found {len(strings_dict)} static strings.")
with open('extracted_strings.xml', 'w') as f:
    f.write(english_strings)
