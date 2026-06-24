package com.example.linee_langer.ui.feature.camera.utils

import com.example.linee_langer.domain.models.LangerLine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot

@Singleton
class LineStabilizer @Inject constructor() {

    /**
     * Stabilizza le linee combinando i frame consecutivi per vicinanza geometrica.
     * Risolve il flickering e lo sdoppiamento visivo delle linee di Langer.
     *
     * @param oldLines  Linee del frame precedente
     * @param newLines  Linee del frame corrente
     * @param alpha     Fattore di smoothing (0=nessuna risposta, 1=nessuno smoothing)
     * @param threshold Distanza massima in pixel per abbinare linee tra frame
     */
    fun smooth(
        oldLines: List<LangerLine>,
        newLines: List<LangerLine>,
        alpha: Float = 0.4f,
        threshold: Float = 100f
    ): List<LangerLine> {
        if (oldLines.isEmpty()) return newLines

        return newLines.map { newLine ->
            val newMidX = (newLine.startX + newLine.endX) / 2f
            val newMidY = (newLine.startY + newLine.endY) / 2f

            val closest = oldLines.minByOrNull { oldLine ->
                val oldMidX = (oldLine.startX + oldLine.endX) / 2f
                val oldMidY = (oldLine.startY + oldLine.endY) / 2f
                hypot((newMidX - oldMidX).toDouble(), (newMidY - oldMidY).toDouble())
            }

            val distance = closest?.let {
                val oldMidX = (it.startX + it.endX) / 2f
                val oldMidY = (it.startY + it.endY) / 2f
                hypot((newMidX - oldMidX).toDouble(), (newMidY - oldMidY).toDouble())
            } ?: Double.MAX_VALUE

            if (distance < threshold && closest != null) {
                newLine.copy(
                    startX = closest.startX + alpha * (newLine.startX - closest.startX),
                    startY = closest.startY + alpha * (newLine.startY - closest.startY),
                    endX   = closest.endX   + alpha * (newLine.endX   - closest.endX),
                    endY   = closest.endY   + alpha * (newLine.endY   - closest.endY)
                )
            } else {
                newLine
            }
        }
    }
}