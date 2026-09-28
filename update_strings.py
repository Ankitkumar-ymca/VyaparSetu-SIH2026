import re

def update_xml(filepath, is_hindi=False):
    with open(filepath, 'r') as f:
        content = f.read()

    new_strings = """
    <string name="err_no_business">No active business session.</string>
    <string name="err_specify_customer">Please specify the customer name.</string>
    <string name="err_specify_product">Please specify the product name.</string>
    <string name="err_specify_sale_amount">Please specify a valid sale amount.</string>
    <string name="err_product_not_found">Product \'%1$s\' was not found in inventory.</string>
    <string name="err_insufficient_stock">Insufficient stock. Only %1$d units of \'%2$s\' are available.</string>
    <string name="err_customer_not_found">Customer \'%1$s\' was not found.</string>
    <string name="title_credit_sale">Credit Sale</string>
    <string name="title_sale">Sale</string>
    <string name="lbl_customer">Customer:</string>
    <string name="lbl_product">Product:</string>
    <string name="lbl_quantity">Quantity:</string>
    <string name="lbl_total">Total:</string>
    <string name="lbl_payment">Payment:</string>
    <string name="lbl_credit">Credit</string>
    <string name="msg_inventory_deduct">Inventory: %1$d units will be deducted</string>
    <string name="msg_customer_outstanding">Customer Outstanding: ₹%1$s will be added</string>
    <string name="err_specify_expense_amount">Please specify a valid expense amount.</string>
    <string name="title_expense">Expense</string>
    <string name="lbl_category">Category:</string>
    <string name="err_specify_payment_amount">Please specify a valid payment amount.</string>
    <string name="title_record_payment">Record Payment</string>
    <string name="lbl_amount">Amount:</string>
    <string name="err_specify_supplier">Please specify the supplier name.</string>
    <string name="err_specify_purchase_amount">Please specify a valid purchase amount.</string>
    <string name="err_supplier_not_found">Supplier \'%1$s\' was not found.</string>
    <string name="title_purchase">Purchase</string>
    <string name="lbl_supplier">Supplier:</string>
    <string name="msg_inventory_add">Inventory: %1$d units will be added</string>
    <string name="err_specify_opening_amount">Please specify a valid opening balance amount.</string>
    <string name="title_opening_balance">Add Opening Balance</string>
    <string name="err_unknown_action">Unknown action or not supported yet.</string>
    <string name="msg_confirm_action">Do you want to confirm this action?</string>
    <string name="msg_provide_more_details">Please provide more details.</string>
    <string name="msg_action_executed">Action executed successfully.</string>
    <string name="msg_action_failed">Failed to execute: %1$s</string>
"""

    if is_hindi:
        new_strings = """
    <string name="err_no_business">कोई सक्रिय व्यवसाय सत्र नहीं है।</string>
    <string name="err_specify_customer">कृपया ग्राहक का नाम बताएं।</string>
    <string name="err_specify_product">कृपया उत्पाद का नाम बताएं।</string>
    <string name="err_specify_sale_amount">कृपया सही बिक्री राशि दर्ज करें।</string>
    <string name="err_product_not_found">उत्पाद \'%1$s\' इन्वेंटरी में नहीं मिला।</string>
    <string name="err_insufficient_stock">स्टॉक पर्याप्त नहीं है। \'%2$s\' के केवल %1$d यूनिट उपलब्ध हैं।</string>
    <string name="err_customer_not_found">ग्राहक \'%1$s\' नहीं मिला।</string>
    <string name="title_credit_sale">उधार बिक्री</string>
    <string name="title_sale">बिक्री</string>
    <string name="lbl_customer">ग्राहक:</string>
    <string name="lbl_product">उत्पाद:</string>
    <string name="lbl_quantity">मात्रा:</string>
    <string name="lbl_total">कुल:</string>
    <string name="lbl_payment">भुगतान:</string>
    <string name="lbl_credit">उधार (Credit)</string>
    <string name="msg_inventory_deduct">इन्वेंटरी: %1$d यूनिट कम हो जाएंगे</string>
    <string name="msg_customer_outstanding">बकाया: ₹%1$s ग्राहक के खाते में जुड़ेंगे</string>
    <string name="err_specify_expense_amount">कृपया सही खर्च राशि दर्ज करें।</string>
    <string name="title_expense">खर्च</string>
    <string name="lbl_category">श्रेणी:</string>
    <string name="err_specify_payment_amount">कृपया सही भुगतान राशि दर्ज करें।</string>
    <string name="title_record_payment">भुगतान दर्ज करें</string>
    <string name="lbl_amount">राशि:</string>
    <string name="err_specify_supplier">कृपया सप्लायर का नाम बताएं।</string>
    <string name="err_specify_purchase_amount">कृपया सही खरीद राशि दर्ज करें।</string>
    <string name="err_supplier_not_found">सप्लायर \'%1$s\' नहीं मिला।</string>
    <string name="title_purchase">खरीद</string>
    <string name="lbl_supplier">सप्लायर:</string>
    <string name="msg_inventory_add">इन्वेंटरी: %1$d यूनिट जुड़ जाएंगे</string>
    <string name="err_specify_opening_amount">कृपया सही प्रारंभिक शेष राशि दर्ज करें।</string>
    <string name="title_opening_balance">प्रारंभिक शेष जोड़ें</string>
    <string name="err_unknown_action">अज्ञात कार्रवाई।</string>
    <string name="msg_confirm_action">क्या आप इस कार्रवाई की पुष्टि करना चाहते हैं?</string>
    <string name="msg_provide_more_details">कृपया अधिक जानकारी प्रदान करें।</string>
    <string name="msg_action_executed">लेन-देन सफलतापूर्वक दर्ज किया गया।</string>
    <string name="msg_action_failed">निष्पादन में विफल: %1$s</string>
"""

    if "err_no_business" not in content:
        content = content.replace("</resources>", new_strings + "</resources>")
        with open(filepath, 'w') as f:
            f.write(content)

update_xml('app/src/main/res/values/strings.xml', False)
update_xml('app/src/main/res/values-hi/strings.xml', True)
