import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

old_buttons = """        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    isGeneratingInsights = true
                    coroutineScope.launch {
                        val result = com.example.data.repository.AiService.generateBusinessInsights()
                        isGeneratingInsights = false
                        aiInsights = result.getOrNull() ?: listOf("Analysis temporarily unavailable. Please try again.")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                border = BorderStroke(1.dp, BlueNavy)
            ) {
                Text(stringResource(R.string.str_analyze_my_business), fontSize = 12.sp)
            }
            
            VyaparButton(
                text = stringResource(R.string.str_ask_my_business),
                onClick = onAskBusiness,
                modifier = Modifier.weight(1f)
            )
        }"""

new_buttons = """        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    isGeneratingInsights = true
                    coroutineScope.launch {
                        val result = com.example.data.repository.AiService.generateBusinessInsights()
                        isGeneratingInsights = false
                        aiInsights = result.getOrNull() ?: listOf("Analysis temporarily unavailable. Please try again.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                border = BorderStroke(1.dp, BlueNavy)
            ) {
                Text(stringResource(R.string.str_analyze_my_business), fontSize = 14.sp)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onNavigateToSubRoute("business_forecast") },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.str_business_forecast), fontSize = 12.sp)
                }
                
                VyaparButton(
                    text = stringResource(R.string.str_ask_my_business),
                    onClick = onAskBusiness,
                    modifier = Modifier.weight(1f)
                )
            }
        }"""

content = content.replace(old_buttons, new_buttons)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)
