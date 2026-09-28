import re

with open('app/src/main/java/com/example/domain/model/Models.kt', 'r') as f:
    content = f.read()
    
content = content.replace("val expenseCategory: String? = null", "val expenseCategory: String? = null,\n    val missingFields: List<String>? = null")

with open('app/src/main/java/com/example/domain/model/Models.kt', 'w') as f:
    f.write(content)
