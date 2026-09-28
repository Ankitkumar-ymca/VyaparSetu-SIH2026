package com.example.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.MockDataStore
import com.example.domain.model.BusinessForecast
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import com.example.data.repository.AiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessForecastScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val forecast = MockDataStore.getBusinessForecast()
    
    var aiAdvice by remember { mutableStateOf<String?>(null) }
    var isGeneratingAdvice by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_business_forecast), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.str_empty_1))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueBackground)
            )
        },
        containerColor = BlueBackground
    ) { innerPadding ->
        if (forecast == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.str_not_enough_data), color = TextSecondary)
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Overall Risk Card
            val riskColor = when (forecast.overallRisk) {
                "Low" -> AccentGreen
                "Moderate" -> AccentOrange
                else -> AccentRed
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.str_overall_risk), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                        Text(getLocalizedRisk(forecast.overallRisk), fontWeight = FontWeight.Bold, color = riskColor, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.str_confidence)}: ${getLocalizedConfidence(forecast.confidence)}", fontSize = 12.sp, color = TextSecondary)
                }
            }
            
            // Details
            ForecastItemCard(stringResource(R.string.str_cash_flow), forecast.cashFlowRisk, forecast.cashFlowMessage)
            ForecastItemCard(stringResource(R.string.str_sales_trend), forecast.salesTrend, forecast.salesMessage, isTrend = true)
            ForecastItemCard(stringResource(R.string.str_expense_trend), forecast.expenseTrend, forecast.expenseMessage, isTrend = true)
            ForecastItemCard(stringResource(R.string.str_inventory_risk), forecast.inventoryRisk, forecast.inventoryMessage)
            ForecastItemCard(stringResource(R.string.str_outstanding_risk), forecast.outstandingRisk, forecast.outstandingMessage)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // What should I do?
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BlueLight.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.str_what_should_i_do), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlueNavy)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isGeneratingAdvice) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueAccent, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.str_generating_advice), fontSize = 12.sp, color = TextSecondary)
                        }
                    } else if (aiAdvice != null) {
                        Text(aiAdvice!!, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                    } else {
                        Button(
                            onClick = {
                                isGeneratingAdvice = true
                                coroutineScope.launch {
                                    val result = AiService.generateForecastAdvice(forecast)
                                    aiAdvice = result.getOrNull() ?: "Unable to generate advice at this time."
                                    isGeneratingAdvice = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.str_get_ai_advice))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun getLocalizedRisk(risk: String): String {
    return when(risk) {
        "Low" -> stringResource(R.string.str_low)
        "Moderate" -> stringResource(R.string.str_moderate)
        "High" -> stringResource(R.string.str_high)
        else -> risk
    }
}

@Composable
fun getLocalizedConfidence(conf: String): String {
    return when(conf) {
        "Low" -> stringResource(R.string.str_low)
        "Medium" -> stringResource(R.string.str_medium)
        "High" -> stringResource(R.string.str_high)
        else -> conf
    }
}

@Composable
fun ForecastItemCard(title: String, riskOrTrend: String, message: String, isTrend: Boolean = false) {
    val color = if (isTrend) {
        when(riskOrTrend) {
            "Increasing" -> AccentGreen
            "Decreasing" -> AccentRed
            "Stable" -> BlueAccent
            else -> TextSecondary
        }
    } else {
        when(riskOrTrend) {
            "Low" -> AccentGreen
            "Moderate" -> AccentOrange
            "High" -> AccentRed
            else -> TextSecondary
        }
    }
    
    val displayValue = if (isTrend) {
        when(riskOrTrend) {
            "Increasing" -> stringResource(R.string.str_increasing)
            "Decreasing" -> stringResource(R.string.str_decreasing)
            "Stable" -> stringResource(R.string.str_stable)
            else -> riskOrTrend
        }
    } else {
        getLocalizedRisk(riskOrTrend)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueNavy)
                Text(displayValue, fontWeight = FontWeight.SemiBold, color = color, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        }
    }
}
