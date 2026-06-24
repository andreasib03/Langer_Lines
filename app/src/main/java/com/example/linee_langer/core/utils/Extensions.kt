package com.example.linee_langer.core.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.domain.models.LangerLine
import com.example.linee_langer.domain.models.TensionLevel
import com.example.linee_langer.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

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

fun TensionLevel.toStringRes(): Int = when (this) {
    TensionLevel.LOW    -> R.string.tension_low
    TensionLevel.MEDIUM -> R.string.tension_medium
    TensionLevel.HIGH   -> R.string.tension_high
}

fun TensionLevel.toContainerColor(colorScheme: ColorScheme, appColors: AppColors): Color =
    when (this) {
        TensionLevel.LOW    -> appColors.qualityHigh       // verde = bassa tensione = buono
        TensionLevel.MEDIUM -> appColors.qualityMedium     // arancione
        TensionLevel.HIGH   -> appColors.qualityLow        // rosso = alta tensione
    }