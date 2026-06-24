package com.example.linee_langer.data.remote

import android.net.Uri
import android.util.Log
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity
import com.example.linee_langer.domain.models.UserFirebaseModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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
            Log.e("FirebaseRepo", "Error getting profile for UID $uid: ${e.message}")
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
            Log.e("FirebaseRepo", "Errore durante il controllo esistenza profilo: ${e.message}")
            false
        }

    }

    suspend fun deleteDocument(uid: String): Boolean {
        return try {
            firestore.collection("users").document(uid).delete().await()
            true
        } catch(e: Exception) {
            Log.e("FirebaseRepo", "Delete document error: ${e.message}")
            false
        }
    }



    suspend fun saveUserProfile(uid: String, user: UserFirebaseModel): Boolean{
        return try {
            firestore.collection("users").document(uid).set(user).await()
            true
        } catch (e: Exception) {
            Log.e("Save user profile problem: ", "${e.message}")
            false
        }
    }

    suspend fun uploadSkinImage(uid: String, imageUri: Uri): String? {
        return try {
            val imageRef = storage.reference.child("skin_images/$uid/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(imageUri).await() // Richiede import kotlinx.coroutines.tasks.await
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("Upload skin image problem: ", "${e.message}")
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
            Log.e("FirebaseRepo", "Sync analysis error: ${e.message}")
            false
        }
    }
}