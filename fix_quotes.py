import re

for filepath in ['app/src/main/res/values/strings.xml', 'app/src/main/res/values-hi/strings.xml']:
    with open(filepath, 'r') as f:
        content = f.read()

    content = content.replace("\\'%1$s\\'", "'%1$s'")
    content = content.replace("\\'%2$s\\'", "'%2$s'")

    with open(filepath, 'w') as f:
        f.write(content)
