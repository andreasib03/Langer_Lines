package com.example.linee_langer.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.FirebaseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.core.net.toUri

/**
 * Carica su Firebase Storage le immagini delle analisi non ancora sincronizzate,
 * poi aggiorna il campo imagePath nel DB locale con l'URL remoto.
 *
 * Va schedulato PRIMA di SyncWorker, oppure SyncWorker può schedularlo come
 * prerequisito tramite WorkManager chain:
 *
 *   WorkManager.getInstance(context)
 *       .beginUniqueWork("Upload", ExistingWorkPolicy.KEEP, uploadRequest)
 *       .then(syncRequest)
 *       .enqueue()
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepo: AnalysisRepository,
    private val firebaseRepo: FirebaseRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UploadWorker"
    }

    override suspend fun doWork(): Result {
        val userEmail = authRepository.currentUser?.email
        if (userEmail.isNullOrBlank()) {
            Log.w(TAG, "Utente non loggato, upload saltato")
            return Result.failure()
        }

        return try {
            // 1. Recupera solo le analisi non sincronizzate (stesso filtro di SyncWorker)
            val pending = analysisRepo.getUnsyncedAnalyses()

            if (pending.isEmpty()) {
                Log.d(TAG, "Nessuna analisi da caricare")
                return Result.success()
            }

            var allUploaded = true

            pending.forEach { analysis ->
                val localPath = analysis.imagePath

                // 2. Salta se è già un URL remoto Firebase Storage
                if (isRemoteUrl(localPath) || localPath.isBlank() || localPath == "internal_placeholder") {
                    Log.d(TAG, "Immagine già su cloud per analisi ${analysis.id}, skip")
                    return@forEach
                }

                val imageUri = try { localPath.toUri() } catch (e: Exception) { null }

                if (imageUri != null && uriIsAccessible(imageUri)) {
                    Log.d(TAG, "Caricamento immagine analisi ${analysis.id}...")

                    // 2. USA LA NUOVA FUNZIONE SUSPEND DEL REPOSITORY
                    val remoteUrl = firebaseRepo.uploadSkinImage(userEmail, imageUri)

                    if (remoteUrl != null) {
                        // 3. Aggiorna il DB locale con l'URL di Firebase
                        analysisRepo.updateImagePath(analysis.date, remoteUrl)
                        Log.d(TAG, "Upload completato: $remoteUrl")
                    } else {
                        Log.e(TAG, "Upload fallito per ${analysis.id}")
                        allUploaded = false
                    }
                }
            }

            if (allUploaded) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e(TAG, "Errore imprevisto durante l'upload: ${e.message}", e)
            Result.retry()
        }
    }


    /**
     * Controlla se il path è già un URL remoto (Firebase Storage o HTTPS generico).
     * Non serve ricaricare immagini già su cloud.
     */
    private fun isRemoteUrl(path: String): Boolean {
        return path.startsWith("https://") || path.startsWith("gs://")
    }

    /**
     * Verifica che l'URI sia ancora accessibile dal ContentResolver.
     * I file in MediaStore possono essere eliminati dall'utente dopo il salvataggio.
     */
    private fun uriIsAccessible(uri: Uri): Boolean {
        return try {
            applicationContext.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
