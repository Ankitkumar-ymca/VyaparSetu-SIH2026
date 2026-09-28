import re

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

replacements = {
    '"Business is healthy."': 'stringResource(R.string.str_business_is_healthy)',
    '"Add products and record sales to build your health score."': 'stringResource(R.string.str_add_products_health)',
    '"High outstanding payments"': 'stringResource(R.string.str_high_outstanding)',
    '"$lowStockCount items low on stock"': 'stringResource(R.string.str_items_low_stock, lowStockCount)',
    '"Operating at a loss"': 'stringResource(R.string.str_operating_loss)',
    'reasons.joinToString(", ") + " need attention."': 'stringResource(R.string.str_need_attention, reasons.joinToString(", "))',
    '"Excellent"': 'stringResource(R.string.str_health_excellent)',
    '"Good"': 'stringResource(R.string.str_health_good)',
    '"Needs Attention"': 'stringResource(R.string.str_health_needs_attention)',
    '"Critical"': 'stringResource(R.string.str_health_critical)',
    '"Not enough business data yet. Keep recording sales and expenses to unlock AI insights."': 'stringResource(R.string.str_not_enough_data)',
    '"Ask VyaparSetu AI"': 'stringResource(R.string.str_ask_vyaparsetu)',
    '"Products"': 'stringResource(R.string.str_products)',
    '"Customers"': 'stringResource(R.string.str_customers)',
    '"Suppliers"': 'stringResource(R.string.str_suppliers)',
    '"Inventory Alert"': 'stringResource(R.string.str_inventory_alert)',
    '"$lowStockCount products are low on stock."': 'stringResource(R.string.str_products_low_stock, lowStockCount)',
    '"Inventory"': 'stringResource(R.string.str_inventory)',
    '"Add your first product to start tracking inventory."': 'stringResource(R.string.str_add_first_product)',
    '"Khata Alert"': 'stringResource(R.string.str_khata_alert)',
    '"$outstandingCount customers have outstanding payments."': 'stringResource(R.string.str_customers_outstanding, outstandingCount)',
    '"Khata / Outstanding"': 'stringResource(R.string.str_khata_outstanding)',
    '"Customer outstanding payments will appear here."': 'stringResource(R.string.str_customer_outstanding_appear)',
    '"Khata"': 'stringResource(R.string.str_khata)',
    '"No outstanding payments."': 'stringResource(R.string.str_no_outstanding)',
    '"Transactions"': 'stringResource(R.string.str_transactions)',
    '"No recent transactions. Start billing to see them here."': 'stringResource(R.string.str_no_recent_tx)',
    'Triple("SALE", "Sale to $cName"': 'Triple(stringResource(R.string.str_tx_sale), stringResource(R.string.str_sale_to, cName)',
    'Triple("PURCHASE", "Purchase from $sName"': 'Triple(stringResource(R.string.str_tx_purchase), stringResource(R.string.str_purchase_from, sName)',
    'Triple("EXPENSE"': 'Triple(stringResource(R.string.str_tx_expense)',
    'Triple(if (entry.type == "OPENING_BALANCE") "BALANCE" else "PAYMENT"': 'Triple(if (entry.type == "OPENING_BALANCE") stringResource(R.string.str_tx_balance) else stringResource(R.string.str_tx_payment)',
    '"User"': 'stringResource(R.string.str_user)',
    '"No Business Setup"': 'stringResource(R.string.str_no_business_setup)',
    'text = if (isHindi) "Switch to English" else "हिंदी में बदलें (Hindi)"': 'text = if (isHindi) stringResource(R.string.str_switch_english) else stringResource(R.string.str_switch_hindi)',
    'text = if (isOnline) "Simulate Offline Mode" else "Simulate Online & Sync"': 'text = if (isOnline) stringResource(R.string.str_simulate_offline) else stringResource(R.string.str_simulate_online)',
    'text = if (isDemoMode) "Exit SIH Demo Mode" else "Enter SIH Demo Mode"': 'text = if (isDemoMode) stringResource(R.string.str_exit_demo) else stringResource(R.string.str_enter_demo)',
    '"Good Morning"': 'stringResource(R.string.str_good_morning)',
    '"Good Afternoon"': 'stringResource(R.string.str_good_afternoon)',
    '"Good Evening"': 'stringResource(R.string.str_good_evening)',
    '"Good Night"': 'stringResource(R.string.str_good_night)',
    '?: "Cash"': '?: stringResource(R.string.str_cash)',
    '?: "Unknown"': '?: stringResource(R.string.str_unknown)',
    'contentDescription = "Add Product"': 'contentDescription = stringResource(R.string.str_add_products)',
    'contentDescription = "Add Expense"': 'contentDescription = stringResource(R.string.str_add_expense_btn)',
    'contentDescription = if (khataSelectedTab == 0) "Add Customer" else "Add Supplier"': 'contentDescription = if (khataSelectedTab == 0) stringResource(R.string.str_add_customer) else stringResource(R.string.str_add_supplier)'
}

for k, v in replacements.items():
    content = content.replace(k, v)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

