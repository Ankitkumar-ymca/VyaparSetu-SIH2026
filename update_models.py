with open('app/src/main/java/com/example/domain/model/Models.kt', 'r') as f:
    content = f.read()
    
content = content.replace("val notes: String? = null", "val notes: String? = null,\n    val expenseCategory: String? = null")

with open('app/src/main/java/com/example/domain/model/Models.kt', 'w') as f:
    f.write(content)
