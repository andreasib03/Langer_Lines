package com.example.linee_langer.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

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

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

}