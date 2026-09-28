import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Add the variable
content = content.replace("val coroutineScope = rememberCoroutineScope()", "val coroutineScope = rememberCoroutineScope()\n    val defaultErrorMsg = stringResource(R.string.str_not_enough_data)")

# Replace the hardcoded english
old_fallback = 'aiInsights = result.getOrNull() ?: listOf(com.example.domain.model.BusinessInsight("Analysis temporarily unavailable. Please try again.", "", ""))'
new_fallback = 'aiInsights = result.getOrNull() ?: listOf(com.example.domain.model.BusinessInsight(defaultErrorMsg, "", ""))'
content = content.replace(old_fallback, new_fallback)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

