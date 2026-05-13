package com.example.linee_langer.ui.viewModels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.FirebaseRepository
import com.example.linee_langer.data.NotificationRepository
import com.example.linee_langer.db.SkinAnalysisEntry
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.logic.ImageUtils.toBitmapFixed
import com.example.linee_langer.logic.LangerDetector
import com.example.linee_langer.service.SyncWorker
import com.example.linee_langer.service.UploadWorker
import com.example.linee_langer.ui.utils.saveImageToPublicGallery
import com.example.linee_langer.ui.utils.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraAnalysisViewModel @Inject constructor(
    application: Application,
    private val detector: LangerDetector,
    private val repositorAnalysis: AnalysisRepository,
    private val repositoryNotification: NotificationRepository,
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) : AndroidViewModel(application) {

    var detectedLines by mutableStateOf<List<LangerLine>>(emptyList())
        private set

    val hasLines by derivedStateOf {
        detectedLines.isNotEmpty()
    }
    var isProcessing by mutableStateOf(false)
        private set

    var previousLines: List<LangerLine> = emptyList()
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var selectedBodyPartId by mutableStateOf<String?>(null)
        private set


    private var lastAnalysisTime = 0L
    fun analyzeLiveFrame(imageProxy: ImageProxy) {

        val currentTime = System.currentTimeMillis()

        if(isProcessing || (currentTime - lastAnalysisTime) < 150){
            imageProxy.close()
            return
        }

        val currentPartId = selectedBodyPartId ?: run {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastAnalysisTime = currentTime

        viewModelScope.launch(Dispatchers.Default) {
            try{
                imageProxy.use { proxy ->
                    // Esegui la conversione pesante in un thread di background
                    val bitmap = proxy.toBitmapFixed()

                    // Opzionale: Scala il bitmap qui se il detector è troppo lento
                    // val scaled = Bitmap.createScaledBitmap(bitmap, 480, 640, true)

                    val results = detector.detectLines(bitmap, 0.5f, currentPartId)

                    val stabilized = smoothLinesProximity(previousLines, results)
                    previousLines = stabilized

                    withContext(Dispatchers.Main) {
                        detectedLines = stabilized
                    }
                }
            } catch (e: Exception){
                Log.e("MainViewModel", "Live analysis failed: ", e)
            } finally {
                delay(100)
                isProcessing = false
            }
        }

    }

    fun analyzeGalleryImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing = true
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                // FIX: Ora inSampleSize viene calcolato DOPO aver riempito options.outWidth
                options.inSampleSize = calculateInSampleSize(options, 1080, 1080)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                val bitmap = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }



                bitmap?.let { b ->
                    val correctedBitmap = rotateImageIfRequired(b, uri)
                    val results = detector.detectLines(correctedBitmap, partId = selectedBodyPartId ?: "face")
                    withContext(Dispatchers.Main) {
                        detectedLines = results
                        repositoryNotification.addNotification(
                            title = "Analisi completata",
                            description = if (results.isNotEmpty())
                                "Rilevate ${results.size} linee di tensione"
                            else "Nessuna linea rilevata nell'immagine"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Gallery analysis failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Stabilizza le linee combinando i vettori tra frame consecutivi per vicinanza geometrica.
     * Risolve il problema del flickering e dello sdoppiamento visivo delle linee di Langer.
     */
    private fun smoothLinesProximity(oldLines: List<LangerLine>, newLines: List<LangerLine>): List<LangerLine> {
        if (oldLines.isEmpty()) return newLines

        val alpha = 0.5f // Fattore di reattività (più è basso, più la linea è stabile ma lenta)

        val cellSize = 0.1f

        // 1. Crea mappa spaziale delle vecchie linee
        val spatialMap = mutableMapOf<Pair<Int, Int>, LangerLine>()
        oldLines.forEach { line ->
            val key = (line.startX / cellSize).toInt() to (line.startY / cellSize).toInt()
            spatialMap[key] = line
        }

        return newLines.map { newLine ->
            val key = (newLine.startX / cellSize).toInt() to (newLine.startY / cellSize).toInt()
            // Cerca solo nella cella corrente o adiacenti (molto più veloce)
            val closestOldLine = spatialMap[key]
                ?: spatialMap[key.first - 1 to key.second]
                ?: spatialMap[key.first + 1 to key.second]

            if (closestOldLine != null) {
                newLine.copy(
                    startX = closestOldLine.startX + alpha * (newLine.startX - closestOldLine.startX),
                    startY = closestOldLine.startY + alpha * (newLine.startY - closestOldLine.startY),
                    endX = closestOldLine.endX + alpha * (newLine.endX - closestOldLine.endX),
                    endY = closestOldLine.endY + alpha * (newLine.endY - closestOldLine.endY)
                )
            } else {
                newLine
            }
        }
    }

    /**
     * Controlla i metadati EXIF del file sorgente e ruota la Bitmap se necessario per allinearla alla UI.
     */
    private fun rotateImageIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        val context = getApplication<Application>()
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val degree = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> return bitmap
                }
                val matrix = Matrix().apply { postRotate(degree.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) bitmap.recycle()
                rotatedBitmap
            } ?: bitmap
        } catch (e: Exception) {
            Log.w("CameraAnalysisVM", "Impossibile leggere i metadati Exif di rotazione", e)
            bitmap
        }
    }

    fun setBodyPart(id: String){
        selectedBodyPartId = id
        cleanLines() // Usa la funzione centralizzata
    }

    fun saveAnalysisResult(date: Long, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing = true
            try {
                val context = getApplication<Application>()
                val fileName = "Langer_${date}"

                val publicUri = saveImageToPublicGallery(context,bitmap,fileName)

                val finalImagePath = publicUri?.toString() ?: "internal_placeholder"

                executeLocalSave(date, finalImagePath)

                scheduleFullSync()

                withContext(Dispatchers.Main){
                    repositoryNotification.addNotification(
                        title = "Analisi Salvata",
                        description = "Analisi memorizzata sul dispositivo"
                    )
                }
            } catch (e: Exception){
                Log.e("CameraAnalysisVM", "Errore durante il salvataggio locale: ${e.message}")
            } finally {
                isProcessing = false
            }

        }
    }

    private fun scheduleFullSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication())
            .beginUniqueWork("DataUploadAndSync", ExistingWorkPolicy.KEEP, uploadWorkRequest)
            .then(syncRequest)
            .enqueue()
    }

    private suspend fun executeLocalSave(date: Long, path:String){
        val analysis = SkinAnalysisEntry(
            date = date,
            bodyPartId = selectedBodyPartId ?: "Generico",
            imagePath = path,
            resultSummary = "Analisi effettuata con successo"
        )
        val linesToSave = detectedLines.map { it.toEntity() }

        repositorAnalysis.saveFullAnalysis(analysis, linesToSave)
    }


    fun onImageSelected(uri: Uri){
        selectedImageUri = uri
        detectedLines = emptyList()
        previousLines = emptyList()
        analyzeGalleryImage(uri)
        // You would typically load the Uri into a Bitmap here before calling runActualAnalysis
        // For now, using your simulate method
    }


    fun getAnalysisById(id: Long): Flow<AnalysisWithLines?> {
        return repositorAnalysis.getAnalysisById(id)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int) : Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if(height > reqHeight || width > reqWidth){
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth){
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun cleanLines(){
        // 1. Resetta le linee attualmente visualizzate sulla UI
        detectedLines = emptyList()

        // 2. Resetta la memoria delle linee precedenti
        // Fondamentale per evitare che lo smoothing (smoothLines)
        // faccia "saltare" le linee quando cambi area
        previousLines = emptyList()

        Log.d("ViewModel", "Linee pulite con successo")
    }





}

