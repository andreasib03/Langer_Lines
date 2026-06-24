package com.example.linee_langer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = Skin500,
    onPrimary          = Neutral0,
    primaryContainer   = Skin100,
    onPrimaryContainer = Skin700,
    secondary          = Skin300,
    onSecondary        = Neutral95,
    secondaryContainer = Skin200,
    onSecondaryContainer = Skin700,
    tertiary           = Skin400,
    onTertiary         = Neutral95,
    tertiaryContainer  = Lavanda,
    onTertiaryContainer = Neutral95,
    background         = Neutral5,
    onBackground       = Neutral95,
    surface            = Neutral0,
    onSurface          = Neutral95,
    surfaceVariant     = Neutral10,
    onSurfaceVariant   = Neutral70,
    outline            = Neutral30,
    outlineVariant     = Neutral20,
    error              = ErrorLight,
    onError            = Neutral0,
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary            = Skin300,
    onPrimary          = Neutral95,
    primaryContainer   = Skin700,
    onPrimaryContainer = Skin100,
    secondary          = Skin400,
    onSecondary        = Neutral95,
    secondaryContainer = Skin600,
    onSecondaryContainer = Skin100,
    tertiary           = Skin300,
    onTertiary         = Neutral95,
    tertiaryContainer  = Color(0xFF4A1060),
    onTertiaryContainer = Color(0xFFF9D8FF),
    background         = Dark10,
    onBackground       = Neutral10,
    surface            = Dark20,
    onSurface          = Neutral10,
    surfaceVariant     = Dark30,
    onSurfaceVariant   = Neutral50,
    outline            = Dark50,
    outlineVariant     = Dark40,
    error              = ErrorDark,
    onError            = Neutral95,
    errorContainer     = Color(0xFF93000A),
    onErrorContainer   = Color(0xFFFFDAD6),
)

@Composable
fun LangerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors   = if (darkTheme) DarkAppColors   else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            shapes      = AppShapes,
            content     = content,
        )
    }
}

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current