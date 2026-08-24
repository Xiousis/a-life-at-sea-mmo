package com.alifeatseammo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.alifeatseammo.ui.components.SocialOverlay

@Composable
fun MainScaffold(
    navController: NavHostController,
    currentChar: com.alifeatseammo.data.model.Character,
    snackbarHostState: SnackbarHostState
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val gameViewModel: GameViewModel = hiltViewModel()
    val announcements by gameViewModel.announcements.collectAsState()

    var showSocialOverlay by remember { mutableStateOf(false) }
    val socialViewModel: SocialViewModel = hiltViewModel()

    val combatState = currentChar.combatState
    val inCombat = combatState != null
    val inTravel = currentChar.travelState != null

    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        navigationSuiteItems = {
            if (!inCombat && !inTravel) {
                item(
                    selected = currentDestination?.hasRoute<Screen.Dashboard>() == true,
                    onClick = {
                        navController.navigate(Screen.Dashboard) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("HUB") }
                )
                item(
                    selected = currentDestination?.hasRoute<Screen.Map>() == true,
                    onClick = {
                        navController.navigate(Screen.Map) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("SEA") }
                )
                item(
                    selected = currentDestination?.hasRoute<Screen.PvP>() == true,
                    onClick = {
                        navController.navigate(Screen.PvP) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.SportsKabaddi, contentDescription = null) },
                    label = { Text("BATTLE") }
                )
                item(
                    selected = currentDestination?.hasRoute<Screen.Crew>() == true,
                    onClick = {
                        navController.navigate(Screen.Crew) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("CREW") }
                )
            }
            item(
                selected = currentDestination?.hasRoute<Screen.More>() == true,
                onClick = {
                    navController.navigate(Screen.More) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                label = { Text("MORE") }
            )
            item(
                selected = showSocialOverlay,
                onClick = { showSocialOverlay = !showSocialOverlay },
                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                label = { Text("SOCIAL") }
            )
        }
    )
    {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (announcements.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = announcements.first(),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    AppNavigation(
                        navController = navController,
                        currentChar = currentChar,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.fillMaxSize()
                    )

                    SocialOverlay(
                        viewModel = socialViewModel,
                        showFriendsList = showSocialOverlay,
                        onDismissFriendsList = { showSocialOverlay = false }
                    )
                }
            }
        }
    }
}
