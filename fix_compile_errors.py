import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    content = f.read()
content = content.replace("isInMonth(it.timestamp", "isInMonth(it.createdAt")
with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("R.string.str_sales", "R.string.str_nav_sales")
content = content.replace("com.example.presentation.utils.Formatters.formatCurrency(value)", 'String.format("%,.2f", value)')

if 'import androidx.compose.foundation.BorderStroke' not in content:
    content = content.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.BorderStroke')

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)
