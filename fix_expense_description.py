import re

# 1. Update NewExpenseScreen.kt
with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'r') as f:
    text = f.read()

# Remove the description validation
validation_code = """
                        if (description.isBlank()) {
                            errorMessage = "Description is required."
                            return@VyaparButton
                        }"""
text = text.replace(validation_code, "")

with open('app/src/main/java/com/example/presentation/expenses/NewExpenseScreen.kt', 'w') as f:
    f.write(text)

# 2. Update string resources
def fix_strings(path, old_str, new_str):
    try:
        with open(path, 'r') as f:
            content = f.read()
        content = content.replace(f'>{old_str}<', f'>{new_str}<')
        with open(path, 'w') as f:
            f.write(content)
    except:
        pass

fix_strings('app/src/main/res/values/strings.xml', 'Description *', 'Description (Optional)')
fix_strings('app/src/main/res/values-hi/strings.xml', 'Description *', 'विवरण (वैकल्पिक)')
# just in case it was translated previously
fix_strings('app/src/main/res/values-hi/strings.xml', 'विवरण *', 'विवरण (वैकल्पिक)')
