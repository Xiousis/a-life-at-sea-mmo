package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.CombatAction
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CombatViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun combatAction(action: CombatAction, techniqueId: String? = null, itemId: String? = null) {
        viewModelScope.launch {
            try {
                gameRepository.combatAction(action, techniqueId, itemId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun attackPlayer(target: Character) {
        viewModelScope.launch {
            try {
                gameRepository.attackPlayer(target.id)
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

    fun claimVictoryRewards() {
        combatAction(CombatAction.Flee)
    }

    fun retreatFromDefeat() {
        combatAction(CombatAction.Flee)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
