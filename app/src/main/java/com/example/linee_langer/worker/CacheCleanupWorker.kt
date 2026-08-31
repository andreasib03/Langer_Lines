package com.example.linee_langer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.core.utils.logCaughtException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "CacheCleanupWorker"
@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val cacheDir = applicationContext.cacheDir
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursInMillis = 24 * 3600 * 1000

            // Recuperiamo un eventuale flag per capire se è una pulizia forzata (manuale)
            val isManual = inputData.getBoolean("is_manual", false)

            var deletedCount = 0

            cacheDir.listFiles()?.forEach { file ->
                val isOldEnough = (currentTime - file.lastModified() > twentyFourHoursInMillis)
                val isTempFile = file.name.startsWith("langer_temp")

                // Se è manuale, cancelliamo tutto il temporaneo subito.
                // Se è automatico, solo quello vecchio di 24h.
                if (isTempFile && (isManual || isOldEnough)) {
                    if (file.delete()) deletedCount++
                }
            }

            Result.success()
        } catch (e: Exception) {
            logCaughtException(TAG, "Pulizia cache temporanea fallita", e)
            Result.failure()
        }
    }
}