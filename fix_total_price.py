import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    content = f.read()

# Fix Sale and Purchase subtotal back to subtotal
# But SaleItem/PurchaseItem still use totalPrice
# We can just change `totalPrice = amount` back to `subtotal = amount` in the Sale instantiation.

content = content.replace("items = items, \n                                        totalPrice = amount, \n                                        discount = 0.0, ", "items = items, \n                                        subtotal = amount, \n                                        discount = 0.0, ")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(content)
