package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.*
import com.alifeatseammo.util.HapticManager
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject

sealed class CharacterState {
    object Loading : CharacterState()
    object NoCharacter : CharacterState()
    data class Loaded(val character: Character) : CharacterState()
    data class Error(val message: String) : CharacterState()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val crewRepository: CrewRepository,
    private val adminRepository: AdminRepository,
    private val hapticManager: HapticManager,
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
        .map { (it as? CharacterState.Loaded)?.character }
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
        viewModelScope.launchUIAction(label, _actionState, _errorMessage, block = block)
    }

    val isAdmin: StateFlow<Boolean> = character
        .map { char ->
            (char?.isAdmin == true) || (char?.isHardcodedAdmin() == true)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = false)

    val worldVersion: StateFlow<Int?> = adminRepository.getWorldVersion()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                try {
                    _isSyncing.value = true
                    gameRepository.heartbeat() 
                    _isSyncing.value = false
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Heartbeat sync failed - this is usually transient", e)
                    _isSyncing.value = false
                    hapticManager.vibrateError()
                }
                delay(30.seconds) 
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleTravelFinish(arrivalTime: Long) {
        travelJob?.cancel()
        val delayMs = (arrivalTime - System.currentTimeMillis()) + 2000 // 2s buffer for clock skew
        travelJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            finishTravel()
        }
    }

    private fun scheduleHealingFinish(endTime: Long) {
        healingJob?.cancel()
        val delayMs = (endTime - System.currentTimeMillis()) + 2000 // 2s buffer for clock skew
        healingJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            finishHealing()
        }
    }

    private fun scheduleTrainingFinish(endTime: Long) {
        trainingJob?.cancel()
        val delayMs = (endTime - System.currentTimeMillis()) + 2000 // 2s buffer for clock skew
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

    private val _leaderboardSort = MutableStateFlow("level")
    val leaderboardSort: StateFlow<String> = _leaderboardSort.asStateFlow()

    private val _leaderboardCrewSort = MutableStateFlow("level")
    val leaderboardCrewSort: StateFlow<String> = _leaderboardCrewSort.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val topPlayers: StateFlow<List<Character>> = combine(
        leaderboardFaction,
        leaderboardSort,
    ) { faction, sort ->
        faction to sort
    }.flatMapLatest { (faction, sort) ->
        gameRepository.getTopPlayers(20, faction, sort)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val topCrews: StateFlow<List<Crew>> = leaderboardCrewSort
        .flatMapLatest { sort ->
            crewRepository.getTopCrews(20, sort)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setLeaderboardFaction(faction: Faction?) {
        _leaderboardFaction.value = faction
    }

    fun setLeaderboardSort(sort: String) {
        _leaderboardSort.value = sort
    }

    fun setLeaderboardCrewSort(sort: String) {
        _leaderboardCrewSort.value = sort
    }

    val missions: StateFlow<List<Mission>> = gameRepository.getAvailableMissions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredMissions: StateFlow<List<Mission>> = combine(
        missions,
        character
    ) { allMissions, char ->
        if (char == null) return@combine emptyList<Mission>()
        allMissions.filter { 
            it.factionRequirement == Faction.Neutral || it.factionRequirement == char.faction 
        }.sortedByDescending { it.isRankUp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val techniques: StateFlow<List<Technique>> = gameRepository.getTechniques()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeRaids: StateFlow<List<RaidBoss>> = gameRepository.getActiveRaids()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val islandQuests: StateFlow<List<IslandQuest>> = gameRepository.getIslandQuests()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val announcements: StateFlow<List<String>> = gameRepository.getGlobalAnnouncements()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val warState: StateFlow<WarState?> = gameRepository.getWarState()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentLocationInfo: StateFlow<LocationDef?> = combine(
        character.map { it?.currentLocation }.distinctUntilChanged(),
        locations,
    ) { locationName, allLocs ->
        if (locationName != null) {
            allLocs.find { it.name.equals(locationName, ignoreCase = true) }
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _travelResult = MutableStateFlow<String?>(null)
    val travelResult: StateFlow<String?> = _travelResult.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearTravelResult() {
        _travelResult.value = null
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

    fun completeQuest(questId: String) {
        performAction("Completing Quest") {
            gameRepository.completeQuest(questId)
        }
    }

    fun raidCombatAction(raidId: String, action: CombatAction, techniqueId: String? = null, itemId: String? = null) {
        performAction("Raid Action") {
            gameRepository.raidCombatAction(raidId, action, techniqueId, itemId)
        }
    }

    fun engageRaid(raidId: String) {
        performAction("Engaging Boss") {
            gameRepository.engageRaid(raidId)
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
                val destination = character.value?.travelState?.destination
                gameRepository.finishTravel()
                _travelResult.value = destination ?: "your destination"
                hapticManager.vibrateSuccess()
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to finish travel", e)
                hapticManager.vibrateError()
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

    fun rollMythicArt() {
        performAction("Rolling Mythic Art") {
            gameRepository.rollMythicArt()
        }
    }

    fun seedWorld(version: Int) {
        performAction("Seeding World V$version") {
            val message = adminRepository.seedWorld(version)
            _errorMessage.value = message
        }
    }

    fun mutePlayer(userId: String, reason: String, durationHours: Int) {
        performAction("Muting Player") {
            adminRepository.mutePlayer(userId, reason, durationHours)
        }
    }

    fun banPlayer(userId: String, reason: String) {
        performAction("Banning Player") {
            adminRepository.banPlayer(userId, reason)
        }
    }

    fun teleportPlayer(userId: String, location: String) {
        performAction("Teleporting Player") {
            adminRepository.teleportPlayer(userId, location)
        }
    }

    fun adjustGold(userId: String, amount: Int, reason: String) {
        performAction("Adjusting Gold") {
            adminRepository.adjustGold(userId, amount, reason)
        }
    }

    fun sendGlobalAnnouncement(message: String) {
        performAction("Sending Announcement") {
            adminRepository.sendGlobalAnnouncement(message)
        }
    }

    fun searchPlayers(query: String) = adminRepository.searchPlayers(query)
}
