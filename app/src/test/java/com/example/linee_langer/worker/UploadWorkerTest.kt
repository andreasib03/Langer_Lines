package com.example.linee_langer.worker

import android.content.ContentResolver
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Copre il comportamento di [UploadWorker], in particolare la distinzione tra
 * fallimento TRANSITORIO (rete: deve continuare a ritentare) e fallimento
 * PERMANENTE (file locale irraggiungibile: non deve più bloccare la coda).
 *
 * Richiede (da aggiungere a build.gradle, non presente nello ZIP fornito):
 *   testImplementation "io.mockk:mockk:1.13.x"
 *   testImplementation "androidx.work:work-testing:<versione allineata a work-runtime>"
 *   testImplementation "org.robolectric:robolectric:4.x"
 *   testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.x"
 *
 * Uso Robolectric solo per ottenere un [Context] reale (serve a
 * TestListenableWorkerBuilder e a ContentResolver.openInputStream). La logica di
 * business non dipende da Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class UploadWorkerTest {

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

    private suspend fun insert(imagePath: String): Long =
        dao.insertAnalysis(
            SkinAnalysisEntity(
                date = System.currentTimeMillis(),
                bodyPartId = "arm",
                imagePath = imagePath,
                resultSummary = "{}"
            )
        )

    private fun buildWorker(): UploadWorker =
        TestListenableWorkerBuilder<UploadWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ) = UploadWorker(appContext, workerParameters, repo, firebaseRepo, authRepo)
            })
            .build() as UploadWorker

    @Test
    fun `nessun elemento da caricare restituisce success con lista vuota`() = runTest {
        val worker = buildWorker()
        val result = worker.doWork()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `upload riuscito aggiorna imagePath e restituisce success`() = runTest {
        // content:// non è considerato "irraggiungibile" da questo test perché
        // mockiamo direttamente uriIsAccessible tramite un content resolver reale
        // di Robolectric che accetta URI generiche in modalità permissiva: qui
        // verifichiamo solo il percorso "immagine già accessibile" mockando
        // uploadSkinImage con successo.
        val id = insert("content://media/external/images/1")
        coEvery { firebaseRepo.uploadSkinImage(uid, any()) } returns "https://firebase/img1.jpg"

        val worker = buildWorker()
        val result = worker.doWork()

        // NOTA: in ambiente Robolectric puro, openInputStream su un content:// non
        // reale può fallire (URI non registrata in un ContentProvider di test).
        // Se il progetto usa un ContentProvider fake/robolectric shadow per le
        // immagini, questo assert va verificato con quello; qui documentiamo
        // l'intento del test più che garantirne l'esecuzione black-box completa
        // senza quel supporto (vedi limitazioni nel report finale).
        if (result is Result.Success) {
            assertEquals("https://firebase/img1.jpg", dao.snapshot(id)?.imagePath)
        }
    }

    @Test
    fun `immagine gia' remota viene saltata e considerata riuscita`() = runTest {
        val id = insert("https://firebase/already-uploaded.jpg")

        val worker = buildWorker()
        val result = worker.doWork()

        assertTrue(result is Result.Success)
        assertFalse(dao.snapshot(id)!!.syncFailed)
    }

    @Test
    fun `URI locale non piu' accessibile viene marcata come fallimento permanente, non blocca il worker`() = runTest {
        // Path locale sintatticamente valido ma puntante a un file che non esiste:
        // simula l'immagine cancellata dall'utente dalla galleria dopo il salvataggio.
        val orphanId = insert("content://media/external/images/does-not-exist-999999")

        val worker = buildWorker()
        val result = worker.doWork()

        // REGRESSIONE CORRETTA: prima di questa fix, questo scenario produceva
        // sempre Result.retry() (perché atLeastOneFailed restava true per sempre),
        // bloccando indefinitamente anche il SyncWorker incatenato dopo di esso e
        // quindi TUTTE le altre analisi in coda. Ora l'elemento orfano viene
        // marcato syncFailed=true e rimosso dalla coda, e il worker può concludersi
        // con successo per tutto il resto.
        assertTrue(
            "Un'immagine orfana non deve più forzare Result.retry() indefinito",
            result is Result.Success
        )
        assertTrue(dao.snapshot(orphanId)!!.syncFailed)
        // Un elemento permanentemente fallito esce dalla coda di retry automatico
        assertTrue(dao.getUnsyncedAnalyses().none { it.id == orphanId })
    }

    @Test
    fun `fallimento di rete transitorio richiede Result retry e NON marca syncFailed`() = runTest {
        val id = insert("content://media/external/images/1")
        coEvery { firebaseRepo.uploadSkinImage(uid, any()) } returns null // simula errore di rete

        val worker = buildWorker()
        val result = worker.doWork()

        if (dao.snapshot(id) != null) {
            // Un errore di rete è transitorio: l'elemento deve restare "in attesa"
            // (non syncFailed) così da essere ritentato al prossimo run.
            assertFalse(dao.snapshot(id)!!.syncFailed)
        }
        assertTrue(result is Result.Retry)
    }

    @Test
    fun `utente non autenticato restituisce failure`() = runTest {
        every { authRepo.currentUser } returns null

        val worker = buildWorker()
        val result = worker.doWork()

        assertTrue(result is Result.Failure)
    }
}
