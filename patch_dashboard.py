import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Change the state type
content = content.replace("var aiInsights by remember { mutableStateOf<List<String>?>(null) }", "var aiInsights by remember { mutableStateOf<List<com.example.domain.model.BusinessInsight>?>(null) }")

# Change the rendering logic
old_render = """            aiInsights?.forEach { insight ->
                val lines = insight.split("Reason:", "Recommended Action:").map { it.trim() }
                if (lines.size >= 3) {
                    val obs = lines[0].removePrefix("Observation:").trim()
                    val reason = lines[1]
                    val action = lines[2]
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("💡", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp, top = 2.dp))
                            Text(obs, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Text(reason, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                        Text(action, fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 24.dp, top = 2.dp))
                    }
                } else {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("💡", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp, top = 2.dp))
                        Text(insight, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }"""

new_render = """            aiInsights?.forEach { insight ->
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

content = content.replace(old_render, new_render)

# Fallback error mapping change (List<String> -> emptyList or something)
old_fallback = """aiInsights = result.getOrNull() ?: listOf("Analysis temporarily unavailable. Please try again.")"""
new_fallback = """aiInsights = result.getOrNull() ?: listOf(com.example.domain.model.BusinessInsight("Analysis temporarily unavailable. Please try again.", "", ""))"""

content = content.replace(old_fallback, new_fallback)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)
