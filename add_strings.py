import re

strings = {
    "str_ai_business_advisor": "AI Business Advisor",
    "str_understand_your_business_and_g": "Understand your business and get actionable recommendations.",
    "str_start_adding_sales_purchases": "Start adding sales, purchases, expenses and inventory data to receive personalized business insights.",
    "str_business_health": "Business Health",
    "str_analyzing_your_business_data": "Analyzing your business...",
    "str_analyze_my_business": "Analyze My Business",
    "str_ask_my_business": "Ask My Business",
    "str_insights_and_recommendations": "Insights and Recommendations",
    "str_insufficient_data": "Not enough historical data for comparison."
}

strings_hi = {
    "str_ai_business_advisor": "AI बिज़नेस एडवाइज़र",
    "str_understand_your_business_and_g": "अपने बिज़नेस को समझें और काम की सलाह पाएं।",
    "str_start_adding_sales_purchases": "निजी बिज़नेस इनसाइट्स पाने के लिए बिक्री, खरीदारी, खर्च और इन्वेंट्री डेटा जोड़ना शुरू करें।",
    "str_business_health": "बिज़नेस हेल्थ",
    "str_analyzing_your_business_data": "आपके बिज़नेस का विश्लेषण हो रहा है...",
    "str_analyze_my_business": "मेरे बिज़नेस का विश्लेषण करें",
    "str_ask_my_business": "मेरे बिज़नेस के बारे में पूछें",
    "str_insights_and_recommendations": "इनसाइट्स और सुझाव",
    "str_insufficient_data": "तुलना के लिए पर्याप्त पुराना डेटा नहीं है।"
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
