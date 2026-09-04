package com.example.linee_langer.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.linee_langer.ui.feature.auth.AuthViewModel
import com.example.linee_langer.ui.feature.auth.LoginScreen
import com.example.linee_langer.ui.feature.auth.RegisterScreen
import com.example.linee_langer.ui.feature.home.HomeScreen
import com.example.linee_langer.ui.feature.onboarding.OnBoardingScreen
import com.example.linee_langer.ui.feature.profile.AdviceScreen
import com.example.linee_langer.ui.feature.camera.CameraScreen
import com.example.linee_langer.ui.feature.profile.ProfileScreen
import com.example.linee_langer.ui.feature.settings.SettingsScreen
import com.example.linee_langer.ui.feature.profile.DataScreen
import com.example.linee_langer.ui.feature.history.detail.AnalysisDetailViewModel
import com.example.linee_langer.ui.feature.camera.CameraAnalysisViewModel
import com.example.linee_langer.ui.feature.history.HistoryScreen
import com.example.linee_langer.ui.feature.history.HistoryViewModel
import com.example.linee_langer.ui.feature.history.detail.AnalysisDetailScreen
import com.example.linee_langer.ui.feature.home.HomeViewModel
import com.example.linee_langer.ui.feature.notifications.NotificationViewModel
import com.example.linee_langer.ui.feature.onboarding.OnBoardingViewModel
import com.example.linee_langer.ui.feature.onboarding.components.EmailVerificationScreen
import com.example.linee_langer.ui.feature.onboarding.components.GoogleAuthHandler
import com.example.linee_langer.ui.feature.onboarding.components.GoogleDataCollectionScreen
import com.example.linee_langer.ui.feature.onboarding.components.WelcomeScreen
import com.example.linee_langer.ui.feature.profile.ProfileViewModel
import com.example.linee_langer.ui.feature.settings.SettingsViewModel
import com.example.linee_langer.ui.feature.settings.components.VideoTutorialScreen
import com.example.linee_langer.ui.feature.settings.components.FaqScreen
import com.example.linee_langer.ui.theme.Dimens

@Composable
inline fun <reified VM : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): VM {
    val navGraphRoute = destination.parent?.route ?: return hiltViewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    CompositionLocalProvider(LocalNavController provides navController) {
        Scaffold(
            bottomBar = {
                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Profile.route,
                    Screen.Settings.route
                )
                if (showBottomBar) {
                    NavigationBar {
                        BottomNavigationItems.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = screen.icon),
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(Dimens.XLarge)
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        // Cancella lo stack fino all'inizio del grafico principali
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Evita di creare doppie istanze della stessa schermata
                                        launchSingleTop = true


                                        restoreState = true

                                    }

                                    if (currentRoute == Screen.Data.route && screen.route == Screen.Settings.route) {
                                        navController.popBackStack(Screen.Settings.route, inclusive = false)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {

                // ── WELCOME ───────────────────────────────────────────────────────
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onLoginClick    = { navController.navigate(Screen.Login.route) },
                        onGoogleClick   = { navController.navigate(Screen.GoogleAuth.route) },
                        onRegisterClick = { navController.navigate(Screen.Register.route) }
                    )
                }

                // ── LOGIN ─────────────────────────────────────────────────────────
                composable(Screen.Login.route) {
                    // Istanza locale — il login non condivide dati con altri flussi
                    val authViewModel: AuthViewModel = hiltViewModel()
                    LoginScreen(
                        authViewModel = authViewModel,
                        onAuthSuccess = { isExistingUser, _ ->
                            if (isExistingUser) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            } else {
                                // Se l'utente non ha un profilo Firestore, lo mandiamo alla raccolta dati.
                                // NON usiamo "google_flow" perché attiverebbe il login Google.
                                // Usiamo "register_flow" ma saltiamo la registrazione (già fatta) andando a EmailVerification (che poi porta a Onboarding)
                                // O meglio, andiamo a una destinazione di raccolta dati se necessaria.
                                // Per ora, se il login ha successo ma manca il profilo, portiamolo all'onboarding.
                                navController.navigate(Screen.OnBoarding.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        },
                        onSwitchToRegister = {
                            authViewModel.resetState()
                            navController.popBackStack()
                        }
                    )
                }

                // Rimosso OnBoarding globale per evitare conflitti di rotta e istanze VM doppie
                // Sarà gestito all'interno dei rispettivi flow (Google/Register) per condividere i dati.

                // ── FLUSSO GOOGLE ─────────────────────────────────────────────────
                // FIX BUG 1: usiamo un navigation() annidato "google_flow" così
                // tutte le destinazioni condividono la stessa istanza di authViewModel
                // e onBoardingViewModel tramite sharedViewModel().
                navigation(
                    startDestination = Screen.GoogleAuth.route,
                    route = "google_flow"
                ) {
                    composable(Screen.GoogleAuth.route) { entry ->
                        // FIX: sharedViewModel() restituisce l'istanza condivisa del graph "google_flow"
                        val authViewModel: AuthViewModel = entry.sharedViewModel(navController)
                        GoogleAuthHandler(
                            authViewModel = authViewModel,
                            onExistingUser = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            },
                            onNewUser = {
                                navController.navigate(Screen.GoogleDataCollection.route)
                            },
                            onBack = {
                                authViewModel.resetState()
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.GoogleDataCollection.route) { entry ->
                        // FIX: stessa istanza del graph — i dati scritti qui
                        // sono visibili in EmailVerification
                        val authViewModel: AuthViewModel = entry.sharedViewModel(navController)
                        val onBoardingViewModel: OnBoardingViewModel = entry.sharedViewModel(navController)
                        GoogleDataCollectionScreen(
                            onBoardingViewModel = onBoardingViewModel,
                            authViewModel = authViewModel,
                            onContinue = {
                                // FIX BUG 2: per Google l'email è già verificata.
                                // Non serve passare per EmailVerification — andiamo
                                // direttamente a OnBoarding dopo aver importato i dati.
                                onBoardingViewModel.importUserData(authViewModel)
                                navController.navigate(Screen.OnBoarding.route)
                            },
                            onBack = {
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── ONBOARDING ────────────────────────────────────────────────────
                    composable(
                        route = Screen.OnBoarding.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(500)) +
                                    scaleOut(targetScale = 0.8f, animationSpec = tween(500))
                        }
                    ) { entry ->
                        val onBoardingViewModel: OnBoardingViewModel = entry.sharedViewModel(navController)
                        OnBoardingScreen(
                            onBoardingViewModel = onBoardingViewModel,
                            onFinished = {
                                navController.navigate("main_flow") {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }



                // ── REGISTRAZIONE EMAIL ───────────────────────────────────────────
                // Anche qui: navigation annidato per condividere authViewModel
                // tra Register e EmailVerification
                navigation(
                    startDestination = Screen.Register.route,
                    route = "register_flow"
                ) {
                    composable(Screen.Register.route) { entry ->
                        val authViewModel: AuthViewModel = entry.sharedViewModel(navController)
                        val onBoardingViewModel: OnBoardingViewModel = entry.sharedViewModel(navController)
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onBoardingViewModel = onBoardingViewModel,
                            onAuthSuccess = { isExistingUser, _ ->
                                if (isExistingUser) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                } else {
                                    // Se l'account esiste già ma non ha profilo (collisione recuperata)
                                    // andiamo all'onboarding DOPO aver importato i dati inseriti nel form
                                    onBoardingViewModel.importUserData(authViewModel)
                                    navController.navigate(Screen.OnBoarding.route)
                                }
                            },
                            onRegistrationComplete = {
                                navController.navigate(Screen.EmailVerification.createRoute("email"))
                            },
                            onSwitchToLogin = {
                                authViewModel.resetState()
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Screen.EmailVerification.route) { entry ->
                        val authViewModel: AuthViewModel = entry.sharedViewModel(navController)
                        val onBoardingViewModel: OnBoardingViewModel = entry.sharedViewModel(navController)
                        EmailVerificationScreen(
                            authViewModel = authViewModel,
                            onVerified = {
                                // FIX: importa i dati pending nell'OnBoardingViewModel
                                // prima di navigare — ora funziona perché condividono
                                // la stessa istanza del VM
                                onBoardingViewModel.importUserData(authViewModel)
                                navController.navigate(Screen.OnBoarding.route)
                            },
                            onBack = {
                                authViewModel.resetState()
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.OnBoarding.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(500)) +
                                    scaleOut(targetScale = 0.8f, animationSpec = tween(500))
                        }
                    ) { entry ->
                        val onBoardingViewModel: OnBoardingViewModel =
                            entry.sharedViewModel(navController)
                        OnBoardingScreen(
                            onBoardingViewModel = onBoardingViewModel,
                            onFinished = {
                                navController.navigate("main_flow") {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }


                // ── MAIN FLOW ─────────────────────────────────────────────────────
                navigation(startDestination = Screen.Home.route, route = "main_flow") {

                    composable(Screen.Home.route) { entry ->
                        val profileVm: ProfileViewModel = entry.sharedViewModel(navController)
                        val notificationVm: NotificationViewModel = entry.sharedViewModel(navController)
                        val historyVm: HistoryViewModel = entry.sharedViewModel(navController)
                        val homeVm: HomeViewModel = hiltViewModel()

                        // FIX BUG 3: quando si arriva a Home dopo l'onboarding,
                        // forziamo il ricaricamento del profilo da Firestore
                        // perché ProfileViewModel è già istanziato dal graph.
                        // Il ricaricamento avviene in ProfileViewModel.loadUserProfile()
                        // che viene chiamato qui se il profilo è null.

                        HomeScreen(
                            notificationViewModel = notificationVm,
                            profileViewModel = profileVm,
                            historyViewModel = historyVm,
                            homeViewModel = homeVm,
                            onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                            onNavigateToAdvice = { navController.navigate(Screen.Advice.route) },
                            onNavigateToHistory = { navController.navigate(Screen.History.route) }
                        )
                    }

                    composable(Screen.Profile.route) { entry ->
                        val profileVm: ProfileViewModel = entry.sharedViewModel(navController)
                        val notificationVm: NotificationViewModel = entry.sharedViewModel(navController)
                        val historyVm: HistoryViewModel = entry.sharedViewModel(navController)
                        ProfileScreen(
                            historyViewModel = historyVm,
                            profileViewModel = profileVm,
                            notificationViewModel = notificationVm,
                            onNavigateToHistory = { navController.navigate(Screen.History.route) },
                            onNavigateToData = { navController.navigate(Screen.Data.route) },
                            onNavigateToAdvice = { navController.navigate(Screen.Advice.route) },
                            onNavigateToCamera = { navController.navigate(Screen.Camera.route) }
                        )
                    }

                    composable(Screen.Camera.route) { entry ->
                        val analysisVm: CameraAnalysisViewModel = entry.sharedViewModel(navController)
                        val notificationVm: NotificationViewModel = entry.sharedViewModel(navController)
                        CameraScreen(
                            analysisViewModel = analysisVm,
                            notificationViewModel = notificationVm,
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Tutorial.route) { entry ->
                        val notificationViewModel: NotificationViewModel = entry.sharedViewModel(navController)
                        VideoTutorialScreen(
                            notificationViewModel = notificationViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Faq.route) { entry ->
                        val notificationViewModel: NotificationViewModel = entry.sharedViewModel(navController)
                        FaqScreen(
                            notificationViewModel = notificationViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) { entry ->
                        val settingsViewModel: SettingsViewModel = entry.sharedViewModel(navController)
                        val notificationViewModel: NotificationViewModel = entry.sharedViewModel(navController)
                        val profileViewModel: ProfileViewModel = entry.sharedViewModel(navController)
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            notificationViewModel = notificationViewModel,
                            onNavigateToData = { navController.navigate(Screen.Data.route) },
                            onLogoutSuccess = { settingsViewModel.logout {} },
                            onNavigateToTutorial = { navController.navigate(Screen.Tutorial.route) },
                            onNavigateToFaq = { navController.navigate(Screen.Faq.route) },
                            profileViewModel = profileViewModel
                        )
                    }

                    composable(Screen.History.route) { entry ->
                        val notificationVm: NotificationViewModel = entry.sharedViewModel(navController)
                        val historyVm: HistoryViewModel = entry.sharedViewModel(navController)
                        HistoryScreen(
                            historyViewModel = historyVm,
                            notificationViewModel = notificationVm,
                            onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Data.route) { entry ->
                        val notificationVm: NotificationViewModel = entry.sharedViewModel(navController)
                        val profileVm: ProfileViewModel = entry.sharedViewModel(navController)
                        DataScreen(
                            profileViewModel = profileVm,
                            notificationViewModel = notificationVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Advice.route) { entry ->
                        val notificationVm: NotificationViewModel = entry.sharedViewModel(navController)
                        AdviceScreen(
                            notificationViewModel = notificationVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route ="detail/{analysisId}",
                        arguments = listOf(
                            navArgument("analysisId") {
                                type = NavType.LongType
                            }
                        )) { backStackEntry ->
                        val detailVm: AnalysisDetailViewModel = hiltViewModel()
                        val notificationVm: NotificationViewModel = backStackEntry.sharedViewModel(navController)
                        AnalysisDetailScreen(
                            detailViewModel = detailVm,
                            notificationViewModel = notificationVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}