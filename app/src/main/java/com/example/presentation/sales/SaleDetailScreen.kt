package com.example.presentation.sales

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allSales by MockDataStore.sales.collectAsState()
    val allCustomers by MockDataStore.customers.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val sale = allSales.find { it.id == saleId }
    val customer = allCustomers.find { it.id == sale?.customerId }

    if (sale == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.str_sale_not_found))
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_invoice), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                OutlinedButton(
                    onClick = {
                        val shareText = """
                            Invoice from ${currentBusiness?.name}
                            No: ${sale.invoiceNumber}
                            Date: ${dateFormat.format(Date(sale.createdAt))}
                            Total Amount: ₹${sale.grandTotal}
                            Status: ${sale.paymentStatus}
                            Thank you for your business!
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Invoice")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BlueNavy)
                ) {
                    Text(stringResource(R.string.str_share__print_invoice), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BlueBackground),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentBusiness?.name?.uppercase() ?: "VYAPARSETU",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BlueNavy
                    )
                    if (!currentBusiness?.address.isNullOrBlank()) {
                        Text(currentBusiness!!.address, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                    if (!currentBusiness?.phone.isNullOrBlank()) {
                        Text(stringResource(R.string.str_phone_val, currentBusiness?.phone ?: ""), fontSize = 14.sp, color = TextSecondary)
                    }
                    if (!currentBusiness?.gstin.isNullOrBlank()) {
                        Text(stringResource(R.string.str_gstin_val, currentBusiness?.gstin ?: ""), fontSize = 14.sp, color = TextSecondary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.str_tax_invoice), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    InvoiceRow("Invoice No", sale.invoiceNumber)
                    InvoiceRow("Date", dateFormat.format(Date(sale.createdAt)))
                    InvoiceRow("Status", sale.paymentStatus)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_billed_to), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(customer?.name ?: "Walk-in Customer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                    if (customer != null) {
                        Text(customer.phone, fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }

            // Items
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.str_item), modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.str_qty), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.str_amount), modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    sale.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(item.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("₹${item.unitPrice}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(item.quantity.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, textAlign = TextAlign.Center)
                            Text(stringResource(R.string.str_2f).format(item.totalPrice), modifier = Modifier.weight(1.5f), fontSize = 14.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InvoiceRow("Subtotal", "₹%.2f".format(sale.subtotal))
                    if (sale.discount > 0) {
                        InvoiceRow("Discount", "-₹%.2f".format(sale.discount), AccentRed)
                    }
                    if (sale.tax > 0) {
                        InvoiceRow("Tax", "₹%.2f".format(sale.tax))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.str_grand_total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                        Text(stringResource(R.string.str_2f).format(sale.grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InvoiceRow("Amount Paid", "₹%.2f".format(sale.amountPaid), AccentGreen)
                    if (sale.outstanding > 0) {
                        InvoiceRow("Balance Due", "₹%.2f".format(sale.outstanding), AccentRed)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
