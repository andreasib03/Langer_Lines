package com.example.linee_langer.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.domain.models.TensionLevel
import com.example.linee_langer.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val dateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
}

fun Long.toDateString(): String = dateFormatter.get()!!.format(Date(this))

fun LangerLineEntity.toDomainModel(): LangerLine {
    return LangerLine(
        startX = this.startX,
        startY = this.startY,
        endX = this.endX,
        endY = this.endY,
        intensity = this.intensity
    )
}
fun LangerLine.toEntity(analysisId: Long = 0): LangerLineEntity {
    return LangerLineEntity(
        analysisId = analysisId,
        startX = this.startX,
        startY = this.startY,
        endX = this.endX,
        endY = this.endY,
        intensity = this.intensity
    )
}


@Composable
fun AnalysisWithLines.summaryText(): String {
    val lineCount = lines.size
    val tensionLevel = TensionLevel.fromLines(lines)
    val tensionText = stringResource(tensionLevel.toStringRes())
    return stringResource(R.string.last_analysis_summary, lineCount, tensionText)
}

fun TensionLevel.toStringRes(): Int = when (this) {
    TensionLevel.LOW    -> R.string.tension_low
    TensionLevel.MEDIUM -> R.string.tension_medium
    TensionLevel.HIGH   -> R.string.tension_high
}

fun TensionLevel.toContainerColor(appColors: AppColors): Color =
    when (this) {
        TensionLevel.LOW    -> appColors.qualityHigh       // verde = bassa tensione = buono
        TensionLevel.MEDIUM -> appColors.qualityMedium     // arancione
        TensionLevel.HIGH   -> appColors.qualityLow        // rosso = alta tensione
    }