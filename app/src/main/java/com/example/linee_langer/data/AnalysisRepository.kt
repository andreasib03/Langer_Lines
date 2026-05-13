package com.example.linee_langer.data

import android.util.Log
import com.example.linee_langer.dao.AnalysisDao
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.db.LangerLineEntity
import com.example.linee_langer.db.SkinAnalysisEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AnalysisRepository @Inject constructor(
    private val dao : AnalysisDao
) {
    val allAnalyses: Flow<List<AnalysisWithLines>> = dao.getAllAnalysesWithLines()
    val analysisCount: Flow<Int> = dao.getAnalysisCount()

    suspend fun saveFullAnalysis(analysis: SkinAnalysisEntry, lines: List<LangerLineEntity>){
        val generatedId = dao.insertAnalysis(analysis)
        val linesWithId = lines.map { it.copy( analysisId = generatedId ) }
        dao.insertLines(linesWithId)

        Log.d("REPO_DEBUG", "Salvate ${linesWithId.size} linee collegate all'ID $generatedId")
    }

    fun getAnalysisById(id:Long): Flow<AnalysisWithLines?>{
        return dao.getAnalysisWithLinesById(id)
    }

    // --- METODI PER SYNC WORKER (Cloud) ---

    suspend fun getUnsyncedAnalyses() = dao.getUnsyncedAnalyses()

    suspend fun updateSyncStatus(date: Long, status: Boolean){
        val analysis = dao.getAnalysisByTimestamp(date)
        analysis?.let {
            dao.updateSyncStatus(it.id, status)
        }
    }

    suspend fun getLastAnalysisDate() = dao.getLastAnalysisDate()

    // --- METODI PER RECOVERY WORKER (Immagini Locali) ---

    /**
     * Recupera tutte le analisi come lista semplice (non Flow)
     * per l'elaborazione rapida nel Worker di recupero immagini.
     */
    suspend fun getAllAnalysesInternal(): List<SkinAnalysisEntry> = dao.getAllAnalyses()

    /**
     * Aggiorna il percorso del file immagine se viene ritrovato nella cartella Pictures.
     */
    suspend fun updateImagePath(date: Long, newPath: String) {
        dao.updateImagePathByTimestamp(date, newPath)
    }

    // --- METODI DI CANCELLAZIONE ---
    suspend fun deleteFullAnalysis(analysis: SkinAnalysisEntry) {
        dao.deleteAnalysisEntry(analysis)
    }

    suspend fun deleteAllAnalysis(){
        dao.deleteAll()
    }


}