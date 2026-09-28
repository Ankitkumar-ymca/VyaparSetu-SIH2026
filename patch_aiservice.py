import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

new_get_context = """
    private fun getBusinessContext(): String {
        val summary = MockDataStore.getBusinessSummary() ?: return "No business data available."
        
        return \"\"\"
            Business Summary:
            Period: ${summary.currentPeriod.periodName}
            Sales: ₹${summary.currentPeriod.totalSales}
            Purchases: ₹${summary.currentPeriod.totalPurchases}
            Expenses: ₹${summary.currentPeriod.totalExpenses}
            Profit: ₹${summary.currentPeriod.profit}
            Profit Margin: ${summary.currentPeriod.profitMargin}%
            
            Previous Period (${summary.previousPeriod?.periodName}):
            Sales: ₹${summary.previousPeriod?.totalSales ?: 0.0}
            Profit: ₹${summary.previousPeriod?.profit ?: 0.0}
            
            Customer Outstanding: ₹${summary.customerOutstanding}
            Supplier Balance: ₹${summary.supplierBalance}
            Inventory Value: ₹${summary.inventoryValue}
            
            Top Expense Categories: ${summary.topExpenseCategories.joinToString(", ")}
            Top Products: ${summary.topProducts.joinToString(", ")}
            Low Stock Products: ${summary.lowStockProducts.joinToString(", ")}
            
            Health Score: ${summary.healthScore}/100 (${summary.healthStatus})
            Health Reasons: ${summary.healthReasons.joinToString(", ")}
        \"\"\".trimIndent()
    }
"""

content = re.sub(
    r'private fun getBusinessContext\(\): String \{.*?\n    \}',
    new_get_context.strip(),
    content,
    flags=re.DOTALL
)

# Update answerBusinessQuestion
anti_hallucination_prompt = """
                You are VyaparSetu AI, an expert business assistant for small business owners in India.
                You are analyzing a small-business database. Use ONLY the supplied business context. 
                Never invent sales, expenses, customers, products, balances, dates or financial values. 
                If information is unavailable, explicitly say that sufficient data is not available.
                Always use a helpful, professional, and encouraging tone.
                IMPORTANT: You MUST reply in the EXACT SAME LANGUAGE the user used in their question (English, Hindi, or Hinglish).
"""
content = re.sub(
    r'You are VyaparSetu AI.*?(?=CURRENT BUSINESS CONTEXT)',
    anti_hallucination_prompt,
    content,
    flags=re.DOTALL
)

# Update generateBusinessInsights
insights_prompt = """
                You are an AI business analyst. Based on the business context, generate exactly 3 short, actionable insights.
                Structure recommendations as:
                Observation: [What happened]
                Reason: [Why it happened]
                Recommended Action: [What to do]
                
                Focus on sales trends, low stock, or outstanding balances.
                Do NOT invent numbers. Use ONLY the provided context. If there is not enough data, say "More business data is required before making a reliable recommendation."
                
                Format as a JSON array of strings: ["Observation: ... Reason: ... Recommended Action: ...", "...", "..."]
"""
content = re.sub(
    r'You are an AI business analyst.*?(?=CURRENT BUSINESS CONTEXT)',
    insights_prompt,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
