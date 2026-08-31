package com.example.linee_langer.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

/**
 * Espone il [NavController] principale dell'app a qualunque composable annidato,
 * senza dover aggiungere un parametro `navController` a ogni schermata.
 *
 * Viene fornito una sola volta, alla radice di [AppNavigation]. Va letto solo da
 * composable che vivono effettivamente dentro quell'albero (es. LangerScaffold e
 * il pannello notifiche), mai da preview o da composable isolati.
 */
val LocalNavController = staticCompositionLocalOf<NavController> {
    error("LocalNavController non è stato fornito. Assicurati di leggerlo solo all'interno di AppNavigation.")
}