package com.example.linee_langer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "langer_lines",
    foreignKeys = [
        ForeignKey(
            entity = SkinAnalysisEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE // eliminate analysis with lines
        )
    ],
    indices = [Index(value = ["analysisId"])]
)

data class LangerLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val analysisId: Long = 0,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val intensity: Float
)