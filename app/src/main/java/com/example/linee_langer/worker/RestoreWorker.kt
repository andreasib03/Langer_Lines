package com.example.linee_langer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.core.utils.ImageUtils
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.core.utils.saveBitmapToGallery
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

private const val TAG = "RestoreWorker"

@HiltWorker
class RestoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepo: AnalysisRepository,
    private val firebaseRepo: FirebaseRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = authRepository.currentUser?.uid ?: return Result.failure()

        return try {
            val remoteAnalyses = firebaseRepo.getAllRemoteAnalyses(uid)

            if (remoteAnalyses.isEmpty()) {
                return Result.success()
            }

            remoteAnalyses.forEach { data ->
                val date = data["date"] as? Long ?: return@forEach
                val bodyPartId = data["bodyPartId"] as? String ?: ""
                val resultSummary = data["resultSummary"] as? String ?: ""
                val imageBase64 = data["imageBase64"] as? String
                @Suppress("UNCHECKED_CAST")
                val linesData = data["lines"] as? List<Map<String, Any>> ?: emptyList()

                // Verifica se l'analisi esiste già localmente
                val localAnalysis = analysisRepo.getAnalysisByTimestamp(date, uid)

                if (localAnalysis == null) {
                    // 1. Caso: Analisi mancante nel DB locale
                    restoreFullEntry(uid, date, bodyPartId, resultSummary, imageBase64, linesData)
                } else if (imageBase64 != null && !localFileExists(localAnalysis.imagePath)) {
                    // 2. Caso: Voce DB presente ma file immagine mancante fisicamente
                    restoreMissingImage(localAnalysis.id, date, imageBase64)
                }
            }

            Result.success()
        } catch (e: Exception) {
            logCaughtException(TAG, "Restore analisi fallito", e)
            Result.retry()
        }
    }

    private suspend fun restoreFullEntry(
        uid: String,
        date: Long,
        bodyPartId: String,
        resultSummary: String,
        imageBase64: String?,
        linesData: List<Map<String, Any>>
    ) {
        val localPath = if (imageBase64 != null) {
            decodeAndSaveImage(date, imageBase64) ?: "internal_placeholder"
        } else {
            "internal_placeholder"
        }

        val newEntity = SkinAnalysisEntity(
            date = date,
            bodyPartId = bodyPartId,
            imagePath = localPath,
            resultSummary = resultSummary,
            isSynced = true,
            userId = uid
        )

        // Convertiamo i dati delle linee da Firestore in Entity Room
        val lineEntities = linesData.map {
            com.example.linee_langer.core.database.entity.LangerLineEntity(
                startX = (it["startX"] as? Number)?.toFloat() ?: 0f,
                startY = (it["startY"] as? Number)?.toFloat() ?: 0f,
                endX = (it["endX"] as? Number)?.toFloat() ?: 0f,
                endY = (it["endY"] as? Number)?.toFloat() ?: 0f,
                intensity = (it["intensity"] as? Number)?.toFloat() ?: 0f
            )
        }

        // Salviamo l'analisi con le relative linee
        analysisRepo.saveFullAnalysis(newEntity, lineEntities)
    }

    private suspend fun restoreMissingImage(id: Long, date: Long, imageBase64: String) {
        val newPath = decodeAndSaveImage(date, imageBase64)
        if (newPath != null) {
            analysisRepo.updateImagePath(id, newPath)
            analysisRepo.updateSyncStatus(id, true)
        }
    }

    private suspend fun decodeAndSaveImage(date: Long, base64: String): String? {
        return withContext(Dispatchers.Default) {
            val bitmap = ImageUtils.base64ToBitmap(base64)
            if (bitmap != null) {
                val uri = saveBitmapToGallery(applicationContext, bitmap, date)
                bitmap.recycle()
                uri?.toString()
            } else null
        }
    }

    private fun localFileExists(path: String): Boolean {
        if (path.isBlank() || path == "internal_placeholder") return false
        return try {
            val uri = path.toUri()
            if (uri.scheme == "content") {
                applicationContext.contentResolver.openInputStream(uri)?.use { true } ?: false
            } else {
                java.io.File(path).exists()
            }
        } catch (e: Exception) {
            false
        }
    }
}
