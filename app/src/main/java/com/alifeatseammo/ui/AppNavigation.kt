package com.alifeatseammo.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.alifeatseammo.data.model.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    currentChar: Character,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel: GameViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val travelResult by viewModel.travelResult.collectAsState()

    // Global navigation for travel completion
    LaunchedEffect(travelResult) {
        if (travelResult != null) {
            navController.navigate(Screen.Dashboard) {
                popUpTo<Screen.Dashboard> { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Global navigation for Combat and Travel states
    val combatState = currentChar.combatState
    val travelState = currentChar.travelState

    LaunchedEffect(combatState?.isFinished, combatState != null, travelState != null) {
        val currentDestination = navController.currentDestination
        
        val isOnMainScreen = currentDestination?.hasRoute<Screen.Dashboard>() == true || 
                             currentDestination?.hasRoute<Screen.Map>() == true || 
                             currentDestination?.hasRoute<Screen.PvP>() == true || 
                             currentDestination?.hasRoute<Screen.Crew>() == true

        if (combatState != null) {
            if (combatState.isFinished) {
                val target = if (combatState.playerWon) Screen.Victory else Screen.Defeat
                if (currentDestination?.hasRoute(target::class) != true) {
                    navController.navigate(target) {
                        popUpTo<Screen.Combat> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                if (currentDestination?.hasRoute<Screen.Combat>() != true && isOnMainScreen) {
                    navController.navigate(Screen.Combat) { launchSingleTop = true }
                }
            }
        } else if (travelState != null) {
            if (currentDestination?.hasRoute<Screen.Traveling>() != true && isOnMainScreen) {
                navController.navigate(Screen.Traveling) {
                    popUpTo<Screen.Dashboard> { inclusive = false }
                    launchSingleTop = true
                }
            }
        } else {
            // Clean up if we are on a specialized state screen but no longer in that state
            if ((currentDestination?.hasRoute<Screen.Combat>() == true) || 
                (currentDestination?.hasRoute<Screen.Victory>() == true) || 
                (currentDestination?.hasRoute<Screen.Defeat>() == true) ||
                (currentDestination?.hasRoute<Screen.Traveling>() == true)) {
                navController.navigate(Screen.Dashboard) {
                    popUpTo<Screen.Dashboard> { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard,
        modifier = modifier,
    ) {
        coreGraph(navController, currentChar, viewModel, authViewModel)
        socialGraph(navController, currentChar, viewModel, snackbarHostState)
        playerGraph(navController, currentChar, viewModel, authViewModel, snackbarHostState)
        worldGraph(navController, currentChar, viewModel, snackbarHostState)
        gameplayGraph(navController, currentChar, viewModel, snackbarHostState)
        economyGraph(navController, currentChar, viewModel, snackbarHostState)
        activityGraph(navController, currentChar, viewModel)
    }
}
