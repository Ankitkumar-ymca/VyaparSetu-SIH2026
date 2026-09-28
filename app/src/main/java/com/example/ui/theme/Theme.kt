package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = BlueNavy,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = BlueNavy,
    secondary = BlueDark,
    onSecondary = Color.White,
    secondaryContainer = BlueSurface,
    onSecondaryContainer = BlueNavy,
    background = BlueBackground,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = Color.White
  )

private val DarkColorScheme = LightColorScheme // MVP: only light mode supported for this theme initially

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force light theme for this clean minimal design
  dynamicColor: Boolean = false, // Disable dynamic color to enforce branding
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
