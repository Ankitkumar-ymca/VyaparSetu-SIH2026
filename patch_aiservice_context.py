import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

new_context = """
            Health Score: ${summary.healthScore}/100 (${summary.healthStatus})
            Health Reasons: ${summary.healthReasons.joinToString(", ")}
"""

forecast_context = """
            Health Score: ${summary.healthScore}/100 (${summary.healthStatus})
            Health Reasons: ${summary.healthReasons.joinToString(", ")}
            
            Business Forecast (Next 30 days):
            ${MockDataStore.getBusinessForecast()?.let {
                "Overall Risk: ${it.overallRisk}, Cash Flow: ${it.cashFlowRisk}, Sales Trend: ${it.salesTrend}, Expense Trend: ${it.expenseTrend}, Inventory Risk: ${it.inventoryRisk}"
            } ?: "Not enough data for forecast."}
"""

content = content.replace(new_context, forecast_context)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
