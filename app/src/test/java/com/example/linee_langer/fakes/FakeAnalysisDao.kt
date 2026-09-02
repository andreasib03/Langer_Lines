package com.example.linee_langer.fakes

import com.example.linee_langer.core.database.dao.AnalysisDao
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Implementazione in-memory di [AnalysisDao] per unit test JVM veloci, senza bisogno
 * di Room/Robolectric/istrumented tests. Riproduce a mano la semantica delle query SQL
 * reali (in particolare il filtro `isSynced = 0 AND syncFailed = 0` di
 * [getUnsyncedAnalyses], che è al centro della regressione corretta in questo PR).
 *
 * NOTA IMPORTANTE (onestà sulla copertura): questo fake NON verifica che le annotazioni
 * @Query di AnalysisDao.kt siano scritte in SQL valido, né che Room le compili
 * correttamente: quella verifica richiede un test strumentato con Room in-memory
 * (androidTest) o Robolectric, che non è incluso in questo pacchetto perché il progetto
 * fornito non include i file Gradle necessari a determinare quali dipendenze di test
 * sono già disponibili. Vedi il file "test-dependencies-note.md" per i dettagli.
 */
class FakeAnalysisDao : AnalysisDao {

    private var nextId = 1L
    private val analyses = linkedMapOf<Long, SkinAnalysisEntity>()
    private val linesByAnalysisId = linkedMapOf<Long, MutableList<LangerLineEntity>>()

    private val allAnalysesFlow = MutableStateFlow<List<AnalysisWithLines>>(emptyList())
    private val countFlow = MutableStateFlow(0)
    private val byIdFlows = mutableMapOf<Long, MutableStateFlow<AnalysisWithLines?>>()

    override suspend fun insertAnalysis(analysis: SkinAnalysisEntity): Long {
        val id = if (analysis.id != 0L) analysis.id else nextId++
        analyses[id] = analysis.copy(id = id)
        if (id >= nextId) nextId = id + 1
        emitState()
        return id
    }

    override suspend fun insertLines(lines: List<LangerLineEntity>) {
        lines.forEach { line ->
            linesByAnalysisId.getOrPut(line.analysisId) { mutableListOf() }.add(line)
        }
        emitState()
    }

    override suspend fun getAnalysisByTimestamp(timestamp: Long, userId: String): SkinAnalysisEntity? =
        analyses.values.firstOrNull { it.date == timestamp && it.userId == userId}

    override suspend fun updateImagePathByTimestamp(timestamp: Long, newPath: String, userId: String) {
        val match = analyses.values
            .firstOrNull { it.date == timestamp && it.userId == userId } ?: return
        analyses[match.id] = match.copy(imagePath = newPath)
        emitState()
    }

    override fun getAllAnalysesWithLines(userId: String): Flow<List<AnalysisWithLines>> =
        allAnalysesFlow

    override fun getAnalysisCount(userId: String): Flow<Int> = countFlow

    override fun getAnalysisWithLinesById(id: Long, userId: String): Flow<AnalysisWithLines?> =
        byIdFlows.getOrPut(id) {
            MutableStateFlow(toAnalysisWithLines(analyses[id]?.takeIf { it.userId == userId })) }

    override suspend fun getAnalysisWithLinesByIdSuspend(id: Long, userId: String): AnalysisWithLines? =
        toAnalysisWithLines(
            analyses[id]?.takeIf { it.userId == userId }
        )


    override suspend fun deleteAnalysisEntry(analysisId: SkinAnalysisEntity) {
        analyses.remove(analysisId.id)
        linesByAnalysisId.remove(analysisId.id)
        emitState()
    }

    override suspend fun deleteAnalysisById(analysisId: Long) {
        analyses.remove(analysisId)
        linesByAnalysisId.remove(analysisId)
        emitState()
    }

    override suspend fun deleteAll() {
        analyses.clear()
        linesByAnalysisId.clear()
        emitState()
    }

    override suspend fun deleteAllForUser(userId: String) {
        val toRemove = analyses.values
            .filter { it.userId == userId }
            .map { it.id }
        toRemove.forEach {
            analyses.remove(it)
            linesByAnalysisId.remove(it)
        }
        emitState()
    }

    override suspend fun getUnsyncedAnalyses(userId: String): List<SkinAnalysisEntity> =
        analyses.values.filter {
            it.userId == userId &&
            !it.isSynced && !it.syncFailed }

    override suspend fun getPermanentlyFailedAnalyses(userId: String): List<SkinAnalysisEntity> =
        analyses.values.filter {
            it.userId == userId &&
            !it.isSynced && it.syncFailed }

    override suspend fun updateSyncFailed(analysisId: Long, failed: Boolean) {
        val current = analyses[analysisId] ?: return
        analyses[analysisId] = current.copy(syncFailed = failed)
        emitState()
    }

    override suspend fun updateImagePath(analysisId: Long, newPath: String) {
        val current = analyses[analysisId] ?: return
        analyses[analysisId] = current.copy(imagePath = newPath)
        emitState()
    }

    override suspend fun getAllAnalyses(userId: String): List<SkinAnalysisEntity> =
        analyses.values
            .filter { it.userId == userId }
            .sortedByDescending { it.date }

    override suspend fun updateSyncStatus(analysisId: Long, status: Boolean) {
        val current = analyses[analysisId] ?: return
        analyses[analysisId] = current.copy(isSynced = status)
        emitState()
    }

    override suspend fun getLastAnalysisDate(userId: String): Long? =
        analyses.values
            .filter { it.userId == userId }
            .maxOfOrNull { it.date }

    override suspend fun getLegacyUnassignedCount(): Int {
        return 0
    }

    override suspend fun assignLegacyRowsToUser(userId: String) { }

    /** Helper di test: legge lo stato corrente di un'analisi per fare asserzioni. */
    fun snapshot(id: Long): SkinAnalysisEntity? = analyses[id]

    private fun toAnalysisWithLines(entity: SkinAnalysisEntity?): AnalysisWithLines? =
        entity?.let { AnalysisWithLines(it, linesByAnalysisId[it.id].orEmpty()) }

    private fun emitState() {
        allAnalysesFlow.value = analyses.values
            .sortedByDescending { it.date }
            .map { toAnalysisWithLines(it)!! }
        countFlow.value = analyses.size
        byIdFlows.forEach { (id, flow) -> flow.value = toAnalysisWithLines(analyses[id]) }
    }

        override suspend fun deleteSyncedAnalyses(userId: String) {
            val toRemove = analyses.values
                .filter { it.userId == userId && it.isSynced }

                .map { it.id }
            toRemove.forEach {
                analyses.remove(it)
                linesByAnalysisId.remove(it)
            }
            emitState()
        }

        override suspend fun getUnsyncedCount(userId: String): Int =
            analyses.values.count { it.userId == userId && !it.isSynced }

}
