package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.CombatAction
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CombatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun combatAction(action: CombatAction, techniqueId: String? = null, itemId: String? = null) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.combatAction(userId, action, techniqueId, itemId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun attackPlayer(target: Character) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                gameRepository.attackPlayer(userId, target.id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
