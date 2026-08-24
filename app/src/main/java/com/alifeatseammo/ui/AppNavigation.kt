package com.alifeatseammo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.alifeatseammo.data.model.*
import com.alifeatseammo.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    currentChar: Character,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel: GameViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val actionState by viewModel.actionState.collectAsState()

    // Global Error Handling for persistent VMs
    val errorMsg by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    val travelResult by viewModel.travelResult.collectAsState()

    // Global navigation for travel completion
    LaunchedEffect(travelResult) {
        if (travelResult != null) {
            navController.navigate(Screen.Dashboard) {
                popUpTo<Screen.Dashboard> {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    // Global navigation for Combat
    val combatState = currentChar.combatState
    val travelState = currentChar.travelState

    LaunchedEffect(combatState?.isFinished, combatState != null, travelState != null) {
        if (combatState != null) {
            if (combatState.isFinished) {
                if (combatState.playerWon) {
                    if (navController.currentDestination?.hasRoute<Screen.Victory>() != true) {
                        navController.navigate(Screen.Victory) {
                            popUpTo<Screen.Combat> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else {
                    if (navController.currentDestination?.hasRoute<Screen.Defeat>() != true) {
                        navController.navigate(Screen.Defeat) {
                            popUpTo<Screen.Combat> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            } else {
                if (navController.currentDestination?.hasRoute<Screen.Combat>() != true) {
                    navController.navigate(Screen.Combat) {
                        launchSingleTop = true
                    }
                }
            }
        } else if (travelState != null) {
            // If traveling and NOT in combat, ensure we are on the Traveling screen
            if (navController.currentDestination?.hasRoute<Screen.Traveling>() != true) {
                navController.navigate(Screen.Traveling) {
                    popUpTo<Screen.Dashboard> { inclusive = false }
                    launchSingleTop = true
                }
            }
        } else {
            val currentDestination = navController.currentDestination
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
        enterTransition = { fadeIn(animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) },
        popExitTransition = { fadeOut(animationSpec = tween(500)) },
    ) {
        composable<Screen.Dashboard> {
            val combatViewModel: CombatViewModel = hiltViewModel()
            val economyViewModel: EconomyViewModel = hiltViewModel()
            
            val location by viewModel.currentLocationInfo.collectAsState()
            val playersNearby by viewModel.playersAtLocation.collectAsState()
            val missions by viewModel.missions.collectAsState()
            val mailMessages by economyViewModel.mailMessages.collectAsState()
            val warState by viewModel.warState.collectAsState()
            
            DashboardScreen(
                character = currentChar,
                location = location,
                playersNearby = playersNearby,
                playerCount = playersNearby.size,
                missionCount = missions.size,
                mailCount = mailMessages.count { !it.isRead },
                travelResult = travelResult,
                warState = warState,
                onActionClick = { actionType, parameter ->
                    when (actionType) {
                        ActionType.Training -> navController.navigate(Screen.Training)
                        ActionType.Docks -> navController.navigate(Screen.Travel)
                        ActionType.Bounties -> {
                            viewModel.setLeaderboardFaction(Faction.Pirate)
                            navController.navigate(Screen.Leaderboard)
                        }
                        ActionType.Crew -> navController.navigate(Screen.Crew)
                        ActionType.Market, ActionType.BlackMarket, ActionType.Smuggler -> {
                            navController.navigate(Screen.Market(category = parameter))
                        }
                        ActionType.Tavern -> navController.navigate(Screen.Tavern)
                        ActionType.Infirmary -> navController.navigate(Screen.Infirmary)
                        ActionType.Camp -> navController.navigate(Screen.Camp)
                        ActionType.Grind -> combatViewModel.startMonsterHunt()
                        ActionType.Shipyard -> navController.navigate(Screen.Shipyard)
                        ActionType.Work -> navController.navigate(Screen.Professions("all"))
                        ActionType.Kitchen -> navController.navigate(Screen.CookBook)
                        ActionType.Forge -> navController.navigate(Screen.Professions("Blacksmith"))
                        ActionType.Observatory -> navController.navigate(Screen.Professions("Navigating"))
                        ActionType.Expedition -> navController.navigate(Screen.Professions("TreasureHunting"))
                        ActionType.Fishing -> navController.navigate(Screen.Fishing)
                        ActionType.MythicRoll -> navController.navigate(Screen.MythicArt)
                        ActionType.Arena -> navController.navigate(Screen.PvP)
                        else -> {}
                    }
                },
                onPlayerClick = { player ->
                    navController.navigate(Screen.Character(player.id))
                },
                onMissionsClick = { navController.navigate(Screen.Missions) },
                onQuestsClick = { navController.navigate(Screen.Quests) },
                onMailClick = { navController.navigate(Screen.Mail) },
                onJoinFaction = { viewModel.joinFaction(it) }
            ) {
                viewModel.clearTravelResult()
            }
        }
        composable<Screen.Missions> {
            val missions by viewModel.missions.collectAsState()
            val scope = rememberCoroutineScope()
            MissionScreen(
                character = currentChar,
                actionState = actionState,
                missions = missions,
                onMissionClick = {
                    scope.launch {
                        if (viewModel.completeMission(it)) {
                            navController.popBackStack()
                        }
                    }
                }
            ) {
                navController.popBackStack()
            }
        }
        composable<Screen.Quests> {
            val quests by viewModel.islandQuests.collectAsState()
            QuestScreen(
                character = currentChar,
                actionState = actionState,
                quests = quests,
                onQuestClick = {
                    viewModel.completeQuest(it.id)
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Travel> {
            val travelViewModel: TravelViewModel = hiltViewModel()
            val locations by travelViewModel.locations.collectAsState()
            val travelActionState by travelViewModel.actionState.collectAsState()
            
            val error by travelViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    travelViewModel.clearErrorMessage()
                }
            }

            TravelScreen(
                character = currentChar,
                actionState = travelActionState,
                locations = locations,
                onTravelClick = { dest -> travelViewModel.startTravel(dest) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Map> {
            val travelViewModel: TravelViewModel = hiltViewModel()
            val locations by travelViewModel.locations.collectAsState()
            val activeRaids by travelViewModel.activeRaids.collectAsState()
            val seaEvents by travelViewModel.seaEvents.collectAsState()
            MapScreen(
                character = currentChar,
                locations = locations,
                activeRaids = activeRaids,
                seaEvents = seaEvents,
                onLocationClick = { loc ->
                    travelViewModel.startTravel(loc.name)
                    navController.navigate(Screen.Traveling)
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Leaderboard> {
            val players by viewModel.topPlayers.collectAsState()
            val crews by viewModel.topCrews.collectAsState()
            val selectedFaction by viewModel.leaderboardFaction.collectAsState()
            val selectedSort by viewModel.leaderboardSort.collectAsState()
            val selectedCrewSort by viewModel.leaderboardCrewSort.collectAsState()
            LeaderboardScreen(
                players = players,
                crews = crews,
                selectedFaction = selectedFaction,
                onFactionSelected = { viewModel.setLeaderboardFaction(it) },
                selectedSort = selectedSort,
                onSortSelected = { viewModel.setLeaderboardSort(it) },
                selectedCrewSort = selectedCrewSort,
                onCrewSortSelected = { viewModel.setLeaderboardCrewSort(it) },
                onBackClick = { navController.popBackStack() },
                onPlayerClick = { player ->
                    navController.navigate(Screen.Character(player.id))
                },
                onCrewClick = { _ ->
                    // Navigate to crew profile if implemented, or just show info
                    // navController.navigate(Screen.CrewProfile(crew.id))
                }
            )
        }
        composable<Screen.PvP> {
            val combatViewModel: CombatViewModel = hiltViewModel()
            val potentialTargets by viewModel.playersAtLocation.collectAsState()
            PvPScreen(
                character = currentChar,
                potentialTargets = potentialTargets.filter { it.id != currentChar.id },
                onAttackClick = { target -> combatViewModel.attackPlayer(target) },
                onPlayerClick = { player ->
                    navController.navigate(Screen.Character(player.id))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Chat> {
            val socialViewModel: SocialViewModel = hiltViewModel()
            val globalMessages by socialViewModel.chatMessages.collectAsState()
            val crewMessages by socialViewModel.crewChatMessages.collectAsState()
            
            val error by socialViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    socialViewModel.clearErrorMessage()
                }
            }

            ChatScreen(
                globalMessages = globalMessages,
                crewMessages = crewMessages,
                crewId = currentChar.crewId,
                onSendMessage = { text, channel -> socialViewModel.sendMessage(text, channel) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Training> {
            TrainingScreen(
                character = currentChar,
                actionState = actionState,
                onTrainClick = { viewModel.train(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Crew> {
            val socialViewModel: SocialViewModel = hiltViewModel()
            val profileViewModel: PlayerProfileViewModel = hiltViewModel()
            
            val crew by profileViewModel.playerCrew.collectAsState()
            val crewMembers by profileViewModel.crewMembers.collectAsState()
            val invites by socialViewModel.crewInvites.collectAsState()
            val socialActionState by socialViewModel.actionState.collectAsState()

            val error by socialViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    socialViewModel.clearErrorMessage()
                }
            }

            LaunchedEffect(currentChar.crewId) {
                currentChar.crewId?.let { profileViewModel.loadPlayer(currentChar.id) }
            }
            CrewScreen(
                character = currentChar,
                crew = crew,
                members = crewMembers,
                invites = invites,
                actionState = socialActionState,
                onCreateCrew = { name, desc -> socialViewModel.createCrew(name, desc) },
                onJoinCrew = { id -> socialViewModel.joinCrew(id) },
                onLeaveCrew = { socialViewModel.leaveCrew() },
                onInviteToCrew = { id -> socialViewModel.inviteToCrew(id) },
                onRespondToInvite = { id, accept -> socialViewModel.respondToInvite(id, accept) },
                onPromoteMember = { id, rank -> socialViewModel.promoteMember(id, rank) },
                onKickMember = { id -> socialViewModel.kickMember(id) },
                onDonateGold = { amount -> socialViewModel.donateGold(amount) },
                onUpdateSettings = { desc, public -> socialViewModel.updateCrewSettings(desc, public) },
                onToggleCrewPvP = { socialViewModel.toggleCrewPvP(it) },
                onUpgradePerk = { socialViewModel.upgradeCrewPerk(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Character> { backStackEntry ->
            val profileViewModel: PlayerProfileViewModel = hiltViewModel()
            val combatViewModel: CombatViewModel = hiltViewModel()
            val socialViewModel: SocialViewModel = hiltViewModel()
            
            val args = backStackEntry.toRoute<Screen.Character>()
            val playerId = args.playerId
            val player by profileViewModel.playerProfile.collectAsState()
            val crew by profileViewModel.playerCrew.collectAsState()

            LaunchedEffect(playerId) {
                profileViewModel.loadPlayer(playerId)
            }

            player?.let { p ->
                val isHighRankTarget = p.rank == "Fleet Admiral" || p.rank == "Pirate King"
                val isSameFaction = p.faction == currentChar.faction
                val isHighLevel = currentChar.level >= 300
                val isHighRankPlayer = currentChar.rank == "Admiral" || currentChar.rank == "Yonko"
                val canChallenge = isHighRankTarget && isSameFaction && isHighLevel && isHighRankPlayer

                ProfileScreen(
                    character = p,
                    crew = crew,
                    isOwnProfile = p.id == currentChar.id,
                    canChallenge = canChallenge,
                    onBackClick = { navController.popBackStack() },
                    onAttackClick = {
                        combatViewModel.attackPlayer(p)
                    },
                    onChallengeClick = {
                        combatViewModel.challengeHighestRank(p.id)
                    },
                    onViewCrewClick = {
                        navController.navigate(Screen.CrewProfile)
                    },
                    onAddFriendClick = { socialViewModel.addFriend(p.id) },
                    onMessageClick = { navController.navigate(Screen.Chat) }
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable<Screen.More> {
            MoreScreen(
                isGuest = authViewModel.currentUser.value?.isAnonymous ?: false,
                onMenuItemClick = { item ->
                    when (item.label) {
                        "Character" -> navController.navigate(Screen.Character(currentChar.id))
                        "Inventory" -> navController.navigate(Screen.Inventory)
                        "Stats" -> navController.navigate(Screen.Stats)
                        "Skills" -> navController.navigate(Screen.Skills)
                        "Auction House" -> navController.navigate(Screen.Auction)
                        "Leaderboard" -> navController.navigate(Screen.Leaderboard)
                        "Chat" -> navController.navigate(Screen.Chat)
                        "Mail" -> navController.navigate(Screen.Mail)
                        "Settings" -> navController.navigate(Screen.Settings)
                        "Help" -> navController.navigate(Screen.Help)
                        "Upgrade Account" -> navController.navigate(Screen.UpgradeAccount)
                    }
                }
            )
        }
        composable<Screen.Inventory> {
            val economyViewModel: EconomyViewModel = hiltViewModel()
            val economyActionState by economyViewModel.actionState.collectAsState()
            
            val error by economyViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    economyViewModel.clearErrorMessage()
                }
            }

            InventoryScreen(
                character = currentChar,
                actionState = economyActionState,
                onEquipItem = { economyViewModel.equipItem(it) },
                onUnequipItem = { economyViewModel.unequipItem(it) },
                onUseItem = { economyViewModel.useItem(it) },
                onCookItem = { economyViewModel.cookFish(it) },
                onSellItem = { economyViewModel.sellItem(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Stats> {
            StatsScreen(
                character = currentChar,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Skills> {
            val allTechniques by viewModel.techniques.collectAsState()
            SkillsScreen(
                character = currentChar,
                allTechniques = allTechniques,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Professions> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Professions>()
            val skill = args.skill
            ProfessionsScreen(
                character = currentChar,
                actionState = actionState,
                skillFilter = skill,
                onTrainClick = { viewModel.train(it) },
                onCookBookClick = { navController.navigate(Screen.CookBook) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Tavern> {
            TavernScreen(
                character = currentChar,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Market> { backStackEntry ->
            val economyViewModel: EconomyViewModel = hiltViewModel()
            val args = backStackEntry.toRoute<Screen.Market>()
            
            LaunchedEffect(args.category) {
                economyViewModel.setMarketCategory(args.category)
            }

            val marketItems by economyViewModel.marketItems.collectAsState()
            val economyActionState by economyViewModel.actionState.collectAsState()
            
            val error by economyViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    economyViewModel.clearErrorMessage()
                }
            }

            MarketScreen(
                character = currentChar,
                actionState = economyActionState,
                marketItems = marketItems,
                onBuyItem = { economyViewModel.purchaseItem(it.id, currentChar.currentLocation) },
                onSellItem = { economyViewModel.sellItem(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Mail> {
            val economyViewModel: EconomyViewModel = hiltViewModel()
            val mailMessages by economyViewModel.mailMessages.collectAsState()
            val economyActionState by economyViewModel.actionState.collectAsState()
            
            val error by economyViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    economyViewModel.clearErrorMessage()
                }
            }

            MailScreen(
                messages = mailMessages,
                actionState = economyActionState,
                onClaimRewards = { economyViewModel.claimMailRewards(it) },
                onDeleteMail = { economyViewModel.deleteMail(it) },
                onMarkAsRead = { economyViewModel.markMailAsRead(it) },
                onSendMail = { recipient, subject, body -> economyViewModel.sendMail(recipient, subject, body) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.UpgradeAccount> {
            val authResult by authViewModel.authResult.collectAsState()
            UpgradeAccountScreen(
                authResult = authResult,
                onUpgrade = { email, password -> authViewModel.upgradeGuestAccount(email, password) },
                onVerifiedEmailUpgrade = { activity -> authViewModel.startVerifiedEmailUpgrade(activity) },
                onBackClick = { navController.popBackStack() },
                onClearError = { authViewModel.clearAuthResult() }
            )
        }
        composable<Screen.Infirmary> {
            val playersNearby by viewModel.playersAtLocation.collectAsState()
            InfirmaryScreen(
                character = currentChar,
                actionState = actionState,
                playersAtLocation = playersNearby,
                onStartRest = { viewModel.startHealing() },
                onInstantHeal = { viewModel.instantHeal() },
                onPurchaseLicense = { viewModel.purchaseMedicalLicense() },
                onHealPlayer = { viewModel.healPlayer(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Camp> {
            val playersNearby by viewModel.playersAtLocation.collectAsState()
            CampScreen(
                character = currentChar,
                actionState = actionState,
                playersAtLocation = playersNearby,
                onStartRest = { viewModel.startHealing() },
                onInstantHeal = { viewModel.instantHeal() },
                onHealPlayer = { viewModel.healPlayer(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Shipyard> {
            val economyViewModel: EconomyViewModel = hiltViewModel()
            
            val error by economyViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    economyViewModel.clearErrorMessage()
                }
            }

            val availableShips = listOf(
                Ship("row_boat", "Row Boat", 0, 1.0f),
                Ship("sloop", "Sloop", 500, 1.5f),
                Ship("caravel", "Caravel", 2500, 2.0f),
                Ship("galleon", "Galleon", 10000, 3.0f),
            )
            ShipyardScreen(
                character = currentChar,
                availableShips = availableShips,
                onBuyShip = { economyViewModel.purchaseShip(it.id) },
                onUpgradeShip = { economyViewModel.upgradeShip(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                viewModel = viewModel,
                authViewModel = authViewModel,
                onAdminPanelClick = { navController.navigate(Screen.AdminPanel) }
            ) {
                navController.popBackStack()
            }
        }
        composable<Screen.Help> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("Help & Support")
            }
        }
        composable<Screen.CrewProfile> {
            val profileViewModel: PlayerProfileViewModel = hiltViewModel()
            val socialViewModel: SocialViewModel = hiltViewModel()
            
            val crew by profileViewModel.playerCrew.collectAsState()
            val crewMembers by profileViewModel.crewMembers.collectAsState()
            CrewProfileScreen(
                crew = crew,
                members = crewMembers,
                onBackClick = { navController.popBackStack() }
            ) { crewId ->
                socialViewModel.joinCrew(crewId)
            }
        }
        composable<Screen.Traveling> {
            val locations by viewModel.locations.collectAsState()
            TravelingScreen(
                character = currentChar,
                locations = locations,
                onCompleteClick = { viewModel.finishTravel() }
            )
        }
        composable<Screen.Fishing> {
            FishingScreen(
                onBackClick = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }
        composable<Screen.Combat> {
            val combatViewModel: CombatViewModel = hiltViewModel()
            val activeRaids by viewModel.activeRaids.collectAsState()
            val raidBoss = activeRaids.find { it.id == currentChar.combatState?.raidId }
            
            val error by combatViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    combatViewModel.clearErrorMessage()
                }
            }

            CombatScreen(
                character = currentChar,
                raidBoss = raidBoss,
                onActionClick = { action, techId, itemId -> 
                    if (currentChar.combatState?.isRaid == true) {
                        viewModel.raidCombatAction(currentChar.combatState.raidId!!, action, techId, itemId)
                    } else {
                        combatViewModel.combatAction(action, techId, itemId)
                    }
                }
            )
        }
        composable<Screen.Victory> {
            val combatViewModel: CombatViewModel = hiltViewModel()
            currentChar.combatState?.let { state ->
                VictoryScreen(
                    combatState = state,
                    onClaimRewards = { combatViewModel.claimVictoryRewards() }
                )
            }
        }
        composable<Screen.Defeat> {
            val combatViewModel: CombatViewModel = hiltViewModel()
            currentChar.combatState?.let { state ->
                DefeatScreen(
                    combatState = state,
                    onRetreat = { combatViewModel.retreatFromDefeat() }
                )
            }
        }
        composable<Screen.MythicArt> {
            MythicArtScreen(
                character = currentChar,
                actionState = actionState,
                onRollClick = { viewModel.rollMythicArt() },
                onAdminGrantTestItems = { viewModel.adminGrantTestItems() },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.Auction> {
            val auctionViewModel: AuctionViewModel = hiltViewModel()
            val listings by auctionViewModel.auctionListings.collectAsState()
            val auctionActionState by auctionViewModel.actionState.collectAsState()
            
            val error by auctionViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    auctionViewModel.clearErrorMessage()
                }
            }

            AuctionScreen(
                character = currentChar,
                listings = listings,
                actionState = auctionActionState,
                onListButtonClick = { item, price -> auctionViewModel.listAuctionItem(item, price) },
                onBuyButtonClick = { auctionViewModel.buyAuctionItem(it) },
                onCancelButtonClick = { auctionViewModel.cancelAuctionListing(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.CookBook> {
            val economyViewModel: EconomyViewModel = hiltViewModel()
            val recipes by economyViewModel.recipes.collectAsState()
            val economyActionState by economyViewModel.actionState.collectAsState()
            
            LaunchedEffect(Unit) {
                economyViewModel.loadRecipes()
            }

            val error by economyViewModel.errorMessage.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    economyViewModel.clearErrorMessage()
                }
            }

            CookBookScreen(
                character = currentChar,
                recipes = recipes,
                actionState = economyActionState,
                onCook = { economyViewModel.cook(it) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<Screen.AdminPanel> {
            AdminPanelScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
