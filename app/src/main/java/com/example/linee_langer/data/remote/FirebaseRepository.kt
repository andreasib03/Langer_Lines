package com.example.linee_langer.data.remote

import android.graphics.Bitmap
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.core.utils.ImageUtils.bitmapToBase64
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.domain.models.UserFirebaseModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirebaseRepository"
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
){

    suspend fun getUserProfile(uid: String): UserFirebaseModel? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(UserFirebaseModel::class.java)
        } catch (e: Exception) {
            logCaughtException(TAG, "Lettura profilo utente fallita (uid=$uid)", e)
            null
        }
    }

    /**
     * Aggiorna un singolo campo del documento utente nella collezione "users".
     * Risolve il problema 'Unresolved reference: updateUserField'.
     */
    suspend fun updateUserField(uid: String, fieldName: String, value: Any?): Boolean {
        return try {
            firestore.collection("users")
                .document(uid)
                .update(fieldName, value)
                .await()
            true
        } catch (e: Exception) {
            logCaughtException(TAG, "Aggiornamento campo $fieldName fallito per utente (uid=$uid)", e)
            false
        }
    }

    /**
     * Alternativa suspend per salvare un'immagine Bitmap codificata in Base64
     * direttamente nel documento del profilo dell'utente.
     */
    suspend fun saveProfileImageBase64(uid: String, bitmap: Bitmap): Boolean {
        return try {
            val base64Image = bitmapToBase64(bitmap)
            updateUserField(uid, "imageBase64", base64Image)
        } catch (e: Exception) {
            logCaughtException(TAG, "Salvataggio Base64 su Firestore fallito (uid=$uid)", e)
            false
        }
    }

    /**
     * Verifica in modo rapido se il profilo dell'utente esiste già su Firestore.
     * Restituisce true se il documento esiste, false altrimenti o in caso di errore.
     */
    suspend fun checkIfUserProfileExists(uid: String): Boolean {
        if (uid.isBlank()) {
            return false
        }
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            document.exists()
        } catch (e: Exception) {
            logCaughtException(TAG, "Verifica esistenza profilo fallita (uid=$uid)", e)
            false
        }

    }

    suspend fun deleteDocument(uid: String): Boolean {
        return try {
            firestore.collection("users").document(uid).delete().await()
            true
        } catch(e: Exception) {
            logCaughtException(TAG, "Eliminazione documento utente fallita (uid=$uid)", e)
            false
        }
    }

    suspend fun saveUserProfile(uid: String, user: UserFirebaseModel): Boolean{
        return try {
            firestore.collection("users").document(uid).set(user).await()
            true
        } catch (e: Exception) {
            logCaughtException(TAG, "Salvataggio profilo utente fallito (uid=$uid)", e)
            false
        }
    }

    suspend fun uploadAnalysisSync(
        uid: String,
        analysis: SkinAnalysisEntity,
        imageBase64: String?,
        lines: List<com.example.linee_langer.core.database.entity.LangerLineEntity> = emptyList()
    ): Boolean {

        return try {
            val analysisData = hashMapOf(
                "analysisId" to analysis.date.toString(), // ID univoco basato su timestamp
                "userId" to uid,
                "date" to analysis.date,
                "bodyPartId" to analysis.bodyPartId,
                "resultSummary" to analysis.resultSummary,
                "imageBase64" to imageBase64,
                "lines" to lines.map {
                    mapOf(
                        "startX" to it.startX,
                        "startY" to it.startY,
                        "endX" to it.endX,
                        "endY" to it.endY,
                        "intensity" to it.intensity
                    )
                }
            )

            firestore.collection("users")
                .document(uid)
                .collection("analyses")
                .document(analysis.date.toString())
                .set(analysisData)
                .await()
            true
        } catch (e: Exception) {
            logCaughtException(TAG, "Sync analisi su Firestore fallita (uid=$uid, date=${analysis.date})", e)
            false
        }
    }

    suspend fun deleteAnalysisDocument(uid: String, date: Long): Boolean {
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("analyses")
                .document(date.toString())
                .delete()
                .await()
            true
        } catch (e: Exception) {
            logCaughtException(TAG, "Eliminazione documento analisi fallita (uid=$uid, date=$date)", e)
            false
        }
    }

    /**
     * Recupera tutte le analisi salvate su Firestore per un determinato utente.
     */
    suspend fun getAllRemoteAnalyses(uid: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("analyses")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            logCaughtException(TAG, "Recupero analisi remote fallito (uid=$uid)", e)
            emptyList()
        }
    }

}