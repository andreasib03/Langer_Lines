package com.example.linee_langer.ui.feature.profile

import androidx.compose.runtime.getValue
import android.content.Context
import android.graphics.Bitmap
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
import com.example.linee_langer.core.utils.ImageUtils
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
import com.example.linee_langer.ui.shared.utils.generateDataHtml
import com.example.linee_langer.worker.SyncScheduler
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
    private val syncScheduler: SyncScheduler,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _deleteDataError = MutableStateFlow(false)
    val deleteDataError: StateFlow<Boolean> = _deleteDataError.asStateFlow()

    val fullHistory: Flow<List<AnalysisWithLines>> = repositoryAnalysis.allAnalyses
    private val _userProfile = MutableStateFlow<UserFirebaseModel?>(null)
    val userProfile: StateFlow<UserFirebaseModel?> = _userProfile.asStateFlow()

    val profileImageUri: StateFlow<Uri?> = userPreferencesManager.profileImageUriFlow
        .map { uriString -> if (!uriString.isNullOrBlank()) uriString.toUri() else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _profileBitmap = MutableStateFlow<Bitmap?>(null)
    val profileBitmap: StateFlow<Bitmap?> = _profileBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userSkinType = _userProfile.map { it?.skinType ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified = _isEmailVerified.asStateFlow()


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

    fun scheduleFullSync() {
        syncScheduler.scheduleFullSync(forceIfPending = true)
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
                    profile.imageBase64?.let { base64 ->
                        withContext(Dispatchers.Default) {
                            _profileBitmap.value = ImageUtils.base64ToBitmap(base64)
                        }
                    }
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
            try {
                // Generazione HTML su thread di background (IO)
                val htmlContent = withContext(Dispatchers.IO) {
                    generateDataHtml(context, userData, analyses, skinType)
                }

                // Apertura PDF viewer su thread Main
                withContext(Dispatchers.Main) {
                    exportDataAsPdf(context, htmlContent)
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

    /**
     * Aggiornamento combinato di email e/o password con riautenticazione obbligatoria.
     */
    fun updateCredentials(
        currentPassword: String,
        newEmail: String?,
        newPassword: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Riautenticazione
                val reauthResult = authRepository.reauthenticateWithPassword(currentPassword)
                if (reauthResult.isFailure) {
                    _isLoading.value = false
                    onResult(reauthResult)
                    return@launch
                }

                // 2. Aggiornamento Password (se richiesto)
                if (!newPassword.isNullOrBlank()) {
                    val passResult = authRepository.updatePassword(newPassword)
                    if (passResult.isFailure) {
                        _isLoading.value = false
                        onResult(passResult)
                        return@launch
                    }
                }

                // 3. Aggiornamento Email (se richiesto)
                if (!newEmail.isNullOrBlank()) {
                    val user = authRepository.currentUser
                    if (user != null && user.email != newEmail) {
                        try {
                            user.verifyBeforeUpdateEmail(newEmail).await()
                        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            _isLoading.value = false
                            onResult(Result.failure(AppException.Authentication.EmailAlreadyExists("unknown")))
                            return@launch
                        } catch (e: Exception) {
                            _isLoading.value = false
                            onResult(Result.failure(e))
                            return@launch
                        }
                    }
                }

                _isLoading.value = false
                onResult(Result.success(Unit))

            } catch (e: Exception) {
                logCaughtException(TAG, "Aggiornamento credenziali fallito", e)
                _isLoading.value = false
                onResult(Result.failure(e))
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