package com.example.linee_langer.ui.screens

import com.example.linee_langer.R

sealed class Screen (
    val route: String,
    val title: String,
    val icon: Int
){
    object Home: Screen("home", "Home", R.drawable.ic_home)
    object Profile : Screen ("profile", "Profile", R.drawable.ic_profile)
    object Settings : Screen ("settings", "Settings", R.drawable.ic_settings)

    object Data : Screen ("data", "Data", R.drawable.ic_star)

    object Advice : Screen ("advice", "Advice", R.drawable.ic_star)

    object History : Screen ("history", "History", R.drawable.ic_star)

    object OnBoarding: Screen ("onboarding", "Welcome", 0)

    object Camera : Screen ("camera", "Camera", 0)
}

val BottomNavigationItems = listOf(
    Screen.Home,
    Screen.Profile,
    Screen.Settings
)