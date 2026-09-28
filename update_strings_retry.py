import re

def update_xml(filepath, is_hindi=False):
    with open(filepath, 'r') as f:
        content = f.read()

    new_strings = """
    <string name="msg_ai_retrying">AI is temporarily busy. Retrying...</string>
"""

    if is_hindi:
        new_strings = """
    <string name="msg_ai_retrying">AI अभी व्यस्त है। पुनः प्रयास किया जा रहा है...</string>
"""

    if "msg_ai_retrying" not in content:
        content = content.replace("</resources>", new_strings + "</resources>")
        with open(filepath, 'w') as f:
            f.write(content)

update_xml('app/src/main/res/values/strings.xml', False)
update_xml('app/src/main/res/values-hi/strings.xml', True)
