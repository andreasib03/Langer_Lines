package com.example.linee_langer.ui.interfacesUser

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandSecondary,
    background = AppBackground,
    surface = AppSurface,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = Color.White,
    surfaceVariant = Color(0xEFEFF4FF) // Aggiunto per dare consistenza ai componenti Glass
)

// You can define a Dark Mode palette here too!
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F)
)

@Composable
fun MyAppTheme(
    darkTheme: Boolean = false, // You can link this to system settings
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography, // Centralized font styles
        content = content
    )
}