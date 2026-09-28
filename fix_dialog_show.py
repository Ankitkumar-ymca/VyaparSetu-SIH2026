import re

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'r') as f:
    text = f.read()

old_code = """    if (showPaymentDialog) {
        PaymentDialog(customer = customer, onDismiss = { showPaymentDialog = false })
    }
}"""

new_code = """    if (showPaymentDialog) {
        PaymentDialog(customer = customer, onDismiss = { showPaymentDialog = false })
    }
    if (showPreviousOutstandingDialog) {
        PreviousOutstandingDialog(customer = customer, onDismiss = { showPreviousOutstandingDialog = false })
    }
}"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/presentation/khata/KhataScreens.kt', 'w') as f:
    f.write(text)
