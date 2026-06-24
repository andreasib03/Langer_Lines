// ui/theme/AppColors.kt
package com.example.linee_langer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colori semantici dell'app che estendono MaterialTheme.colorScheme.
 * Usati per concetti non coperti da M3: qualità analisi, overlay camera,
 * sync badge, stelle rating, swipe actions, linee Langer.
 *
 * Accesso: MaterialTheme.appColors.qualityHigh
 */
@Immutable
data class AppColors(
    // Qualità rilevamento analisi
    val qualityHigh:   Color,
    val qualityMedium: Color,
    val qualityLow:    Color,

    // Overlay camera (valori fissi — non cambiano con il tema)
    val cameraOverlayBg:          Color = CameraOverlayBg,
    val cameraOverlayBgMedium:    Color = CameraOverlayBgMedium,
    val cameraOverlayBgDark:      Color = CameraOverlayBgDark,
    val cameraOverlayBorder:      Color = CameraOverlayBorder,
    val cameraOverlayText:        Color = CameraOverlayText,
    val cameraOverlayTextMuted:   Color = CameraOverlayTextMuted,
    val cameraOverlayTextStrong:  Color = CameraOverlayTextStrong,
    val cameraOverlayError:       Color = CameraOverlayError,

    // Linee Langer (ciano fisso — colore scientifico)
    val langerLineColor: Color = LangerLineColor,

    // Stato sincronizzazione
    val syncPending: Color,
    val syncDone:    Color,

    // Gradient tone di pelle (header profilo)
    val gradientStart: Color,
    val gradientEnd:   Color,

    // Stelle rating
    val starColor: Color,

    // Swipe-to-delete action
    val swipeDeleteBg:   Color,
    val swipeDeleteIcon: Color,
)

val LightAppColors = AppColors(
    qualityHigh   = SuccessLight,
    qualityMedium = WarningLight,
    qualityLow    = ErrorLight,
    syncPending   = WarningLight,
    syncDone      = SuccessLight,
    gradientStart = Skin200,
    gradientEnd   = Skin500,
    starColor     = WarningLight,
    swipeDeleteBg   = ErrorLight.copy(alpha = 0.8f),
    swipeDeleteIcon = Neutral0,
)

val DarkAppColors = AppColors(
    qualityHigh   = SuccessDark,
    qualityMedium = WarningDark,
    qualityLow    = ErrorDark,
    syncPending   = WarningDark,
    syncDone      = SuccessDark,
    gradientStart = Skin600,
    gradientEnd   = Skin700,
    starColor     = WarningDark,
    swipeDeleteBg   = ErrorDark.copy(alpha = 0.8f),
    swipeDeleteIcon = Neutral95,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }