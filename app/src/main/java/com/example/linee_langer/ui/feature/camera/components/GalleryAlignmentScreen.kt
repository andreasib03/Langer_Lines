package com.example.linee_langer.ui.feature.camera.components

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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.linee_langer.R
import com.example.linee_langer.ui.feature.camera.ActionButton
import com.example.linee_langer.ui.theme.CameraOverlayBg
import com.example.linee_langer.ui.theme.CameraOverlayText
import com.example.linee_langer.ui.theme.CameraOverlayError
import com.example.linee_langer.ui.theme.CameraOverlayBorder
import com.example.linee_langer.ui.theme.Dimens

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

    val state = rememberTransformableState { _, zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        rotation += rotationChange

        val maxOffset = (scale - 1f) * 500f

        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-maxOffset, maxOffset),
            y = (offset.y + panChange.y).coerceIn(-maxOffset, maxOffset)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(CameraOverlayBg)) {
        // Area di visualizzazione Immagine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Dimens.ButtonWidth)
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
                .padding(bottom = Dimens.Guide)
                .size(Dimens.SuperSuperHuge)
                .border(Dimens.CardElevation, CameraOverlayBorder.copy(alpha = 0.2f), CircleShape)
        )

        // 3. ISTRUZIONI
        Text(
            text = stringResource(R.string.camera_istructions),
            color = CameraOverlayText.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Dimens.TopHuge),
            textAlign = TextAlign.Center
        )

        // Bottoni di controllo
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(Dimens.XXLarge),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {

            ActionButton(
                label = stringResource(R.string.email_undo),
                icon = R.drawable.ic_close,
                color = CameraOverlayError.copy(alpha = 0.2f),
                onClick = onCancel
            )

            // Pulsante Analizza
            ActionButton(
                label = stringResource(R.string.analyze_now),
                icon = R.drawable.ic_check,
                color = MaterialTheme.colorScheme.primary, // O il colore verde che preferisci
                onClick = { onConfirm(scale, offset, rotation) }
            )
        }
    }
}