package com.example.linee_langer.ui.feature.profile

import androidx.compose.runtime.getValue
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.BuildConfig
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.AnalysisWithLines
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import com.example.linee_langer.domain.exceptions.AppException
import com.example.linee_langer.domain.models.UserFirebaseModel
import com.example.linee_langer.domain.usecases.UserUseCase
import com.example.linee_langer.ui.shared.utils.exportDataAsPdf
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "ProfileViewModel"
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val repositoryAnalysis: AnalysisRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val userUseCase: UserUseCase,
    private val notificationRepository: NotificationRepository,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val fullHistory: Flow<List<AnalysisWithLines>> = repositoryAnalysis.allAnalyses
    private val _userProfile = MutableStateFlow<UserFirebaseModel?>(null)
    val userProfile: StateFlow<UserFirebaseModel?> = _userProfile.asStateFlow()

    val profileImageUri: StateFlow<Uri?> = userPreferencesManager.profileImageUriFlow
        .map { uriString -> if (!uriString.isNullOrBlank()) uriString.toUri() else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userSkinType = _userProfile.map { it?.skinType ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified = _isEmailVerified.asStateFlow()

    private val _deleteDataError = MutableStateFlow(false)
    val deleteDataError: StateFlow<Boolean> = _deleteDataError.asStateFlow()

    val isGoogleUser: Boolean by lazy {
        authRepository.currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } ?: false
    }

    init {
        loadUserProfile()

    }

    fun refreshIfNeeded() {
        if (_userProfile.value == null) {
            loadUserProfile()
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = authRepository.currentUser ?: return@launch
                user.reload().await()

                _isEmailVerified.value = user.isEmailVerified
                val uid = user.uid

                val profile = firebaseRepository.getUserProfile(uid)

                if (profile != null) {
                    _userProfile.value = profile
                } else {
                    loadProfileFromDataStore()
                }

            } catch (e: Exception) {
                logCaughtException(TAG, "Caricamento profilo da Firebase fallito, uso fallback DataStore", e)
                loadProfileFromDataStore()
            }
        }
    }

    var isExporting by mutableStateOf(false)
        private set

    fun generateReport(context: Context, userData: UserFirebaseModel, analyses: List<AnalysisWithLines>, skinType: String) {
        viewModelScope.launch(Dispatchers.Default) {
            isExporting = true
            // Chiamiamo la tua utility modificata
            try {
                withContext(Dispatchers.Main) {
                    exportDataAsPdf(context, userData, analyses, skinType)
                }
            } finally {
                isExporting = false
            }
        }
    }

    // Funzione di Reset modificata per scollegare l'account da Firebase Auth
    fun signOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _userProfile.value = null
            onSignOutComplete()
        }
    }

    fun updateProfileImage(uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO){
                    val newFileName = uri.lastPathSegment
                    appContext.filesDir
                        .listFiles{ file -> file.name.startsWith("profile_") && file.name.endsWith(".jpg")}
                        ?.filter { it.name != newFileName }
                        ?.forEach { it.delete() }
                }
                userPreferencesManager.saveProfileImageUri(uri.toString())
            } catch (e: Exception) {
                logCaughtException(TAG, "Salvataggio URI immagine profilo fallito (uri=$uri)", e)
            }
        }
    }

    fun updateEmail(newEmail: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = authRepository.currentUser ?: run {
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
                authRepository.reauthenticateWithPassword(password)
                    .onSuccess {
                        user.verifyBeforeUpdateEmail(newEmail).await()
                        _isLoading.value = false
                        onResult(true)
                    }
                    .onFailure {
                        _isLoading.value = false
                        onResult(false)
                    }
            } catch (e: Exception) {
                logCaughtException(TAG, "Aggiornamento email fallito", e)
                _isLoading.value = false
                onResult(false)
            }
        }
    }

    fun sendVerification(onResult: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val success = authRepository.sendVerificationEmail()
            _isLoading.value = false
            onResult(success)
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        // 1. Attiva un eventuale indicatore di caricamento nella UI
        _isLoading.value = true
        val uid = authRepository.currentUser?.uid

        viewModelScope.launch {
            try {
                if(uid != null){
                    // 2. Cancella il documento da Firestore PRIMA del sign out (necessario per le regole di autenticazione)
                    firebaseRepository.deleteDocument(uid)
                }

                // 3. Svuota il database locale Room e le SharedPreferences/DataStore
                repositoryAnalysis.deleteAllAnalysis()
                notificationRepository.deleteAllNotifications()
                userPreferencesManager.clearUserSession()
                // 4. Reset dello stato della UI del profilo
                _userProfile.value = null

                // 5. Effettua il sign out da Firebase Auth (Scollega l'utente)
                authRepository.signOut()
                // 6. Fine del caricamento e navigazione alla schermata di Login/Onboarding
                _isLoading.value = false
                onComplete()

            } catch (e: Exception) {
                logCaughtException(TAG, "Cancellazione completa dati utente fallita (uid=$uid)", e)
                _isLoading.value = false
                _deleteDataError.value = true
            }
        }
    }

    private fun loadProfileFromDataStore() {
        viewModelScope.launch {
            try {
                // Raccoglie una sola emissione dal DataStore (non un Flow continuo)
                val name     = userPreferencesManager.getUserName()
                val email    = userPreferencesManager.getUserEmail()
                val skinType = userPreferencesManager.getSkinType()
                val eta      = userPreferencesManager.getEta()

                if (name.isNotBlank() || email.isNotBlank()) {
                    _userProfile.value = UserFirebaseModel(
                        name     = name,
                        email    = email,
                        skinType = skinType,
                        eta      = eta
                    )
                }
            } catch (e: Exception) {
                logCaughtException(TAG, "Caricamento profilo da DataStore fallito", e)
            }
        }
    }

    fun verifyPasswordOnly(
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.reauthenticateWithPassword(password)
                .onSuccess {
                    _isLoading.value = false
                    onSuccess()   // La UI può procedere a mostrare lo Step 2
                }
                .onFailure { e ->
                    _isLoading.value = false
                    when (e) {
                        is AppException.Authentication.InvalidCredentials ->
                            onError(appContext.getString(R.string.error_wrong_password))
                        else -> onError(e.localizedMessage
                            ?: appContext.getString(R.string.error_auth_generic))
                    }
                }
        }
    }


    fun verifyGoogleOnly(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                if (result.credential is GoogleIdTokenCredential) {
                    authRepository.reauthenticateWithGoogle(
                        (result.credential as GoogleIdTokenCredential).idToken
                    )
                        .onSuccess {
                            _isLoading.value = false
                            onSuccess()
                        }
                        .onFailure { e ->
                            _isLoading.value = false
                            onError(e.localizedMessage
                                ?: appContext.getString(R.string.error_google_reauth))
                        }
                } else {
                    _isLoading.value = false
                    onError(appContext.getString(R.string.error_google_credential_invalid))
                }
            } catch (_: GetCredentialException) {
                _isLoading.value = false
                onError(appContext.getString(R.string.error_google_cancelled))
            }
        }
    }

    fun executeAccountDeletion(onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            userUseCase.performFullAccountDeletion()
                .onSuccess {
                    _userProfile.value = null
                    _isLoading.value = false
                    onComplete()
                }
                .onFailure { e ->
                    _isLoading.value = false
                    onError(e.localizedMessage
                        ?: appContext.getString(R.string.error_delete_account))
                }
        }
    }

}