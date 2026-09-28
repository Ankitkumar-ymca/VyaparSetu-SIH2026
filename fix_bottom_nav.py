import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

# Add import
if 'import androidx.compose.ui.text.style.TextOverflow' not in text:
    text = text.replace('import androidx.compose.ui.text.style.TextAlign', 'import androidx.compose.ui.text.style.TextAlign\nimport androidx.compose.ui.text.style.TextOverflow')

# Replacement code for BottomNavigationBar
new_nav = """@Composable
fun BottomNavigationBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val items = listOf(
        Triple("Home", "🏠", R.string.str_nav_home),
        Triple("Sales", "🏷️", R.string.str_nav_sales),
        Triple("Inventory", "📦", R.string.str_nav_inventory),
        Triple("Khata", "👥", R.string.str_nav_khata),
        Triple("Expenses", "💸", R.string.str_nav_expenses),
        Triple("Reports", "📊", R.string.str_nav_reports)
    )
    
    Surface(
        color = Color.White,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        modifier = Modifier.border(width = 1.dp, color = CardBorder).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, stringRes) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(label) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .weight(1f, fill = false) // Allows it to take needed space but not force equal weight
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (currentTab == label) BlueLight else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = stringRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentTab == label) BlueNavy else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}"""

# Replace
pattern = r'@Composable\nfun BottomNavigationBar\(.*?\}\n\}'
text = re.sub(pattern, new_nav, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)
