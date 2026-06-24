package com.example.linee_langer.domain.detector

import android.graphics.Bitmap
import com.example.linee_langer.domain.models.LangerLine

class FakeLangerDetector : ILangerDetector {
    override val isAvailable: Boolean = true

    override fun detectLines(bitmap: Bitmap, sensitivity: Float, partId: String): List<LangerLine> {
        // Restituisci dati predefiniti per il test senza calcoli reali
        return listOf(LangerLine(0.1f, 0.1f, 0.2f, 0.2f, 0.8f))
    }
}