package com.example.linee_langer.ui.interfacesUser

// Type.kt
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 1. Define your custom font family (if using custom TTF/OTF files)
// val MyCustomFont = FontFamily(Font(R.font.my_font_regular))

// 2. Create the centralized Typography object
val AppTypography = Typography(

    // Headline Large - Used for Onboarding Titles
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default, // Or your custom font
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // Body Large - Used for standard text and inputs
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // Label Large - Used for "Styled Input" labels
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)