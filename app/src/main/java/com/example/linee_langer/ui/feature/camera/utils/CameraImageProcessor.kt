package com.example.linee_langer.ui.feature.camera.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Carica una bitmap dalla gallery applicando subsampling per evitare OOM.
     * Corregge automaticamente la rotazione EXIF.
     * Restituisce null se il file non è leggibile.
     */
    fun loadFromUri(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options)
            options.inJustDecodeBounds = false

            val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            rotateIfRequired(sampled, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Errore caricamento immagine da URI", e)
            null
        }
    }

    /**
     * Applica una trasformazione affine (scala, rotazione, traslazione) alla bitmap sorgente.
     */
    fun applyTransform(
        source: Bitmap,
        scale: Float,
        rotation: Float,
        offsetX: Float,
        offsetY: Float
    ): Bitmap {
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
            postTranslate(offsetX, offsetY)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun rotateIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val degree = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> return bitmap
                }
                val matrix = Matrix().apply { postRotate(degree) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) bitmap.recycle()
                rotated
            } ?: bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Impossibile leggere metadati EXIF", e)
            bitmap
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int = 1080,
        reqHeight: Int = 1080
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private const val TAG = "CameraImageProcessor"
    }
}