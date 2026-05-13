package com.example.linee_langer

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.dataStore
import com.example.linee_langer.ui.interfacesUser.MyAppTheme
import com.example.linee_langer.ui.navigation.AppNavigation
import com.example.linee_langer.ui.screens.Screen
import com.example.linee_langer.ui.viewModels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

//entry point
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val userDarkModeSelection by settingsViewModel.isDarkMode.collectAsState()
            val onboardingDone by settingsViewModel.isOnBoardingCompleted.collectAsState()

            val useDarkTheme = userDarkModeSelection ?: isSystemInDarkTheme()

            val isLoggedIn = remember { authRepository.isUserLoggedIn() }

            MyAppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when(onboardingDone){
                        null -> {
                            // DataStore non ancora letto — mostra loading
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> {
                            // FIX: considera ENTRAMBE le condizioni:
                            //   1. onboarding completato nel DataStore
                            //   2. utente effettivamente loggato in Firebase Auth
                            // Se una delle due manca → OnBoarding
                            val startDestination = if (onboardingDone == true && isLoggedIn) {
                                Screen.Home.route
                            } else {
                                Screen.OnBoarding.route
                            }
                            AppNavigation(
                                startDestination = startDestination,
                                preferencesManager = settingsViewModel.userPreferencesManager
                            )
                        }
                    }
                }
            }

            val context = LocalContext.current

            LaunchedEffect(Unit) {
                printDataStoreContents(context) // eliminate in prod, solo per debug se i dati correctly saved con datastore

            }

        }


    }

    // out of class
    suspend fun printDataStoreContents(context: Context) {
        try {
            // Usa il context per login al dataStore
            // Nota: che 'dataStore' sia accessible (non private nel Manager)
            // o usa prefManager.dataStore
            context.dataStore.data.first().let { prefs ->
                val map = prefs.asMap()
                Log.d("DATASTORE_CHECK", "--- CONTENT DATASTORE ---")
                if (map.isEmpty()) {
                    Log.d("DATASTORE_CHECK", "Il DataStore è empty.")
                } else {
                    map.forEach { (key, value) ->
                        Log.d("DATASTORE_CHECK", "${key.name} = $value")
                    }
                }
                Log.d("DATASTORE_CHECK", "---------------------------")
            }
        } catch (e: Exception) {
            Log.e("DATASTORE_CHECK", "Error: ${e.message}")
        }
    }



}