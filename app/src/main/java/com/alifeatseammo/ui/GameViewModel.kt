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
        .onEach { char ->
            val travel = char?.travelState
            if (travel != null) {
                scheduleTravelFinish(travel.arrivalTime)
            } else {
                travelJob?.cancel()
                travelJob = null
            }

            val healing = char?.healingState
            if (healing != null) {
                scheduleHealingFinish(healing.endTime)
            } else {
                healingJob?.cancel()
                healingJob = null
            }

            val training = char?.trainingState
            if (training != null) {
                scheduleTrainingFinish(training.endTime)
            } else {
                trainingJob?.cancel()
                trainingJob = null
            }

            if (char != null && heartbeatJob == null) {
                startHeartbeat()
            } else if (char == null) {
                stopHeartbeat()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

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
                    Log.e("GameViewModel", "Heartbeat failed", e)
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

    fun adminGrantTestItems() {
        viewModelScope.launch {
            try {
                gameRepository.adminGrantTestItems()
            } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
                Log.e("GameViewModel", "Functions error: ${e.code}", e)
                _errorMessage.value = "Grant Error: ${e.code} (${e.message})"
            } catch (e: Exception) {
                Log.e("GameViewModel", "Grant failed", e)
                _errorMessage.value = "Failed to grant test items: ${e.message}"
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

    fun purchaseMedicalLicense() {
        viewModelScope.launch {
            try {
                gameRepository.purchaseMedicalLicense()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun healPlayer(targetPlayerId: String) {
        viewModelScope.launch {
            try {
                gameRepository.healPlayer(targetPlayerId)
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

    fun finishHealing() {
        viewModelScope.launch {
            try {
                gameRepository.finishHealing()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun finishTraining() {
        viewModelScope.launch {
            try {
                gameRepository.finishTraining()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun startMonsterHunt() {
        viewModelScope.launch {
            try {
                gameRepository.startMonsterHunt()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun rollMythicArt() {
        viewModelScope.launch {
            try {
                gameRepository.rollMythicArt()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
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
