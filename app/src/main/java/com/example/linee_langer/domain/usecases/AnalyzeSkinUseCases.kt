package com.example.linee_langer.domain.usecases

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.logic.LangerDetector
import androidx.core.graphics.scale

class AnalyzeSkinUseCases(private val detector: LangerDetector) {

    operator fun invoke(imageProxy: ImageProxy, partId: String): List<LangerLine> {
        // 1. Use the built-in CameraX member function (no null check needed)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()

        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rb = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle() // Libera subito original
            rb
        } else {
            bitmap
        }

        val maxSide = 720

        val scaledBitmap = if (rotatedBitmap.width > maxSide || rotatedBitmap.height > maxSide) {
            val factor = if (rotatedBitmap.width > rotatedBitmap.height) maxSide.toFloat() / rotatedBitmap.width
            else maxSide.toFloat() / rotatedBitmap.height
            val sb = rotatedBitmap.scale((rotatedBitmap.width * factor).toInt(), (rotatedBitmap.height * factor).toInt())
            rotatedBitmap.recycle()
            sb
        } else rotatedBitmap

        val sensitivity = when(partId) {
            "face" -> 0.85f

            "arms", "legs" -> 0.65f

            else -> 0.5f
        }

        val lines = detector.detectLines(scaledBitmap, sensitivity, partId)

        scaledBitmap.recycle()
        return lines

    }

}