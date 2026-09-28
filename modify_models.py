import re

with open('app/src/main/java/com/example/domain/model/Models.kt', 'r') as f:
    text = f.read()

# Add isActive to Product
text = re.sub(
    r'val updatedAt: Long\n\)',
    r'val updatedAt: Long,\n    val isActive: Boolean = true\n)',
    text,
    count=1
)

# Add isActive to Customer
text = re.sub(
    r'val updatedAt: Long\n\)',
    r'val updatedAt: Long,\n    val isActive: Boolean = true\n)',
    text,
    count=1
)

# Add isActive to Supplier
text = re.sub(
    r'val updatedAt: Long\n\)',
    r'val updatedAt: Long,\n    val isActive: Boolean = true\n)',
    text,
    count=1
)

with open('app/src/main/java/com/example/domain/model/Models.kt', 'w') as f:
    f.write(text)
