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
    @Query("SELECT * FROM skin_analyses WHERE date = :timestamp AND userId = :userId LIMIT 1")
    suspend fun getAnalysisByTimestamp(timestamp: Long, userId: String): SkinAnalysisEntity?

    @Query("UPDATE skin_analyses SET imagePath = :newPath WHERE date = :timestamp AND userId = :userId")
    suspend fun updateImagePathByTimestamp(timestamp: Long, newPath: String, userId: String)

    @Transaction
    @Query("SELECT * FROM skin_analyses WHERE userId = :userId ORDER BY date DESC")
    fun getAllAnalysesWithLines(userId: String): Flow<List<AnalysisWithLines>>

    @Query("SELECT COUNT(*) FROM skin_analyses WHERE userId = :userId")
    fun getAnalysisCount(userId: String): Flow<Int>

    @Transaction
    @Query("SELECT * FROM skin_analyses WHERE id = :id AND userId = :userId")
    fun getAnalysisWithLinesById(id: Long, userId: String): Flow<AnalysisWithLines?>

    @Delete
    suspend fun deleteAnalysisEntry(analysisId: SkinAnalysisEntity)

    @Query("DELETE FROM skin_analyses WHERE id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: Long)
    @Query("DELETE FROM skin_analyses")
    suspend fun deleteAll()

    @Query("DELETE FROM skin_analyses WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM skin_analyses WHERE isSynced = 1 AND userId = :userId")
    suspend fun deleteSyncedAnalyses(userId: String)

    @Query("SELECT COUNT(*) FROM skin_analyses WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedCount(userId: String): Int

    @Query("SELECT * FROM skin_analyses WHERE isSynced = 0 AND syncFailed = 0 AND userId = :userId")
    suspend fun getUnsyncedAnalyses(userId: String): List<SkinAnalysisEntity>

    @Query("SELECT * FROM skin_analyses WHERE isSynced = 0 AND syncFailed = 1 AND userId = :userId")
    suspend fun getPermanentlyFailedAnalyses(userId: String): List<SkinAnalysisEntity>

    @Query("UPDATE skin_analyses SET syncFailed = :failed WHERE id = :analysisId")
    suspend fun updateSyncFailed(analysisId: Long, failed: Boolean)

    @Query("UPDATE skin_analyses SET imagePath = :newPath WHERE id = :analysisId")
    suspend fun updateImagePath(analysisId: Long, newPath: String)

    @Query("SELECT * FROM skin_analyses WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllAnalyses(userId: String): List<SkinAnalysisEntity>

    @Query("UPDATE skin_analyses SET isSynced = :status WHERE id = :analysisId")
    suspend fun updateSyncStatus(analysisId: Long, status: Boolean)

    @Query("SELECT MAX(date) FROM skin_analyses WHERE userId = :userId")
    suspend fun getLastAnalysisDate(userId: String): Long?

    @Query("SELECT COUNT(*) FROM skin_analyses WHERE userId = ''")
    suspend fun getLegacyUnassignedCount(): Int

    @Query("UPDATE skin_analyses SET userId = :userId WHERE userId = ''")
    suspend fun assignLegacyRowsToUser(userId: String)


}

