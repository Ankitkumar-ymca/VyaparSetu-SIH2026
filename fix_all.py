import re

# Fix SmartActionValidator
with open('app/src/main/java/com/example/domain/model/SmartActionValidator.kt', 'r') as f:
    content = f.read()

content = content.replace('com.example.aistudio.R', 'com.example.R')

# Fix the broken line
old_broken = 'val outLbl = if(paymentMethod == "CREDIT") "\n" + context.getString(R.string.msg_customer_outstanding, amount.toString()) else ""'
new_fixed = 'val outLbl = if(paymentMethod == "CREDIT") "\\n" + context.getString(R.string.msg_customer_outstanding, amount.toString()) else ""'
content = content.replace('val outLbl = if(paymentMethod == "CREDIT") "\n" + context.getString(R.string.msg_customer_outstanding, amount.toString()) else ""', new_fixed)

with open('app/src/main/java/com/example/domain/model/SmartActionValidator.kt', 'w') as f:
    f.write(content)

# Fix AiAssistantScreen
with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('com.example.aistudio.R', 'com.example.R')

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
