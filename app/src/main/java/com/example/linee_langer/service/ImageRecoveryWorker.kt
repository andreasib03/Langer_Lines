package com.example.linee_langer.service

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.data.AnalysisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

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
                Log.d("ImageRecoveryWorker", "Cartella non trovata")
                return Result.success()
            }

            val files = langerDir.listFiles { _, name ->
                name.startsWith("Langer_") && name.endsWith(".webp")
            } ?: return Result.success()

            Log.d("ImageRecoveryWorker", "Trovati ${files.size} file potenziali")

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
                        repository.updateImagePath(
                            timestamp,
                            file.absolutePath
                        )

                        Log.d(
                            "ImageRecoveryWorker",
                            "Immagine recuperata per il record: $timestamp"
                        )
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {

            Log.e(
                "ImageRecoveryWorker",
                "Errore nel recupero immagini",
                e
            )

            Result.failure()
        }
    }
}