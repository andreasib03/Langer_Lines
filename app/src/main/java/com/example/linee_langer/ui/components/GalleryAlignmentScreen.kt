package com.example.linee_langer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.linee_langer.R
import com.example.linee_langer.ui.screens.ActionButton

@Composable
fun GalleryAlignmentScreen(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (scale: Float, offset: Offset, rotation: Float) -> Unit
) {
    // Stati per le trasformazioni manuali
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }

    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 5f) // Partiamo da 1f per non rimpicciolire troppo
        rotation += rotationChange

        // Calcoliamo i limiti (approssimativi) per evitare di vedere troppo sfondo nero
        // Più zoomi, più puoi spostarti
        val maxOffset = (scale - 1f) * 500f

        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-maxOffset, maxOffset),
            y = (offset.y + panChange.y).coerceIn(-maxOffset, maxOffset)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Area di visualizzazione Immagine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp)
                .transformable(state = state)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        rotationZ = rotation,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Crop
            )
        }

        // 2. OVERLAY GUIDA (Centered Guide)
        // Invece di una icona a tutto schermo, usiamo un cerchio di riferimento centrale
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp)
                .size(280.dp)
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )

        // 3. ISTRUZIONI
        Text(
            text = "Pizzica per zoomare e ruotare\nTrascina per centrare l'area",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Bottoni di controllo
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pulsante Annulla
            ActionButton(
                label = "Annulla",
                icon = R.drawable.ic_close,
                color = Color.White.copy(alpha = 0.2f),
                onClick = onCancel
            )

            // Pulsante Analizza
            ActionButton(
                label = "Analizza",
                icon = R.drawable.ic_check,
                color = MaterialTheme.colorScheme.primary, // O il colore verde che preferisci
                onClick = { onConfirm(scale, offset, rotation) }
            )
        }
    }
}