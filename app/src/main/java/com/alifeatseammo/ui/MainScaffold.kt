package com.alifeatseammo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MainScaffold(
    navController: NavHostController,
    currentChar: com.alifeatseammo.data.model.Character,
    snackbarHostState: SnackbarHostState
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val combatState = currentChar.combatState
    val inCombat = combatState != null
    val inTravel = currentChar.travelState != null

    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!inCombat && !inTravel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = { navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        } },
                        icon = { Text("🏠 HUB") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Map.route,
                        onClick = { navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        } },
                        icon = { Text("🗺 SEA") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.PvP.route,
                        onClick = { navController.navigate(Screen.PvP.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        } },
                        icon = { Text("☠ BATTLE") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Crew.route,
                        onClick = { navController.navigate(Screen.Crew.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        } },
                        icon = { Text("👥 CREW") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.More.route,
                        onClick = { navController.navigate(Screen.More.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        } },
                        icon = { Text("☰ MORE") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AppNavigation(
                navController = navController,
                currentChar = currentChar,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
