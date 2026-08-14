package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EconomyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val currentUser = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    private val character = currentUser.flatMapLatest { user ->
        if (user != null) gameRepository.getCharacter(user.uid)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val marketItems: StateFlow<List<Item>> = gameRepository.getMarketItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mailMessages: StateFlow<List<MailMessage>> = currentUser
        .flatMapLatest { user ->
            if (user != null) gameRepository.getMailMessages(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun cookFish(item: Item) {
        val char = character.value ?: return
        if (char.mythicArt?.tier == "Z") {
            _errorMessage.value = "A God's Eye user does not cook. They create or annihilate."
            return
        }
        viewModelScope.launch {
            try {
                gameRepository.cookFish(item.id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun sellItem(item: Item) {
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

    fun claimMailRewards(mailId: String) {
        viewModelScope.launch {
            try {
                gameRepository.claimMailRewards(mailId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteMail(mailId: String) {
        viewModelScope.launch {
            try {
                gameRepository.deleteMail(mailId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun markMailAsRead(mailId: String) {
        viewModelScope.launch {
            try {
                gameRepository.markMailAsRead(mailId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
