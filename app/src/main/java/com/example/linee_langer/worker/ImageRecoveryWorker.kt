package com.example.linee_langer.worker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

private const val TAG = "ImageRecoveryWorker"
@HiltWorker
class ImageRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AnalysisRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {

            val uid = authRepository.currentUser?.uid
            if (uid.isNullOrBlank()) {
                return Result.success(workDataOf("recovered_count" to 0))
            }

            val picturesDir =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )

            val langerDir = File(picturesDir, "LangerAnalysis")

            if (!langerDir.exists()) {
                // Ritorna 0 se la cartella non esiste
                return Result.success(workDataOf("recovered_count" to 0))
            }

            val files = langerDir.listFiles { _, name ->
                name.startsWith("Langer_") && name.endsWith(".webp")
            } ?: return Result.success(workDataOf("recovered_count" to 0))


            val localAnalyses = repository.getAllAnalysesInternal(uid)
            val analysesMap = localAnalyses.associateBy { it.date }

            var recoveredCount = 0

            files.forEach { file ->
                val timestamp = file.name
                    .removePrefix("Langer_")
                    .removeSuffix(".webp")
                    .toLongOrNull()

                if (timestamp != null) {
                    val match = analysesMap[timestamp]

                    if (match != null) {

                        repository.updateImagePathByTimestamp(
                            timestamp,
                            file.absolutePath,
                            uid
                        )

                        if (match.syncFailed) {
                            repository.updateSyncFailed(match.id, false)
                        }

                        recoveredCount++
                    }
                }
            }

            Result.success(workDataOf("recovered_count" to recoveredCount))

        } catch (e: Exception) {
            logCaughtException(TAG, "Recupero percorsi immagine fallito", e)
            Result.failure()
        }
    }
}