with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Fix stringResource inside launch block
content = content.replace(
    "aiInsights = result.getOrNull() ?: listOf(stringResource(R.string.str_not_enough_data))",
    "val msg = noDataMsg\naiInsights = result.getOrNull() ?: listOf(msg)"
)
content = content.replace(
    "val coroutineScope = rememberCoroutineScope()",
    "val coroutineScope = rememberCoroutineScope()\n                val noDataMsg = stringResource(R.string.str_not_enough_data)"
)

# Fix StatCardSimple currentTab assignment (must be the raw string, or we change it back to just string)
# Wait, currentTab == "Inventory" was replaced with currentTab == stringResource(...)
# Let's revert the state identifiers to English strings.
content = content.replace('currentTab == stringResource(R.string.str_inventory)', 'currentTab == "Inventory"')
content = content.replace('currentTab == stringResource(R.string.str_khata)', 'currentTab == "Khata"')
content = content.replace('currentTab = stringResource(R.string.str_inventory)', 'currentTab = "Inventory"')
content = content.replace('currentTab = stringResource(R.string.str_khata)', 'currentTab = "Khata"')
content = content.replace('Triple(stringResource(R.string.str_inventory)', 'Triple("Inventory"')
content = content.replace('Triple(stringResource(R.string.str_khata)', 'Triple("Khata"')
content = content.replace('EmptyStateCard(stringResource(R.string.str_inventory)', 'EmptyStateCard("Inventory"')
content = content.replace('EmptyStateCard(stringResource(R.string.str_khata)', 'EmptyStateCard("Khata"')
content = content.replace('EmptyStateCard(stringResource(R.string.str_khata_outstanding)', 'EmptyStateCard("Khata / Outstanding"')

# Fix @Composable getGreetingMessage
content = content.replace("fun getGreetingMessage(): String {", "@Composable\nfun getGreetingMessage(): String {")

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

