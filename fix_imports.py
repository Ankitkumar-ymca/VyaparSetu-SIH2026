import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import com.example.domain.model.ValidationResult", "import com.example.domain.model.ValidationResult\nimport com.example.aistudio.R")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
