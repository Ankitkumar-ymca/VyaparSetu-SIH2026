import os
import re

kt_files = []
for root, dirs, files in os.walk('app/src/main/java/com/example/presentation'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root, file))

def slugify(text):
    text = re.sub(r'[^a-zA-Z0-9 ]', '', text)
    text = text.strip().lower().replace(' ', '_')
    if not text: return "empty"
    return text[:30]

strings_dict = {}

for filepath in kt_files:
    with open(filepath, 'r') as f:
        content = f.read()
    
    pattern = r'(Text\(\s*text\s*=\s*)"([^"]+)"'
    matches = re.finditer(pattern, content)
    for match in matches:
        s = match.group(2)
        if "{" in s or "$" in s or len(s) == 0: continue
        slug = "str_" + slugify(s)
        if slug in strings_dict and strings_dict[slug] != s: slug = slug + "_1"
        strings_dict[slug] = s
        
    pattern2 = r'(Text\(\s*)"([^"]+)"'
    matches = re.finditer(pattern2, content)
    for match in matches:
        s = match.group(2)
        if "{" in s or "$" in s or len(s) == 0: continue
        slug = "str_" + slugify(s)
        if slug in strings_dict and strings_dict[slug] != s: slug = slug + "_1"
        strings_dict[slug] = s

    # Also handle placeholder = { Text("...") } and label = { Text("...") }
    
# Write string resources
english_strings = "<resources>\n"
english_strings += '    <string name="app_name">VyaparSetu</string>\n'
for slug, s in strings_dict.items():
    safe_s = s.replace('&', '&amp;').replace('<', '&lt;').replace("'", "\\'")
    english_strings += f'    <string name="{slug}">{safe_s}</string>\n'
english_strings += "</resources>"

with open('app/src/main/res/values/strings.xml', 'w') as f:
    f.write(english_strings)

# Let's perform replacements in the Kotlin files
for filepath in kt_files:
    with open(filepath, 'r') as f:
        content = f.read()
        
    original = content
    
    def replacer(match):
        prefix = match.group(1)
        s = match.group(2)
        if "{" in s or "$" in s or len(s) == 0: return match.group(0)
        slug = "str_" + slugify(s)
        if slug in strings_dict and strings_dict[slug] != s: slug = slug + "_1"
        return f'{prefix}stringResource(R.string.{slug})'

    content = re.sub(r'(Text\(\s*text\s*=\s*)"([^"]+)"', replacer, content)
    content = re.sub(r'(Text\(\s*)"([^"]+)"', replacer, content)
    
    # Also for buttons if they have Text("...")
    # Add imports if changed
    if content != original:
        if "androidx.compose.ui.res.stringResource" not in content:
            content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.ui.res.stringResource\nimport com.example.R")
            if "import com.example.R" not in content: # Fallback if replace failed
                content = "import androidx.compose.ui.res.stringResource\nimport com.example.R\n" + content
                
        with open(filepath, 'w') as f:
            f.write(content)
            
print("Done replacing in files.")
