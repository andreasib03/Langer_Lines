package com.example.linee_langer.ui.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.R
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.domain.usecases.AnalyzeSkinUseCases
import com.example.linee_langer.worker.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.linee_langer.domain.models.BodyPartIds
import com.example.linee_langer.ui.feature.camera.utils.AnalysisPersistenceHelper
import com.example.linee_langer.ui.feature.camera.utils.CameraError
import com.example.linee_langer.ui.feature.camera.utils.CameraImageProcessor
import com.example.linee_langer.ui.feature.camera.utils.LineStabilizer
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class CameraAnalysisViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val analyzeSkinUseCases: AnalyzeSkinUseCases,
    private val imageProcessor: CameraImageProcessor,
    private val stabilizer: LineStabilizer,
    private val persistence: AnalysisPersistenceHelper,
    private val notificationRepository: NotificationRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    var detectedLines by mutableStateOf<List<LangerLine>>(emptyList())
        private set

    val hasLines by derivedStateOf {
        detectedLines.isNotEmpty()
    }
    var isProcessing by mutableStateOf(false)
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var selectedBodyPartId by mutableStateOf<String?>(null)
        private set

    var galleryBitmapToEdit by mutableStateOf<Bitmap?>(null)
        private set

    var errorMessage by mutableStateOf<CameraError?>(null)
        private set

    var isOpenCvAvailable: Boolean = analyzeSkinUseCases.isDetectorAvailable

    // Stato interno non esposto alla UI
    private var previousLines: List<LangerLine> = emptyList()
    private var lastAnalysisTime = 0L

    fun cleanLines(){
        detectedLines = emptyList()
        previousLines = emptyList()

        Log.d("ViewModel", "Linee pulite con successo")
    }

    fun clearError(){
        errorMessage = null
    }

    fun setBodyPart(id: String){
        selectedBodyPartId = id
        cleanLines() // Usa la funzione centralizzata
    }


    fun clearGalleryEdit() {
        galleryBitmapToEdit = null
        selectedImageUri = null
        cleanLines() // Pulisce anche le linee rilevate
        Log.d("CameraAnalysisVM", "Gallery edit cleared")
    }


    fun scheduleFullSync(){
        syncScheduler.scheduleFullSync()
    }


    // Immagine da gallery

    fun onImageSelected(uri: Uri) {
        selectedImageUri = uri
        cleanLines()

        viewModelScope.launch {
            withContext(Dispatchers.Main) { isProcessing = true }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    imageProcessor.loadFromUri(uri)
                }
                galleryBitmapToEdit = bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Errore caricamento immagine gallery", e)
            } finally {
                withContext(Dispatchers.Main) { isProcessing = false }
            }
        }
    }

    fun analyzeGalleryImageWithTransform(
        source: Bitmap,
        scale: Float,
        rotation: Float,
        offsetX: Float,
        offsetY: Float
    ) {


        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isProcessing = true }
            try {
                val transformed = imageProcessor.applyTransform(source, scale, rotation, offsetX, offsetY)
                val results = analyzeSkinUseCases(transformed, partId = selectedBodyPartId ?: BodyPartIds.DEFAULT)

                withContext(Dispatchers.Main) {
                    detectedLines = results
                    galleryBitmapToEdit = transformed
                }

                sendAnalysisNotification(results.size)

            } catch (e: Exception) {
                Log.e(TAG, "Analisi gallery fallita", e)
            } finally {
                withContext(Dispatchers.Main) { isProcessing = false }
            }
        }
    }
    // analisi live

    fun analyzeLiveFrame(imageProxy: ImageProxy) {

        if(!isOpenCvAvailable){
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()

        if(isProcessing || (currentTime - lastAnalysisTime) < 180){
            imageProxy.close()
            return
        }

        val partId = selectedBodyPartId ?: run { imageProxy.close(); return }
        lastAnalysisTime = currentTime

        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isProcessing = true }
            try {
                val results    = analyzeSkinUseCases(imageProxy, partId)
                val stabilized = stabilizer.smooth(previousLines, results)
                previousLines  = stabilized

                withContext(Dispatchers.Main) { detectedLines = stabilized }
            } catch (e: Exception) {
                Log.e(TAG, "Analisi live fallita", e)
            } finally {
                withContext(Dispatchers.Main) { isProcessing = false }
            }
        }

    }

    // salvataggio in galleria

    fun persistAnalysis(
        bitmap: Bitmap,
        lines: List<LangerLine>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) { isProcessing = true }
            clearError()
            try {
                withContext(Dispatchers.IO) {
                    persistence.save(
                        date       = System.currentTimeMillis(),
                        bitmap     = bitmap,
                        lines      = lines,
                        bodyPartId = selectedBodyPartId ?: BodyPartIds.DEFAULT
                    )
                }
                scheduleFullSync()
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                Log.e(TAG, "Errore salvataggio analisi", e)
                withContext(Dispatchers.Main) { errorMessage = CameraError.SaveFailed }
            } finally {
                withContext(Dispatchers.Main) { isProcessing = false }
            }
        }
    }


    private suspend fun sendAnalysisNotification(lineCount: Int) {
        notificationRepository.addNotification(
            title = appContext.getString(R.string.notification_gallery_complete),
            description = appContext.getString(R.string.notification_lines_detected, lineCount)
        )
    }

    companion object {
        private const val TAG = "CameraAnalysisVM"
    }


}