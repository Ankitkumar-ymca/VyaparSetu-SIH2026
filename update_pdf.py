import re

with open('app/src/main/java/com/example/presentation/utils/PdfGenerator.kt', 'r') as f:
    content = f.read()

replacements = {
    '"Date: $dateString"': 'context.getString(R.string.str_date_val, dateString)',
    '"Ref No: ${purchase.referenceNumber}"': 'context.getString(R.string.str_ref_no_val, purchase.referenceNumber)',
    '"Supplier: ${supplier?.name ?: ""}"': 'context.getString(R.string.str_supplier_val, supplier?.name ?: "")',
    '"Item"': 'context.getString(R.string.str_item)',
    '"Qty"': 'context.getString(R.string.str_qty)',
    '"Price"': 'context.getString(R.string.str_price_val)',
    '"Total"': 'context.getString(R.string.str_total_val)',
    '"Rs. ${item.unitPrice}"': '"Rs. ${item.unitPrice}"', # Keep Rs. format
    '"Rs. ${item.totalPrice}"': '"Rs. ${item.totalPrice}"',
    '"Subtotal: Rs. ${purchase.subtotal}"': 'context.getString(R.string.str_subtotal_val, purchase.subtotal)',
    '"Tax: Rs. ${purchase.tax}"': 'context.getString(R.string.str_tax_val, purchase.tax)',
    '"Discount: Rs. ${purchase.discount}"': 'context.getString(R.string.str_discount_val, purchase.discount)',
    '"Grand Total: Rs. ${purchase.grandTotal}"': 'context.getString(R.string.str_grand_total_val, purchase.grandTotal)',
    '"PURCHASE INVOICE"': 'context.getString(R.string.str_purchase_invoice)',
}

changed = False
for k, v in replacements.items():
    if k in content:
        content = content.replace(k, v)
        changed = True

if changed and 'import com.example.R' not in content:
    content = 'import com.example.R\n' + content

if changed:
    with open('app/src/main/java/com/example/presentation/utils/PdfGenerator.kt', 'w') as f:
        f.write(content)

