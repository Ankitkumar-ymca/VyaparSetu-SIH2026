package com.example.presentation.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.domain.model.Purchase
import com.example.presentation.sales.ContainerBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PurchasesListScreen(
    onNavigateToPurchaseDetail: (String) -> Unit,
    onNavigateToNewPurchase: () -> Unit
) {
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allPurchases by MockDataStore.purchases.collectAsState()
    val myPurchases = allPurchases.filter { it.businessId == currentBusiness?.id }.sortedByDescending { it.createdAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.str_purchases_history),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueNavy
                )
                Text(
                    text = stringResource(R.string.str_manage_your_supplier_purchases),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Button(
                onClick = onNavigateToNewPurchase,
                colors = ButtonDefaults.buttonColors(containerColor = BlueNavy)
            ) {
                Text(stringResource(R.string.str_new_purchase_1))
            }
        }

        if (myPurchases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.str_no_purchases_yet_1),
                        color = BlueNavy,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.str_your_purchase_history_will_app),
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(myPurchases) { purchase ->
                    PurchaseListItem(
                        purchase = purchase,
                        onClick = { onNavigateToPurchaseDetail(purchase.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun PurchaseListItem(purchase: Purchase, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(purchase.createdAt))
    val isPaid = purchase.paymentStatus == "PAID"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = purchase.referenceNumber,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BlueNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateString,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.str_2f).format(purchase.grandTotal),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            ContainerBadge(
                text = purchase.paymentStatus,
                backgroundColor = if (isPaid) AccentGreen.copy(alpha = 0.1f) else if (purchase.paymentStatus == "CREDIT") AccentRed.copy(alpha = 0.1f) else AccentOrange.copy(alpha = 0.1f),
                textColor = if (isPaid) AccentGreen else if (purchase.paymentStatus == "CREDIT") AccentRed else AccentOrange
            )
        }
    }
}
