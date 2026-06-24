package com.example.linee_langer.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.core.net.toUri
import androidx.work.workDataOf

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
        const val KEY_UPLOADED_IDS = "uploaded_analysis_ids"
    }

    override suspend fun doWork(): Result {
        val uid = authRepository.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.w(TAG, "Utente non loggato, upload saltato")
            return Result.failure()
        }

        return try {
            // 1. Recupera solo le analisi non sincronizzate (stesso filtro di SyncWorker)
            val pending = analysisRepo.getUnsyncedAnalyses()

            if (pending.isEmpty()) {
                Log.d(TAG, "Nessuna analisi da caricare")
                return Result.success(
                    workDataOf(KEY_UPLOADED_IDS to longArrayOf())
                )
            }

            val successfulIds = mutableListOf<Long>()
            var atLeastOneFailed = false

            pending.forEach { analysis ->
                val localPath = analysis.imagePath

                // 2. Salta se è già un URL remoto Firebase Storage
                if (isRemoteUrl(localPath) || localPath.isBlank() || localPath == "internal_placeholder") {
                    Log.d(TAG, "Immagine già su cloud per analisi ${analysis.id}, skip")
                    successfulIds.add(analysis.id)
                    return@forEach
                }

                val imageUri = try {
                    localPath.toUri()
                } catch (e: Exception) {
                    Log.e(TAG, "URI non valido per analisi ${analysis.id}: $localPath",e)
                    atLeastOneFailed = true
                    return@forEach
                }

                if (!uriIsAccessible(imageUri)) {
                    Log.e(TAG, "File non accessibile per analisi ${analysis.id}")
                    atLeastOneFailed = true
                    return@forEach
                }

                Log.d(TAG, "Caricamento immagine analisi ${analysis.id}...")
                val remoteUrl = firebaseRepo.uploadSkinImage(uid, imageUri)

                if(remoteUrl != null) {

                    // 3. Aggiorna il DB locale con l'URL di Firebase
                    analysisRepo.updateImagePath(analysis.id, remoteUrl)
                    successfulIds.add(analysis.id)
                    Log.d(TAG, "Upload completato per analisi ${analysis.id}: $remoteUrl")
                } else {
                        Log.e(TAG, "Upload fallito per ${analysis.id}")
                        atLeastOneFailed = true
                        // Se l'immagine locale è persa, non possiamo riprovare all'infinito
                        // Potresti decidere di segnare l'analisi come "orfana"
                }
            }

            val outputData = workDataOf(
                KEY_UPLOADED_IDS to successfulIds.toLongArray()
            )

            if (atLeastOneFailed) {
                Log.w(TAG, "Upload parziale: ${successfulIds.size} ok, riprovo per i falliti")
                Result.retry()
                // Nota: in caso di retry WorkManager non usa outputData.
                // Gli ID riusciti verranno ri-tentati ma updateImagePath è idempotente
                // (sovrascrive con lo stesso URL remoto), quindi non è un problema.
            } else {
                Result.success(outputData)
            }

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
