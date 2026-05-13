package com.example.linee_langer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.linee_langer.db.LangerLineEntity
import com.example.linee_langer.db.SkinAnalysisEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnalysis(analysis: SkinAnalysisEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<LangerLineEntity>)

    @Transaction
    @Query("SELECT * FROM skin_analyses ORDER BY date DESC")
    fun getAllAnalysesWithLines(): Flow<List<AnalysisWithLines>>

    @Query("SELECT COUNT(*) FROM skin_analyses")
    fun getAnalysisCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM skin_analyses WHERE id = :id")
    fun getAnalysisWithLinesById(id: Long): Flow<AnalysisWithLines?>

    @Delete
    suspend fun deleteAnalysisEntry(analysisId: SkinAnalysisEntry)

    @Query("DELETE FROM skin_analyses WHERE id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: Long)
    @Query("DELETE FROM skin_analyses")
    suspend fun deleteAll()

    @Query("SELECT * FROM skin_analyses WHERE isSynced = 0")
    suspend fun getUnsyncedAnalyses(): List<SkinAnalysisEntry>

    @Query("UPDATE skin_analyses SET imagePath = :newPath WHERE id = :analysisId")
    suspend fun updateImagePath(analysisId: Long, newPath: String)

    // Query per trovare analisi che hanno il database ma non l'immagine (path rotto)
    @Query("SELECT * FROM skin_analyses ORDER BY date DESC")
    suspend fun getAllAnalyses(): List<SkinAnalysisEntry>

    @Query("UPDATE skin_analyses SET isSynced = :status WHERE id = :analysisId")
    suspend fun updateSyncStatus(analysisId: Long, status: Boolean)

    @Query("SELECT MAX(date) FROM skin_analyses")
    suspend fun getLastAnalysisDate(): Long?

}

data class AnalysisWithLines(
    @Embedded
    val analysis: SkinAnalysisEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "analysisId"
    )
    val lines: List<LangerLineEntity>
)