import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

old_action_create = """                    paymentMethod = jsonObj["paymentMethod"]?.jsonPrimitive?.contentOrNull,
                    notes = jsonObj["summary"]?.jsonPrimitive?.contentOrNull ?: "Unknown action",
                    expenseCategory = jsonObj["expenseCategory"]?.jsonPrimitive?.contentOrNull
                )"""

new_action_create = """                    paymentMethod = jsonObj["paymentMethod"]?.jsonPrimitive?.contentOrNull,
                    notes = jsonObj["summary"]?.jsonPrimitive?.contentOrNull ?: "Unknown action",
                    expenseCategory = jsonObj["expenseCategory"]?.jsonPrimitive?.contentOrNull,
                    missingFields = jsonObj["missingFields"]?.jsonArray?.map { it.jsonPrimitive.content }
                )"""

content = content.replace(old_action_create, new_action_create)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
