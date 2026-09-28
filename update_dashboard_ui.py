import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# First, remove the old calculate health score block in the DashboardScreen main function
content = re.sub(
    r'    // Calculate Business Health Score \(0-100\)\s*var healthScore = 100\s*var healthReason = .*?val healthColor = when \{\s*healthScore >= 80 -> AccentGreen\s*healthScore >= 60 -> BlueAccent\s*healthScore >= 40 -> AccentOrange\s*else -> AccentRed\s*\}',
    '',
    content,
    flags=re.DOTALL
)

# Then replace the UI components
old_ui_block = r'                // Business Health Score.*?                    VyaparButton\(\s*text = stringResource\(R.string.str_ask_vyaparsetu\),\s*onClick = \{ onNavigateToSubRoute\("ai_assistant"\) \},\s*modifier = Modifier.fillMaxWidth\(\)\s*\)\s*\}'

new_ui_block = """
                AiAdvisorCard(
                    onAskBusiness = { onNavigateToSubRoute("ai_assistant") },
                    onNavigateToSubRoute = onNavigateToSubRoute
                )
"""

content = re.sub(old_ui_block, new_ui_block.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)
