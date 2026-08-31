package com.example.linee_langer.worker

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

private const val TAG = "ImageRecoveryWorker"
@HiltWorker
class ImageRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AnalysisRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {

            val picturesDir =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )

            val langerDir = File(picturesDir, "LangerAnalysis")

            if (!langerDir.exists()) {
                return Result.success()
            }

            val files = langerDir.listFiles { _, name ->
                name.startsWith("Langer_") && name.endsWith(".webp")
            } ?: return Result.success()


            val localAnalyses = repository.getAllAnalysesInternal()
            val analysesMap = localAnalyses.associateBy { it.date }

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
                            file.absolutePath
                        )

                        if (match.syncFailed){
                            repository.updateSyncFailed(match.id, false)
                        }
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {
            logCaughtException(TAG, "Recupero percorsi immagine fallito", e)
            Result.failure()
        }
    }
}