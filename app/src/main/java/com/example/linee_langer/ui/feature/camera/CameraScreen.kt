package com.example.linee_langer.ui.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.linee_langer.R
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.core.utils.ImageUtils.toBitmapFixed
import com.example.linee_langer.ui.feature.camera.components.GalleryAlignmentScreen
import com.example.linee_langer.ui.feature.camera.components.LangerOverlay
import com.example.linee_langer.ui.shared.components.OpenCvUnavailableBanner
import com.example.linee_langer.ui.shared.components.PermissionDeniedUI
import com.example.linee_langer.ui.shared.components.PermissionPermanentlyDeniedUI
import com.example.linee_langer.ui.feature.camera.model.BodyPart
import com.example.linee_langer.ui.feature.camera.model.QualityInfo
import com.example.linee_langer.ui.feature.camera.model.bodyPartsList
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

import android.provider.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import com.example.linee_langer.ui.feature.camera.components.BodyPartCard
import com.example.linee_langer.ui.feature.camera.utils.CameraError
import com.example.linee_langer.ui.navigation.Screen
import com.example.linee_langer.ui.theme.CameraOverlayBg
import com.example.linee_langer.ui.theme.CameraOverlayBorder
import com.example.linee_langer.ui.theme.CameraOverlayText
import com.example.linee_langer.ui.theme.CameraOverlayTextMuted
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.WarningLight
import com.example.linee_langer.ui.theme.appColors
import kotlin.time.Duration.Companion.milliseconds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val TAG = "CameraScreen"

@SuppressLint("SuspiciousIndentation")
@Composable
fun CameraScreen(
    analysisViewModel: CameraAnalysisViewModel,
    notificationViewModel: NotificationViewModel,
    onClose: () -> Unit
) {

    val errorMessage = analysisViewModel.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    var permissionState by remember {
        mutableIntStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        )
    }

    var hasRequestedOnce by rememberSaveable{ mutableStateOf(false)}

    val lifecycleOwnerForPermission = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwnerForPermission) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                }
            }
            lifecycleOwnerForPermission.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwnerForPermission.lifecycle.removeObserver(observer)
            }
        }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var selectedBodyPart by remember { mutableStateOf<BodyPart?>(null) } // body part selection

    var isAnalyzing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState =
            if(isGranted) {
                PackageManager.PERMISSION_GRANTED
            } else {
                PackageManager.PERMISSION_DENIED
            }
        hasRequestedOnce = true
    }

    LaunchedEffect(Unit) {
        if (permissionState != PackageManager.PERMISSION_GRANTED)
            launcher.launch(Manifest.permission.CAMERA)
    }

    val saveFailedMessage = stringResource(R.string.error_save_failed)
    val galleryFailedMessage = stringResource(R.string.error_gallery_analysis_failed)

    LaunchedEffect(errorMessage) {
        val message = when (errorMessage) {
            is CameraError.SaveFailed ->
                saveFailedMessage

            is CameraError.GalleryAnalysisFailed ->
                galleryFailedMessage

            is CameraError.Generic ->
                errorMessage.message

            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "OK",
            duration = SnackbarDuration.Long
        )
        analysisViewModel.clearError()
    }


    when {
        permissionState == PackageManager.PERMISSION_GRANTED -> {
            Box(
                modifier = Modifier.fillMaxSize().background(CameraOverlayBg),
                Alignment.TopCenter
            ) {
                when {
                    selectedBodyPart == null -> {
                        BodyPartSelectionOverlay(
                            onPartSelected = {
                                selectedBodyPart = it
                                analysisViewModel.setBodyPart(it.id)
                            }
                        )
                    }

                    analysisViewModel.galleryBitmapToEdit != null && !analysisViewModel.hasLines -> {
                        GalleryAlignmentScreen(
                            bitmap = analysisViewModel.galleryBitmapToEdit!!,
                            onConfirm = { scale, offset, rotation ->
                                analysisViewModel.analyzeGalleryImageWithTransform(
                                    source = analysisViewModel.galleryBitmapToEdit!!,
                                    scale = scale,
                                    rotation = rotation,
                                    offsetX = offset.x,
                                    offsetY = offset.y
                                )
                            },
                            onCancel = {
                                analysisViewModel.clearGalleryEdit()
                                // Aggiungi una funzione nel VM per resettare galleryBitmapToEdit = null
                            }
                        )
                    }

                    isAnalyzing -> {
                        AnalysisLoadingScreen()
                        LaunchedEffect(Unit) {
                            delay(2000.milliseconds)
                            isAnalyzing = false
                        }
                    }
                    // 1. Mostra la Preview finale solo se l'analisi è finita e abbiamo la foto
                    capturedBitmap != null || analysisViewModel.galleryBitmapToEdit != null -> {
                        val bitmapToShow = capturedBitmap ?: analysisViewModel.galleryBitmapToEdit

                        if(bitmapToShow != null) {
                            ImagePreviewScreen(
                                bitmap = bitmapToShow,
                                analysisViewModel = analysisViewModel,
                                onConfirm = { linesToSave ->
                                    analysisViewModel.persistAnalysis(
                                        bitmap = bitmapToShow,
                                        lines = linesToSave,
                                        onSuccess = {
                                            notificationViewModel.sendAnalysisSuccessNotification(targetRoute = Screen.History.route)
                                            onClose()
                                        }
                                    )
                                },
                                onRetake = {
                                    capturedBitmap = null
                                    analysisViewModel.clearGalleryEdit()
                                }
                            )
                        }
                    }

                    // 3. Altrimenti mostra la Fotocamera (solo se non siamo in preview/analisi)
                    else -> {
                        CameraContent(
                            analysisViewModel = analysisViewModel,
                            onClose = { selectedBodyPart = null },
                            onImageCaptured = { bitmap ->
                                capturedBitmap = bitmap
                                isAnalyzing = true // Avvia l'animazione di analisi
                            }
                        )
                    }
                }


                // Indicatore dell'area scelta (il "chip" in alto)
                // CHIP IN ALTO (Solo se una parte è selezionata e non siamo in analisi/preview)
                if (selectedBodyPart != null && capturedBitmap == null && !isAnalyzing && analysisViewModel.galleryBitmapToEdit == null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Dimens.XLarge)
                            .clickable { selectedBodyPart = null },
                        color = CameraOverlayBg.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(Dimens.RadiusSmall),
                        border = BorderStroke(Dimens.ExtraSmall, CameraOverlayBorder.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Dimens.Medium, vertical = Dimens.Medium)
                        ) {
                            Text(
                                text = stringResource(R.string.body_part_label, stringResource(selectedBodyPart!!.name)),
                                color = CameraOverlayText,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(Dimens.Small))
                            Icon(
                                painterResource(R.drawable.ic_sync),
                                contentDescription = "",
                                tint = CameraOverlayText,
                                modifier = Modifier.size(Dimens.IconXSmall)
                            )
                        }
                    }
                }
            }
        }

        hasRequestedOnce && !ActivityCompat.shouldShowRequestPermissionRationale(
            context as Activity,
            Manifest.permission.CAMERA
        ) -> {
            PermissionPermanentlyDeniedUI(
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )

                    context.startActivity(intent)
                }
            )
        }
        else -> {
            PermissionDeniedUI (
                onRetry = { launcher.launch(Manifest.permission.CAMERA) }
            )
        }
    }
}




@Composable
fun AnalysisLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraOverlayBg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = CameraOverlayText,
            strokeWidth = Dimens.ExtraSmall,
            modifier = Modifier.size(Dimens.Guide)
        )
        Spacer(modifier = Modifier.height(Dimens.XLarge))
        Text(
            text = stringResource(R.string.analysis_loading),
            color = CameraOverlayText,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.identification),
            color = CameraOverlayText.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CameraContent(
    analysisViewModel: CameraAnalysisViewModel,
    onClose: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    val captureFailedMessage = stringResource(R.string.error_capture_failed)
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(
            AspectRatioStrategy(
                AspectRatio.RATIO_4_3, // Il formato desiderato
                AspectRatioStrategy.FALLBACK_RULE_AUTO // Se 4:3 non esiste, sceglie il migliore disponibile
            )
        ).build()
    val imageCapture = remember { ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector) // Forza lo stesso 4:3
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build() }
    var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }


    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            analysisViewModel.onImageSelected(it)
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Cleanup della camera non riuscito: nessuna azione necessaria, la Activity verrà distrutta.
                logCaughtException(TAG, "Unbind camera in onDispose fallito", e)
            }
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CameraOverlayBg)) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Creiamo solo la vista nella factory
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
            },
            update = { previewView ->
                // La logica di binding si sposta qui: viene chiamata ogni volta che lensFacing cambia

                val currentLens = lensFacing
                val lastLens = previewView.getTag(R.id.lens_facing_tag) as? Int
                if (lastLens == currentLens)
                    return@AndroidView
                previewView.setTag(R.id.lens_facing_tag, currentLens)

                val executor = ContextCompat.getMainExecutor(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(currentLens)
                        .build()

                    val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }


                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .apply {
                            setAnalyzer(cameraExecutor) { imageProxy ->
                                analysisViewModel.analyzeLiveFrame(imageProxy)
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analysis,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        // Binding fotocamera non riuscito per questo lens: il preview resta fermo all'ultimo frame valido.
                        logCaughtException(TAG, "Bind camera al lifecycle fallito (lens=$currentLens)", e)
                    }
                }, executor)
            }
        )

        LangerOverlay(lines = analysisViewModel.detectedLines)

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(top = Dimens.Medium, start = Dimens.Medium)
                .size(Dimens.CameraIconButton)
                .background(CameraOverlayBg.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.close),
                tint = CameraOverlayText,
                modifier = Modifier.size(Dimens.XLarge)
            )
        }

        if(!analysisViewModel.isOpenCvAvailable){
            OpenCvUnavailableBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Dimens.CameraTopPadding, start = Dimens.Standard, end = Dimens.Standard)
            )
        }



        if (!analysisViewModel.hasLines && analysisViewModel.isOpenCvAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = Dimens.CameraBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.camera_advice),
                    color = CameraOverlayText.copy(alpha = 0.7f),
                    modifier = Modifier
                        .background(CameraOverlayBg.copy(alpha = 0.6f), RoundedCornerShape(Dimens.Large))
                        .padding(horizontal = Dimens.Large, vertical = Dimens.SmallMedium),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // BARRA DEI PULSANTI INFERIORE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Ancoraggio in basso
                .padding(bottom = Dimens.BottomNavHeight)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. PULSANTE GALLERIA
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(Dimens.CameraShutterSize)
                        .background(CameraOverlayBorder.copy(alpha = 0.2f), CircleShape)
                        .border(Dimens.BorderThin, CameraOverlayBorder.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gallery), // Meglio ic_gallery se disponibile
                        contentDescription = stringResource(R.string.gallery),
                        tint = CameraOverlayText,
                        modifier = Modifier.size(Dimens.XMLarge)
                    )
                }

                // 2. PULSANTE DI SCATTO
                Box(
                    modifier = Modifier
                        .size(Dimens.TopHuge)
                        .border(Dimens.ExtraSmall, CameraOverlayBorder, CircleShape)
                        .padding(Dimens.MediumSmall)
                        .clip(CircleShape)
                        .background(CameraOverlayText)
                        .clickable {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = image.toBitmapFixed()
                                        image.close()
                                        onImageCaptured(bitmap)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        analysisViewModel.reportError(CameraError.Generic(captureFailedMessage))
                                    }
                                }
                            )
                        }
                )

                // 3. PULSANTE CAMBIO CAMERA (Oppure Box vuoto per bilanciamento)
                IconButton(
                    onClick = {
                        lensFacing =
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                    },
                    modifier = Modifier
                        .size(Dimens.CameraShutterSize)
                        .background(CameraOverlayBorder.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_switchcamera), // Usa un'icona flip camera
                        contentDescription = stringResource(R.string.camera_flip),
                        tint = CameraOverlayText,
                        modifier = Modifier.size(Dimens.XMLarge)
                    )
                }
            }
        }

    }
}

@Composable
fun ImagePreviewScreen(
    bitmap: Bitmap,
    analysisViewModel: CameraAnalysisViewModel,
    onConfirm: (List<LangerLine>) -> Unit,
    onRetake: () -> Unit
) {
    val lineCount = remember { analysisViewModel.detectedLines.size }

    val staticLines = remember { analysisViewModel.detectedLines.toList() }

    // Logica di valutazione
    val qualityStatus = when {
        lineCount > 40 -> QualityInfo(
            stringResource(R.string.quality_optimal),
            MaterialTheme.appColors.qualityHigh,
            R.drawable.ic_check
        )
        lineCount > 15 -> QualityInfo(
            stringResource(R.string.quality_sufficient),
            MaterialTheme.appColors.qualityMedium,
            R.drawable.ic_warning
        )
        else -> QualityInfo(
            stringResource(R.string.quality_low),
            MaterialTheme.appColors.qualityLow,
            R.drawable.ic_danger
        )
    }

    // CONTENITORE PRINCIPALE (BoxScope)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraOverlayBg),
    ) {
        // 1. Box centrale per Foto e Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .aspectRatio(3f / 4f)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            LangerOverlay(
                lines = staticLines,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Info Panel Superiore (Figlio diretto del Box principale)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter) // Corretto: Figlio di Box
                .padding(top = Dimens.Guide),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = CameraOverlayBg.copy(alpha = 0.7f),
                shape = RoundedCornerShape(Dimens.XLarge),
                border = BorderStroke(Dimens.BorderThin, qualityStatus.color.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.Standard, vertical = Dimens.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Small)
                ) {
                    Icon(
                        painter = painterResource(qualityStatus.icon),
                        contentDescription = "",
                        tint = qualityStatus.color,
                        modifier = Modifier.size(Dimens.Large)
                    )
                    Text(
                        text = stringResource(R.string.quality_status_lines, qualityStatus.label, lineCount),
                        color = CameraOverlayText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (lineCount <= 15) {
                Text(
                    text = stringResource(R.string.advice_near),
                    color = CameraOverlayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = Dimens.Medium)
                        .background(MaterialTheme.appColors.qualityLow, RoundedCornerShape(Dimens.Small))
                        .padding(horizontal = Dimens.Medium, vertical = Dimens.ExtraSmall)
                )
            }
        }

        // 3. Barra dei pulsanti inferiore (Figlio diretto del Box principale)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Corretto: Figlio di Box
                .padding(bottom = Dimens.CameraIconButton, start = Dimens.BottomNavHeight, end = Dimens.BottomNavHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                label = stringResource(R.string.retry),
                icon = R.drawable.ic_close,
                color = CameraOverlayText.copy(alpha = 0.2f),
                onClick = onRetake
            )

            ActionButton(
                label = if (analysisViewModel.isProcessing) stringResource(R.string.saving) else stringResource(R.string.confirm),
                icon = if (analysisViewModel.isProcessing) R.drawable.ic_sync else R.drawable.ic_check,
                color = if (analysisViewModel.isProcessing) CameraOverlayTextMuted else qualityStatus.color,
                onClick = {
                    if (!analysisViewModel.isProcessing) {
                        onConfirm(staticLines)
                    }
                }
            )
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    icon: Int,
    color: Color,
    onClick: () -> Unit
) {
    val size = Dimens.ThumbnailSize
    val iconSize = Dimens.XXLarge

    // Determiniamo il colore del contenuto (icona) in base allo sfondo
    // Se il colore è giallo (sufficiente), l'icona è nera, altrimenti bianca
    val contentColor = if (color == WarningLight) CameraOverlayBg else CameraOverlayText

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Small)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .clickable { onClick() }
                .border(
                    width = Dimens.BorderThin,
                    color = CameraOverlayBorder.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }

        Text(
            text = label,
            color = CameraOverlayText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}




@Composable
fun BodyPartSelectionOverlay(
    onPartSelected: (BodyPart) -> Unit
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraOverlayBg.copy(alpha = 0.8f))
            .padding(Dimens.XLarge),
        contentAlignment = Alignment.Center
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.question_camera),
                style = MaterialTheme.typography.headlineSmall,
                color = CameraOverlayText,
                modifier = Modifier.padding(bottom = Dimens.XXLarge)
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Standard)) {
                for(i in bodyPartsList.indices step 2){
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Standard)
                    ) {
                        bodyPartsList.subList(i, (i+2).coerceAtMost(bodyPartsList.size)).forEach { part ->
                            BodyPartCard(part, onClick = { onPartSelected(part)})
                        }
                    }
                }
            }
        }
    }
}