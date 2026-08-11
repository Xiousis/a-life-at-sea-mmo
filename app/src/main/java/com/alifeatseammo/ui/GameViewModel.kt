package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val gameRepository: GameRepository = FirestoreGameRepository(),
    private val chatRepository: ChatRepository = FirestoreChatRepository(),
    private val crewRepository: CrewRepository = FirestoreCrewRepository()
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    val character: StateFlow<Character?> = currentUser
        .flatMapLatest { user ->
            if (user != null) gameRepository.getCharacter(user.uid)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playersAtLocation: StateFlow<List<Character>> = character
        .mapNotNull { it?.currentLocation }
        .distinctUntilChanged()
        .flatMapLatest { location ->
            gameRepository.getPlayersAtLocation(location)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topPlayers: StateFlow<List<Character>> = gameRepository.getTopPlayers(20)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentLocationInfo: StateFlow<Location?> = character
        .map { it?.currentLocation }
        .distinctUntilChanged()
        .map { locationName ->
            if (locationName != null) {
                allLocations[locationName] ?: allLocations.values.first()
            } else null
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val missions: StateFlow<List<Mission>> = gameRepository.getAvailableMissions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = chatRepository.getMessages("global")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val crewChatMessages: StateFlow<List<ChatMessage>> = character
        .map { it?.crewId }
        .distinctUntilChanged()
        .flatMapLatest { crewId ->
            if (crewId != null) chatRepository.getMessages(crewId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun signIn() {
        authRepository.signInAnonymously { }
    }

    fun signIn(email: String, password: String) {
        authRepository.signIn(email, password) { }
    }

    fun signUp(email: String, password: String, username: String) {
        authRepository.signUp(email, password, username) { }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun createCharacter(name: String, gender: Gender, race: Race) {
        val user = currentUser.value ?: return
        gameRepository.createCharacter(user.uid, name, gender, race)
    }

    fun train(statType: StatType) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.train(user.uid, statType)
        }
    }

    fun completeMission(mission: Mission) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.completeMission(user.uid, mission.id)
        }
    }

    fun startTravel(destination: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.startTravel(user.uid, destination)
        }
    }

    fun combatAction(action: CombatAction, techniqueId: String? = null, itemId: String? = null) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.combatAction(user.uid, action, techniqueId, itemId)
        }
    }

    fun attackPlayer(target: Character) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.attackPlayer(user.uid, target.id)
        }
    }

    fun equipItem(item: Item) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.equipItem(item.id, item.type.name)
        }
    }

    fun unequipItem(slot: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.unequipItem(slot)
        }
    }

    fun useItem(item: Item) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            // Placeholder: currently using purchase logic or combat action logic
            // gameRepository.useItem(item.id)
        }
    }

    fun purchaseItem(itemId: String, shopId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.purchaseItem(itemId, shopId)
        }
    }

    fun sellItem(item: Item) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.sellItem(item.id)
        }
    }

    fun createCrew(name: String, description: String) {
        viewModelScope.launch {
            crewRepository.createCrew(name, description)
        }
    }

    fun joinCrew(crewId: String) {
        viewModelScope.launch {
            crewRepository.joinCrew(crewId)
        }
    }

    fun leaveCrew() {
        viewModelScope.launch {
            crewRepository.leaveCrew()
        }
    }

    fun inviteToCrew(targetId: String) {
        viewModelScope.launch {
            crewRepository.inviteToCrew(targetId)
        }
    }

    fun respondToInvite(crewId: String, accept: Boolean) {
        viewModelScope.launch {
            crewRepository.respondToInvite(crewId, accept)
        }
    }

    fun promoteMember(targetId: String, rank: String) {
        viewModelScope.launch {
            crewRepository.promoteMember(targetId, rank)
        }
    }

    fun joinFaction(faction: Faction) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            gameRepository.joinFaction(user.uid, faction)
        }
    }

    val crewInvites: StateFlow<List<CrewInvite>> = currentUser
        .flatMapLatest { user ->
            if (user != null) crewRepository.getInvitesForUser(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sendMessage(text: String, channelId: String = "global") {
        if (currentUser.value == null) return
        val char = character.value ?: return
        chatRepository.sendMessage(char.name, text, channelId)
    }

    private val allLocations = mapOf(
        "Port Haven" to Location(
            name = "PORT HAVEN",
            region = "Western Blue • Starter Region",
            isSafe = true,
            description = "A bustling trade port watched over by the Royal Navy.",
            weather = "Clear",
            playersHere = 37,
            recommendedLevel = 1,
            actions = listOf(
                LocationAction(ActionType.Docks, "DOCKS", "⚓"),
                LocationAction(ActionType.Tavern, "TAVERN", "🍺"),
                LocationAction(ActionType.Training, "TRAINING", "🥊"),
                LocationAction(ActionType.Market, "MARKET", "🛒"),
                LocationAction(ActionType.Bounties, "BOUNTIES", "☠"),
                LocationAction(ActionType.Crew, "CREW", "👥")
            )
        ),
        "Blacktooth Island" to Location(
            name = "BLACKTOOTH ISLAND",
            region = "Dangerous Waters",
            isSafe = false,
            description = "A rugged island known for its pirate activity and treacherous reefs.",
            weather = "Stormy",
            playersHere = 11,
            recommendedLevel = 20,
            actions = listOf(
                LocationAction(ActionType.Arena, "ARENA", "🥊"),
                LocationAction(ActionType.Smuggler, "SMUGGLER", "👤"),
                LocationAction(ActionType.BlackMarket, "BLACK MARKET", "🛒"),
                LocationAction(ActionType.Shipyard, "SHIPYARD", "🏗"),
                LocationAction(ActionType.Tavern, "TAVERN", "🍺"),
                LocationAction(ActionType.Docks, "DOCKS", "⚓")
            )
        ),
        "Fogi Tail Island" to Location(
            name = "FOGI TAIL ISLAND",
            region = "East Blue • Remote",
            isSafe = true,
            description = "A quiet, mist-covered island with a small fishing village.",
            weather = "Foggy",
            playersHere = 5,
            recommendedLevel = 5,
            actions = listOf(
                LocationAction(ActionType.Camp, "CAMP", "🏕"),
                LocationAction(ActionType.Cave, "CAVE", "🕳"),
                LocationAction(ActionType.Fishing, "FISHING", "🎣"),
                LocationAction(ActionType.Docks, "DOCKS", "⚓")
            )
        )
    )
}
