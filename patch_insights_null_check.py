import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Replace rendering to check for null string instead of just isNotBlank
old_render = """            aiInsights?.forEach { insight ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp, top = 2.dp))
                        Text(insight.observation, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    if (insight.reason.isNotBlank()) {
                        Text(insight.reason, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                    if (insight.recommendation.isNotBlank()) {
                        Text(insight.recommendation, fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                }
            }"""

new_render = """            aiInsights?.forEach { insight ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp, top = 2.dp))
                        Text(insight.observation ?: "", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    if (!insight.reason.isNullOrBlank()) {
                        Text(insight.reason ?: "", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                    if (!insight.recommendation.isNullOrBlank()) {
                        Text(insight.recommendation ?: "", fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                }
            }"""

content = content.replace(old_render, new_render)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)
