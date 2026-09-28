import os
import re

# We will collect all hardcoded strings we want to replace and add them to a dictionary
# Then we will update strings.xml and values-hi/strings.xml
# Then we will replace them in Kotlin files.

strings = {
    "str_sale_to": {"en": "Sale to %1$s", "hi": "%1$s को बिक्री"},
    "str_cash": {"en": "Cash", "hi": "नकद"},
    "str_purchase_from": {"en": "Purchase from %1$s", "hi": "%1$s से खरीद"},
    "str_unknown": {"en": "Unknown", "hi": "अज्ञात"},
    "str_tx_sale": {"en": "SALE", "hi": "बिक्री"},
    "str_tx_purchase": {"en": "PURCHASE", "hi": "खरीद"},
    "str_tx_expense": {"en": "EXPENSE", "hi": "खर्च"},
    "str_tx_payment": {"en": "PAYMENT", "hi": "भुगतान"},
    "str_tx_balance": {"en": "BALANCE", "hi": "बकाया"},
    "str_business_is_healthy": {"en": "Business is healthy.", "hi": "व्यवसाय स्वस्थ है।"},
    "str_add_products_health": {"en": "Add products and record sales to build your health score.", "hi": "अपना स्वास्थ्य स्कोर बनाने के लिए उत्पाद जोड़ें और बिक्री दर्ज करें।"},
    "str_high_outstanding": {"en": "High outstanding payments", "hi": "अधिक बकाया भुगतान"},
    "str_items_low_stock": {"en": "%1$s items low on stock", "hi": "%1$s आइटम कम स्टॉक में हैं"},
    "str_operating_loss": {"en": "Operating at a loss", "hi": "घाटे में चल रहा है"},
    "str_need_attention": {"en": "%1$s need attention.", "hi": "%1$s पर ध्यान देने की आवश्यकता है।"},
    "str_health_excellent": {"en": "Excellent", "hi": "उत्कृष्ट"},
    "str_health_good": {"en": "Good", "hi": "अच्छा"},
    "str_health_needs_attention": {"en": "Needs Attention", "hi": "ध्यान देने की आवश्यकता है"},
    "str_health_critical": {"en": "Critical", "hi": "गंभीर"},
    "str_not_enough_data": {"en": "Not enough business data yet. Keep recording sales and expenses to unlock AI insights.", "hi": "अभी पर्याप्त व्यावसायिक डेटा नहीं है। AI इनसाइट्स अनलॉक करने के लिए बिक्री और खर्च दर्ज करते रहें।"},
    "str_ask_vyaparsetu": {"en": "Ask VyaparSetu AI", "hi": "VyaparSetu AI से पूछें"},
    "str_customers": {"en": "Customers", "hi": "ग्राहक"},
    "str_suppliers": {"en": "Suppliers", "hi": "आपूर्तिकर्ता"},
    "str_inventory_alert": {"en": "Inventory Alert", "hi": "इन्वेंटरी अलर्ट"},
    "str_products_low_stock": {"en": "%1$s products are low on stock.", "hi": "%1$s उत्पाद कम स्टॉक में हैं।"},
    "str_add_first_product": {"en": "Add your first product to start tracking inventory.", "hi": "इन्वेंटरी को ट्रैक करना शुरू करने के लिए अपना पहला उत्पाद जोड़ें।"},
    "str_khata_alert": {"en": "Khata Alert", "hi": "खाता अलर्ट"},
    "str_customers_outstanding": {"en": "%1$s customers have outstanding payments.", "hi": "%1$s ग्राहकों के बकाया भुगतान हैं।"},
    "str_khata_outstanding": {"en": "Khata / Outstanding", "hi": "खाता / बकाया"},
    "str_customer_outstanding_appear": {"en": "Customer outstanding payments will appear here.", "hi": "ग्राहक के बकाया भुगतान यहां दिखाई देंगे।"},
    "str_no_outstanding": {"en": "No outstanding payments.", "hi": "कोई बकाया भुगतान नहीं।"},
    "str_transactions": {"en": "Transactions", "hi": "लेन-देन"},
    "str_no_recent_tx": {"en": "No recent transactions. Start billing to see them here.", "hi": "कोई हालिया लेन-देन नहीं। उन्हें यहाँ देखने के लिए बिलिंग शुरू करें।"},
    "str_switch_english": {"en": "Switch to English", "hi": "अंग्रेजी में बदलें (English)"},
    "str_switch_hindi": {"en": "हिंदी में बदलें (Hindi)", "hi": "हिंदी में बदलें (Hindi)"},
    "str_simulate_offline": {"en": "Simulate Offline Mode", "hi": "ऑफ़लाइन मोड का अनुकरण करें"},
    "str_simulate_online": {"en": "Simulate Online & Sync", "hi": "ऑनलाइन और सिंक का अनुकरण करें"},
    "str_exit_demo": {"en": "Exit SIH Demo Mode", "hi": "SIH डेमो मोड से बाहर निकलें"},
    "str_enter_demo": {"en": "Enter SIH Demo Mode", "hi": "SIH डेमो मोड में प्रवेश करें"},
    "str_user": {"en": "User", "hi": "उपयोगकर्ता"},
    "str_no_business_setup": {"en": "No Business Setup", "hi": "कोई व्यवसाय सेटअप नहीं"},
    "str_good_morning": {"en": "Good Morning", "hi": "सुप्रभात"},
    "str_good_afternoon": {"en": "Good Afternoon", "hi": "शुभ दोपहर"},
    "str_good_evening": {"en": "Good Evening", "hi": "शुभ संध्या"},
    "str_good_night": {"en": "Good Night", "hi": "शुभ रात्रि"},
    
    "str_edit_entity": {"en": "Edit %1$s", "hi": "%1$s संपादित करें"},
    "str_add_entity": {"en": "Add %1$s", "hi": "%1$s जोड़ें"},
    "str_select_customer": {"en": "Select Customer", "hi": "ग्राहक चुनें"},
    "str_balance_amt": {"en": "Balance: ₹%1$s", "hi": "बकाया: ₹%1$s"},
    "str_outstanding_amt": {"en": "Outstanding: ₹%1$s", "hi": "बकाया: ₹%1$s"},
    
    "str_in_stock": {"en": "%1$s in stock", "hi": "स्टॉक में %1$s"},
    "str_min_level": {"en": "Min Level: %1$s", "hi": "न्यूनतम स्तर: %1$s"},
    "str_current_stock_val": {"en": "Current Stock: %1$s", "hi": "वर्तमान स्टॉक: %1$s"},
    
    "str_phone_val": {"en": "Phone: %1$s", "hi": "फोन: %1$s"},
    "str_gstin_val": {"en": "GSTIN: %1$s", "hi": "GSTIN: %1$s"},
    "str_stock_val": {"en": "Stock: %1$s", "hi": "स्टॉक: %1$s"},
    
    "str_supplier_val": {"en": "Supplier: %1$s", "hi": "आपूर्तिकर्ता: %1$s"},
    "str_qty": {"en": "Qty", "hi": "मात्रा"},
    "str_item": {"en": "Item", "hi": "आइटम"},
    "str_price_val": {"en": "Price", "hi": "कीमत"},
    "str_total_val": {"en": "Total", "hi": "कुल"},
    "str_subtotal_val": {"en": "Subtotal: Rs. %1$s", "hi": "उप-कुल: रु. %1$s"},
    "str_discount_val": {"en": "Discount: Rs. %1$s", "hi": "छूट: रु. %1$s"},
    "str_tax_val": {"en": "Tax: Rs. %1$s", "hi": "टैक्स: रु. %1$s"},
    "str_grand_total_val": {"en": "Grand Total: Rs. %1$s", "hi": "कुल योग: रु. %1$s"},
    "str_date_val": {"en": "Date: %1$s", "hi": "दिनांक: %1$s"},
    "str_ref_no_val": {"en": "Ref No: %1$s", "hi": "संदर्भ संख्या: %1$s"},
    
    "str_offline_sync": {"en": "OFFLINE - Pending Sync", "hi": "ऑफ़लाइन - सिंक लंबित"},
    "str_demo_mode": {"en": "DEMO MODE", "hi": "डेमो मोड"},
    
    "str_add_products": {"en": "Add Product", "hi": "उत्पाद जोड़ें"},
    "str_add_customer": {"en": "Add Customer", "hi": "ग्राहक जोड़ें"},
    "str_add_supplier": {"en": "Add Supplier", "hi": "आपूर्तिकर्ता जोड़ें"},
    "str_add_expense_btn": {"en": "Add Expense", "hi": "खर्च जोड़ें"},
}

def inject_strings(file_path, lang):
    with open(file_path, 'r') as f:
        content = f.read()
    
    insertion_point = content.rfind("</resources>")
    if insertion_point == -1:
        return
        
    new_xml = ""
    for k, v in strings.items():
        if f'name="{k}"' not in content:
            new_xml += f'    <string name="{k}">{v[lang]}</string>\n'
            
    if new_xml:
        content = content[:insertion_point] + new_xml + content[insertion_point:]
        with open(file_path, 'w') as f:
            f.write(content)

inject_strings('app/src/main/res/values/strings.xml', 'en')
inject_strings('app/src/main/res/values-hi/strings.xml', 'hi')

