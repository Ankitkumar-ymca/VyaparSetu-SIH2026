import re

def update_xml(filepath, is_hindi=False):
    with open(filepath, 'r') as f:
        content = f.read()

    new_strings = """
    <string name="err_ai_busy">AI service is temporarily busy. Please try again in a moment.</string>
    <string name="msg_ai_connecting">Connecting to AI...</string>
"""

    if is_hindi:
        new_strings = """
    <string name="err_ai_busy">AI सेवा अभी व्यस्त है। कृपया कुछ देर बाद फिर प्रयास करें।</string>
    <string name="msg_ai_connecting">AI से कनेक्ट हो रहा है...</string>
"""

    if "err_ai_busy" not in content:
        content = content.replace("</resources>", new_strings + "</resources>")
        with open(filepath, 'w') as f:
            f.write(content)

update_xml('app/src/main/res/values/strings.xml', False)
update_xml('app/src/main/res/values-hi/strings.xml', True)
