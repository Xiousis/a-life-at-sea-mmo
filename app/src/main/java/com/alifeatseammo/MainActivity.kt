package com.alifeatseammo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alifeatseammo.data.model.ActionType
import com.alifeatseammo.data.model.StatType
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.PlayerProfileViewModel
import com.alifeatseammo.ui.screens.*
import com.alifeatseammo.ui.theme.ALifeAtSeaMMOTheme
import com.alifeatseammo.util.MusicManager

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
                    val characterState by viewModel.characterState.collectAsState()
                    val user by viewModel.currentUser.collectAsState()
                    val authResult by viewModel.authResult.collectAsState()
                    val creationResult by viewModel.createCharacterResult.collectAsState()
                    val errorMsg by viewModel.errorMessage.collectAsState()
                    val snackbarHostState = remember { SnackbarHostState() }
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                    var selectedPlayerId by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(errorMsg) {
                        errorMsg?.let {
                            snackbarHostState.showSnackbar(it)
                            viewModel.clearErrorMessage()
                        }
                    }

                    LaunchedEffect(selectedPlayerId) {
                        selectedPlayerId?.let { profileViewModel.loadPlayer(it) }
                    }

                    if (user == null) {
                        LoginScreen(
                            authResult = authResult,
                            onLogin = { email, password -> viewModel.signIn(email, password) },
                            onSignUp = { email, password, username -> viewModel.signUp(email, password, username) },
                            onGuestSignIn = { viewModel.signIn() },
                            onForgotPassword = { viewModel.resetPassword(it) },
                            onClearError = { viewModel.clearAuthResult() }
                        )
                    } else {
                        when (val state = characterState) {
                            is com.alifeatseammo.ui.CharacterState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is com.alifeatseammo.ui.CharacterState.NoCharacter -> {
                                CharacterCreationScreen(
                                    creationResult = creationResult,
                                    onCharacterCreated = { name, gender, race ->
                                        viewModel.createCharacter(name, gender, race)
                                    },
                                    onClearError = { viewModel.clearCreateCharacterResult() },
                                    onLogout = { viewModel.signOut() }
                                )
                            }
                            is com.alifeatseammo.ui.CharacterState.Loaded -> {
                                val currentChar = state.character
                                if (currentChar.combatState != null) {
                                    CombatScreen(
                                        character = currentChar,
                                        onActionClick = { action, techId, itemId -> viewModel.combatAction(action, techId, itemId) }
                                    )
                                } else if (currentChar.travelState != null) {
                                    TravelingScreen(
                                        character = currentChar,
                                        onArrival = { viewModel.finishTravel() }
                                    )
                                } else {
                                    Scaffold(
                                        snackbarHost = { SnackbarHost(snackbarHostState) },
                                        bottomBar = {
                                            NavigationBar {
                                                NavigationBarItem(
                                                    selected = currentScreen == Screen.Dashboard,
                                                    onClick = { currentScreen = Screen.Dashboard },
                                                    icon = { Text("🏠 HUB") }
                                                )
                                                NavigationBarItem(
                                                    selected = (currentScreen == Screen.Travel || currentScreen == Screen.Map),
                                                    onClick = { currentScreen = Screen.Map },
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
                                                    val location by viewModel.currentLocationInfo.collectAsState()
                                                    val playersNearby by viewModel.playersAtLocation.collectAsState()
                                                    val missions by viewModel.missions.collectAsState()
                                                    DashboardScreen(
                                                        character = currentChar,
                                                        location = location,
                                                        playersNearby = playersNearby,
                                                        playerCount = playersNearby.size,
                                                        missionCount = missions.size,
                                                        onActionClick = { actionType ->
                                                            when (actionType) {
                                                                ActionType.Training -> currentScreen = Screen.Training
                                                                ActionType.Docks -> currentScreen = Screen.Travel
                                                                ActionType.Bounties -> currentScreen = Screen.Leaderboard
                                                                ActionType.Crew -> currentScreen = Screen.Crew
                                                                ActionType.Market -> currentScreen = Screen.Market
                                                                ActionType.Tavern -> currentScreen = Screen.Tavern
                                                                ActionType.Infirmary -> currentScreen = Screen.Infirmary
                                                                ActionType.Shipyard -> currentScreen = Screen.Shipyard
                                                                else -> {}
                                                            }
                                                        },
                                                        onPlayerClick = { player ->
                                                            selectedPlayerId = player.id
                                                            currentScreen = Screen.Character
                                                        },
                                                        onMissionsClick = { currentScreen = Screen.Missions },
                                                        onMailClick = { currentScreen = Screen.Mail },
                                                        onJoinFaction = { viewModel.joinFaction(it) }
                                                    )
                                                }
                                                Screen.Missions -> {
                                                    val missions by viewModel.missions.collectAsState()
                                                    val scope = rememberCoroutineScope()
                                                    MissionScreen(
                                                        character = currentChar,
                                                        missions = missions,
                                                        onMissionClick = {
                                                            scope.launch {
                                                                if (viewModel.completeMission(it)) {
                                                                    currentScreen = Screen.Dashboard
                                                                }
                                                            }
                                                        },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Travel -> {
                                                    val locations by viewModel.locations.collectAsState()
                                                    TravelScreen(
                                                        character = currentChar,
                                                        locations = locations,
                                                        onTravelClick = { dest ->
                                                            viewModel.startTravel(dest)
                                                        },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Map -> {
                                                    val locations by viewModel.locations.collectAsState()
                                                    MapScreen(
                                                        character = currentChar,
                                                        locations = locations,
                                                        onLocationClick = { loc ->
                                                            viewModel.startTravel(loc.name)
                                                            currentScreen = Screen.Travel
                                                        },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
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
                                                    val potentialTargets by viewModel.playersAtLocation.collectAsState()
                                                    PvPScreen(
                                                        character = currentChar,
                                                        potentialTargets = potentialTargets.filter { it.id != currentChar.id },
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
                                                Screen.Chat -> {
                                                    val globalMessages by viewModel.chatMessages.collectAsState()
                                                    val crewMessages by viewModel.crewChatMessages.collectAsState()
                                                    ChatScreen(
                                                        globalMessages = globalMessages,
                                                        crewMessages = crewMessages,
                                                        crewId = currentChar.crewId,
                                                        onSendMessage = { text, channel -> viewModel.sendMessage(text, channel) },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Combat -> {} // Handled by forced override above
                                                Screen.Traveling -> {} // Handled by forced override above
                                                Screen.Training -> {
                                                    TrainingScreen(
                                                        character = currentChar,
                                                        onTrainClick = { viewModel.train(it) },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Crew -> {
                                                    val crew by profileViewModel.playerCrew.collectAsState()
                                                    val invites by viewModel.crewInvites.collectAsState()
                                                    LaunchedEffect(currentChar.crewId) {
                                                        currentChar.crewId?.let { profileViewModel.loadPlayer(currentChar.id) }
                                                    }
                                                    CrewScreen(
                                                        character = currentChar,
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
                                                Screen.Character -> {
                                                    val player by profileViewModel.playerProfile.collectAsState()
                                                    val crew by profileViewModel.playerCrew.collectAsState()
                                                    
                                                    player?.let { p ->
                                                        ProfileScreen(
                                                            character = p,
                                                            crew = crew,
                                                            isOwnProfile = p.id == currentChar.id,
                                                            onBackClick = { currentScreen = Screen.Dashboard },
                                                            onAttackClick = {
                                                                viewModel.attackPlayer(p)
                                                                currentScreen = Screen.Dashboard
                                                            },
                                                            onViewCrewClick = {
                                                                selectedPlayerId = p.id
                                                                currentScreen = Screen.CrewProfile
                                                            },
                                                            onAddFriendClick = {
                                                                viewModel.addFriend(p.id)
                                                            },
                                                            onMessageClick = {
                                                                currentScreen = Screen.Chat
                                                            }
                                                        )
                                                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator()
                                                    }
                                                }
                                                Screen.More -> {
                                                    MoreScreen(
                                                        isGuest = user?.isAnonymous ?: false,
                                                        onMenuItemClick = { item ->
                                                            when (item.label) {
                                                                "Character" -> {
                                                                    selectedPlayerId = currentChar.id
                                                                    currentScreen = Screen.Character
                                                                }
                                                                "Inventory" -> currentScreen = Screen.Inventory
                                                                "Skills" -> currentScreen = Screen.Skills
                                                                "Leaderboard" -> currentScreen = Screen.Leaderboard
                                                                "Chat" -> currentScreen = Screen.Chat
                                                                "Mail" -> currentScreen = Screen.Mail
                                                                "Settings" -> currentScreen = Screen.Settings
                                                                "Help" -> currentScreen = Screen.Help
                                                                "Upgrade Account" -> currentScreen = Screen.UpgradeAccount
                                                            }
                                                        }
                                                    )
                                                }
                                                Screen.Inventory -> {
                                                    InventoryScreen(
                                                        character = currentChar,
                                                        onEquipItem = { viewModel.equipItem(it) },
                                                        onUnequipItem = { viewModel.unequipItem(it) },
                                                        onUseItem = { viewModel.useItem(it) },
                                                        onSellItem = { viewModel.sellItem(it) },
                                                        onBackClick = { currentScreen = Screen.More }
                                                    )
                                                }
                                                Screen.Skills -> {
                                                    SkillsScreen(
                                                        character = currentChar,
                                                        onBackClick = { currentScreen = Screen.More }
                                                    )
                                                }
                                                Screen.Tavern -> {
                                                    TavernScreen(
                                                        character = currentChar,
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Market -> {
                                                    val marketItems by viewModel.marketItems.collectAsState()
                                                    MarketScreen(
                                                        character = currentChar,
                                                        marketItems = marketItems,
                                                        onBuyItem = { viewModel.purchaseItem(it.id, "default") },
                                                        onSellItem = { viewModel.sellItem(it) },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Mail -> {
                                                    val mailMessages by viewModel.mailMessages.collectAsState()
                                                    MailScreen(
                                                        messages = mailMessages,
                                                        onClaimRewards = { /* viewModel.claimMailRewards(it) */ },
                                                        onDeleteMail = { /* viewModel.deleteMail(it) */ },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.UpgradeAccount -> {
                                                    UpgradeAccountScreen(
                                                        authResult = authResult,
                                                        onUpgrade = { email, password -> viewModel.upgradeGuestAccount(email, password) },
                                                        onBackClick = { currentScreen = Screen.More },
                                                        onClearError = { viewModel.clearAuthResult() }
                                                    )
                                                }
                                                Screen.Infirmary -> {
                                                    InfirmaryScreen(
                                                        character = currentChar,
                                                        onStartRest = { viewModel.startHealing() },
                                                        onInstantHeal = { viewModel.instantHeal() },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Shipyard -> {
                                                    val availableShips = listOf(
                                                        com.alifeatseammo.data.model.Ship("row_boat", "Row Boat", 0, 1.0f),
                                                        com.alifeatseammo.data.model.Ship("sloop", "Sloop", 500, 1.5f),
                                                        com.alifeatseammo.data.model.Ship("caravel", "Caravel", 2500, 2.0f),
                                                        com.alifeatseammo.data.model.Ship("galleon", "Galleon", 10000, 3.0f)
                                                    )
                                                    ShipyardScreen(
                                                        character = currentChar,
                                                        availableShips = availableShips,
                                                        onBuyShip = { viewModel.purchaseShip(it.id) },
                                                        onBackClick = { currentScreen = Screen.Dashboard }
                                                    )
                                                }
                                                Screen.Settings -> {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text("Settings Screen")
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            Button(onClick = { viewModel.seedWorld() }) {
                                                                Text("Seed World Data")
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
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
                                                Screen.CrewProfile -> {
                                                    val crew by profileViewModel.playerCrew.collectAsState()
                                                    CrewProfileScreen(
                                                        crew = crew,
                                                        onBackClick = { currentScreen = Screen.Character },
                                                        onJoinClick = { crewId -> viewModel.joinCrew(crewId) }
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
        }
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pause()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.release()
    }
}

sealed class Screen {
    object Dashboard : Screen()
    object Missions : Screen()
    object Travel : Screen()
    object Map : Screen()
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
    object CrewProfile : Screen()
    object Tavern : Screen()
    object Market : Screen()
    object Mail : Screen()
    object UpgradeAccount : Screen()
    object Infirmary : Screen()
    object Traveling : Screen()
    object Shipyard : Screen()
}
