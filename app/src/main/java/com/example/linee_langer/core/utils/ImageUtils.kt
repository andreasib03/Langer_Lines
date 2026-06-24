package com.example.linee_langer.core.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object ImageUtils {

    fun ImageProxy.toBitmapFixed(): Bitmap {
        val originalBitmap = this.toBitmap()
        val rotationDegrees = this.imageInfo.rotationDegrees

        // Se non c'è rotazione, restituisci subito il bitmap originale
        if (rotationDegrees == 0) {
            return originalBitmap
        }

        // Ora il codice seguente è allineato correttamente perché l'if sopra è chiuso
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )

        // Se è stato creato un nuovo bitmap, ricicla quello vecchio per liberare memoria
        if (originalBitmap != rotatedBitmap) {
            originalBitmap.recycle()
        }

        return rotatedBitmap
    }
}