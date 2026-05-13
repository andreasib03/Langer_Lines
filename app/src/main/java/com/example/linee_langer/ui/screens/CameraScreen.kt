package com.example.linee_langer.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.util.Log
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.linee_langer.R
import com.example.linee_langer.logic.ImageUtils.toBitmapFixed
import com.example.linee_langer.logic.saveBitmapAndFinish
import com.example.linee_langer.ui.components.LangerOverlay
import com.example.linee_langer.ui.interfacesUser.AppDimension
import com.example.linee_langer.ui.components.PermissionDeniedUI
import com.example.linee_langer.ui.utils.BodyPart
import com.example.linee_langer.ui.utils.QualityInfo
import com.example.linee_langer.ui.utils.bodyPartsList
import com.example.linee_langer.ui.viewModels.CameraAnalysisViewModel
import com.example.linee_langer.ui.viewModels.NotificationViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    analysisViewModel: CameraAnalysisViewModel,
    notificationViewModel: NotificationViewModel,
    onClose: () -> Unit
) {

    val context = LocalContext.current
    // State unique per la photo
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var selectedBodyPart by remember { mutableStateOf<BodyPart?>(null) } // body part selection

    var isAnalyzing by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        Alignment.TopCenter
    ) {
        if (hasCameraPermission) {
            when {
                // STATO 0: Selezione Parte del Corpo
                selectedBodyPart == null -> {
                    BodyPartSelectionOverlay(
                        onPartSelected = {
                            selectedBodyPart = it
                            analysisViewModel.setBodyPart(it.id)
                        }
                    )
                }

                isAnalyzing -> {
                    AnalysisLoadingScreen()
                    LaunchedEffect(Unit) {
                        delay(2000)
                        isAnalyzing = false
                    }
                }
                // 1. Mostra la Preview finale solo se l'analisi è finita e abbiamo la foto
                capturedBitmap != null -> {
                    ImagePreviewScreen(
                        bitmap = capturedBitmap!!,
                        analysisViewModel = analysisViewModel,
                        onConfirm = {
                            analysisViewModel.setBodyPart(selectedBodyPart?.id ?: "unknown")
                            saveBitmapAndFinish(
                                context = context,
                                bitmap = capturedBitmap!!,
                                analysisViewModel = analysisViewModel,
                                notificationViewModel = notificationViewModel,
                                onClose = onClose
                            )
                        },
                        onRetake = {
                            capturedBitmap = null
                            analysisViewModel.cleanLines()
                        }
                    )
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
            if (selectedBodyPart != null && capturedBitmap == null && !isAnalyzing) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .clickable { selectedBodyPart = null },
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Area: ${stringResource(selectedBodyPart!!.name)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.ic_sync),
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
                PermissionDeniedUI {
                    launcher.launch(Manifest.permission.CAMERA)
                }

            }
        }
    }

@Composable
fun AnalysisLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 4.dp,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.analyis_loading),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.identification),
            color = Color.White.copy(alpha = 0.5f),
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
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap?.let { b ->
                onImageCaptured(b)
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraX", "Cleanup failed", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

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
                        Log.d("CameraFlip", "Binding completato con successo per: $currentLens")
                    } catch (e: Exception) {
                        Log.e("CameraX", "Binding failed", e)
                    }
                }, executor)
            }
        )

            LangerOverlay(lines = analysisViewModel.detectedLines)

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(top = AppDimension.PaddingTop, start = AppDimension.PaddingStart)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }



        if (!analysisViewModel.hasLines) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.camera_advice),
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // BARRA DEI PULSANTI INFERIORE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Ancoraggio in basso
                .padding(bottom = 50.dp)
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
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gallery), // Meglio ic_gallery se disponibile
                        contentDescription = stringResource(R.string.gallery),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 2. PULSANTE DI SCATTO
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
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
                                        Log.e("CameraX", "Capture failed", exception)
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
                                Log.d("CameraFlip", "Passo alla FRONT")
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                Log.d("CameraFlip", "Passo alla BACK")
                                CameraSelector.LENS_FACING_BACK
                            }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_switchcamera), // Usa un'icona flip camera
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
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
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    val lineCount = analysisViewModel.detectedLines.size

    // Logica di valutazione
    val qualityStatus = when {
        lineCount > 40 -> QualityInfo(
            stringResource(R.string.optime),
            Color(0xFF4CAF50),
            R.drawable.ic_check
        )
        lineCount > 15 -> QualityInfo(
            stringResource(R.string.sufficient),
            Color(0xFFFFC107),
            R.drawable.ic_warning
        )
        else -> QualityInfo(
            stringResource(R.string.low),
            Color(0xFFF44336),
            R.drawable.ic_danger
        )
    }

    // CONTENITORE PRINCIPALE (BoxScope)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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
                lines = analysisViewModel.detectedLines,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Info Panel Superiore (Figlio diretto del Box principale)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter) // Corretto: Figlio di Box
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, qualityStatus.color.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(qualityStatus.icon),
                        contentDescription = null,
                        tint = qualityStatus.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Qualità ${qualityStatus.label}: $lineCount linee",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (lineCount <= 15) {
                Text(
                    text = stringResource(R.string.advice_near),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.Red.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // 3. Barra dei pulsanti inferiore (Figlio diretto del Box principale)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Corretto: Figlio di Box
                .padding(bottom = 40.dp, start = 50.dp, end = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                label = stringResource(R.string.retry),
                icon = R.drawable.ic_close,
                color = Color.White.copy(alpha = 0.2f),
                onClick = onRetake
            )

            ActionButton(
                label = stringResource(R.string.confirm),
                icon = R.drawable.ic_check,
                color = qualityStatus.color,
                onClick = onConfirm
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
    val size = 72.dp
    val iconSize = 32.dp

    // Determiniamo il colore del contenuto (icona) in base allo sfondo
    // Se il colore è giallo (sufficiente), l'icona è nera, altrimenti bianca
    val contentColor = if (color == Color(0xFFFFC107)) Color.Black else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .clickable { onClick() }
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
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
            color = Color.White,
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
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.question_camera),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for(i in bodyPartsList.indices step 2){
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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

@Composable
fun BodyPartCard(part: BodyPart, onClick: () -> Unit){
    Card(
        modifier = Modifier
            .size(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(part.icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(part.name), color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}