package com.example.linee_langer.domain.models

/**
 * Represents a single tension line.
 * All coordinates are normalized (0.0 to 1.0).
 * (0,0) is Top-Left, (1,1) is Bottom-Right.
 */

data class LangerLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val intensity: Float = 0.1f,
    val label: String? = null

)
