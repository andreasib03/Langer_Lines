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
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF2C2C2C)
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