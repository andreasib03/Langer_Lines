package com.example.linee_langer.core.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.linee_langer.domain.models.LangerLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

private const val TAG = "HelperImages"

suspend fun saveAnalysisData(
    context: Context,
    bitmap: Bitmap,
    lines: List<LangerLine>,
    onLocalSave: suspend (Long, String) -> Unit,
    onSync: () -> Unit
) = withContext(Dispatchers.IO){

    val timestamp = System.currentTimeMillis()
    val savedUri = saveBitmapToGallery(context, bitmap, timestamp) ?: throw Exception("Impossibile scrivere il file nella galleria")

    try {
        onLocalSave(timestamp, savedUri.toString())
        onSync()
    } catch (e: Exception) {
        logCaughtException(TAG, "Salvataggio dati analisi su DB fallito dopo scrittura immagine (uri=$savedUri)", e)
        // Se il salvataggio su DB fallisce, sarebbe opportuno gestire
        // la pulizia dell'immagine appena salvata (opzionale ma consigliato)
        throw Exception("Errore database: ${e.message}", e)
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
            return uri
        }
    } catch (e: Exception) {
        logCaughtException(TAG, "Scrittura bitmap in galleria fallita (uri=$imageUri)", e)
        return null
    }

    return null
}