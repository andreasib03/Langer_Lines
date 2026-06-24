package com.example.linee_langer.domain.usecases

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.example.linee_langer.domain.models.LangerLine
import androidx.core.graphics.scale
import com.example.linee_langer.domain.detector.ILangerDetector
import com.example.linee_langer.domain.models.BodyPartIds

class AnalyzeSkinUseCases(private val detector: ILangerDetector) {

    val isDetectorAvailable: Boolean
        get() = detector.isAvailable

    operator fun invoke(imageProxy: ImageProxy, partId: String): List<LangerLine> {

        return imageProxy.use { proxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = proxy.toBitmap()

            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rb =
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle() // Libera subito original
                rb
            } else {
                bitmap
            }

            processAndDetect(rotatedBitmap, partId)
        }
    }

    operator fun invoke(bitmap: Bitmap, partId: String): List<LangerLine> {
        return processAndDetect(bitmap, partId, recycleInput = false)
    }

    private fun processAndDetect(input: Bitmap, partId: String, recycleInput: Boolean = true): List<LangerLine> {
        val maxSide = 720

        // Scalare
        val scaledBitmap = if (input.width > maxSide || input.height > maxSide) {
            val factor = if (input.width > input.height) maxSide.toFloat() / input.width
            else maxSide.toFloat() / input.height
            input.scale((input.width * factor).toInt(), (input.height * factor).toInt())
        } else {
            // Se non scaliamo e recycleInput è true, dobbiamo stare attenti a non riciclare
            // l'input se lo restituiamo direttamente. Creiamo una copia o gestiamo il riferimento.
            input
        }

        // Sensibilità dinamica
        val sensitivity = when(partId.lowercase()) {
            BodyPartIds.FACE -> 0.85f
            BodyPartIds.ARMS, BodyPartIds.LEGS -> 0.65f
            else -> 0.5f
        }

        val lines = detector.detectLines(scaledBitmap, sensitivity, partId)

        if (scaledBitmap != input && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        if (recycleInput && !input.isRecycled) {
            input.recycle()
        }

        return lines
    }


}

