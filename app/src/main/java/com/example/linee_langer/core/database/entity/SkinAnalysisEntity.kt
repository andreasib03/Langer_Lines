package com.example.linee_langer.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "skin_analyses",
    indices = [Index(value = ["date"])])
data class SkinAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val bodyPartId: String,
    val imagePath: String,
    val resultSummary: String,
    val isSynced: Boolean = false
)



val SkinAnalysisEntity.dateFormatted: String
    get() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(this.date))
    }

val SkinAnalysisEntity.timeFormatted: String
    get() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(this.date))
    }

