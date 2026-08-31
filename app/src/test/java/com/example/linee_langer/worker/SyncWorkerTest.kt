package com.example.linee_langer.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import com.example.linee_langer.fakes.FakeAnalysisDao
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Copre [SyncWorker]: il worker che dovrebbe portare un'analisi da
 * "In attesa" (isSynced = false) a "Sincronizzato" (isSynced = true).
 *
 * Vedi header di [UploadWorkerTest] per le dipendenze di test richieste.
 */
@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private lateinit var dao: FakeAnalysisDao
    private lateinit var repo: AnalysisRepository
    private lateinit var firebaseRepo: FirebaseRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var context: Context

    private val uid = "user-123"

    @Before
    fun setUp() {
        dao = FakeAnalysisDao()
        repo = AnalysisRepository(dao)
        firebaseRepo = mockk()
        authRepo = mockk()

        val user = mockk<FirebaseUser>()
        every { user.uid } returns uid
        every { authRepo.currentUser } returns user

        context = ApplicationProvider.getApplicationContext()
    }

    private suspend fun insertRemote(): SkinAnalysisEntity {
        val id = dao.insertAnalysis(
            SkinAnalysisEntity(
                date = System.currentTimeMillis(),
                bodyPartId = "arm",
                imagePath = "https://firebase/img.jpg",
                resultSummary = "{}"
            )
        )
        return dao.snapshot(id)!!
    }

    private fun buildWorker(): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ) = SyncWorker(appContext, workerParameters, repo, firebaseRepo, authRepo)
            })
            .build() as SyncWorker

    @Test
    fun `caso successo - analisi con immagine remota viene marcata isSynced`() = runTest {
        val analysis = insertRemote()
        coEvery { firebaseRepo.uploadAnalysisSync(uid, any()) } returns true

        val result = buildWorker().doWork()

        assertTrue(result is Result.Success)
        assertTrue(dao.snapshot(analysis.id)!!.isSynced)
    }

    @Test
    fun `caso errore - fallimento Firestore lascia isSynced false e richiede retry`() = runTest {
        val analysis = insertRemote()
        coEvery { firebaseRepo.uploadAnalysisSync(uid, any()) } returns false

        val result = buildWorker().doWork()

        assertTrue(result is Result.Retry)
        assertFalse(dao.snapshot(analysis.id)!!.isSynced)
    }

    @Test
    fun `caso dati mancanti - nessuna analisi da sincronizzare restituisce success`() = runTest {
        val result = buildWorker().doWork()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `caso stato intermedio - immagine ancora locale (non caricata) non viene sincronizzata`() = runTest {
        val id = dao.insertAnalysis(
            SkinAnalysisEntity(
                date = System.currentTimeMillis(),
                bodyPartId = "arm",
                imagePath = "content://media/still-local.jpg", // upload non ancora avvenuto
                resultSummary = "{}"
            )
        )

        val result = buildWorker().doWork()

        // Nessuna chiamata a Firestore dovrebbe avvenire per un'immagine non ancora
        // caricata: l'elemento resta "in attesa", in attesa del prossimo UploadWorker.
        assertTrue(result is Result.Success)
        assertFalse(dao.snapshot(id)!!.isSynced)
    }

    @Test
    fun `elemento con syncFailed=true viene escluso e non causa piu' retry indefinito`() = runTest {
        // REGRESSIONE: prima della fix, un elemento "orfano" restava per sempre in
        // getUnsyncedAnalyses() e, se filtrato per isRemoteUrl, poteva anche entrare
        // qui; con syncFailed=true viene ora escluso a monte da getUnsyncedAnalyses(),
        // quindi SyncWorker può concludere con successo il resto della coda.
        val healthy = insertRemote()
        val orphanId = dao.insertAnalysis(
            SkinAnalysisEntity(
                date = System.currentTimeMillis() - 1000,
                bodyPartId = "arm",
                imagePath = "content://media/orphan.jpg",
                resultSummary = "{}",
                syncFailed = true
            )
        )
        coEvery { firebaseRepo.uploadAnalysisSync(uid, any()) } returns true

        val result = buildWorker().doWork()

        assertTrue(result is Result.Success)
        assertTrue(dao.snapshot(healthy.id)!!.isSynced)
        assertFalse(dao.snapshot(orphanId)!!.isSynced) // resta non sincronizzato, ma non blocca più nulla
    }

    @Test
    fun `utente non autenticato restituisce failure`() = runTest {
        every { authRepo.currentUser } returns null

        val result = buildWorker().doWork()

        assertTrue(result is Result.Failure)
    }
}
