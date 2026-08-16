package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.GameRepository
import com.alifeatseammo.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin
import kotlin.random.Random

enum class FishingState {
    IDLE, CASTING, WAITING, HOOKED, SUCCESS, FAILURE
}

@HiltViewModel
class FishingViewModel @Inject constructor(
    private val authRepository: com.alifeatseammo.data.repository.AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FishingState.IDLE)
    val state: StateFlow<FishingState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _fishPosition = MutableStateFlow(50f) // 0 to 100
    val fishPosition: StateFlow<Float> = _fishPosition.asStateFlow()

    private val _barPosition = MutableStateFlow(50f) // 0 to 100
    val barPosition: StateFlow<Float> = _barPosition.asStateFlow()

    private val _progress = MutableStateFlow(0f) // 0 to 100
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _caughtFish = MutableStateFlow<FishDef?>(null)
    val caughtFish: StateFlow<FishDef?> = _caughtFish.asStateFlow()

    private var gameJob: Job? = null
    private var barVelocity = 0f
    private val gravity = 0.15f
    private val thrust = 0.35f
    private var isPressing = false

    private val availableFish = listOf(
        FishDef("sardine", "Sardine", Rarity.Common, 0.8f, FishingMovementPattern.Steady, 5, 2),
        FishDef("mackerel", "Mackerel", Rarity.Common, 1.2f, FishingMovementPattern.Steady, 10, 5),
        FishDef("tuna", "Tuna", Rarity.Uncommon, 2.0f, FishingMovementPattern.Darting, 50, 15),
        FishDef("swordfish", "Swordfish", Rarity.Rare, 3.5f, FishingMovementPattern.Darting, 200, 50),
        FishDef("kraken_tentacle", "Kraken Tentacle", Rarity.Legendary, 5.0f, FishingMovementPattern.Sinker, 1000, 250)
    )

    fun startFishing() {
        if (_state.value != FishingState.IDLE) return
        
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser.value?.uid ?: throw Exception("User not logged in")
                val character = gameRepository.getCharacter(userId).firstOrNull() ?: throw Exception("Character not found")
                
                if (character.mythicArt?.tier == "Z") {
                    _errorMessage.value = "Your Mythic Art is too powerful for such a mundane activity as fishing."
                    return@launch
                }

                val fishId = gameRepository.startFishing() ?: throw Exception("Failed to start fishing session")
                val fish = availableFish.find { it.id == fishId } ?: availableFish.first()

                _state.value = FishingState.CASTING
                delay(1000)
                if (_state.value != FishingState.CASTING) return@launch // Cancelled

                _state.value = FishingState.WAITING
                delay(Random.nextLong(2000, 5000))
                if (_state.value == FishingState.WAITING) {
                    hookFish(fish)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to start fishing"
                _state.value = FishingState.IDLE
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun hookFish(fish: FishDef) {
        _state.value = FishingState.HOOKED
        _caughtFish.value = fish
        _progress.value = 30f
        _barPosition.value = 50f
        _fishPosition.value = 50f
        barVelocity = 0f
        startGameLoop(fish)
    }

    private fun startGameLoop(fish: FishDef) {
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            var time = 0f
            while (_state.value == FishingState.HOOKED) {
                // Update Bar Physics
                if (isPressing) {
                    barVelocity -= thrust
                } else {
                    barVelocity += gravity
                }
                
                _barPosition.value = (_barPosition.value + barVelocity).coerceIn(0f, 100f)
                if (_barPosition.value == 0f || _barPosition.value == 100f) {
                    barVelocity = 0f
                }

                // Update Fish Movement
                updateFishPosition(fish, time)
                time += 0.05f

                // Check Progress
                val barSize = 15f // Size of the catching bar
                val overlap = _fishPosition.value >= _barPosition.value && 
                             _fishPosition.value <= _barPosition.value + barSize
                
                if (overlap) {
                    _progress.value += 0.5f
                } else {
                    _progress.value -= 0.3f
                }

                if (_progress.value >= 100f) {
                    completeFishing(true)
                } else if (_progress.value <= 0f) {
                    completeFishing(false)
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun updateFishPosition(fish: FishDef, time: Float) {
        val target = when (fish.movementPattern) {
            FishingMovementPattern.Steady -> 50f + 30f * sin(time * fish.baseDifficulty)
            FishingMovementPattern.Sinker -> if (sin(time) > 0) 80f else 20f
            FishingMovementPattern.Floater -> if (sin(time) > 0) 20f else 80f
            FishingMovementPattern.Darting -> {
                if (Random.nextFloat() < 0.05f) Random.nextFloat() * 100f else _fishPosition.value
            }
        }
        // Smoothly move towards target
        _fishPosition.value = _fishPosition.value + (target - _fishPosition.value) * 0.1f
    }

    fun onBarPress(pressing: Boolean) {
        isPressing = pressing
    }

    private fun completeFishing(success: Boolean) {
        gameJob?.cancel()
        viewModelScope.launch {
            if (success) {
                _state.value = FishingState.SUCCESS
                gameRepository.catchFish()
            } else {
                _state.value = FishingState.FAILURE
            }
            delay(3000)
            reset()
        }
    }

    fun reset() {
        _state.value = FishingState.IDLE
        _progress.value = 0f
        _caughtFish.value = null
    }
}
