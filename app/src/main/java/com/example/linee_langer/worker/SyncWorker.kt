package com.example.linee_langer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.core.utils.WorkerUtils.isRemoteUrl
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "SyncWorker"
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepo: AnalysisRepository,
    private val firebaseRepo: FirebaseRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        val uid = authRepository.currentUser?.uid ?: return Result.failure()

        val uploadedIds = inputData.getLongArray(UploadWorker.KEY_UPLOADED_IDS)

        val analysesToSync = if (uploadedIds != null && uploadedIds.isNotEmpty()) {
            analysisRepo.getUnsyncedAnalyses().filter { it.id in uploadedIds }
        } else {
            analysisRepo.getUnsyncedAnalyses().filter { isRemoteUrl(it.imagePath) }
        }

        if (analysesToSync.isEmpty()) {
            return Result.success()
        }

        return try {
            var allSynced = true
            analysesToSync.forEach { analysis ->
                // Se non carichi l'immagine, assicurati di caricare i dati corretti
                if (!isRemoteUrl(analysis.imagePath)) {
                    allSynced = false
                    return@forEach
                }

                val success = firebaseRepo.uploadAnalysisSync(uid, analysis)

                if (success) {
                    analysisRepo.updateSyncStatus(analysis.id, true)
                } else {
                    allSynced = false
                }
            }

            if (allSynced)
                Result.success()
            else
                Result.retry()
        } catch (e: Exception) {
            logCaughtException(TAG, "Sync analisi non sincronizzate fallita (uid=$uid)", e)
            Result.retry()
        }
    }
}

