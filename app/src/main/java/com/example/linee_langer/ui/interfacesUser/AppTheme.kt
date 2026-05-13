package com.example.linee_langer.ui.interfacesUser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*

@Composable
fun StyledCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp),
        shape = RoundedCornerShape(24.dp), // Extra arrotondato per un look moderno
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Trasparenza per glassmorphism
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), // Bordo sottile stile "Glass"
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                    )
                )
            )
        ) {
            content()
        }
    }
}
