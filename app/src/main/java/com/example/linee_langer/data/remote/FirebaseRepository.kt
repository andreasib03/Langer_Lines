package com.example.linee_langer.data.remote

import android.net.Uri
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.domain.models.UserFirebaseModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirebaseRepository"
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage

){

    // Dentro FirebaseRepository.kt
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

    suspend fun uploadSkinImage(uid: String, imageUri: Uri): String? {
        return try {
            val imageRef = storage.reference.child("skin_images/$uid/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(imageUri).await() // Richiede import kotlinx.coroutines.tasks.await
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            logCaughtException(TAG, "Upload immagine skin fallito (uid=$uid)", e)
            null
        }
    }

    suspend fun uploadAnalysisSync(uid: String, analysis: SkinAnalysisEntity): Boolean {

        return try {
            val analysisData = hashMapOf(
                "date" to analysis.date,
                "bodyPartId" to analysis.bodyPartId,
                "resultSummary" to analysis.resultSummary,
                "imagePath" to analysis.imagePath
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
}