package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.LocationDef
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TravelViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _actionState = MutableStateFlow<UIActionState>(UIActionState.Idle)
    val actionState: StateFlow<UIActionState> = _actionState.asStateFlow()

    private fun performAction(label: String, block: suspend () -> Unit) {
        viewModelScope.launchUIAction(label, _actionState, _errorMessage, block = block)
    }

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeRaids: StateFlow<List<com.alifeatseammo.data.model.RaidBoss>> = gameRepository.getActiveRaids()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val seaEvents: StateFlow<List<com.alifeatseammo.data.model.SeaEvent>> = gameRepository.getSeaEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun startTravel(destination: String) {
        performAction("Traveling to $destination") {
            gameRepository.startTravel(destination)
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

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
