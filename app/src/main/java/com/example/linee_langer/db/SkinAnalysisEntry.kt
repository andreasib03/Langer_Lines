package com.example.linee_langer.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Locale

@Entity(
    tableName = "skin_analyses",
    indices = [Index(value = ["date"])])
data class SkinAnalysisEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val bodyPartId: String,
    val imagePath: String,
    val resultSummary: String,
    val isSynced: Boolean = false
)



@Entity(
    tableName = "langer_lines",
    foreignKeys = [
        ForeignKey(
            entity = SkinAnalysisEntry::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE // eliminate analysis with lines
        )
    ],
    indices = [Index(value = ["analysisId"])]
)



// data class for every line
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

val SkinAnalysisEntry.dateFormatted: String
    get() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(java.util.Date(this.date))
    }


