import re

with open('app/src/main/java/com/example/presentation/navigation/AppNavigation.kt', 'r') as f:
    content = f.read()

forecast_route = """
        composable("business_forecast") {
            com.example.presentation.ai.BusinessForecastScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }
        
        composable("ai_assistant") {
"""

content = content.replace('composable("ai_assistant") {', forecast_route)

with open('app/src/main/java/com/example/presentation/navigation/AppNavigation.kt', 'w') as f:
    f.write(content)
