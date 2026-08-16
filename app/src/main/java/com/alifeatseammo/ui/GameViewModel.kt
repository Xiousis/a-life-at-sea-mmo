package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.*
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

sealed class CharacterState {
    object Loading : CharacterState()
    object NoCharacter : CharacterState()
    data class Loaded(val character: Character) : CharacterState()
    data class Error(val message: String) : CharacterState()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    val characterState: StateFlow<CharacterState> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                gameRepository.getCharacter(user.uid)
                    .map { if (it == null) CharacterState.NoCharacter else CharacterState.Loaded(it) }
                    .catch { e ->
                        Log.e("GameViewModel", "Error fetching character", e)
                        emit(CharacterState.Error(e.message ?: "Unknown error"))
                    }
            } else flowOf(CharacterState.NoCharacter)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, CharacterState.Loading)

    private var travelJob: Job? = null
    private var healingJob: Job? = null
    private var trainingJob: Job? = null
    private var heartbeatJob: Job? = null

    val character: StateFlow<Character?> = characterState
        .map { if (it is CharacterState.Loaded) it.character else null }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _actionState = MutableStateFlow<UIActionState>(UIActionState.Idle)
    val actionState: StateFlow<UIActionState> = _actionState.asStateFlow()

    init {
        // Optimize Travel Timer
        character
            .map { it?.travelState?.arrivalTime }
            .distinctUntilChanged()
            .onEach { arrivalTime ->
                if (arrivalTime != null) {
                    scheduleTravelFinish(arrivalTime)
                } else {
                    travelJob?.cancel()
                    travelJob = null
                }
            }
            .launchIn(viewModelScope)

        // Optimize Healing Timer
        character
            .map { it?.healingState?.endTime }
            .distinctUntilChanged()
            .onEach { endTime ->
                if (endTime != null) {
                    scheduleHealingFinish(endTime)
                } else {
                    healingJob?.cancel()
                    healingJob = null
                }
            }
            .launchIn(viewModelScope)

        // Optimize Training Timer
        character
            .map { it?.trainingState?.endTime }
            .distinctUntilChanged()
            .onEach { endTime ->
                if (endTime != null) {
                    scheduleTrainingFinish(endTime)
                } else {
                    trainingJob?.cancel()
                    trainingJob = null
                }
            }
            .launchIn(viewModelScope)

        // Heartbeat management
        character
            .map { it != null }
            .distinctUntilChanged()
            .onEach { isActive ->
                if (isActive) startHeartbeat() else stopHeartbeat()
            }
            .launchIn(viewModelScope)
    }

    private fun performAction(label: String, block: suspend () -> Unit) {
        if (_actionState.value is UIActionState.Loading) return

        viewModelScope.launch {
            _actionState.value = UIActionState.Loading(label)
            try {
                block()
                _actionState.value = UIActionState.Success(label)
                delay(2000)
                if (_actionState.value is UIActionState.Success && (_actionState.value as UIActionState.Success).label == label) {
                    _actionState.value = UIActionState.Idle
                }
            } catch (e: Exception) {
                _actionState.value = UIActionState.Error(e.message ?: "Action failed")
                _errorMessage.value = e.message
            }
        }
    }

    val isAdmin: StateFlow<Boolean> = character
        .map { char ->
            val name = char?.name?.lowercase()?.trim() ?: ""
            val adminNames = listOf("sedna", "von")
            val isMatch = adminNames.contains(name)
            Log.d("GameViewModel", "Admin check for name '$name': $isMatch")
            isMatch
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                try {
                    gameRepository.heartbeat() 
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Heartbeat sync failed - this is usually transient", e)
                }
                delay(30000) 
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleTravelFinish(arrivalTime: Long) {
        travelJob?.cancel()
        val delayMs = arrivalTime - System.currentTimeMillis()
        travelJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            finishTravel()
        }
    }

    private fun scheduleHealingFinish(endTime: Long) {
        healingJob?.cancel()
        val delayMs = endTime - System.currentTimeMillis()
        healingJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            finishHealing()
        }
    }

    private fun scheduleTrainingFinish(endTime: Long) {
        trainingJob?.cancel()
        val delayMs = (endTime - System.currentTimeMillis()) + 500 // Add 500ms buffer for clock skew
        trainingJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            finishTraining()
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

    private val _leaderboardFaction = MutableStateFlow<Faction?>(Faction.Pirate)
    val leaderboardFaction: StateFlow<Faction?> = _leaderboardFaction.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val topPlayers: StateFlow<List<Character>> = leaderboardFaction
        .flatMapLatest { faction ->
            gameRepository.getTopPlayers(20, faction)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setLeaderboardFaction(faction: Faction?) {
        _leaderboardFaction.value = faction
    }

    val missions: StateFlow<List<Mission>> = gameRepository.getAvailableMissions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val techniques: StateFlow<List<Technique>> = gameRepository.getTechniques()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentLocationInfo: StateFlow<LocationDef?> = combine(
        character.map { it?.currentLocation }.distinctUntilChanged(),
        locations
    ) { locationName, allLocs ->
        if (locationName != null) {
            allLocs.find { it.name.equals(locationName, ignoreCase = true) }
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun train(statType: StatType) {
        performAction("Training ${statType.name}") {
            gameRepository.train(statType)
        }
    }

    suspend fun completeMission(mission: Mission): Boolean {
        _actionState.value = UIActionState.Loading("Completing Mission")
        return try {
            gameRepository.completeMission(mission.id)
            _actionState.value = UIActionState.Success("Mission Completed")
            true
        } catch (e: Exception) {
            _actionState.value = UIActionState.Error(e.message ?: "Mission failed")
            _errorMessage.value = e.message
            false
        } finally {
            delay(2000)
            if (_actionState.value is UIActionState.Success) {
                _actionState.value = UIActionState.Idle
            }
        }
    }

    fun joinFaction(faction: Faction) {
        performAction("Joining ${faction.name}") {
            gameRepository.joinFaction(faction)
        }
    }

    fun adminGrantTestItems() {
        performAction("Granting Test Items") {
            gameRepository.adminGrantTestItems()
        }
    }

    fun startHealing() {
        performAction("Starting Rest") {
            gameRepository.startHealing()
        }
    }

    fun instantHeal() {
        performAction("Instant Healing") {
            gameRepository.instantHeal()
        }
    }

    fun purchaseMedicalLicense() {
        performAction("Purchasing Medical License") {
            gameRepository.purchaseMedicalLicense()
        }
    }

    fun healPlayer(targetPlayerId: String) {
        performAction("Healing Player") {
            gameRepository.healPlayer(targetPlayerId)
        }
    }

    fun finishTravel() {
        viewModelScope.launch {
            try {
                gameRepository.finishTravel()
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to finish travel", e)
            }
        }
    }

    fun finishHealing() {
        viewModelScope.launch {
            try {
                gameRepository.finishHealing()
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to finish healing", e)
            }
        }
    }

    fun finishTraining() {
        viewModelScope.launch {
            try {
                gameRepository.finishTraining()
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to finish training", e)
            }
        }
    }

    fun startMonsterHunt() {
        performAction("Starting Monster Hunt") {
            gameRepository.startMonsterHunt()
        }
    }

    fun rollMythicArt() {
        performAction("Rolling Mythic Art") {
            gameRepository.rollMythicArt()
        }
    }

    fun seedWorld() {
        viewModelScope.launch {
            try {
                val message = adminRepository.seedWorld()
                _errorMessage.value = message
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }
}
