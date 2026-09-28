import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """            }
                    } else if (currentTab == "Expenses") {
                FloatingActionButton(
                    onClick = { onNavigateToSubRoute("new_expense") },
                    containerColor = BlueLight,
                    contentColor = BlueNavy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
                }
            } else if (currentTab == "Khata") {"""

if target in content:
    content = content.replace(target, '            } else if (currentTab == "Khata") {')
    with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
        f.write(content)
    print("Fixed.")
else:
    print("Target not found. Let's try regex.")
    
