package com.example.linee_langer.ui.feature.history.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.linee_langer.R
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.camera.components.LangerOverlay
import com.example.linee_langer.ui.theme.CameraOverlayBg
import com.example.linee_langer.core.utils.toDomainModel
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import androidx.compose.runtime.getValue
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun AnalysisDetailScreen(
    notificationViewModel: NotificationViewModel,
    detailViewModel: AnalysisDetailViewModel,
    onBack: () -> Unit
) {
    val analysis by detailViewModel.analysis.collectAsState(initial = null)

    LangerScaffold(
        title = stringResource(R.string.detailed_analysis),
        canNavigateBack = true,
        notificationViewModel = notificationViewModel,
        onBackClick = onBack
    ) { innerPadding ->
        when (val data = analysis) {
            null -> {
                // Stato di caricamento iniziale (null = non ancora emesso dal Flow)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(CameraOverlayBg)
                ) {
                    // 1. Visualization con Overlay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = Dimens.XXLarge, bottomEnd = Dimens.XXLarge))
                    ) {
                        AsyncImage(
                            model = data.analysis.imagePath,
                            contentDescription = data.analysis.bodyPartId,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        LangerOverlay(
                            lines = data.lines.map { it.toDomainModel() },
                            isVisible = true
                        )
                    }

                    // 2. Info Panel
                    AnalysisInfoPanel(
                        data = data,
                        onRetrySync = { detailViewModel.retrySync() })
                }
            }
        }
    }
}