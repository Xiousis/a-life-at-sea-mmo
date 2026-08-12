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
    private val socialRepository: SocialRepository = FirestoreSocialRepository(),
    private val adminRepository: AdminRepository = FirestoreAdminRepository()
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
    val currentLocationInfo: StateFlow<LocationDef?> = combine(
        character.map { it?.currentLocation }.distinctUntilChanged(),
        locations
    ) { locationName, allLocs ->
        if (locationName != null) {
            allLocs.find { it.name.equals(locationName, ignoreCase = true) }
                ?: allLocs.firstOrNull()
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val mailMessages: StateFlow<List<MailMessage>> = currentUser
        .flatMapLatest { user ->
            if (user != null) gameRepository.getMailMessages(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val marketItems: StateFlow<List<Item>> = gameRepository.getMarketItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult.asStateFlow()

    private val _createCharacterResult = MutableStateFlow<AuthResult?>(null)
    val createCharacterResult: StateFlow<AuthResult?> = _createCharacterResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
        android.util.Log.d("GameViewModel", "Creating character for $userId: $name, $race")
        viewModelScope.launch {
            _createCharacterResult.value = AuthResult.Loading
            try {
                gameRepository.createCharacter(userId, name, gender, race)
                android.util.Log.d("GameViewModel", "Character creation call successful")
                // Success: Stay in Loading state briefly, then clear to ensure spinner stops 
                // if Firestore sync is slow. The screen unmounts when characterState -> Loaded.
                delay(2000) 
                _createCharacterResult.value = null
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Character creation failed", e)
                val message = if (e is com.google.firebase.functions.FirebaseFunctionsException && e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND) {
                    "Function not found. Please ensure backend is deployed and on Blaze plan."
                } else {
                    e.message ?: "Failed to create character. Name may be taken."
                }
                _createCharacterResult.value = AuthResult.Error(message)
            }
        }
    }

    fun clearCreateCharacterResult() {
        _createCharacterResult.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun train(statType: StatType) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.train(userId, statType)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    suspend fun completeMission(mission: Mission): Boolean {
        val userId = currentUser.value?.uid ?: return false
        return try {
            gameRepository.completeMission(userId, mission.id)
            true
        } catch (e: Exception) {
            _errorMessage.value = e.message
            false
        }
    }

    fun startTravel(destination: String) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.startTravel(userId, destination)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun combatAction(action: CombatAction, techniqueId: String? = null, itemId: String? = null) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.combatAction(userId, action, techniqueId, itemId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun attackPlayer(target: Character) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.attackPlayer(userId, target.id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun equipItem(item: Item) {
        val char = character.value ?: return
        if (char.level < item.levelRequirement) return
        viewModelScope.launch {
            try {
                gameRepository.equipItem(item.id, item.type.name)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun unequipItem(slot: String) {
        if (currentUser.value == null) return
        viewModelScope.launch {
            try {
                gameRepository.unequipItem(slot)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun useItem(item: Item) {
        viewModelScope.launch {
            try {
                gameRepository.useItem(item.id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun sellItem(item: Item) {
        if (currentUser.value == null) return
        viewModelScope.launch {
            try {
                gameRepository.sellItem(item.id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun purchaseItem(itemId: String, shopId: String) {
        viewModelScope.launch {
            try {
                gameRepository.purchaseItem(itemId, shopId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun purchaseShip(shipId: String) {
        viewModelScope.launch {
            try {
                gameRepository.purchaseShip(shipId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun finishTravel() {
        viewModelScope.launch {
            try {
                gameRepository.finishTravel()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun createCrew(name: String, description: String) {
        viewModelScope.launch {
            try {
                crewRepository.createCrew(name, description)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun joinCrew(crewId: String) {
        viewModelScope.launch {
            try {
                crewRepository.joinCrew(crewId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun leaveCrew() {
        viewModelScope.launch {
            try {
                crewRepository.leaveCrew()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun inviteToCrew(targetId: String) {
        viewModelScope.launch {
            try {
                crewRepository.inviteToCrew(targetId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun respondToInvite(crewId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                crewRepository.respondToInvite(crewId, accept)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun promoteMember(targetId: String, rank: String) {
        viewModelScope.launch {
            try {
                crewRepository.promoteMember(targetId, rank)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun joinFaction(faction: Faction) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.joinFaction(userId, faction)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun startHealing() {
        viewModelScope.launch {
            try {
                gameRepository.startHealing()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun instantHeal() {
        viewModelScope.launch {
            try {
                gameRepository.instantHeal()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
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
            try {
                socialRepository.blockPlayer(targetId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun seedWorld() {
        viewModelScope.launch {
            try {
                adminRepository.seedWorld()
                _errorMessage.value = "World seeded successfully!"
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
