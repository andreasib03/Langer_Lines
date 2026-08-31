package com.example.linee_langer.data.local

import androidx.room.Transaction
import com.example.linee_langer.core.database.dao.AnalysisDao
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AnalysisRepository @Inject constructor(
    private val dao : AnalysisDao
) {
    val allAnalyses: Flow<List<AnalysisWithLines>> = dao.getAllAnalysesWithLines()
    val analysisCount: Flow<Int> = dao.getAnalysisCount()

    @Transaction
    suspend fun saveFullAnalysis(analysis: SkinAnalysisEntity, lines: List<LangerLineEntity>){
        val generatedId = dao.insertAnalysis(analysis)
        val linesWithId = lines.map { it.copy( analysisId = generatedId ) }
        dao.insertLines(linesWithId)
    }

    fun getAnalysisById(id:Long): Flow<AnalysisWithLines?> {
        return dao.getAnalysisWithLinesById(id)
    }

    // --- METODI PER SYNC WORKER (Cloud) ---

    suspend fun getUnsyncedAnalyses() = dao.getUnsyncedAnalyses()

    suspend fun getPermanentlyFailedAnalyses() = dao.getPermanentlyFailedAnalyses()

    suspend fun updateSyncStatus(id: Long, status: Boolean){
        dao.updateSyncStatus(id, status)
    }

    suspend fun updateSyncFailed(id: Long, failed: Boolean) {
        dao.updateSyncFailed(id, failed)
    }

    suspend fun getLastAnalysisDate() = dao.getLastAnalysisDate()

    // --- METODI PER RECOVERY WORKER ---

    /**
     * Recupera tutte le analisi come lista semplice (non Flow)
     * per l'elaborazione rapida nel Worker di recupero immagini.
     */
    suspend fun getAllAnalysesInternal(): List<SkinAnalysisEntity> = dao.getAllAnalyses()

    /**
     * Aggiorna il percorso del file immagine se viene ritrovato nella cartella Pictures.
     */
    suspend fun updateImagePath(id: Long, newPath: String) {
        dao.updateImagePath(id, newPath)
    }

    suspend fun updateImagePathByTimestamp(timestamp: Long, newPath: String) {
        dao.updateImagePathByTimestamp(timestamp, newPath)
    }


    // --- METODI DI CANCELLAZIONE ---
    suspend fun deleteFullAnalysis(analysis: SkinAnalysisEntity) {
        dao.deleteAnalysisEntry(analysis)
    }

    suspend fun deleteAllAnalysis(){
        dao.deleteAll()
    }

    suspend fun restoreFullAnalysis(analysisWithLines: AnalysisWithLines) {
        val restoredId = dao.insertAnalysis(analysisWithLines.analysis)
        val lines = analysisWithLines.lines.map { it.copy(analysisId = restoredId) }
        dao.insertLines(lines)
    }


}