package com.example.linee_langer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: SkinAnalysisEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<LangerLineEntity>)

    @Transaction
    @Query("SELECT * FROM skin_analyses WHERE date = :timestamp LIMIT 1")
    suspend fun getAnalysisByTimestamp(timestamp: Long): SkinAnalysisEntity?

    @Query("UPDATE skin_analyses SET imagePath = :newPath WHERE date = :timestamp")
    suspend fun updateImagePathByTimestamp(timestamp: Long, newPath: String)
    @Transaction
    @Query("SELECT * FROM skin_analyses ORDER BY date DESC")
    fun getAllAnalysesWithLines(): Flow<List<AnalysisWithLines>>

    @Query("SELECT COUNT(*) FROM skin_analyses")
    fun getAnalysisCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM skin_analyses WHERE id = :id")
    fun getAnalysisWithLinesById(id: Long): Flow<AnalysisWithLines?>

    @Delete
    suspend fun deleteAnalysisEntry(analysisId: SkinAnalysisEntity)

    @Query("DELETE FROM skin_analyses WHERE id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: Long)
    @Query("DELETE FROM skin_analyses")
    suspend fun deleteAll()

    @Query("SELECT * FROM skin_analyses WHERE isSynced = 0")
    suspend fun getUnsyncedAnalyses(): List<SkinAnalysisEntity>

    @Query("UPDATE skin_analyses SET imagePath = :newPath WHERE id = :analysisId")
    suspend fun updateImagePath(analysisId: Long, newPath: String)

    @Query("SELECT * FROM skin_analyses ORDER BY date DESC")
    suspend fun getAllAnalyses(): List<SkinAnalysisEntity>

    @Query("UPDATE skin_analyses SET isSynced = :status WHERE id = :analysisId")
    suspend fun updateSyncStatus(analysisId: Long, status: Boolean)

    @Query("SELECT MAX(date) FROM skin_analyses")
    suspend fun getLastAnalysisDate(): Long?


}

