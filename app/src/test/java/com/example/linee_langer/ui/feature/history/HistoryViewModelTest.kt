package com.example.linee_langer.ui.feature.history

import app.cash.turbine.test
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import com.example.linee_langer.fakes.FakeAnalysisDao
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Copre [HistoryViewModel], in particolare la domanda al centro dell'analisi del bug:
 * "la UI osserva realmente il dato aggiornato dal DB?".
 *
 * Usando [FakeAnalysisDao] (che riproduce la semantica reattiva di Room: ogni scrittura
 * ri-emette la Flow) invece di mockare l'intera catena, questi test verificano il
 * comportamento end-to-end reale del binding Flow → StateFlow → UI, non solo che i
 * singoli metodi vengano "chiamati".
 *
 * Richiede (non incluso nello ZIP fornito):
 *   testImplementation "app.cash.turbine:turbine:1.1.x"
 *   testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.x"
 *   testImplementation "io.mockk:mockk:1.13.x"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var dao: FakeAnalysisDao
    private lateinit var repo: AnalysisRepository
    private lateinit var notificationRepo: NotificationRepository
    private lateinit var viewModel: HistoryViewModel

    private lateinit var authRepository: AuthRepository

    private lateinit var firebaseRepository: FirebaseRepository

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeAnalysisDao()
        repo = AnalysisRepository(dao, authRepository, firebaseRepository)
        notificationRepo = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        firebaseRepository = mockk(relaxed = true)
        viewModel = HistoryViewModel(
            repo,
            notificationRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun insert(isSynced: Boolean = false, syncFailed: Boolean = false): Long =
        dao.insertAnalysis(
            SkinAnalysisEntity(
                date = System.currentTimeMillis(),
                bodyPartId = "arm",
                imagePath = "content://media/1",
                resultSummary = "{}",
                isSynced = isSynced,
                syncFailed = syncFailed
            )
        )

    @Test
    fun `caso dati mancanti - storico vuoto emette lista vuota`(): Unit = runTest {
        viewModel.history.test {
            assertEquals(emptyList<Any>(), awaitItem())
        }
    }

    @Test
    fun `caso successo - nuova analisi inserita compare nello storico`(): Unit = runTest {
        viewModel.history.test {
            assertEquals(emptyList<Any>(), awaitItem()) // stato iniziale
            insert()
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun `regressione - un aggiornamento isSynced dal worker si riflette nello storico osservato dalla UI`(): Unit = runTest {
        val id = insert(isSynced = false)

        viewModel.history.test {
            val initial = awaitItem()
            assertEquals(1, initial.size)
            assertTrue("Appena creata, l'analisi deve risultare non sincronizzata", !initial.first().analysis.isSynced)

            // Simula esattamente ciò che fa SyncWorker.updateSyncStatus(id, true)
            repo.updateSyncStatus(id, true)

            val updated = awaitItem()
            assertTrue(
                "La Flow di Room (via repository) deve ri-emettere dopo l'update: " +
                    "questo è il punto che confermiamo NON essere la causa del bug " +
                    "'In attesa' bloccato — l'osservazione reattiva funziona correttamente.",
                updated.first().analysis.isSynced
            )
        }
    }

    @Test
    fun `elemento con syncFailed compare nello storico con lo stato distinto, non piu' confuso con in attesa`(): Unit = runTest {
        insert(isSynced = false, syncFailed = true)

        viewModel.history.test {
            awaitItem() // iniziale vuoto
            val withOrphan = awaitItem()
            assertEquals(1, withOrphan.size)
            assertTrue(withOrphan.first().analysis.syncFailed)
            assertTrue(!withOrphan.first().analysis.isSynced)
        }
    }

    @Test
    fun `eliminazione analisi la rimuove dallo storico ed emette evento ShowUndo`(): Unit = runTest {
        insert()

        viewModel.events.test {
            viewModel.history.test {
                awaitItem() // stato iniziale vuoto (sottoscrizione avvenuta dopo l'insert sopra
                             // solo se WhileSubscribed(5000) non ha già raccolto il valore:
                             // per sicurezza il test lavora sul primo item non vuoto ricevuto)
                val withItem = awaitItem()
                assertEquals(1, withItem.size)

                viewModel.deleteAnalysis(withItem.first())

                val afterDelete = awaitItem()
                assertEquals(0, afterDelete.size)
            }
            val event = awaitItem()
            assertTrue(event is HistoryEvent.ShowUndo)
        }
    }

    @Test
    fun `restoreAnalysis rimette in storico un elemento eliminato`(): Unit = runTest {
        insert()

        viewModel.history.test {
            awaitItem() // iniziale
            val withItem = awaitItem()
            val toDelete = withItem.first()

            viewModel.deleteAnalysis(toDelete)
            assertEquals(0, awaitItem().size)

            viewModel.restoreAnalysis(toDelete)
            val restored = awaitItem()
            assertEquals(1, restored.size)
            assertEquals(toDelete.analysis.date, restored.first().analysis.date)
        }
    }
}
