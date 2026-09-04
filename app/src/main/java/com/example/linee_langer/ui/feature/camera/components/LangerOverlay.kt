package com.example.linee_langer.ui.feature.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.example.linee_langer.domain.models.LangerLine
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import com.example.linee_langer.ui.theme.CameraOverlayText
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.LangerLineColor

@Composable
fun LangerOverlay(
    lines: List<LangerLine>,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
){


    val drawingProcess = remember { Animatable(0f) }

    LaunchedEffect(isVisible, lines) {
        if(isVisible && lines.isNotEmpty()){
            drawingProcess.animateTo(
                targetValue = 1f,
                animationSpec = tween (
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        } else if (!isVisible){
            drawingProcess.snapTo(0f)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        lines.forEach { line ->
            val start = Offset(
                (line.startX * canvasWidth),
                (line.startY * canvasHeight)
            )
            val fullEnd = Offset(
                (line.endX * canvasWidth),
                (line.endY * canvasHeight)
            )

            val currentEnd = Offset(
                x = start.x + (fullEnd.x - start.x) * drawingProcess.value,
                y = start.y + (fullEnd.y - start.y) * drawingProcess.value
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        LangerLineColor.copy(alpha = line.intensity),
                        CameraOverlayText
                    ),
                    start = start,
                    end = currentEnd
                ),
                start = start,
                end = currentEnd,
                strokeWidth = Dimens.BorderStandard.toPx(),
                cap = StrokeCap.Round,
            )

            if(drawingProcess.value > 0.5f){
                drawCircle(
                    color = CameraOverlayText,
                    radius = Dimens.CardElevation.toPx(),
                    center = currentEnd
                )
            }
        }
    }
}