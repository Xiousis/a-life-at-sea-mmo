package com.alifeatseammo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alifeatseammo.data.model.ActionType
import com.alifeatseammo.data.model.Faction
import com.alifeatseammo.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    currentChar: com.alifeatseammo.data.model.Character,
    viewModel: GameViewModel,
    authViewModel: AuthViewModel,
    combatViewModel: CombatViewModel,
    travelViewModel: TravelViewModel,
    socialViewModel: SocialViewModel,
    economyViewModel: EconomyViewModel,
    profileViewModel: PlayerProfileViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            val location by viewModel.currentLocationInfo.collectAsState()
            val playersNearby by viewModel.playersAtLocation.collectAsState()
            val missions by viewModel.missions.collectAsState()
            val mailMessages by economyViewModel.mailMessages.collectAsState()
            DashboardScreen(
                character = currentChar,
                location = location,
                playersNearby = playersNearby,
                playerCount = playersNearby.size,
                missionCount = missions.size,
                mailCount = mailMessages.count { !it.isRead },
                onActionClick = { actionType ->
                    when (actionType) {
                        ActionType.Training -> navController.navigate(Screen.Training.route)
                        ActionType.Docks -> navController.navigate(Screen.Travel.route)
                        ActionType.Bounties -> {
                            viewModel.setLeaderboardFaction(Faction.Pirate)
                            navController.navigate(Screen.Leaderboard.route)
                        }
                        ActionType.Crew -> navController.navigate(Screen.Crew.route)
                        ActionType.Market -> navController.navigate(Screen.Market.route)
                        ActionType.Tavern -> navController.navigate(Screen.Tavern.route)
                        ActionType.Infirmary -> navController.navigate(Screen.Infirmary.route)
                        ActionType.Shipyard -> navController.navigate(Screen.Shipyard.route)
                        ActionType.Work -> navController.navigate(Screen.Professions.createRoute("all"))
                        ActionType.Kitchen -> navController.navigate(Screen.Professions.createRoute("Cooking"))
                        ActionType.Forge -> navController.navigate(Screen.Professions.createRoute("Blacksmith"))
                        ActionType.Observatory -> navController.navigate(Screen.Professions.createRoute("Navigating"))
                        ActionType.Expedition -> navController.navigate(Screen.Professions.createRoute("TreasureHunting"))
                        ActionType.Fishing -> navController.navigate(Screen.Fishing.route)
                        else -> {}
                    }
                },
                onPlayerClick = { player ->
                    navController.navigate(Screen.Character.createRoute(player.id))
                },
                onMissionsClick = { navController.navigate(Screen.Missions.route) },
                onMailClick = { navController.navigate(Screen.Mail.route) },
                onJoinFaction = { viewModel.joinFaction(it) }
            )
        }
        composable(Screen.Missions.route) {
            val missions by viewModel.missions.collectAsState()
            val scope = rememberCoroutineScope()
            MissionScreen(
                character = currentChar,
                missions = missions,
                onMissionClick = {
                    scope.launch {
                        if (viewModel.completeMission(it)) {
                            navController.popBackStack()
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Travel.route) {
            val locations by travelViewModel.locations.collectAsState()
            TravelScreen(
                character = currentChar,
                locations = locations,
                onTravelClick = { dest -> travelViewModel.startTravel(dest) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) {
            val locations by travelViewModel.locations.collectAsState()
            MapScreen(
                character = currentChar,
                locations = locations,
                onLocationClick = { loc ->
                    travelViewModel.startTravel(loc.name)
                    navController.navigate(Screen.Traveling.route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Leaderboard.route) {
            val players by viewModel.topPlayers.collectAsState()
            val selectedFaction by viewModel.leaderboardFaction.collectAsState()
            LeaderboardScreen(
                players = players,
                selectedFaction = selectedFaction,
                onFactionSelected = { viewModel.setLeaderboardFaction(it) },
                onBackClick = { navController.popBackStack() },
                onPlayerClick = { player ->
                    navController.navigate(Screen.Character.createRoute(player.id))
                }
            )
        }
        composable(Screen.PvP.route) {
            val potentialTargets by viewModel.playersAtLocation.collectAsState()
            PvPScreen(
                character = currentChar,
                potentialTargets = potentialTargets.filter { it.id != currentChar.id },
                onAttackClick = { target -> combatViewModel.attackPlayer(target) },
                onPlayerClick = { player ->
                    navController.navigate(Screen.Character.createRoute(player.id))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Chat.route) {
            val globalMessages by socialViewModel.chatMessages.collectAsState()
            val crewMessages by socialViewModel.crewChatMessages.collectAsState()
            ChatScreen(
                globalMessages = globalMessages,
                crewMessages = crewMessages,
                crewId = currentChar.crewId,
                onSendMessage = { text, channel -> socialViewModel.sendMessage(text, channel) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Training.route) {
            TrainingScreen(
                character = currentChar,
                onTrainClick = { viewModel.train(it) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Crew.route) {
            val crew by profileViewModel.playerCrew.collectAsState()
            val invites by socialViewModel.crewInvites.collectAsState()
            LaunchedEffect(currentChar.crewId) {
                currentChar.crewId?.let { profileViewModel.loadPlayer(currentChar.id) }
            }
            CrewScreen(
                character = currentChar,
                crew = crew,
                invites = invites,
                onCreateCrew = { name, desc -> socialViewModel.createCrew(name, desc) },
                onJoinCrew = { id -> socialViewModel.joinCrew(id) },
                onLeaveCrew = { socialViewModel.leaveCrew() },
                onInviteToCrew = { id -> socialViewModel.inviteToCrew(id) },
                onRespondToInvite = { id, accept -> socialViewModel.respondToInvite(id, accept) },
                onPromoteMember = { id, rank -> socialViewModel.promoteMember(id, rank) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Character.route,
            arguments = listOf(navArgument("playerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId")
            val player by profileViewModel.playerProfile.collectAsState()
            val crew by profileViewModel.playerCrew.collectAsState()

            LaunchedEffect(playerId) {
                playerId?.let { profileViewModel.loadPlayer(it) }
            }

            player?.let { p ->
                ProfileScreen(
                    character = p,
                    crew = crew,
                    isOwnProfile = p.id == currentChar.id,
                    onBackClick = { navController.popBackStack() },
                    onAttackClick = {
                        combatViewModel.attackPlayer(p)
                        navController.navigate(Screen.Dashboard.route)
                    },
                    onViewCrewClick = {
                        navController.navigate(Screen.CrewProfile.route)
                    },
                    onAddFriendClick = { socialViewModel.addFriend(p.id) },
                    onMessageClick = { navController.navigate(Screen.Chat.route) }
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Screen.More.route) {
            MoreScreen(
                isGuest = authViewModel.currentUser.value?.isAnonymous ?: false,
                onMenuItemClick = { item ->
                    when (item.label) {
                        "Character" -> navController.navigate(Screen.Character.createRoute(currentChar.id))
                        "Inventory" -> navController.navigate(Screen.Inventory.route)
                        "Skills" -> navController.navigate(Screen.Skills.route)
                        "Professions" -> navController.navigate(Screen.Professions.createRoute("all"))
                        "Leaderboard" -> navController.navigate(Screen.Leaderboard.route)
                        "Chat" -> navController.navigate(Screen.Chat.route)
                        "Mail" -> navController.navigate(Screen.Mail.route)
                        "Settings" -> navController.navigate(Screen.Settings.route)
                        "Help" -> navController.navigate(Screen.Help.route)
                        "Upgrade Account" -> navController.navigate(Screen.UpgradeAccount.route)
                    }
                }
            )
        }
        composable(Screen.Inventory.route) {
            InventoryScreen(
                character = currentChar,
                onEquipItem = { economyViewModel.equipItem(it) },
                onUnequipItem = { economyViewModel.unequipItem(it) },
                onUseItem = { economyViewModel.useItem(it) },
                onCookItem = { economyViewModel.cookFish(it) },
                onSellItem = { economyViewModel.sellItem(it) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Skills.route) {
            SkillsScreen(
                character = currentChar,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Professions.route,
            arguments = listOf(navArgument("skill") { defaultValue = "all"; type = NavType.StringType })
        ) { backStackEntry ->
            val skill = backStackEntry.arguments?.getString("skill") ?: "all"
            ProfessionsScreen(
                character = currentChar,
                skillFilter = skill,
                onTrainClick = { viewModel.train(it) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Tavern.route) {
            TavernScreen(
                character = currentChar,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Market.route) {
            val marketItems by economyViewModel.marketItems.collectAsState()
            MarketScreen(
                character = currentChar,
                marketItems = marketItems,
                onBuyItem = { economyViewModel.purchaseItem(it.id, currentChar.currentLocation) },
                onSellItem = { economyViewModel.sellItem(it) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Mail.route) {
            val mailMessages by economyViewModel.mailMessages.collectAsState()
            MailScreen(
                messages = mailMessages,
                onClaimRewards = { economyViewModel.claimMailRewards(it) },
                onDeleteMail = { economyViewModel.deleteMail(it) },
                onMarkAsRead = { economyViewModel.markMailAsRead(it) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.UpgradeAccount.route) {
            val authResult by authViewModel.authResult.collectAsState()
            UpgradeAccountScreen(
                authResult = authResult,
                onUpgrade = { email, password -> authViewModel.upgradeGuestAccount(email, password) },
                onBackClick = { navController.popBackStack() },
                onClearError = { authViewModel.clearAuthResult() }
            )
        }
        composable(Screen.Infirmary.route) {
            InfirmaryScreen(
                character = currentChar,
                onStartRest = { viewModel.startHealing() },
                onInstantHeal = { viewModel.instantHeal() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Shipyard.route) {
            val availableShips = listOf(
                com.alifeatseammo.data.model.Ship("row_boat", "Row Boat", 0, 1.0f),
                com.alifeatseammo.data.model.Ship("sloop", "Sloop", 500, 1.5f),
                com.alifeatseammo.data.model.Ship("caravel", "Caravel", 2500, 2.0f),
                com.alifeatseammo.data.model.Ship("galleon", "Galleon", 10000, 3.0f)
            )
            ShipyardScreen(
                character = currentChar,
                availableShips = availableShips,
                onBuyShip = { economyViewModel.purchaseShip(it.id) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Help.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("Help & Support")
            }
        }
        composable(Screen.CrewProfile.route) {
            val crew by profileViewModel.playerCrew.collectAsState()
            CrewProfileScreen(
                crew = crew,
                onBackClick = { navController.popBackStack() },
                onJoinClick = { crewId -> socialViewModel.joinCrew(crewId) }
            )
        }
        composable(Screen.Traveling.route) {
            TravelingScreen(character = currentChar)
        }
        composable(Screen.Fishing.route) {
            FishingScreen(
                onBackClick = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }
        composable(Screen.Combat.route) {
            CombatScreen(
                character = currentChar,
                onActionClick = { action, techId, itemId -> combatViewModel.combatAction(action, techId, itemId) }
            )
        }
    }
}
