package com.example.linee_langer

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
import androidx.lifecycle.lifecycleScope
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.local.dataStore
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.ui.theme.LangerTheme
import com.example.linee_langer.ui.navigation.AppNavigation
import com.example.linee_langer.ui.navigation.Screen
import com.example.linee_langer.ui.feature.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

            val currentUser by authRepository.currentUserFlow.collectAsState(initial = authRepository.currentUser)


            LangerTheme(darkTheme = useDarkTheme) {
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
                            val startDestination = when {
                                currentUser == null -> Screen.Welcome.route
                                onboardingDone == false -> Screen.Welcome.route
                                else -> "main_flow"
                            }
                            // usata per riforzare la ricreazione di AppNavigation quando startDestination cambia dopo logout o cancellazione utente
                            key(startDestination) {
                                AppNavigation(startDestination = startDestination)
                            }
                        }
                    }
                }
            }
        }



    }


}





