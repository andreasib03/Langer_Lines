package com.example.linee_langer.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.linee_langer.R
import com.example.linee_langer.data.UserPreferencesManager.PreferencesKeys.ONBOARDING_COMPLETE
import com.example.linee_langer.data.UserPreferencesManager.PreferencesKeys.PROFILE_IMAGE_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(context: Context) {

    private object PreferencesKeys {

        val PROFILE_IMAGE_KEY = stringPreferencesKey("profile_image_uri")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val USER_NAME = stringPreferencesKey("username")

        val AUTOCLEAN = booleanPreferencesKey("autoclean")

        val ETA = stringPreferencesKey("eta")
        val USER_EMAIL = stringPreferencesKey("email")
        val USER_GOAL_ID = intPreferencesKey("user_goal_id")

        val SKIN_TYPE = stringPreferencesKey("skin_type")

        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    private val dataStore = context.dataStore

    val userSkinType: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SKIN_TYPE]
        }

    val isAutoCleanEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTOCLEAN] ?: true
        }

    val profileImageUriFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PROFILE_IMAGE_KEY]
        }
    val isOnBoardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETE] == true
        }

    val isDarkMode: Flow<Boolean?> = dataStore.data
        .map {
            preferences -> preferences[PreferencesKeys.DARK_MODE]
        }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exc ->
            if(exc is IOException) emit(emptyPreferences())
            else throw exc
        }
        .map { prefs ->
            UserPreferences(
                name = prefs[PreferencesKeys.USER_NAME] ?: "",
                eta = prefs[PreferencesKeys.ETA] ?: "",
                email = prefs[PreferencesKeys.USER_EMAIL] ?: "",
                goalId = prefs[PreferencesKeys.USER_GOAL_ID] ?: 0,
                skinType = prefs[PreferencesKeys.SKIN_TYPE] ?: "",
                isCompleted = prefs[ONBOARDING_COMPLETE] ?: false
            )

        }

    val isNotificationEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
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


    suspend fun saveUserData(name: String, email:String, eta: String, goalId: Int, skinType: String){

        try {
            dataStore.edit { prefs->
                prefs[PreferencesKeys.USER_NAME] = name
                prefs[PreferencesKeys.ETA] = eta
                prefs[PreferencesKeys.USER_EMAIL] = email
                prefs[PreferencesKeys.USER_GOAL_ID] = goalId
                prefs[PreferencesKeys.SKIN_TYPE] = skinType
                prefs[PreferencesKeys.ONBOARDING_COMPLETE] = true
            }
        } catch (e: IOException) {
            Log.e("${R.string.datastore}", "${R.string.log_datastore_user}", e)
            throw e
        }

    }

    suspend fun updateEmail(newEmail: String){
        dataStore.edit {
            it[PreferencesKeys.USER_EMAIL] = newEmail
        }
    }



    suspend fun setDarkMode(enabled: Boolean){
        dataStore.edit { it[PreferencesKeys.DARK_MODE] = enabled }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean){
        dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    // DEBUG OPTION

    data class UserPreferences(
        val name: String,
        val eta: String,
        val email: String,
        val goalId: Int,
        val isCompleted: Boolean,
        val skinType: String
    )

}