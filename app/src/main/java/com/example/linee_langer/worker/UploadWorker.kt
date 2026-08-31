package com.example.linee_langer.worker

import android.content.Context
import android.net.Uri
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
import com.example.linee_langer.core.utils.WorkerUtils.isRemoteUrl
import com.example.linee_langer.core.utils.logCaughtException

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

private const val TAG = "UploadWorker"
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepo: AnalysisRepository,
    private val firebaseRepo: FirebaseRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_UPLOADED_IDS = "uploaded_analysis_ids"
    }

    override suspend fun doWork(): Result {
        val uid = authRepository.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure()
        }

        return try {
            // 1. Recupera solo le analisi non sincronizzate (stesso filtro di SyncWorker)
            val pending = analysisRepo.getUnsyncedAnalyses()

            if (pending.isEmpty()) {
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
                    successfulIds.add(analysis.id)
                    return@forEach
                }

                val imageUri = try {
                    localPath.toUri()
                } catch (e: Exception) {
                    logCaughtException(TAG, "Parsing URI locale non valido (path=$localPath)", e)
                    analysisRepo.updateSyncFailed(analysis.id, true)
                    return@forEach
                }

                if (!uriIsAccessible(imageUri)) {
                    analysisRepo.updateSyncFailed(analysis.id, true)
                    return@forEach
                }

                val remoteUrl = firebaseRepo.uploadSkinImage(uid, imageUri)

                if(remoteUrl != null) {
                    // 3. Aggiorna il DB locale con l'URL di Firebase
                    analysisRepo.updateImagePath(analysis.id, remoteUrl)
                    successfulIds.add(analysis.id)
                } else {
                    atLeastOneFailed = true
                }
            }

            val outputData = workDataOf(
                KEY_UPLOADED_IDS to successfulIds.toLongArray()
            )

            if (atLeastOneFailed) {
                Result.retry()
            } else {
                Result.success(outputData)
            }

        } catch (e: Exception) {
            logCaughtException(TAG, "Upload analisi non sincronizzate fallito (uid=$uid)", e)
            Result.retry()
        }
    }


    /**
     * Verifica che l'URI sia ancora accessibile dal ContentResolver.
     * I file in MediaStore possono essere eliminati dall'utente dopo il salvataggio.
     */
    private fun uriIsAccessible(uri: Uri): Boolean {
        return try {
            applicationContext.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            logCaughtException(TAG, "URI locale non più accessibile (uri=$uri)", e)
            false
        }
    }
}
