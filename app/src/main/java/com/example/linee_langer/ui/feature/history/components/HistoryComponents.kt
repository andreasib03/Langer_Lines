package com.example.linee_langer.ui.feature.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.dateFormatted
import com.example.linee_langer.core.database.entity.timeFormatted
import com.example.linee_langer.ui.theme.Dimens

@Composable
    fun HistoryCard(
        analysis: AnalysisWithLines,
        progressiveNumber: Int,
        onClick: () -> Unit
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(Dimens.RadiusXLarge),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(Dimens.BorderThin, colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(Dimens.Standard),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image Preview with corner border
                Box(modifier = Modifier.size(Dimens.ThumbnailSize)) {
                    AsyncImage(
                        model = analysis.analysis.imagePath,
                        contentDescription = stringResource(R.string.history_image_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(Dimens.RadiusStandard)),
                        contentScale = ContentScale.Crop
                    )
                    // Badge della parte del corpo (es: Face)
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.ExtraSmall),
                        shape = CircleShape,
                        color = colorScheme.primary,
                        tonalElevation = Dimens.CardElevation
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star), // O Icon parte corpo
                            contentDescription = "",
                            modifier = Modifier.size(Dimens.IconXSmall).padding(Dimens.ExtraSmall),
                            tint = colorScheme.onError
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Dimens.Standard))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.history_item_title,
                            progressiveNumber,
                            analysis.analysis.timeFormatted
                        ),
                        style = typography.titleMedium,
                        color = colorScheme.primary
                    )

                    Text(
                        text = analysis.analysis.dateFormatted,
                        style = typography.bodySmall,
                        color = colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.detected_lines, analysis.lines.size),
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "",
                    modifier = Modifier.size(Dimens.IconMedium).rotate(180f),
                    tint = colorScheme.outline
                )
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableHistoryCard(
    analysis: AnalysisWithLines,
    progressiveNumber: Int,
    onDelete: (AnalysisWithLines) -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * .4f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if(dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd){
            onDelete(analysis)
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        // This defines what shows up BEHIND the card while swiping
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> colorScheme.errorContainer.copy(alpha = 0.8f)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Dimens.RadiusXLarge)) // Match your card shape
                    .background(color)
                    .padding(horizontal = Dimens.XLarge),
                contentAlignment = Alignment.CenterStart
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        painterResource(R.drawable.ic_trash),
                        contentDescription = stringResource(R.string.delete_yes),
                        tint = colorScheme.onError
                    )
                }
            }
        },
        enableDismissFromEndToStart = false, // Disable right-to-left swipe
        content = {
            HistoryCard(
                analysis = analysis,
                progressiveNumber = progressiveNumber,
                onClick = onClick
            )
        }
    )
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_home),
            contentDescription = "",
            modifier = Modifier.size(Dimens.IconSuper),
            tint = colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(Dimens.Standard))
        Text(
            stringResource(R.string.noanalysis),
            style = typography.titleMedium,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.show_analysis),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
