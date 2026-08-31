package com.example.linee_langer.ui.feature.history.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.utils.toContainerColor
import com.example.linee_langer.core.utils.toDateString
import com.example.linee_langer.core.utils.toStringRes
import com.example.linee_langer.domain.models.TensionLevel
import com.example.linee_langer.ui.feature.camera.model.bodyPartLabel
import com.example.linee_langer.ui.theme.Dimens
import com.example.linee_langer.ui.theme.appColors

/**
 * Pannello informazioni tecniche di una scansione (sezione "analysis_info").
 *
 * Tutte le informazioni tecniche relative all'ultima scansione — livello di tensione,
 * numero di linee, intensità media e il JSON grezzo salvato a fini di sincronizzazione —
 * vivono esclusivamente qui. La Home mostra solo un riepilogo discorsivo e amichevole
 * (vedi [com.example.linee_langer.ui.feature.home.HomeScreen]); questi dettagli non
 * vengono duplicati altrove nell'app.
 */
@Composable
fun AnalysisInfoPanel(data: AnalysisWithLines) {

    val tensionLevel = remember(data.lines) {
        TensionLevel.fromLines(data.lines)
    }
    val tensionText = stringResource(tensionLevel.toStringRes())
    val tensionColor = tensionLevel.toContainerColor(
        MaterialTheme.appColors
    )

    val lineCount = data.lines.size
    val avgIntensityPercent = remember(data.lines) {
        if (data.lines.isEmpty()) 0
        else (data.lines.map { it.intensity }.average() * 100).toInt()
    }
    val bodyLabel = bodyPartLabel(data.analysis.bodyPartId)

    var showTechnicalDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.XLarge)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = bodyLabel.uppercase(),
                    style = typography.labelLarge,
                    color = MaterialTheme.appColors.cameraOverlayTextStrong
                )
                Text(
                    text = data.analysis.date.toDateString(),
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.appColors.cameraOverlayTextStrong
                )
            }

            // Badge intensity media
            Surface(
                color = tensionColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(Dimens.RadiusMedium)
            ) {
                Text(
                    text = tensionText,
                    modifier = Modifier.padding(horizontal = Dimens.Medium, Dimens.ExtraSmall),
                    style = typography.labelMedium,
                    color = tensionColor
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Large))

        // Card riassuntiva — riepilogo testuale leggibile
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.cameraOverlayBgMedium),
            shape = RoundedCornerShape(Dimens.Standard)
        ) {
            Row(modifier = Modifier.padding(Dimens.Standard), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.starColor,
                    modifier = Modifier.size(Dimens.XLarge)
                )
                Spacer(modifier = Modifier.width(Dimens.Medium))
                Text(
                    text = stringResource(R.string.analysis_info_summary, lineCount, bodyLabel),
                    color = MaterialTheme.appColors.cameraOverlayText,
                    style = typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Standard))

        // Sezione dettagli tecnici — espandibile, qui e solo qui vivono i dati "grezzi"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTechnicalDetails = !showTechnicalDetails },
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
            shape = RoundedCornerShape(Dimens.Standard)
        ) {
            Column(modifier = Modifier.padding(Dimens.Standard)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.analysis_info_technical_title),
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
                    val rotation by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showTechnicalDetails) 90f else 0f,
                        label = "chevronRotation"
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(Dimens.IconSmall)
                            .graphicsLayer { rotationZ = 180f + rotation }
                    )
                }

                AnimatedVisibility(visible = showTechnicalDetails) {
                    Column(modifier = Modifier.padding(top = Dimens.Standard)) {
                        TechnicalInfoRow(
                            label = stringResource(R.string.analysis_info_lines_count),
                            value = lineCount.toString()
                        )
                        TechnicalInfoRow(
                            label = stringResource(R.string.analysis_info_avg_intensity),
                            value = "$avgIntensityPercent%"
                        )
                        TechnicalInfoRow(
                            label = stringResource(R.string.analysis_info_tension_level),
                            value = tensionText
                        )
                        TechnicalInfoRow(
                            label = stringResource(R.string.analysis_info_body_part),
                            value = bodyLabel
                        )
                        TechnicalInfoRow(
                            label = stringResource(R.string.detail_synced),
                            value = when {
                                data.analysis.isSynced -> stringResource(R.string.detail_synced_yes)
                                data.analysis.syncFailed -> stringResource(R.string.detail_synced_error)
                                else -> stringResource(R.string.detail_synced_pending)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicalInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.ExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
        Text(text = value, style = typography.bodySmall, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
    }
}

