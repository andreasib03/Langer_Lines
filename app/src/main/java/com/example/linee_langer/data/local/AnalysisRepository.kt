package com.example.linee_langer.data.local

import androidx.room.Transaction
import com.example.linee_langer.core.database.dao.AnalysisDao
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class AnalysisRepository @Inject constructor(
    private val dao : AnalysisDao,
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allAnalyses: Flow<List<AnalysisWithLines>> = authRepository.currentUserFlow.flatMapLatest { user ->
        val uid = user?.uid
        if (uid.isNullOrBlank()) flowOf(emptyList()) else dao.getAllAnalysesWithLines(uid)
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val analysisCount: Flow<Int> = authRepository.currentUserFlow.flatMapLatest { user ->
        val uid = user?.uid
        if (uid.isNullOrBlank()) flowOf(0) else dao.getAnalysisCount(uid)
    }

    @Transaction
    suspend fun saveFullAnalysis(analysis: SkinAnalysisEntity, lines: List<LangerLineEntity>){
        val generatedId = dao.insertAnalysis(analysis)
        val linesWithId = lines.map { it.copy( analysisId = generatedId ) }
        dao.insertLines(linesWithId)
    }

    suspend fun resetFailedAnalyses(uid: String) {
        val failedList = dao.getPermanentlyFailedAnalyses(uid)
        failedList.forEach { analysis ->
            dao.updateSyncFailed(analysis.id, false)
        }
    }

    suspend fun getAnalysisByTimestamp(timestamp: Long, uid: String): SkinAnalysisEntity? {
        return dao.getAnalysisByTimestamp(timestamp, uid)
    }

    fun getAnalysisById(id:Long): Flow<AnalysisWithLines?> {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        return authRepository.currentUserFlow.flatMapLatest { user ->
            val uid = user?.uid
            if (uid.isNullOrBlank()) flowOf(null) else dao.getAnalysisWithLinesById(id, uid)
        }
    }

    suspend fun getAnalysisWithLinesSuspend(id: Long, uid: String): AnalysisWithLines? {
        return dao.getAnalysisWithLinesByIdSuspend(id, uid)
    }

    // --- METODI PER SYNC WORKER (Cloud) ---

    suspend fun getUnsyncedAnalyses(uid: String) = dao.getUnsyncedAnalyses(uid)

    suspend fun getPermanentlyFailedAnalyses(uid: String) = dao.getPermanentlyFailedAnalyses(uid)

    suspend fun updateSyncStatus(id: Long, status: Boolean){
        dao.updateSyncStatus(id, status)
    }

    suspend fun updateSyncFailed(id: Long, failed: Boolean) {
        dao.updateSyncFailed(id, failed)
    }

    suspend fun getLastAnalysisDate(uid: String) = dao.getLastAnalysisDate(uid)

    suspend fun getUnsyncedCount(uid: String): Int = dao.getUnsyncedCount(uid)


    // --- METODI PER RECOVERY WORKER ---

    /**
     * Recupera tutte le analisi come lista semplice (non Flow)
     * per l'elaborazione rapida nel Worker di recupero immagini.
     */
    suspend fun getAllAnalysesInternal(uid: String): List<SkinAnalysisEntity> = dao.getAllAnalyses(uid)

    /**
     * Aggiorna il percorso del file immagine se viene ritrovato nella cartella Pictures.
     */
    suspend fun updateImagePath(id: Long, newPath: String) {
        dao.updateImagePath(id, newPath)
    }

    suspend fun updateImagePathByTimestamp(timestamp: Long, newPath: String, uid: String) {
        dao.updateImagePathByTimestamp(timestamp, newPath, uid)
    }


    // --- METODI DI CANCELLAZIONE ---
    suspend fun deleteFullAnalysis(analysis: SkinAnalysisEntity) {
        val uid = authRepository.currentUser?.uid
        dao.deleteAnalysisEntry(analysis)
        
        // Cancellazione remota per mantenere i database allineati
        if (uid != null && analysis.isSynced) {
            firebaseRepository.deleteAnalysisDocument(uid, analysis.date)
        }
    }

    suspend fun deleteSyncedAnalysis(uid: String){
        dao.deleteSyncedAnalyses(uid)
    }

    suspend fun deleteAllAnalysisForUser(uid: String){
        dao.deleteAllForUser(uid)
    }

    suspend fun restoreFullAnalysis(analysisWithLines: AnalysisWithLines) {
        val restoredId = dao.insertAnalysis(analysisWithLines.analysis)
        val lines = analysisWithLines.lines.map { it.copy(analysisId = restoredId) }
        dao.insertLines(lines)
    }

    suspend fun getLegacyUnassignedCount(): Int = dao.getLegacyUnassignedCount()

    suspend fun assignLegacyRowsToUser(uid: String) = dao.assignLegacyRowsToUser(uid)


}