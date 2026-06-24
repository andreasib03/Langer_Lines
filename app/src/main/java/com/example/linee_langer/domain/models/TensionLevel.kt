package com.example.linee_langer.domain.models

import com.example.linee_langer.core.database.entity.LangerLineEntity

enum class TensionLevel {
    LOW, MEDIUM, HIGH;

    companion object {
        fun fromLines(lines: List<LangerLineEntity>): TensionLevel {
            if (lines.isEmpty()) return LOW
            val avgIntensity = lines.map { it.intensity }.average().toFloat()
            return when {
                avgIntensity >= 0.65f -> HIGH
                avgIntensity >= 0.45f -> MEDIUM
                else                  -> LOW
            }
        }

        fun fromAvgIntensity(avg: Float): String = when {
            avg >= 0.65f -> "HIGH"
            avg >= 0.45f -> "MEDIUM"
            else         -> "LOW"
        }
    }
}
