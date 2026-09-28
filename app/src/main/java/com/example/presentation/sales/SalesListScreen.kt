package com.example.presentation.sales

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
import com.example.domain.model.Sale
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesListScreen(
    onNavigateToSaleDetail: (String) -> Unit
) {
    val currentBusiness by MockDataStore.currentBusiness.collectAsState()
    val allSales by MockDataStore.sales.collectAsState()
    val mySales = allSales.filter { it.businessId == currentBusiness?.id }.sortedByDescending { it.createdAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.str_sales_history),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BlueNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.str_view_and_manage_your_recent_sa),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        if (mySales.isEmpty()) {
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
                        text = stringResource(R.string.str_no_sales_yet),
                        color = BlueNavy,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.str_tap_the__button_below_to_creat),
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
                items(mySales) { sale ->
                    SaleListItem(
                        sale = sale,
                        onClick = { onNavigateToSaleDetail(sale.id) }
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
fun SaleListItem(sale: Sale, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(sale.createdAt))
    val isPaid = sale.paymentStatus == "PAID"
    
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
                text = sale.invoiceNumber,
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
                text = stringResource(R.string.str_2f).format(sale.grandTotal),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            ContainerBadge(
                text = sale.paymentStatus,
                backgroundColor = if (isPaid) AccentGreen.copy(alpha = 0.1f) else if (sale.paymentStatus == "CREDIT") AccentRed.copy(alpha = 0.1f) else AccentOrange.copy(alpha = 0.1f),
                textColor = if (isPaid) AccentGreen else if (sale.paymentStatus == "CREDIT") AccentRed else AccentOrange
            )
        }
    }
}

@Composable
fun ContainerBadge(text: String, backgroundColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
