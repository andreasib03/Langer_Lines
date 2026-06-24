package com.example.linee_langer.ui.navigation

import com.example.linee_langer.R

sealed class Screen (
    val route: String,
    val title: String,
    val icon: Int = 0
){
    object Welcome : Screen("welcome", "Welcome")
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object GoogleAuth : Screen("google_auth", "Google Auth")
    object GoogleDataCollection : Screen(
        "google_data_collection",
        "Google Data Collection"
    )

    object EmailVerification : Screen(
        "email_verification/{flow}",
        "Email Verification"
    ) {
        fun createRoute(flow: String) =
            "email_verification/$flow"
    }
    object Home: Screen("home", "Home", R.drawable.ic_home)
    object Profile : Screen ("profile", "Profile", R.drawable.ic_profile)
    object Settings : Screen ("settings", "Settings", R.drawable.ic_settings)

    object Data : Screen ("data", "Data", R.drawable.ic_star)

    object Advice : Screen ("advice", "Advice", R.drawable.ic_star)

    object History : Screen ("history", "History", R.drawable.ic_star)

    object OnBoarding: Screen ("onboarding", "On Boarding")

    object Camera : Screen ("camera", "Camera", 0)

    object Tutorial : Screen("tutorial", "Tutorial")
}

val BottomNavigationItems = listOf(
    Screen.Home,
    Screen.Profile,
    Screen.Settings
)