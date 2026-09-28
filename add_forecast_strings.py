import re

strings = {
    "str_business_forecast": "Business Forecast",
    "str_cash_flow": "Cash Flow",
    "str_sales_trend": "Sales Trend",
    "str_expense_trend": "Expense Trend",
    "str_inventory_risk": "Inventory Risk",
    "str_outstanding_risk": "Outstanding Risk",
    "str_overall_risk": "Overall Risk",
    "str_confidence": "Confidence",
    "str_low": "Low",
    "str_moderate": "Moderate",
    "str_medium": "Medium",
    "str_high": "High",
    "str_what_should_i_do": "What should I do?",
    "str_generating_advice": "Generating advice...",
    "str_get_ai_advice": "Get AI Advice",
    "str_increasing": "Increasing",
    "str_decreasing": "Decreasing",
    "str_stable": "Stable"
}

strings_hi = {
    "str_business_forecast": "व्यवसाय पूर्वानुमान",
    "str_cash_flow": "कैश फ्लो",
    "str_sales_trend": "बिक्री का रुझान",
    "str_expense_trend": "खर्च का रुझान",
    "str_inventory_risk": "इन्वेंटरी रिस्क",
    "str_outstanding_risk": "बकाया रिस्क",
    "str_overall_risk": "कुल रिस्क",
    "str_confidence": "विश्वास",
    "str_low": "कम",
    "str_moderate": "मध्यम",
    "str_medium": "मध्यम",
    "str_high": "उच्च",
    "str_what_should_i_do": "मुझे क्या करना चाहिए?",
    "str_generating_advice": "सलाह तैयार हो रही है...",
    "str_get_ai_advice": "AI सलाह लें",
    "str_increasing": "बढ़ रहा है",
    "str_decreasing": "घट रहा है",
    "str_stable": "स्थिर"
}

def add_strings(filepath, d):
    with open(filepath, 'r') as f:
        content = f.read()
    
    for key, val in d.items():
        if f'name="{key}"' not in content:
            content = content.replace('</resources>', f'    <string name="{key}">{val}</string>\n</resources>')
            
    with open(filepath, 'w') as f:
        f.write(content)

add_strings('app/src/main/res/values/strings.xml', strings)
add_strings('app/src/main/res/values-hi/strings.xml', strings_hi)
