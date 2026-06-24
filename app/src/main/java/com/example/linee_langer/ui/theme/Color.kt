package com.example.linee_langer.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Skin Palette ──────────────────────────────────────────────────────
val Skin100 = Color(0xFFFFF3E0)
val Skin200 = Color(0xFFFFE0B2)
val Skin300 = Color(0xFFFFCC80)
val Skin400 = Color(0xFFFFB74D)
val Skin500 = Color(0xFFF9A825)
val Skin600 = Color(0xFFF57F17)
val Skin700 = Color(0xFFE65100)

val Lavanda = Color(0xFFF3E5F5)

// ─── Neutrali ────────────────────────────────────────────────────────────────
val Neutral0   = Color(0xFFFFFFFF)
val Neutral5   = Color(0xFFF8F9FA)
val Neutral10  = Color(0xFFF1F3F5)
val Neutral20  = Color(0xFFE9ECEF)
val Neutral30  = Color(0xFFDEE2E6)
val Neutral50  = Color(0xFFADB5BD)
val Neutral70  = Color(0xFF6C757D)
val Neutral90  = Color(0xFF343A40)
val Neutral95  = Color(0xFF212529)
val Neutral100 = Color(0xFF000000)

// ─── Dark surfaces ───────────────────────────────────────────────────────────
val Dark10 = Color(0xFF121212)
val Dark20 = Color(0xFF1E1E1E)
val Dark30 = Color(0xFF2C2C2C)
val Dark40 = Color(0xFF3A3A3A)
val Dark50 = Color(0xFF4A4A4A)

// ─── Semantici fissi ─────────────────────────────────────────────────────────
val ErrorLight   = Color(0xFFB00020)
val ErrorDark    = Color(0xFFCF6679)
val SuccessLight = Color(0xFF4CAF50)
val SuccessDark  = Color(0xFF81C784)
val WarningLight = Color(0xFFFFC107)
val WarningDark  = Color(0xFFFFD54F)

// ─── Camera overlay (sempre scuri — invarianti al tema) ──────────────────────
// La camera ha sempre uno sfondo nero fisso indipendentemente dal tema
val CameraOverlayBg         = Color(0xCC000000)  // 80% black — bg principale
val CameraOverlayBgMedium   = Color(0x80000000)  // 50% black — bg secondario
val CameraOverlayBgDark     = Color(0x99000000)  // 60% black — bg etichette
val CameraOverlayBorder     = Color(0x4DFFFFFF)  // 30% white — bordi sottili
val CameraOverlayText       = Color(0xB3FFFFFF)  // 70% white — testo standard
val CameraOverlayTextMuted  = Color(0x80FFFFFF)  // 50% white — testo secondario
val CameraOverlayTextStrong = Color(0xCCFFFFFF)  // 80% white — testo enfatizzato
val CameraOverlayError      = Color(0xFFB71C1C)  // rosso errore overlay

// ─── Langer line (ciano fisso — colore scientifico delle linee rilevate) ─────
val LangerLineColor = Color(0xFF00E5FF)