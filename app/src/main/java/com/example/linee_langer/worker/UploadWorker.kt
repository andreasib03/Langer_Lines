package com.example.linee_langer.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.linee_langer.core.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

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
        val uid = authRepository.currentUser?.uid ?: return Result.failure()

        return try {
            // 1. Recupera solo le analisi non sincronizzate
            val pending = analysisRepo.getUnsyncedAnalyses(uid)

            if (pending.isEmpty()) {
                return Result.success(
                    workDataOf(KEY_UPLOADED_IDS to longArrayOf())
                )
            }

            val successfulIds = mutableListOf<Long>()
            var atLeastOneFailed = false

            pending.forEach { analysis ->
                val localPath = analysis.imagePath

                // Se è già un URL remoto (es. caricato da un altro dispositivo) o non valido
                if (isRemoteUrl(localPath) || localPath.isBlank() || localPath == "internal_placeholder") {
                    // Sincronizziamo comunque i metadati e le linee se l'immagine è già remota o non disponibile
                    val success = withContext(Dispatchers.IO) {
                        val fullAnalysis = analysisRepo.getAnalysisWithLinesSuspend(analysis.id, uid)
                        val lines = fullAnalysis?.lines ?: emptyList()
                        firebaseRepo.uploadAnalysisSync(uid, analysis, null, lines)
                    }
                    if (success) {
                        analysisRepo.updateSyncStatus(analysis.id, true)
                        successfulIds.add(analysis.id)
                    } else {
                        atLeastOneFailed = true
                    }
                    return@forEach
                }

                val imageUri = try {
                    localPath.toUri()
                } catch (e: Exception) {
                    logCaughtException(TAG, "Parsing URI locale non valido (path=$localPath)", e)
                    analysisRepo.updateSyncFailed(analysis.id, true)
                    atLeastOneFailed = true
                    return@forEach
                }

                if (!uriIsAccessible(imageUri)) {
                    analysisRepo.updateSyncFailed(analysis.id, true)
                    atLeastOneFailed = true
                    return@forEach
                }

                // 2. Conversione in Base64 e caricamento atomico su Firestore
                val success = withContext(Dispatchers.IO) {
                    var bitmap: Bitmap? = null
                    try {
                        // Carichiamo la bitmap
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 2
                        }

                        val inputStream: InputStream? = when (imageUri.scheme) {
                            "content" -> applicationContext.contentResolver.openInputStream(imageUri)
                            "file" -> FileInputStream(File(imageUri.path ?: ""))
                            else -> {
                                // Gestisce path assoluti come "/storage/emulated/0/..."
                                val path = imageUri.path ?: localPath
                                val file = File(path)
                                if (file.exists()) FileInputStream(file) else null
                            }
                        }

                        bitmap = inputStream?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                        
                        if (bitmap != null) {
                            val base64 = ImageUtils.bitmapToBase64(bitmap, quality = 60)
                            
                            // Recuperiamo anche le linee di Langer per non perdere il dettaglio
                            val fullAnalysis = analysisRepo.getAnalysisWithLinesSuspend(analysis.id, uid)
                            val lines = fullAnalysis?.lines ?: emptyList()

                            firebaseRepo.uploadAnalysisSync(uid, analysis, base64, lines)
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        logCaughtException(TAG, "Conversione o upload fallito per id=${analysis.id}", e)
                        false
                    } finally {
                        bitmap?.recycle()
                    }
                }

                if (success) {
                    analysisRepo.updateSyncStatus(analysis.id, true)
                    analysisRepo.updateSyncFailed(analysis.id, false)
                    successfulIds.add(analysis.id)
                } else {
                    atLeastOneFailed = true
                    if (runAttemptCount >= 2) {
                        analysisRepo.updateSyncFailed(analysis.id, true)
                    }
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
            logCaughtException(TAG, "Upload analisi fallito (uid=$uid)", e)
            if (runAttemptCount >= 2) Result.failure() else Result.retry()
        }
    }


    /**
     * Verifica che l'URI sia ancora accessibile dal ContentResolver.
     */
    private fun uriIsAccessible(uri: Uri): Boolean {
        return try {
            when (uri.scheme){
                "content" -> {
                    applicationContext.contentResolver.openInputStream(uri)?.use { true } ?: false
                }

                "file", null -> {
                    val path = uri.path ?: uri.toString()
                    val file = File(path)
                    file.exists() && file.canRead()
                }

                else -> false
            }

        } catch (e: Exception) {
            logCaughtException(TAG, "URI locale non più accessibile (uri=$uri)", e)
            false
        }
    }
}
