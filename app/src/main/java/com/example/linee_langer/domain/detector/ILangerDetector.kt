package com.example.linee_langer.domain.detector

import android.graphics.Bitmap
import com.example.linee_langer.domain.models.LangerLine

interface ILangerDetector {
    val isAvailable: Boolean
    fun detectLines(
        bitmap: Bitmap,
        sensitivity: Float = 0.5f,
        partId: String = "face"
    ): List<LangerLine>
}