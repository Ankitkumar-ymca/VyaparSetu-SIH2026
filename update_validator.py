import re

with open('app/src/main/java/com/example/domain/model/SmartActionValidator.kt', 'r') as f:
    content = f.read()

new_content = """package com.example.domain.model

import android.content.Context
import com.example.data.repository.MockDataStore
import kotlinx.coroutines.flow.first
import com.example.aistudio.R

object SmartActionValidator {

    suspend fun validateAction(action: ProposedAction, context: Context): ValidationResult {
        val businessId = MockDataStore.currentBusiness.value?.id ?: return ValidationResult.Failure(context.getString(R.string.err_no_business))
        
        when (action.actionType) {
            "CREATE_SALE", "CREATE_CREDIT_SALE" -> {
                val qty = action.quantity ?: 1
                val amount = action.amount
                val custName = action.customerName
                val prodName = action.productName

                if (custName.isNullOrBlank()) return ValidationResult.Failure(context.getString(R.string.err_specify_customer))
                if (prodName.isNullOrBlank()) return ValidationResult.Failure(context.getString(R.string.err_specify_product))
                if (amount == null || amount <= 0) return ValidationResult.Failure(context.getString(R.string.err_specify_sale_amount))
                
                val products = MockDataStore.products.first()
                val matchedProduct = products.find { it.name.equals(prodName, ignoreCase = true) }
                if (matchedProduct == null) {
                    return ValidationResult.Failure(context.getString(R.string.err_product_not_found, prodName))
                }
                
                if (matchedProduct.currentStock < qty) {
                    return ValidationResult.Failure(context.getString(R.string.err_insufficient_stock, matchedProduct.currentStock, matchedProduct.name))
                }

                val customers = MockDataStore.customers.first()
                val matchedCustomer = customers.find { it.name.equals(custName, ignoreCase = true) }
                if (matchedCustomer == null) {
                    return ValidationResult.Failure(context.getString(R.string.err_customer_not_found, custName))
                }

                val paymentMethod = if (action.actionType == "CREATE_CREDIT_SALE") "CREDIT" else (action.paymentMethod ?: "CASH")
                val paymentMethodDisplay = if (paymentMethod == "CREDIT") context.getString(R.string.lbl_credit) else paymentMethod
                
                val title = if(action.actionType == "CREATE_CREDIT_SALE") context.getString(R.string.title_credit_sale) else context.getString(R.string.title_sale)
                val customerLbl = context.getString(R.string.lbl_customer)
                val productLbl = context.getString(R.string.lbl_product)
                val qtyLbl = context.getString(R.string.lbl_quantity)
                val totalLbl = context.getString(R.string.lbl_total)
                val payLbl = context.getString(R.string.lbl_payment)
                val invLbl = context.getString(R.string.msg_inventory_deduct, qty)
                val outLbl = if(paymentMethod == "CREDIT") "\n" + context.getString(R.string.msg_customer_outstanding, amount.toString()) else ""

                val preview = \"\"\"
                    $title
                    $customerLbl ${matchedCustomer.name}
                    $productLbl ${matchedProduct.name}
                    $qtyLbl $qty
                    $totalLbl ₹$amount
                    $payLbl $paymentMethodDisplay
                    
                    $invLbl$outLbl
                \"\"\".trimIndent()
                
                return ValidationResult.Success(preview, action.copy(actionType = action.actionType, customerName = matchedCustomer.id, productName = matchedProduct.id, paymentMethod = paymentMethod))
            }
            "CREATE_EXPENSE" -> {
                val amount = action.amount
                val category = action.expenseCategory ?: "Other"
                if (amount == null || amount <= 0) return ValidationResult.Failure(context.getString(R.string.err_specify_expense_amount))
                
                val title = context.getString(R.string.title_expense)
                val catLbl = context.getString(R.string.lbl_category)
                val totalLbl = context.getString(R.string.lbl_total)

                val preview = \"\"\"
                    $title
                    $catLbl $category
                    $totalLbl ₹$amount
                \"\"\".trimIndent()
                
                return ValidationResult.Success(preview, action)
            }
            "RECORD_PAYMENT" -> {
                val amount = action.amount
                val custName = action.customerName
                if (amount == null || amount <= 0) return ValidationResult.Failure(context.getString(R.string.err_specify_payment_amount))
                if (custName.isNullOrBlank()) return ValidationResult.Failure(context.getString(R.string.err_specify_customer))
                
                val customers = MockDataStore.customers.first()
                val matchedCustomer = customers.find { it.name.equals(custName, ignoreCase = true) }
                if (matchedCustomer == null) {
                    return ValidationResult.Failure(context.getString(R.string.err_customer_not_found, custName))
                }
                
                val title = context.getString(R.string.title_record_payment)
                val customerLbl = context.getString(R.string.lbl_customer)
                val amountLbl = context.getString(R.string.lbl_amount)

                val preview = \"\"\"
                    $title
                    $customerLbl ${matchedCustomer.name}
                    $amountLbl ₹$amount
                \"\"\".trimIndent()
                
                return ValidationResult.Success(preview, action.copy(customerName = matchedCustomer.id))
            }
            "CREATE_PURCHASE" -> {
                val qty = action.quantity ?: 1
                val amount = action.amount
                val suppName = action.supplierName
                val prodName = action.productName
                
                if (suppName.isNullOrBlank()) return ValidationResult.Failure(context.getString(R.string.err_specify_supplier))
                if (prodName.isNullOrBlank()) return ValidationResult.Failure(context.getString(R.string.err_specify_product))
                if (amount == null || amount <= 0) return ValidationResult.Failure(context.getString(R.string.err_specify_purchase_amount))
                
                val products = MockDataStore.products.first()
                val matchedProduct = products.find { it.name.equals(prodName, ignoreCase = true) }
                if (matchedProduct == null) {
                    return ValidationResult.Failure(context.getString(R.string.err_product_not_found, prodName))
                }
                
                val suppliers = MockDataStore.suppliers.first()
                val matchedSupplier = suppliers.find { it.name.equals(suppName, ignoreCase = true) }
                if (matchedSupplier == null) {
                    return ValidationResult.Failure(context.getString(R.string.err_supplier_not_found, suppName))
                }
                
                val title = context.getString(R.string.title_purchase)
                val suppLbl = context.getString(R.string.lbl_supplier)
                val productLbl = context.getString(R.string.lbl_product)
                val qtyLbl = context.getString(R.string.lbl_quantity)
                val totalLbl = context.getString(R.string.lbl_total)
                val invLbl = context.getString(R.string.msg_inventory_add, qty)

                val preview = \"\"\"
                    $title
                    $suppLbl ${matchedSupplier.name}
                    $productLbl ${matchedProduct.name}
                    $qtyLbl $qty
                    $totalLbl ₹$amount
                    
                    $invLbl
                \"\"\".trimIndent()
                
                return ValidationResult.Success(preview, action.copy(supplierName = matchedSupplier.id, productName = matchedProduct.id))
            }
            "ADD_OPENING_BALANCE" -> {
                val amount = action.amount
                val custName = action.customerName
                if (amount == null || amount <= 0) return ValidationResult.Failure(context.getString(R.string.err_specify_opening_amount))
                if (custName.isNullOrBlank()) return ValidationResult.Failure(context.getString(R.string.err_specify_customer))
                
                val customers = MockDataStore.customers.first()
                val matchedCustomer = customers.find { it.name.equals(custName, ignoreCase = true) }
                if (matchedCustomer == null) {
                    return ValidationResult.Failure(context.getString(R.string.err_customer_not_found, custName))
                }
                
                val title = context.getString(R.string.title_opening_balance)
                val customerLbl = context.getString(R.string.lbl_customer)
                val amountLbl = context.getString(R.string.lbl_amount)

                val preview = \"\"\"
                    $title
                    $customerLbl ${matchedCustomer.name}
                    $amountLbl ₹$amount
                \"\"\".trimIndent()
                
                return ValidationResult.Success(preview, action.copy(customerName = matchedCustomer.id))
            }
            else -> return ValidationResult.Failure(context.getString(R.string.err_unknown_action))
        }
    }
}

sealed class ValidationResult {
    data class Success(val previewText: String, val resolvedAction: ProposedAction) : ValidationResult()
    data class Failure(val error: String) : ValidationResult()
}
"""

with open('app/src/main/java/com/example/domain/model/SmartActionValidator.kt', 'w') as f:
    f.write(new_content)
