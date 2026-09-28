import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

old_fallback = 'aiInsights = result.getOrNull() ?: listOf(com.example.domain.model.BusinessInsight("Analysis temporarily unavailable. Please try again.", "", ""))'
new_fallback = 'val errorMsg = stringResource(R.string.str_not_enough_data)\n                        aiInsights = result.getOrNull() ?: listOf(com.example.domain.model.BusinessInsight(errorMsg, "", ""))'

content = content.replace(old_fallback, new_fallback)

# Actually stringResource cannot be called inside a coroutine launch context because it's a @Composable function!
# We must move it outside the onClick or handle it differently.
