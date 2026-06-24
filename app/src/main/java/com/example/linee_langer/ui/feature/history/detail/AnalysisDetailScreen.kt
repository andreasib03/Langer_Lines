package com.example.linee_langer.ui.feature.history.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.linee_langer.ui.feature.profile.AnalysisInfoPanel
import com.example.linee_langer.ui.theme.CameraOverlayBg
import com.example.linee_langer.core.utils.toDomainModel
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import androidx.compose.runtime.getValue
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun AnalysisDetailScreen(
    analysisId: Long,
    notificationViewModel: NotificationViewModel,
    detailViewModel: AnalysisDetailViewModel,
    onBack: () -> Unit
) {
    // Retrieve analysis dal database with ViewModel
    val analysis by detailViewModel.getAnalysisById(analysisId).collectAsState(initial = null)

    LangerScaffold(
        title = stringResource(R.string.detailed_analysis),
        canNavigateBack = true,
        notificationViewModel = notificationViewModel,
        onBackClick = onBack
    ) { innerPadding ->
        analysis?.let { data ->
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
                    // Saved image
                    AsyncImage(
                        model = data.analysis.imagePath,
                        contentDescription = data.analysis.bodyPartId,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay delle linee saved
                    // animation
                    LangerOverlay(
                        lines = data.lines.map { it.toDomainModel() }, // Convert Entity -> Model
                        isVisible = true
                    )
                }

                // 2. Info Panel (Modern Sheet)
                AnalysisInfoPanel(data)
            }
        }
    }
}