package com.example.linee_langer.logic

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.R
import com.example.linee_langer.ui.viewModels.CameraAnalysisViewModel
import com.example.linee_langer.ui.viewModels.NotificationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream


fun saveBitmapAndFinish(
    context: Context,
    bitmap: Bitmap,
    analysisViewModel: CameraAnalysisViewModel,
    notificationViewModel: NotificationViewModel,
    onClose: () -> Unit
){

    analysisViewModel.viewModelScope.launch (Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()

            val savedUri = saveBitmapToGallery(context, bitmap, timestamp)

            if(savedUri != null) {
                analysisViewModel.saveAnalysisResult(
                    date = timestamp,
                    bitmap = bitmap
                )
                analysisViewModel.scheduleFullSync()
                notificationViewModel.sendAnalysisSuccessNotification()
            }

            withContext(Dispatchers.Main){
                onClose()
            }
        } catch(e: Exception){
            Log.e("Save bitmap", "Error on save: ${e.message}")
        }
    }

}

/**
 * Salva fisicamente la bitmap all'interno della memoria pubblica del dispositivo (Pictures/LangerAnalysis).
 * Utilizza il formato WebP per garantire file leggeri senza perdere i dettagli della scansione.
 */
fun saveBitmapToGallery(context: Context, bitmap: Bitmap, timestamp: Long): Uri? {
    val filename = "Langer_$timestamp.webp"
    val contentResolver = context.contentResolver

    // Seleziona la collection corretta in base alla versione Android
    val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/webp")

        // Creazione della cartella dedicata visibile all'utente nei file multimediali
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LangerAnalysis")
        }
    }

    val imageUri = contentResolver.insert(imageCollection, contentValues)

    try {
        imageUri?.let { uri ->
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                // Compressione WebP lossy all'85% (Rapporto qualità/peso perfetto per la pelle)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, stream)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 85, stream)
                }
            }
            Log.d("SaveBitmapToGallery", context.getString(R.string.log_success_image, uri.toString()))
            return uri
        }
    } catch (e: Exception) {
        Log.e("SaveBitmapToGallery", context.getString(R.string.log_failed_image), e)
    }

    return null
}