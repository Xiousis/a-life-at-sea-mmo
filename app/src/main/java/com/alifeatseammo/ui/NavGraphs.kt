package com.alifeatseammo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alifeatseammo.data.model.*
import com.alifeatseammo.ui.screens.*
import kotlinx.coroutines.launch

fun NavGraphBuilder.coreGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel,
    authViewModel: AuthViewModel
) {
    composable<Screen.Dashboard> {
        val combatViewModel: CombatViewModel = hiltViewModel()
        val economyViewModel: EconomyViewModel = hiltViewModel()
        
        val location by gameViewModel.currentLocationInfo.collectAsState()
        val playersNearby by gameViewModel.playersAtLocation.collectAsState()
        val missions by gameViewModel.filteredMissions.collectAsState()
        val activeRaids by gameViewModel.activeRaids.collectAsState()
        val mailMessages by economyViewModel.mailMessages.collectAsState()
        val warState by gameViewModel.warState.collectAsState()
        val travelResult by gameViewModel.travelResult.collectAsState()
        
        DashboardScreen(
            character = currentChar,
            location = location,
            playersNearby = playersNearby,
            playerCount = playersNearby.size,
            missionCount = missions.size,
            mailCount = mailMessages.count { !it.isRead },
            activeRaids = activeRaids,
            travelResult = travelResult,
            warState = warState,
            onActionClick = { actionType, parameter ->
                when (actionType) {
                    ActionType.Training -> navController.navigate(Screen.Training)
                    ActionType.Docks -> navController.navigate(Screen.Travel)
                    ActionType.Bounties -> {
                        gameViewModel.setLeaderboardFaction(Faction.Pirate)
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
            onMapClick = { navController.navigate(Screen.Map) },
            onJoinFaction = { gameViewModel.joinFaction(it) }
        ) {
            gameViewModel.clearTravelResult()
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
}

fun NavGraphBuilder.socialGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel,
    snackbarHostState: SnackbarHostState
) {
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

    composable<Screen.Character> {
        val profileViewModel: PlayerProfileViewModel = hiltViewModel()
        val combatViewModel: CombatViewModel = hiltViewModel()
        val socialViewModel: SocialViewModel = hiltViewModel()
        
        val player by profileViewModel.playerProfile.collectAsState()
        val crew by profileViewModel.playerCrew.collectAsState()

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
                onAttackClick = { combatViewModel.attackPlayer(p) },
                onChallengeClick = { combatViewModel.challengeHighestRank(p.id) },
                onViewCrewClick = { crewId -> navController.navigate(Screen.CrewProfile(crewId)) },
                onAddFriendClick = { socialViewModel.addFriend(p.id) },
                onMessageClick = { navController.navigate(Screen.Chat) }
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    composable<Screen.Leaderboard> {
        val players by gameViewModel.topPlayers.collectAsState()
        val crews by gameViewModel.topCrews.collectAsState()
        val selectedFaction by gameViewModel.leaderboardFaction.collectAsState()
        val selectedSort by gameViewModel.leaderboardSort.collectAsState()
        val selectedCrewSort by gameViewModel.leaderboardCrewSort.collectAsState()
        LeaderboardScreen(
            players = players,
            crews = crews,
            selectedFaction = selectedFaction,
            onFactionSelected = { gameViewModel.setLeaderboardFaction(it) },
            selectedSort = selectedSort,
            onSortSelected = { gameViewModel.setLeaderboardSort(it) },
            selectedCrewSort = selectedCrewSort,
            onCrewSortSelected = { gameViewModel.setLeaderboardCrewSort(it) },
            onBackClick = { navController.popBackStack() },
            onPlayerClick = { player -> navController.navigate(Screen.Character(player.id)) },
            onCrewClick = { crewId -> navController.navigate(Screen.CrewProfile(crewId)) }
        )
    }

    composable<Screen.PvP> {
        val combatViewModel: CombatViewModel = hiltViewModel()
        val potentialTargets by gameViewModel.playersAtLocation.collectAsState()
        PvPScreen(
            character = currentChar,
            potentialTargets = potentialTargets.filter { it.id != currentChar.id },
            onAttackClick = { target -> combatViewModel.attackPlayer(target) },
            onPlayerClick = { player -> navController.navigate(Screen.Character(player.id)) },
            onBackClick = { navController.popBackStack() },
        )
    }
}

fun NavGraphBuilder.playerGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel,
    authViewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState
) {
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
        val allTechniques by gameViewModel.techniques.collectAsState()
        SkillsScreen(
            character = currentChar,
            allTechniques = allTechniques,
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.Settings> {
        SettingsScreen(
            viewModel = gameViewModel,
            authViewModel = authViewModel,
            onAdminPanelClick = { navController.navigate(Screen.AdminPanel) }
        ) {
            navController.popBackStack()
        }
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

    composable<Screen.AdminPanel> {
        AdminPanelScreen(
            viewModel = gameViewModel,
            onBackClick = { navController.popBackStack() },
        )
    }
}

fun NavGraphBuilder.worldGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel,
    snackbarHostState: SnackbarHostState
) {
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
            onRaidClick = { raid -> gameViewModel.engageRaid(raid.id) },
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.Missions> {
        val missions by gameViewModel.filteredMissions.collectAsState()
        val scope = rememberCoroutineScope()
        val actionState by gameViewModel.actionState.collectAsState()
        MissionScreen(
            character = currentChar,
            actionState = actionState,
            missions = missions,
            onMissionClick = {
                scope.launch {
                    if (gameViewModel.completeMission(it)) {
                        navController.popBackStack()
                    }
                }
            },
            onSetSailClick = { destination ->
                navController.navigate(Screen.Travel)
            }
        ) {
            navController.popBackStack()
        }
    }

    composable<Screen.Quests> {
        val quests by gameViewModel.islandQuests.collectAsState()
        val actionState by gameViewModel.actionState.collectAsState()
        QuestScreen(
            character = currentChar,
            actionState = actionState,
            quests = quests,
            onQuestClick = { gameViewModel.completeQuest(it.id) },
            onBackClick = { navController.popBackStack() },
        )
    }
}

fun NavGraphBuilder.gameplayGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel,
    snackbarHostState: SnackbarHostState
) {
    composable<Screen.Combat> {
        val combatViewModel: CombatViewModel = hiltViewModel()
        val activeRaids by gameViewModel.activeRaids.collectAsState()
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
                    gameViewModel.raidCombatAction(currentChar.combatState.raidId!!, action, techId, itemId)
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

    composable<Screen.Traveling> {
        val locations by gameViewModel.locations.collectAsState()
        TravelingScreen(
            character = currentChar,
            locations = locations,
            onCompleteClick = { gameViewModel.finishTravel() }
        )
    }

    composable<Screen.Fishing> {
        FishingScreen(
            onBackClick = { navController.popBackStack() },
            snackbarHostState = snackbarHostState
        )
    }

    composable<Screen.MythicArt> {
        val actionState by gameViewModel.actionState.collectAsState()
        MythicArtScreen(
            character = currentChar,
            actionState = actionState,
            onRollClick = { gameViewModel.rollMythicArt() },
            onAdminGrantTestItems = { gameViewModel.adminGrantTestItems() },
            onBackClick = { navController.popBackStack() },
        )
    }
}

fun NavGraphBuilder.economyGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel,
    snackbarHostState: SnackbarHostState
) {
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

    composable<Screen.Market> { backStackEntry ->
        val economyViewModel: EconomyViewModel = hiltViewModel()
        val args = backStackEntry.toRoute<Screen.Market>()
        
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

    composable<Screen.Shipyard> {
        val economyViewModel: EconomyViewModel = hiltViewModel()
        val availableShips by economyViewModel.availableShips.collectAsState()
        
        val error by economyViewModel.errorMessage.collectAsState()
        LaunchedEffect(error) {
            error?.let {
                snackbarHostState.showSnackbar(it)
                economyViewModel.clearErrorMessage()
            }
        }

        ShipyardScreen(
            character = currentChar,
            availableShips = availableShips,
            onBuyShip = { economyViewModel.purchaseShip(it.id) },
            onUpgradeShip = { economyViewModel.upgradeShip(it) },
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.CookBook> {
        val economyViewModel: EconomyViewModel = hiltViewModel()
        val recipes by economyViewModel.recipes.collectAsState()
        val economyActionState by economyViewModel.actionState.collectAsState()
        
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
}

fun NavGraphBuilder.activityGraph(
    navController: NavHostController,
    currentChar: Character,
    gameViewModel: GameViewModel
) {
    composable<Screen.Professions> { backStackEntry ->
        val args = backStackEntry.toRoute<Screen.Professions>()
        val actionState by gameViewModel.actionState.collectAsState()
        ProfessionsScreen(
            character = currentChar,
            actionState = actionState,
            skillFilter = args.skill,
            onTrainClick = { gameViewModel.train(it) },
            onCookBookClick = { navController.navigate(Screen.CookBook) },
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.Training> {
        val actionState by gameViewModel.actionState.collectAsState()
        TrainingScreen(
            character = currentChar,
            actionState = actionState,
            onTrainClick = { gameViewModel.train(it) },
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.Tavern> {
        TavernScreen(
            character = currentChar,
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.Infirmary> {
        val playersNearby by gameViewModel.playersAtLocation.collectAsState()
        val actionState by gameViewModel.actionState.collectAsState()
        InfirmaryScreen(
            character = currentChar,
            actionState = actionState,
            playersAtLocation = playersNearby,
            onStartRest = { gameViewModel.startHealing() },
            onInstantHeal = { gameViewModel.instantHeal() },
            onPurchaseLicense = { gameViewModel.purchaseMedicalLicense() },
            onHealPlayer = { gameViewModel.healPlayer(it) },
            onBackClick = { navController.popBackStack() },
        )
    }

    composable<Screen.Camp> {
        val playersNearby by gameViewModel.playersAtLocation.collectAsState()
        val actionState by gameViewModel.actionState.collectAsState()
        CampScreen(
            character = currentChar,
            actionState = actionState,
            playersAtLocation = playersNearby,
            onStartRest = { gameViewModel.startHealing() },
            onInstantHeal = { gameViewModel.instantHeal() },
            onHealPlayer = { gameViewModel.healPlayer(it) },
            onBackClick = { navController.popBackStack() },
        )
    }
}
