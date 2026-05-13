package com.example.linee_langer.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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
                // Elimina i file più vecchi di 24 ore o tutti i file temporanei
                cacheDir.listFiles()?.forEach { file ->

                    val isOldEnough = (currentTime - file.lastModified() > twentyFourHoursInMillis)
                    val isTempFile = file.name.startsWith("langer_temp") || file.extension == "jpg"
                    if (isTempFile && isOldEnough) {
                        file.delete()
                    }
                }

                // aggiungere controllo System.currentTimeMillis() - file.lastModified() > 24 * 3600 * 1000
                Log.d("CacheWorker", "Pulizia cache completata con successo")
                Result.success()
            } catch (e: Exception) {
                Log.e("CacheWorker", "Errore durante la pulizia", e)
                Result.failure()
            }
        }
}