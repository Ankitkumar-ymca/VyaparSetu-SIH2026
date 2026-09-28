package com.example.presentation.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.presentation.sales.InvoiceRow
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.presentation.utils.PdfGenerator
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    purchaseId: String,
    onNavigateBack: () -> Unit
) {
    val allPurchases by MockDataStore.purchases.collectAsState()
    val allSuppliers by MockDataStore.suppliers.collectAsState()
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    
    val purchase = allPurchases.find { it.id == purchaseId }
    val supplier = allSuppliers.find { it.id == purchase?.supplierId }

    if (purchase == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.str_purchase_not_found))
        }
        return
    }

    
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && purchase != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    PdfGenerator.generatePurchasePdf(context, purchase, supplier, outputStream)
                    Toast.makeText(context, "PDF Saved Successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_purchase_details), fontWeight = FontWeight.Bold, color = BlueNavy) },
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
                    onClick = { launcher.launch("Purchase_${purchase?.referenceNumber ?: "Invoice"}.pdf") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNavy),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BlueNavy)
                ) {
                    Text(stringResource(R.string.str_download_pdf), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.str_purchase_record), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                    InvoiceRow("Ref No", purchase.referenceNumber)
                    InvoiceRow("Date", dateFormat.format(Date(purchase.createdAt)))
                    InvoiceRow("Status", purchase.paymentStatus)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_supplier_1), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(supplier?.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueNavy)
                    if (supplier != null) {
                        Text(supplier.phone, fontSize = 14.sp, color = TextSecondary)
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
                    
                    purchase.items.forEach { item ->
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
                    
                    InvoiceRow("Subtotal", "₹%.2f".format(purchase.subtotal))
                    if (purchase.discount > 0) {
                        InvoiceRow("Discount", "-₹%.2f".format(purchase.discount), AccentRed)
                    }
                    if (purchase.tax > 0) {
                        InvoiceRow("Tax", "₹%.2f".format(purchase.tax))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.str_grand_total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                        Text(stringResource(R.string.str_2f).format(purchase.grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InvoiceRow("Amount Paid", "₹%.2f".format(purchase.amountPaid), AccentGreen)
                    if (purchase.outstanding > 0) {
                        InvoiceRow("Balance Due", "₹%.2f".format(purchase.outstanding), AccentRed)
                    }
                }
            }
        }
    }
}
