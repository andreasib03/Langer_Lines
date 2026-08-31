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

    // Indicatori tipo pelle (onboarding/profilo) — sfondo chip + colore icona
    val skinTypeDryBg:     Color,
    val skinTypeDryIcon:   Color,
    val skinTypeOilyBg:    Color,
    val skinTypeOilyIcon:  Color,
    val skinTypeMixedBg:   Color,
    val skinTypeMixedIcon: Color,
    val skinTypeNormalBg:  Color,
    val skinTypeNormalIcon:Color,
    val skinTypeDefaultBg:  Color,
    val skinTypeDefaultIcon:Color,
)

val LightAppColors = AppColors(
    qualityHigh   = SuccessLight,
    qualityMedium = WarningLight,
    qualityLow    = errorLight,
    syncPending   = WarningLight,
    syncDone      = SuccessLight,
    gradientStart = Skin200,
    gradientEnd   = Skin500,
    starColor     = WarningLight,
    swipeDeleteBg   = errorLight.copy(alpha = 0.8f),
    swipeDeleteIcon = Neutral0,
    skinTypeDryBg      = SkinTypeDryBgLight,
    skinTypeDryIcon    = SkinTypeDryIconLight,
    skinTypeOilyBg     = SkinTypeOilyBgLight,
    skinTypeOilyIcon   = SkinTypeOilyIconLight,
    skinTypeMixedBg    = SkinTypeMixedBgLight,
    skinTypeMixedIcon  = SkinTypeMixedIconLight,
    skinTypeNormalBg   = SkinTypeNormalBgLight,
    skinTypeNormalIcon = SkinTypeNormalIconLight,
    skinTypeDefaultBg   = SkinTypeDefaultBgLight,
    skinTypeDefaultIcon = SkinTypeDefaultIconLight,
)

val DarkAppColors = AppColors(
    qualityHigh   = SuccessDark,
    qualityMedium = WarningDark,
    qualityLow    = errorDark,
    syncPending   = WarningDark,
    syncDone      = SuccessDark,
    gradientStart = Skin600,
    gradientEnd   = Skin700,
    starColor     = WarningDark,
    swipeDeleteBg   = errorDark.copy(alpha = 0.8f),
    swipeDeleteIcon = onErrorDark,
    skinTypeDryBg      = SkinTypeDryBgDark,
    skinTypeDryIcon    = SkinTypeDryIconDark,
    skinTypeOilyBg     = SkinTypeOilyBgDark,
    skinTypeOilyIcon   = SkinTypeOilyIconDark,
    skinTypeMixedBg    = SkinTypeMixedBgDark,
    skinTypeMixedIcon  = SkinTypeMixedIconDark,
    skinTypeNormalBg   = SkinTypeNormalBgDark,
    skinTypeNormalIcon = SkinTypeNormalIconDark,
    skinTypeDefaultBg   = Dark30,
    skinTypeDefaultIcon = Neutral50,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }