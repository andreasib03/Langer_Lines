package com.example.linee_langer.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AnalysisWithLines(
    @Embedded
    val analysis: SkinAnalysisEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "analysisId"
    )
    val lines: List<LangerLineEntity>
)