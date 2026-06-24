package com.example.linee_langer.ui.feature.settings.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.R
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import androidx.core.net.toUri
import com.example.linee_langer.ui.theme.Dimens

// ─── Modello ─────────────────────────────────────────────────────────────────
data class VideoTutorial(
    val id: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val rawResourceName: String,     // nome del file in res/raw senza estensione
    val thumbnailRes: Int? = null    // drawable opzionale per l'anteprima
)

// ─── Lista tutorial disponibili ───────────────────────────────────────────────
// Aggiungere nuovi video qui — il resto si aggiorna automaticamente.
val availableTutorials = listOf(
    VideoTutorial(
        id = 1,
        titleRes = R.string.tutorial_intro_title,
        descriptionRes = R.string.tutorial_intro_desc,
        rawResourceName = "tutorial_intro"
    ),
    VideoTutorial(
        id = 2,
        titleRes = R.string.tutorial_camera_title,
        descriptionRes = R.string.tutorial_camera_desc,
        rawResourceName = "tutorial_camera"
    ),
    VideoTutorial(
        id = 3,
        titleRes = R.string.tutorial_analysis_title,
        descriptionRes = R.string.tutorial_analysis_desc,
        rawResourceName = "tutorial_analysis"
    )
)

// ─── VideoTutorialScreen ──────────────────────────────────────────────────────
@Composable
fun VideoTutorialScreen(
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LangerScaffold(
        title = stringResource(R.string.tutorial_title),
        notificationViewModel = notificationViewModel,
        canNavigateBack = true,
        onBackClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Dimens.Standard),
            verticalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = Dimens.Standard)) {
                    Text(
                        text = stringResource(R.string.tutorial_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(availableTutorials, key = { it.id }) { tutorial ->
                val isAvailable = isVideoAvailable(context, tutorial.rawResourceName)
                VideoTutorialCard(
                    tutorial    = tutorial,
                    isAvailable = isAvailable,
                    onPlay      = {
                        if (isAvailable) {
                            playVideo(context, tutorial.rawResourceName)
                        }
                    }
                )
            }
        }
    }
}

// ─── VideoTutorialCard ────────────────────────────────────────────────────────
@Composable
private fun VideoTutorialCard(
    tutorial: VideoTutorial,
    isAvailable: Boolean,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.RadiusStandard),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAvailable) Dimens.BorderThin else Dimens.None)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Standard)
        ) {
            // Anteprima / placeholder
            Surface(
                modifier = Modifier.size(Dimens.ThumbnailSize),
                shape = RoundedCornerShape(Dimens.RadiusMedium),
                color = if (isAvailable)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            if (isAvailable) R.drawable.ic_camera
                            else R.drawable.ic_warning
                        ),
                        contentDescription = null,
                        tint = if (isAvailable)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                }
            }

            // Testo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(tutorial.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAvailable)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.ExtraSmall))
                Text(
                    text = stringResource(tutorial.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                if (!isAvailable) {
                    Spacer(Modifier.height(Dimens.ExtraSmall))
                    Text(
                        text = stringResource(R.string.tutorial_not_available),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Pulsante play
            FilledIconButton(
                onClick = onPlay,
                enabled = isAvailable,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera), // sostituire con ic_play
                    contentDescription = stringResource(R.string.tutorial_play),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
private fun isVideoAvailable(context: Context, rawName: String): Boolean {
    return try {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        resId != 0
    } catch (e: Exception) {
        false
    }
}

private fun playVideo(context: Context, rawName: String) {
    try {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        if (resId == 0) return
        val uri = "android.resource://${context.packageName}/$resId".toUri()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Il player non è disponibile — gestito dalla UI (card disabilitata)
    }
}