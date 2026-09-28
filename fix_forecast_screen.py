import re

with open('app/src/main/java/com/example/presentation/ai/BusinessForecastScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("R.string.str_back", 'R.string.str_empty_1')

if 'import androidx.compose.foundation.BorderStroke' not in content:
    content = content.replace('import androidx.compose.foundation.border', 'import androidx.compose.foundation.border\nimport androidx.compose.foundation.BorderStroke')

with open('app/src/main/java/com/example/presentation/ai/BusinessForecastScreen.kt', 'w') as f:
    f.write(content)
