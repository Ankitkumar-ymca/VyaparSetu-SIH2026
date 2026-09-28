import re
import os

replacements = {
    'if (isEditing) "Edit $entityName" else "Add $entityName"': 'if (isEditing) stringResource(R.string.str_edit_entity, entityName) else stringResource(R.string.str_add_entity, entityName)',
    '"Select Customer"': 'stringResource(R.string.str_select_customer)',
    '"Outstanding: ₹${customer.outstandingAmount}"': 'stringResource(R.string.str_outstanding_amt, customer.outstandingAmount)',
    '"Balance: ₹${entry.balanceAfter}"': 'stringResource(R.string.str_balance_amt, entry.balanceAfter)',
    
    'Phone: ${currentBusiness!!.phone}': 'stringResource(R.string.str_phone_val, currentBusiness!!.phone)',
    'GSTIN: ${currentBusiness!!.gstin}': 'stringResource(R.string.str_gstin_val, currentBusiness!!.gstin)',
    
    '"${product.currentStock} in stock"': 'stringResource(R.string.str_in_stock, product.currentStock)',
    '"Current Stock: ${product.currentStock}"': 'stringResource(R.string.str_current_stock_val, product.currentStock)',
    '"Min Level: ${product.minStock}"': 'stringResource(R.string.str_min_level, product.minStock)',
    
    '"Stock: ${p.currentStock}"': 'stringResource(R.string.str_stock_val, p.currentStock)',
    
    '"OFFLINE - Pending Sync"': 'stringResource(R.string.str_offline_sync)',
    '"DEMO MODE"': 'stringResource(R.string.str_demo_mode)',
}

for root, _, files in os.walk('app/src/main/java/com/example'):
    for file in files:
        if file.endswith('.kt') and file != 'PdfGenerator.kt': # skip pdfgenerator for a sec
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            changed = False
            for k, v in replacements.items():
                if k in content:
                    content = content.replace(k, v)
                    changed = True
            
            # also handle R.string import if missing and changed
            if changed and 'import androidx.compose.ui.res.stringResource' not in content:
                content = content.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport androidx.compose.ui.res.stringResource\nimport com.example.R')
                
            if changed:
                with open(path, 'w') as f:
                    f.write(content)

