package com.example.linee_langer.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.FirebaseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepo: AnalysisRepository,
    private val firebaseRepo: FirebaseRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userEmail = authRepository.currentUser?.email ?: return Result.failure()
        val pending = analysisRepo.getUnsyncedAnalyses()

        return try {
            pending.forEach { analysis ->
                // Se non carichi l'immagine, assicurati di caricare i dati corretti
                val success = firebaseRepo.uploadAnalysisSync(userEmail, analysis)
                if (success) {
                    analysisRepo.updateSyncStatus(analysis.id, true) // Usa ID, non DATE!
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}