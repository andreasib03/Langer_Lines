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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.graphics.scale
import com.example.linee_langer.logic.saveBitmapToGallery

@HiltViewModel
class CameraAnalysisViewModel @Inject constructor(
    application: Application,
    private val detector: LangerDetector,
    private val repositorAnalysis: AnalysisRepository,
    private val repositoryNotification: NotificationRepository,
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

    var galleryBitmapToEdit by mutableStateOf<Bitmap?>(null)
        private set

    fun analyzeGalleryImageWithTransform(
        source: Bitmap,
        scale: Float,
        rotation: Float,
        offsetX: Float,
        offsetY: Float
    ) {

        viewModelScope.launch(Dispatchers.Default) {
            isProcessing = true
            try {
                val matrix = Matrix().apply {
                    postScale(scale,scale)
                    postRotate(rotation)

                    postTranslate(offsetX,offsetY)
                }

                val transformedBitmap = Bitmap.createBitmap(
                    source, 0, 0, source.width, source.height, matrix, true
                )

                val results = detector.detectLines(
                    transformedBitmap,
                    partId = selectedBodyPartId ?: "face"
                )

                withContext(Dispatchers.Main){
                    detectedLines = results
                    galleryBitmapToEdit = transformedBitmap

                    repositoryNotification.addNotification(
                        title = "Analisi Galleria Completata",
                        description = "Rilevate ${results.size} linee nell'immagine allineata."
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Manual alignment analysis failed", e)
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearGalleryEdit() {
        galleryBitmapToEdit = null
        selectedImageUri = null
        cleanLines() // Pulisce anche le linee rilevate
        Log.d("CameraAnalysisVM", "Gallery edit cleared")
    }

    private var lastAnalysisTime = 0L
    fun analyzeLiveFrame(imageProxy: ImageProxy) {

        val currentTime = System.currentTimeMillis()

        if(isProcessing || (currentTime - lastAnalysisTime) < 180){
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

                    val analysisBitmap = bitmap.scale(360, 480, false)

                    if(bitmap != analysisBitmap)
                        bitmap.recycle()

                    val results = detector.detectLines(analysisBitmap, 0.5f, currentPartId)


                    val stabilized = smoothLinesProximity(previousLines, results)
                    previousLines = stabilized

                    withContext(Dispatchers.Main) {
                        detectedLines = stabilized
                    }

                    analysisBitmap.recycle()
                }
            } catch (e: Exception){
                Log.e("MainViewModel", "Live analysis failed: ", e)
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

        val alpha = 0.4f // Fattore di reattività (più è basso, più la linea è stabile ma lenta)

        return newLines.take(60).mapIndexed { index, newLine ->
            val closestOldLine = oldLines.getOrNull(index)

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

    fun saveAnalysisResult(date: Long, bitmap: Bitmap, linesToSave: List<LangerLine>) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing = true
            try {
                val context = getApplication<Application>()

                val publicUri = saveBitmapToGallery(context,bitmap,date)

                if(publicUri != null){
                    executeLocalSave(date, publicUri.toString(), linesToSave)
                    scheduleFullSync()
                }

            } finally {
                isProcessing = false
            }

        }
    }

    fun scheduleFullSync() {
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

    suspend fun executeLocalSave(date: Long, path:String, lines: List<LangerLine>){

        withContext(Dispatchers.IO){
            try {
                val analysis = SkinAnalysisEntry(
                    date = date,
                    bodyPartId = selectedBodyPartId ?: "Generico",
                    imagePath = path,
                    resultSummary = "Analisi effettuata con successo"
                )

                val entities = lines.map { it.toEntity()  }


                repositorAnalysis.saveFullAnalysis(analysis,entities)

                Log.d("CameraAnalysisVM", "Salvataggio database completato per l'analisi del $date")
            } catch (e: Exception) {
                Log.e("CameraAnalysisVM", "Errore nel salvataggio locale", e)
                throw e // Rilanciamo l'errore per gestirlo in saveBitmapAndFinish
            }
        }




    }


    fun onImageSelected(uri: Uri){
        selectedImageUri = uri
        cleanLines()
        isProcessing = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    bitmap?.let {
                        val corrected = rotateImageIfRequired(it, uri)
                        withContext(Dispatchers.Main) {
                            // SETTIAMO LA BITMAP: Questo farà apparire lo schermo di Edit nella UI
                            galleryBitmapToEdit = corrected
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to load gallery image", e)
            } finally {
                isProcessing = false
            }
        }
    }


    fun getAnalysisById(id: Long): Flow<AnalysisWithLines?> {
        return repositorAnalysis.getAnalysisById(id)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int = 1080, reqHeight: Int = 1080) : Int {
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

