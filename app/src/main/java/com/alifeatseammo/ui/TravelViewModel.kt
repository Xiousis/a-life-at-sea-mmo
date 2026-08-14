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
        if (_actionState.value is UIActionState.Loading) return

        viewModelScope.launch {
            _actionState.value = UIActionState.Loading(label)
            try {
                block()
                _actionState.value = UIActionState.Success(label)
                kotlinx.coroutines.delay(2000)
                if (_actionState.value is UIActionState.Success && (_actionState.value as UIActionState.Success).label == label) {
                    _actionState.value = UIActionState.Idle
                }
            } catch (e: Exception) {
                _actionState.value = UIActionState.Error(e.message ?: "Action failed")
                _errorMessage.value = e.message
            }
        }
    }

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
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
