package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.LocationDef
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TravelViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val locations: StateFlow<List<LocationDef>> = gameRepository.getLocations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun startTravel(destination: String) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.startTravel(userId, destination)
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

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
