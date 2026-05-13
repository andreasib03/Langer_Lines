package com.example.linee_langer.ui.utils

import com.example.linee_langer.db.LangerLineEntity
import com.example.linee_langer.domain.models.LangerLine
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