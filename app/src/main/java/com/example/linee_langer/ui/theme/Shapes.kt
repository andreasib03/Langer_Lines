package com.example.linee_langer.ui.theme


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    // Chip, badge, snackbar
    extraSmall = RoundedCornerShape(4.dp),
    // Card compatte, text field
    small      = RoundedCornerShape(Dimens.Small),
    // Card standard
    medium     = RoundedCornerShape(16.dp),
    // Card prominenti, bottom sheet
    large      = RoundedCornerShape(Dimens.XLarge),
    // Dialog, modal full
    extraLarge = RoundedCornerShape(32.dp),
)

// Shortcut per forme usate frequentemente nell'app
val CircleShape = RoundedCornerShape(50)