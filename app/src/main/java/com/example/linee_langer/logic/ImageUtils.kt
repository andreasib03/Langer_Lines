package com.example.linee_langer.logic

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

//bitmap rotations and Uri conversion


object ImageUtils{

    fun ImageProxy.toBitmapFixed(): Bitmap {
        val originalBitmap = this.toBitmap()
        val rotationDegrees = this.imageInfo.rotationDegrees

        // 2. Only rotate if actually necessary to save memory/cycles

        if(rotationDegrees == 0)
            return originalBitmap

            val matrix = android.graphics.Matrix().apply {
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

        if(originalBitmap != rotatedBitmap){
            originalBitmap.recycle()
        }

        return rotatedBitmap
    }
}

