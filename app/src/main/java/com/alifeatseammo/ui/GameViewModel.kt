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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed class CharacterState {
    object Loading : CharacterState()
    object NoCharacter : CharacterState()
    data class Loaded(val character: Character) : CharacterState()
}

class GameViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val gameRepository: GameRepository = FirestoreGameRepository(),
    private val chatRepository: ChatRepository = FirestoreChatRepository(),
    private val crewRepository: CrewRepository = FirestoreCrewRepository(),
    private val socialRepository: SocialRepository = FirestoreSocialRepository()
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    val characterState: StateFlow<CharacterState> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                gameRepository.getCharacter(user.uid)
                    .map { if (it == null) CharacterState.NoCharacter else CharacterState.Loaded(it) }
            } else flowOf(CharacterState.NoCharacter)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, CharacterState.Loading)

    private var travelJob: Job? = null

    val character: StateFlow<Character?> = characterState
        .map { if (it is CharacterState.Loaded) it.character else null }
        .onEach { char ->
            val travel = char?.travelState
            if (travel != null) {
                scheduleTravelFinish(travel.arrivalTime)
            } else {
                travelJob?.cancel()
                travelJob = null
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private fun scheduleTravelFinish(arrivalTime: Long) {
        travelJob?.cancel()
        val delayMs = arrivalTime - System.currentTimeMillis()
        travelJob = viewModelScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            gameRepository.finishTravel()
        }
    }

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

    val missions: StateFlow<List<Mission>> = gameRepository.getAvailableMissions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentLocationInfo: StateFlow<Location?> = combine(
        character.map { it?.currentLocation }.distinctUntilChanged(),
        locations
    ) { locationName, allLocs ->
        if (locationName != null) {
            val def = allLocs.find { it.name.equals(locationName, ignoreCase = true) }
            if (def != null) {
                Location(
                    name = def.name,
                    region = def.region,
                    isSafe = def.isSafe,
                    description = def.description,
                    weather = def.weather,
                    recommendedLevel = def.recommendedLevel,
                    actions = def.actions.map { 
                        LocationAction(ActionType.valueOf(it.type), it.label, it.icon)
                    }
                )
            } else {
                allLocs.firstOrNull()?.let { first ->
                    Location(
                        name = first.name,
                        region = first.region,
                        isSafe = first.isSafe,
                        description = first.description,
                        weather = first.weather,
                        recommendedLevel = first.recommendedLevel,
                        actions = first.actions.map { 
                            LocationAction(ActionType.valueOf(it.type), it.label, it.icon)
                        }
                    )
                }
            }
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val chatMessages: StateFlow<List<ChatMessage>> = chatRepository.getMessages("global")
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

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult.asStateFlow()

    fun signIn() {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signInAnonymously()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signIn(email, password)
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signUp(email, password, username)
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.sendPasswordResetEmail(email)
        }
    }

    fun upgradeGuestAccount(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.upgradeGuestAccount(email, password)
        }
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun signOut() {
        authRepository.signOut()
        _authResult.value = null
    }

    fun createCharacter(name: String, gender: Gender, race: Race) {
        val userId = currentUser.value?.uid ?: return
        gameRepository.createCharacter(userId, name, gender, race)
    }

    fun train(statType: StatType) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            gameRepository.train(userId, statType)
        }
    }

    suspend fun completeMission(mission: Mission): Boolean {
        val userId = currentUser.value?.uid ?: return false
        return gameRepository.completeMission(userId, mission.id)
    }

    fun startTravel(destination: String) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            gameRepository.startTravel(userId, destination)
        }
    }

    fun combatAction(action: CombatAction, techniqueId: String? = null, itemId: String? = null) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            gameRepository.combatAction(userId, action, techniqueId, itemId)
        }
    }

    fun attackPlayer(target: Character) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            gameRepository.attackPlayer(userId, target.id)
        }
    }

    fun equipItem(item: Item) {
        val char = character.value ?: return
        if (char.level < item.levelRequirement) return
        viewModelScope.launch {
            gameRepository.equipItem(item.id, item.type.name)
        }
    }

    fun unequipItem(slot: String) {
        if (currentUser.value == null) return
        viewModelScope.launch {
            gameRepository.unequipItem(slot)
        }
    }

    fun useItem(item: Item) {
        viewModelScope.launch {
            gameRepository.useItem(item.id)
        }
    }

    fun sellItem(item: Item) {
        if (currentUser.value == null) return
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
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            gameRepository.joinFaction(userId, faction)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

    fun addFriend(targetId: String) {
        viewModelScope.launch {
            socialRepository.sendFriendRequest(targetId)
        }
    }

    fun blockPlayer(targetId: String) {
        viewModelScope.launch {
            socialRepository.blockPlayer(targetId)
        }
    }
}
