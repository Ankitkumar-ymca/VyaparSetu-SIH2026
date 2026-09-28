import re
import os

def add_strings(file_path, strings):
    with open(file_path, 'r') as f:
        text = f.read()
    
    insert_str = ""
    for k, v in strings.items():
        if f'name="{k}"' not in text:
            insert_str += f'    <string name="{k}">{v}</string>\n'
    
    text = text.replace('</resources>', f'{insert_str}</resources>')
    with open(file_path, 'w') as f:
        f.write(text)

eng = {
    "str_delete_product": "Delete Product",
    "str_delete_chat": "Delete Chat"
}

hi = {
    "str_delete_product": "उत्पाद हटाएं",
    "str_delete_chat": "चैट हटाएं"
}

add_strings('app/src/main/res/values/strings.xml', eng)
if os.path.exists('app/src/main/res/values-hi/strings.xml'):
    add_strings('app/src/main/res/values-hi/strings.xml', hi)

