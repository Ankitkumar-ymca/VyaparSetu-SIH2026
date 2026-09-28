

package com.example

import androidx.compose.ui.res.stringResource
import com.example.R

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MockDataStore
import com.example.presentation.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

const val ROUTE_AI_ASSISTANT = "ai_assistant"

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    MockDataStore.init(applicationContext)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavigation()
            GlobalBanners(modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp))
        }
      }
    }
  }
}

@Composable
fun GlobalBanners(modifier: Modifier = Modifier) {
    val isDemoMode by MockDataStore.isDemoMode.collectAsState()
    val isOnline by MockDataStore.isOnline.collectAsState()
    
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isDemoMode) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFF9800)).padding(4.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.str_demo_mode), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        if (!isOnline) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE53935)).padding(4.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.str_offline_sync), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

