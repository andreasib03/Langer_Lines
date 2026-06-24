package com.example.linee_langer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.DARK_MODE
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.ETA
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.NOTIFICATIONS_ENABLED
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.ONBOARDING_COMPLETE
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.PROFILE_IMAGE_KEY
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.SKIN_TYPE
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.USER_EMAIL
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.USER_GOALS
import com.example.linee_langer.data.local.UserPreferencesManager.PreferencesKeys.USER_NAME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.IOException

val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val dataStore: DataStore<Preferences>) {
    private object PreferencesKeys {
        val PROFILE_IMAGE_KEY = stringPreferencesKey("profile_image_uri")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val USER_NAME = stringPreferencesKey("username")
        val AUTOCLEAN = booleanPreferencesKey("autoclean")
        val ETA = stringPreferencesKey("eta")
        val USER_EMAIL = stringPreferencesKey("email")
        val USER_GOALS = stringPreferencesKey("user_goal_id")
        val SKIN_TYPE = stringPreferencesKey("skin_type")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    suspend fun getUserName(): String =
        dataStore.data.map { it[PreferencesKeys.USER_NAME] ?: "" }.first()

    suspend fun getUserEmail(): String =
        dataStore.data.map { it[PreferencesKeys.USER_EMAIL] ?: "" }.first()

    suspend fun getSkinType(): String =
        dataStore.data.map { it[PreferencesKeys.SKIN_TYPE] ?: "" }.first()

    suspend fun getEta(): String =
        dataStore.data.map { it[PreferencesKeys.ETA] ?: "" }.first()


    // Flussi di dati puliti e coerenti
    val isAutoCleanEnabledFlow: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.AUTOCLEAN] ?: true }

    val profileImageUriFlow: Flow<String?> = dataStore.data
        .map { it[PROFILE_IMAGE_KEY] }

    val isOnBoardingCompleted: Flow<Boolean> = dataStore.data
        .map { it[ONBOARDING_COMPLETE] == true }

    val isDarkMode: Flow<Boolean?> = dataStore.data
        .map { it[DARK_MODE] }

    val isNotificationEnabled: Flow<Boolean> = dataStore.data
        .map { it[NOTIFICATIONS_ENABLED] ?: true }

    val userGoalsFlow: Flow<List<String>> = dataStore.data
        .map { prefs ->
            val raw = prefs[PreferencesKeys.USER_GOALS] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",")
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE] = completed
        }
    }


    suspend fun saveAutoCleanPreference(enabled: Boolean){
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOCLEAN] = enabled
        }
    }

    suspend fun saveProfileImageUri(uriString: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_KEY] = uriString
        }
    }


    suspend fun saveUserData(name: String, email:String, eta: String, goals: List<String>, skinType: String){

        try {
            dataStore.edit { prefs->
                prefs[USER_NAME] = name
                prefs[ETA] = eta
                prefs[USER_EMAIL] = email
                prefs[USER_GOALS] = goals.joinToString (",")
                prefs[SKIN_TYPE] = skinType
                prefs[ONBOARDING_COMPLETE] = true
            }
        } catch (e: IOException) {
            throw e
        }

    }

    suspend fun updateEmail(newEmail: String){
        dataStore.edit {
            it[USER_EMAIL] = newEmail
        }
    }

    suspend fun clearUserSession() {
        dataStore.edit { prefs ->
            // Rimuove solo i dati legati all'utente
            prefs.remove(PreferencesKeys.ONBOARDING_COMPLETE)
            prefs.remove(PreferencesKeys.USER_NAME)
            prefs.remove(PreferencesKeys.ETA)
            prefs.remove(PreferencesKeys.USER_EMAIL)
            prefs.remove(PreferencesKeys.USER_GOALS)
            prefs.remove(PreferencesKeys.SKIN_TYPE)
            prefs.remove(PreferencesKeys.PROFILE_IMAGE_KEY)
            // NON tocca DARK_MODE, AUTOCLEAN, NOTIFICATIONS_ENABLED
        }
    }



    suspend fun setDarkMode(enabled: Boolean){
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean){
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    // DEBUG OPTION


}