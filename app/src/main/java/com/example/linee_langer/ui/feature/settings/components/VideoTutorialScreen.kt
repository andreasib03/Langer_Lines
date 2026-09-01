package com.example.linee_langer.ui.feature.settings.components

import android.widget.VideoView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.linee_langer.R
import com.example.linee_langer.ui.shared.components.LangerScaffold
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import androidx.core.net.toUri
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.VideoPlayerBg
import com.example.linee_langer.ui.theme.VideoPlayerControlBg
import com.example.linee_langer.ui.theme.VideoPlayerIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalContext

// ─── Modello ─────────────────────────────────────────────────────────────────
data class VideoTutorial(
    val id: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val rawRes: Int?,           // riferimento diretto a R.raw.* — null se il video non è ancora disponibile
    @param:DrawableRes val iconRes: Int,  // icona mostrata nel thumbnail della card
)

// ─── Lista tutorial disponibili ───────────────────────────────────────────────
// Aggiungere nuovi video qui — il resto si aggiorna automaticamente.
val availableTutorials = listOf(
    VideoTutorial(
        id             = 1,
        titleRes       = R.string.tutorial_intro_title,
        descriptionRes = R.string.tutorial_intro_desc,
        rawRes         = R.raw.video_2,
        iconRes        = R.drawable.ic_introduction        // icona "introduzione all'app"
    ),
    VideoTutorial(
        id             = 2,
        titleRes       = R.string.tutorial_camera_title,
        descriptionRes = R.string.tutorial_camera_desc,
        rawRes         = R.raw.video_1,
        iconRes        = R.drawable.ic_camera_tutorial       // icona "come usare la fotocamera"
    ),
    VideoTutorial(
        id             = 3,
        titleRes       = R.string.tutorial_analysis_title,
        descriptionRes = R.string.tutorial_analysis_desc,
        rawRes         = R.raw.video_3,
        iconRes        = R.drawable.ic_instruction_analysis      // icona "interpretare i risultati"
    ),
    VideoTutorial(
        id = 4,
        titleRes = R.string.tutorial_onboarding_title,
        descriptionRes = R.string.tutorial_onboarding_desc,
        rawRes = R.raw.video_4,
        iconRes = R.drawable.ic_onboarding
    )
)

// ─── VideoTutorialScreen ──────────────────────────────────────────────────────
@Composable
fun VideoTutorialScreen(
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    var playingTutorialId by rememberSaveable { mutableStateOf<Int?>(null) }
    val playingTutorial = remember(playingTutorialId){
        availableTutorials.find { it.id == playingTutorialId }
    }

    playingTutorial?.let { tutorial ->
        if (tutorial.rawRes != null) {
            VideoPlayerDialog(
                rawRes = tutorial.rawRes,
                onDismiss = { playingTutorialId = null }
            )
        }
    }

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
                val isAvailable = tutorial.rawRes != null
                VideoTutorialCard(
                    tutorial    = tutorial,
                    isAvailable = isAvailable,
                    onPlay      = {
                        if (isAvailable) {
                            playingTutorialId = tutorial.id
                        }
                    }
                )
            }
        }
    }
}
private fun formatTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ─── Player in-app (VideoView a schermo intero) ───────────────────────────────
@Composable
private fun VideoPlayerDialog(
    rawRes: Int,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var playbackPosition by rememberSaveable { mutableIntStateOf(0) } // sopravvive a config change/process death

    // Polling della posizione: attivo solo mentre il video è in riproduzione.
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                videoView?.let { currentPosition = it.currentPosition }
                delay(500.milliseconds)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoView?.apply {
                playbackPosition = currentPosition
                stopPlayback()
                setOnPreparedListener(null)
                setOnCompletionListener(null)
                setOnErrorListener(null)
            }
            videoView = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoPlayerBg),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    VideoView(context).apply {
                        videoView = this
                        val uri = "android.resource://${context.packageName}/$rawRes".toUri()
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            duration = mp.duration
                            if (playbackPosition > 0) seekTo(playbackPosition)
                            start()
                            isPlaying = true
                        }
                        setOnErrorListener { _, _, _ ->
                            onDismiss()
                            true
                        }
                    }
                }
            )

            FilledIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.Standard),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = VideoPlayerControlBg
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                    tint = VideoPlayerIcon
                )
            }

            // ─── Barra controlli ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(VideoPlayerControlBg)
                    .padding(horizontal = Dimens.Standard, vertical = Dimens.Small)
            ) {
                Slider(
                    value = currentPosition.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    onValueChange = { newValue ->
                        // durante il trascinamento aggiorna solo la UI, non il player
                        currentPosition = newValue.toInt()
                    },
                    onValueChangeFinished = {
                        videoView?.seekTo(currentPosition)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VideoPlayerIcon
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Small)) {
                        IconButton(onClick = {
                            videoView?.let {
                                it.seekTo(0)
                                it.pause()
                                currentPosition = 0
                                isPlaying = false
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stop),
                                contentDescription = stringResource(R.string.stop),
                                tint = VideoPlayerIcon
                            )
                        }

                        IconButton(onClick = {
                            videoView?.let {
                                if (isPlaying) it.pause() else it.start()
                                isPlaying = !isPlaying
                            }
                        }) {
                            Icon(
                                painter = if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play),
                                contentDescription = stringResource(
                                    if (isPlaying) R.string.pause else R.string.play
                                ),
                                tint = VideoPlayerIcon
                            )
                        }
                    }
                }
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

    val icon = if(isAvailable) tutorial.iconRes else R.drawable.ic_warning
    Card(
        onClick = onPlay,
        enabled = isAvailable,
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
                        painter = painterResource(icon),
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

            // Indicatore play (decorativo — l'intera card è cliccabile)
            Surface(
                shape = RoundedCornerShape(Dimens.RadiusXLarge),
                color = if (isAvailable)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play), // sostituire con ic_play
                    contentDescription = null,
                    tint = if (isAvailable)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Dimens.Small)
                )
            }
        }
    }
}