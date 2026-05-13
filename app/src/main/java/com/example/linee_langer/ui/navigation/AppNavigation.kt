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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.linee_langer.data.UserPreferencesManager
import com.example.linee_langer.ui.screens.HomeScreen
import com.example.linee_langer.ui.screens.OnBoardingScreen
import com.example.linee_langer.ui.screens.AdviceScreen
import com.example.linee_langer.ui.screens.AnalysisDetailScreen
import com.example.linee_langer.ui.screens.BottomNavigationItems
import com.example.linee_langer.ui.screens.CameraScreen
import com.example.linee_langer.ui.screens.HistoryScreen
import com.example.linee_langer.ui.screens.ProfileScreen
import com.example.linee_langer.ui.screens.Screen
import com.example.linee_langer.ui.screens.SettingsScreen
import com.example.linee_langer.ui.screens.DataScreen
import com.example.linee_langer.ui.viewModels.CameraAnalysisViewModel
import com.example.linee_langer.ui.viewModels.HistoryViewModel
import com.example.linee_langer.ui.viewModels.HomeViewModel
import com.example.linee_langer.ui.viewModels.NotificationViewModel
import com.example.linee_langer.ui.viewModels.OnBoardingViewModel
import com.example.linee_langer.ui.viewModels.ProfileViewModel
import com.example.linee_langer.ui.viewModels.SettingsViewModel

@Composable
fun AppNavigation(startDestination: String, preferencesManager: UserPreferencesManager) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Determina dove mostrare la barra di navigazione inferiore
            val showBottomBar = currentRoute in listOf(
                Screen.Home.route,
                Screen.Profile.route,
                Screen.Settings.route
                // Se vuoi mantenere la barra anche nella cronologia, aggiungi: Screen.History.route
            )

            if (showBottomBar) {
                NavigationBar {
                    BottomNavigationItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = screen.icon),
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            composable(
                route = Screen.OnBoarding.route,
                exitTransition = {
                    fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.8f, animationSpec = tween(500))
                }
            ) {
                val onBoardingViewModel: OnBoardingViewModel = hiltViewModel()
                OnBoardingScreen(
                    onBoardingViewModel = onBoardingViewModel,
                    onFinished = {
                        // CORRETTO: La scrittura delle preferenze avviene già dentro finishOnBoarding nel ViewModel.
                        // Qui eseguiamo solo il routing sicuro svuotando il backstack.
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.OnBoarding.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {

                val homeVm: HomeViewModel = hiltViewModel()
                val notificationVm: NotificationViewModel = hiltViewModel()
                val profileVm: ProfileViewModel = hiltViewModel()
                val historyVm: HistoryViewModel = hiltViewModel()
                HomeScreen(
                    notificationViewModel = notificationVm,
                    profileViewModel = profileVm,
                    historyViewModel = historyVm,
                    homeViewModel = homeVm,

                    onNavigateToCamera = {
                        navController.navigate(Screen.Camera.route)
                    },
                    onNavigateToAdvice = {
                        navController.navigate(Screen.Advice.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    }

                )
            }

            composable(Screen.Profile.route) {
                val notificationVm: NotificationViewModel = hiltViewModel()
                val profileVm: ProfileViewModel = hiltViewModel()
                val historyVm: HistoryViewModel = hiltViewModel()

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

            composable(Screen.Camera.route) {
                val analysisVm: CameraAnalysisViewModel = hiltViewModel()
                val notificationVm: NotificationViewModel = hiltViewModel()

                CameraScreen(
                    analysisViewModel = analysisVm,
                    notificationViewModel = notificationVm,
                    onClose = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    notificationViewModel = hiltViewModel(),
                    onNavigateToData = {
                        navController.navigate(Screen.Data.route)
                    },
                    onLogoutSuccess = {
                        settingsViewModel.logout{
                            navController.navigate(Screen.OnBoarding.route){
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }

                    },
                    profileViewModel = hiltViewModel()
                )
            }

            composable(Screen.History.route) {
                val notificationVm: NotificationViewModel = hiltViewModel()
                val historyVm: HistoryViewModel = hiltViewModel()

                HistoryScreen(
                    historyViewModel = historyVm,
                    notificationViewModel = notificationVm,
                    onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Data.route) {
                val notificationVm: NotificationViewModel = hiltViewModel()
                val profileVm: ProfileViewModel = hiltViewModel()
                DataScreen(
                    profileViewModel = profileVm,
                    notificationViewModel = notificationVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Advice.route) {
                val notificationVm: NotificationViewModel = hiltViewModel()
                AdviceScreen(
                    notificationViewModel = notificationVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("detail/{analysisId}") { backStackEntry ->
                val analysisId = backStackEntry.arguments?.getString("analysisId")?.toLongOrNull() ?: 0L
                val analysisVm: CameraAnalysisViewModel = hiltViewModel()
                val notificationVm: NotificationViewModel = hiltViewModel()

                AnalysisDetailScreen(
                    analysisId = analysisId,
                    analysisViewModel = analysisVm,
                    notificationViewModel = notificationVm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}