package com.alifeatseammo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alifeatseammo.data.model.ActionType
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.StatType
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.PlayerProfileViewModel
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
                    val profileViewModel: PlayerProfileViewModel = viewModel()
                    val character by viewModel.character.collectAsState()
                    val user by viewModel.currentUser.collectAsState()
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                    var selectedPlayerId by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(selectedPlayerId) {
                        selectedPlayerId?.let { profileViewModel.loadPlayer(it) }
                    }

                    if (user == null) {
                        LoginScreen(
                            onLogin = { email, password -> viewModel.signIn(email, password) },
                            onSignUp = { email, password, username -> viewModel.signUp(email, password, username) },
                            onGuestSignIn = { viewModel.signIn() }
                        )
                    } else if (character == null) {
                        CharacterCreationScreen { name, gender, race ->
                            viewModel.createCharacter(name, gender, race)
                        }
                    } else {
                        val currentChar = character
                        if (currentChar?.combatState != null) {
                            CombatScreen(
                                character = currentChar,
                                onActionClick = { action -> viewModel.combatAction(action) }
                            )
                        } else {
                            Scaffold(
                                bottomBar = {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.Dashboard,
                                            onClick = { currentScreen = Screen.Dashboard },
                                            icon = { Text("🏠 HUB") }
                                        )
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.Travel,
                                            onClick = { currentScreen = Screen.Travel },
                                            icon = { Text("🗺 SEA") }
                                        )
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.PvP,
                                            onClick = { currentScreen = Screen.PvP },
                                            icon = { Text("☠ BATTLE") }
                                        )
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.Crew,
                                            onClick = { currentScreen = Screen.Crew },
                                            icon = { Text("👥 CREW") }
                                        )
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.More,
                                            onClick = { currentScreen = Screen.More },
                                            icon = { Text("☰ MORE") }
                                        )
                                    }
                                }
                            ) { padding ->
                                Box(modifier = Modifier.padding(padding)) {
                                    when (currentScreen) {
                                        Screen.Dashboard -> {
                                            currentChar?.let { char ->
                                                val location by viewModel.currentLocationInfo.collectAsState()
                                                val playersNearby by viewModel.playersAtLocation.collectAsState()
                                                DashboardScreen(
                                                    character = char,
                                                    location = location,
                                                    playersNearby = playersNearby,
                                                    onActionClick = { actionType ->
                                                        when (actionType) {
                                                            ActionType.Training -> currentScreen = Screen.Training
                                                            ActionType.Docks -> currentScreen = Screen.Travel
                                                            ActionType.Bounties -> currentScreen = Screen.Leaderboard
                                                            ActionType.Crew -> currentScreen = Screen.Crew
                                                            ActionType.Market -> { /* TODO */ }
                                                            ActionType.Tavern -> { /* TODO */ }
                                                            else -> {}
                                                        }
                                                    },
                                                    onPlayerClick = { player ->
                                                        selectedPlayerId = player.id
                                                        currentScreen = Screen.Character
                                                    },
                                                    onMissionsClick = { currentScreen = Screen.Missions },
                                                    onMailClick = { /* TODO */ },
                                                    onJoinFaction = { viewModel.joinFaction(it) }
                                                )
                                            }
                                        }
                                        Screen.Missions -> {
                                            val missions by viewModel.missions.collectAsState()
                                            MissionScreen(
                                                missions = missions,
                                                onMissionClick = {
                                                    viewModel.completeMission(it)
                                                    currentScreen = Screen.Dashboard
                                                },
                                                onBackClick = { currentScreen = Screen.Dashboard }
                                            )
                                        }
                                        Screen.Travel -> {
                                            currentChar?.let {
                                                TravelScreen(
                                                    character = it,
                                                    onTravelClick = { dest, arrival ->
                                                        viewModel.startTravel(dest, arrival)
                                                    },
                                                    onBackClick = { currentScreen = Screen.Dashboard }
                                                )
                                            }
                                        }
                                        Screen.Leaderboard -> {
                                            val players by viewModel.topPlayers.collectAsState()
                                            LeaderboardScreen(
                                                players = players,
                                                onBackClick = { currentScreen = Screen.Dashboard },
                                                onPlayerClick = { player ->
                                                    selectedPlayerId = player.id
                                                    currentScreen = Screen.Character
                                                }
                                            )
                                        }
                                        Screen.PvP -> {
                                            currentChar?.let { char ->
                                                val potentialTargets by viewModel.playersAtLocation.collectAsState()
                                                PvPScreen(
                                                    character = char,
                                                    potentialTargets = potentialTargets.filter { it.id != char.id },
                                                    onAttackClick = { target ->
                                                        viewModel.attackPlayer(target)
                                                    },
                                                    onPlayerClick = { player ->
                                                        selectedPlayerId = player.id
                                                        currentScreen = Screen.Character
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
                                        Screen.Combat -> {} // Handled by forced override above
                                        Screen.Training -> {
                                            // TODO: Implement a proper Training Screen
                                            // Reusing the old stat training logic for now in a simple list
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("Training Room", style = MaterialTheme.typography.headlineMedium)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                val stats = listOf(StatType.Strength, StatType.Endurance, StatType.Agility)
                                                stats.forEach { stat ->
                                                    Button(onClick = { viewModel.train(stat) }) {
                                                        Text("Train $stat")
                                                    }
                                                }
                                                Button(onClick = { currentScreen = Screen.Dashboard }) {
                                                    Text("Back")
                                                }
                                            }
                                        }
                                        Screen.Crew -> {
                                            val crew by profileViewModel.playerCrew.collectAsState()
                                            val invites by viewModel.crewInvites.collectAsState()
                                            currentChar?.let { char ->
                                                LaunchedEffect(char.crewId) {
                                                    char.crewId?.let { profileViewModel.loadPlayer(char.id) }
                                                }
                                                CrewScreen(
                                                    character = char,
                                                    crew = crew,
                                                    invites = invites,
                                                    onCreateCrew = { name, desc -> viewModel.createCrew(name, desc) },
                                                    onJoinCrew = { id -> viewModel.joinCrew(id) },
                                                    onLeaveCrew = { viewModel.leaveCrew() },
                                                    onInviteToCrew = { id -> viewModel.inviteToCrew(id) },
                                                    onRespondToInvite = { id, accept -> viewModel.respondToInvite(id, accept) },
                                                    onPromoteMember = { id, rank -> viewModel.promoteMember(id, rank) },
                                                    onBackClick = { currentScreen = Screen.Dashboard }
                                                )
                                            }
                                        }
                                        Screen.Character -> {
                                            val player by profileViewModel.playerProfile.collectAsState()
                                            val crew by profileViewModel.playerCrew.collectAsState()
                                            
                                            player?.let { p ->
                                                ProfileScreen(
                                                    character = p,
                                                    crew = crew,
                                                    isOwnProfile = p.id == currentChar?.id,
                                                    onBackClick = { currentScreen = Screen.Dashboard },
                                                    onAttackClick = {
                                                        viewModel.attackPlayer(p)
                                                        currentScreen = Screen.Dashboard
                                                    },
                                                    onViewCrewClick = {
                                                        selectedPlayerId = p.id
                                                        currentScreen = Screen.Crew
                                                    }
                                                )
                                            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        Screen.More -> {
                                            MoreScreen(
                                                onMenuItemClick = { item ->
                                                    when (item.label) {
                                                        "Character" -> {
                                                            selectedPlayerId = currentChar?.id
                                                            currentScreen = Screen.Character
                                                        }
                                                        "Inventory" -> currentScreen = Screen.Inventory
                                                        "Skills" -> currentScreen = Screen.Skills
                                                        "Leaderboard" -> currentScreen = Screen.Leaderboard
                                                        "Chat" -> currentScreen = Screen.Chat
                                                        "Settings" -> currentScreen = Screen.Settings
                                                        "Help" -> currentScreen = Screen.Help
                                                    }
                                                }
                                            )
                                        }
                                        Screen.Inventory -> {
                                            currentChar?.let { char ->
                                                InventoryScreen(
                                                    character = char,
                                                    onEquipItem = { viewModel.equipItem(it) },
                                                    onUnequipItem = { viewModel.unequipItem(it) },
                                                    onUseItem = { viewModel.useItem(it) },
                                                    onSellItem = { viewModel.sellItem(it) },
                                                    onBackClick = { currentScreen = Screen.More }
                                                )
                                            }
                                        }
                                        Screen.Skills -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Skills Screen")
                                            }
                                        }
                                        Screen.Settings -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Settings Screen")
                                                    Button(onClick = { viewModel.signOut() }) {
                                                        Text("Logout")
                                                    }
                                                    Button(onClick = { currentScreen = Screen.More }) {
                                                        Text("Back")
                                                    }
                                                }
                                            }
                                        }
                                        Screen.Help -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Help & Support")
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
    }
}

sealed class Screen {
    object Dashboard : Screen()
    object Missions : Screen()
    object Travel : Screen()
    object Leaderboard : Screen()
    object PvP : Screen()
    object Chat : Screen()
    object Combat : Screen()
    object Training : Screen()
    object Character : Screen()
    object Crew : Screen()
    object More : Screen()
    object Inventory : Screen()
    object Skills : Screen()
    object Settings : Screen()
    object Help : Screen()
}
