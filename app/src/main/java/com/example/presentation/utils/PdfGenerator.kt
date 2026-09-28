
package com.example.presentation.utils

import com.example.R

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.domain.model.Purchase
import com.example.domain.model.Supplier
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generatePurchasePdf(context: Context, purchase: Purchase, supplier: Supplier?, outputStream: OutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.str_purchase_invoice), 50f, 50f, paint)
        
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(purchase.createdAt))
        
        canvas.drawText(context.getString(R.string.str_ref_no_val, purchase.referenceNumber), 50f, 90f, paint)
        canvas.drawText(context.getString(R.string.str_date_val, dateString), 50f, 110f, paint)
        canvas.drawText("Supplier: ${supplier?.name ?: "Unknown"}", 50f, 130f, paint)
        
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.str_item), 50f, 170f, paint)
        canvas.drawText(context.getString(R.string.str_qty), 300f, 170f, paint)
        canvas.drawText(context.getString(R.string.str_price_val), 400f, 170f, paint)
        canvas.drawText(context.getString(R.string.str_total_val), 500f, 170f, paint)
        
        paint.isFakeBoldText = false
        var y = 200f
        purchase.items.forEach { item ->
            canvas.drawText(item.productName, 50f, y, paint)
            canvas.drawText(item.quantity.toString(), 300f, y, paint)
            canvas.drawText("Rs. ${item.unitPrice}", 400f, y, paint)
            canvas.drawText("Rs. ${item.totalPrice}", 500f, y, paint)
            y += 25f
        }
        
        y += 20f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.str_subtotal_val, purchase.subtotal), 400f, y, paint)
        y += 25f
        if (purchase.discount > 0) {
            canvas.drawText(context.getString(R.string.str_discount_val, purchase.discount), 400f, y, paint)
            y += 25f
        }
        if (purchase.tax > 0) {
            canvas.drawText(context.getString(R.string.str_tax_val, purchase.tax), 400f, y, paint)
            y += 25f
        }
        canvas.drawText(context.getString(R.string.str_grand_total_val, purchase.grandTotal), 400f, y, paint)
        
        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
