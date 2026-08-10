package com.alifeatseammo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.screens.*
import com.alifeatseammo.ui.theme.ALifeAtSeaMMOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ALifeAtSeaMMOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: GameViewModel = viewModel()
                    val character by viewModel.character.collectAsState()
                    val user by viewModel.currentUser.collectAsState()
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                    if (user == null) {
                        Column {
                            Text("Please sign in via Firebase console to use this app.")
                        }
                    } else if (character == null) {
                        CharacterCreationScreen(
                            onCharacterCreated = { name, origin, style ->
                                viewModel.createCharacter(name, origin, style)
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Dashboard,
                                        onClick = { currentScreen = Screen.Dashboard },
                                        icon = { Text("Home") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Missions,
                                        onClick = { currentScreen = Screen.Missions },
                                        icon = { Text("Missions") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Travel,
                                        onClick = { currentScreen = Screen.Travel },
                                        icon = { Text("Travel") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Leaderboard,
                                        onClick = { currentScreen = Screen.Leaderboard },
                                        icon = { Text("Tops") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.PvP,
                                        onClick = { currentScreen = Screen.PvP },
                                        icon = { Text("PvP") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Chat,
                                        onClick = { currentScreen = Screen.Chat },
                                        icon = { Text("Chat") }
                                    )
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                val currentChar = character
                                when (currentScreen) {
                                    Screen.Dashboard -> {
                                        if (currentChar != null) {
                                            DashboardScreen(
                                                character = currentChar,
                                                onTrainClick = { viewModel.train(it) },
                                                onMissionsClick = { currentScreen = Screen.Missions }
                                            )
                                        }
                                    }
                                    Screen.Missions -> {
                                        MissionScreen(
                                            missions = viewModel.missions,
                                            onMissionClick = {
                                                viewModel.completeMission(it)
                                                currentScreen = Screen.Dashboard
                                            },
                                            onBackClick = { currentScreen = Screen.Dashboard }
                                        )
                                    }
                                    Screen.Travel -> {
                                        if (currentChar != null) {
                                            TravelScreen(
                                                character = currentChar,
                                                onTravelClick = { dest, arrival ->
                                                    viewModel.startTravel(dest, arrival)
                                                },
                                                onBackClick = { currentScreen = Screen.Dashboard }
                                            )
                                        }
                                    }
                                    Screen.Leaderboard -> {
                                        LeaderboardScreen(onBackClick = { currentScreen = Screen.Dashboard })
                                    }
                                    Screen.PvP -> {
                                        if (currentChar != null) {
                                            PvPScreen(
                                                character = currentChar,
                                                onAttackClick = { target ->
                                                    viewModel.attackPlayer(target)
                                                },
                                                onBackClick = { currentScreen = Screen.Dashboard }
                                            )
                                        }
                                    }
                                    Screen.Chat -> {
                                        val messages by viewModel.chatMessages.collectAsState()
                                        ChatScreen(
                                            messages = messages,
                                            onSendMessage = { viewModel.sendMessage(it) },
                                            onBackClick = { currentScreen = Screen.Dashboard }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Dashboard : Screen()
    object Missions : Screen()
    object Travel : Screen()
    object Leaderboard : Screen()
    object PvP : Screen()
    object Chat : Screen()
}
