import xml.etree.ElementTree as ET
import os

tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()

dict_hi = {
    "Home": "होम",
    "Sales": "बिक्री",
    "Inventory": "इन्वेंटरी",
    "Khata": "खाता",
    "Reports": "रिपोर्ट",
    "Expenses": "खर्चे",
    "Profile & Settings": "प्रोफ़ाइल और सेटिंग्स",
    "Products": "उत्पाद",
    "Customers": "ग्राहक",
    "Suppliers": "आपूर्तिकर्ता",
    "Purchases": "खरीद",
    "Dashboard": "डैशबोर्ड",
    "Profit": "मुनाफ़ा",
    "Amount Due": "बाकी रकम",
    "Total Paid": "कुल भुगतान",
    "Save": "सहेजें",
    "Cancel": "रद्द करें",
    "Add Expense": "खर्च जोड़ें",
    "Logout": "लॉग आउट",
    "Business Details": "व्यवसाय विवरण",
    "App Settings": "ऐप सेटिंग्स",
    "Switch to English": "English में बदलें",
    "Receive": "प्राप्त करें",
    "Add Previous Outstanding": "पिछला बकाया जोड़ें",
    "Recent Transactions": "हाल के लेनदेन",
    "No recent transactions.": "कोई हाल का लेनदेन नहीं।",
    "Download PDF": "PDF डाउनलोड करें",
    "Amount (₹) *": "रकम (₹) *",
    "Save Payment": "भुगतान सहेजें"
}

def translate(text):
    if not text: return ""
    for k, v in dict_hi.items():
        if k.lower() == text.lower():
            return v
    # Fallback to English if not explicitly handled
    return text

for child in root:
    original = child.text
    if original:
        child.text = translate(original)
        
os.makedirs('app/src/main/res/values-hi', exist_ok=True)
tree.write('app/src/main/res/values-hi/strings.xml', encoding='utf-8', xml_declaration=True)
