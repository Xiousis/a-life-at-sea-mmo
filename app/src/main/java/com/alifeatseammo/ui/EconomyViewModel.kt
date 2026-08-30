package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.*
import com.google.firebase.functions.FirebaseFunctionsException
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

    private val _actionState = MutableStateFlow<UIActionState>(UIActionState.Idle)
    val actionState: StateFlow<UIActionState> = _actionState.asStateFlow()

    private fun performAction(label: String, block: suspend () -> Unit) {
        viewModelScope.launchUIAction(
            label = label,
            actionState = _actionState,
            errorState = _errorMessage,
            onError = { e ->
                if (e is FirebaseFunctionsException) {
                    android.util.Log.e("EconomyViewModel", "Function failed: code=${e.code}, details=${e.details}", e)
                    e.message ?: "${e.code}: Action failed"
                } else {
                    e.message ?: "Action failed"
                }
            },
            block = block
        )
    }

    private val currentUser = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    private val character = currentUser.flatMapLatest { user ->
        if (user != null) gameRepository.getCharacter(user.uid)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _marketCategory = MutableStateFlow<String?>(null)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val marketItems: StateFlow<List<Item>> = _marketCategory
        .flatMapLatest { gameRepository.getMarketItems(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setMarketCategory(category: String?) {
        _marketCategory.value = category
    }

    val availableShips: StateFlow<List<Ship>> = flowOf(
        listOf(
            Ship("row_boat", "Row Boat", 0, 1.0f),
            Ship("sloop", "Sloop", 500, 1.5f),
            Ship("caravel", "Caravel", 2500, 2.0f),
            Ship("galleon", "Galleon", 10000, 3.0f),
        )
    ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mailMessages: StateFlow<List<MailMessage>> = currentUser
        .flatMapLatest { user ->
            if (user != null) gameRepository.getMailMessages(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun equipItem(item: Item) {
        val char = character.value ?: return
        if (!char.canEquip(item)) {
            val missing = char.getMissingRequirements(item).joinToString(", ")
            _errorMessage.value = "Missing requirements: $missing"
            return
        }
        performAction("Equipping ${item.name}") {
            gameRepository.equipItem(item.id, item.slot ?: item.type.name)
        }
    }

    fun unequipItem(slot: String) {
        performAction("Unequipping $slot") {
            gameRepository.unequipItem(slot)
        }
    }

    fun useItem(item: Item) {
        performAction("Using ${item.name}") {
            gameRepository.useItem(item.id)
        }
    }

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    fun loadRecipes() {
        viewModelScope.launch {
            try {
                _recipes.value = gameRepository.getRecipes()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load recipes"
            }
        }
    }

    fun cook(recipe: Recipe) {
        val char = character.value ?: return
        if (char.professionStats.cooking < recipe.levelRequirement) {
            _errorMessage.value = "Cooking level too low"
            return
        }
        performAction("Cooking ${recipe.name}") {
            gameRepository.cook(recipe.id)
        }
    }

    fun cookFish(item: Item) {
        performAction("Cooking ${item.name}") {
            gameRepository.cookFish(item.id)
        }
    }

    fun sellItem(item: Item) {
        performAction("Selling ${item.name}") {
            gameRepository.sellItem(item.id)
        }
    }

    fun purchaseItem(itemId: String, shopId: String) {
        performAction("Purchasing Item") {
            gameRepository.purchaseItem(itemId, shopId)
        }
    }

    fun purchaseShip(shipId: String) {
        performAction("Purchasing Ship") {
            gameRepository.purchaseShip(shipId)
        }
    }

    fun switchActiveShip(shipId: String) {
        performAction("Switching Active Ship") {
            gameRepository.switchActiveShip(shipId)
        }
    }

    fun upgradeShip(upgradeType: String) {
        performAction("Upgrading Ship") {
            gameRepository.upgradeShip(upgradeType)
        }
    }

    fun claimMailRewards(mailId: String) {
        performAction("Claiming Rewards") {
            gameRepository.claimMailRewards(mailId)
        }
    }

    fun deleteMail(mailId: String) {
        performAction("Deleting Mail") {
            gameRepository.deleteMail(mailId)
        }
    }

    fun sendMail(recipientId: String, subject: String, body: String) {
        if (recipientId.isBlank() || subject.isBlank() || body.isBlank()) {
            _errorMessage.value = "Recipient, subject, and body are required"
            return
        }
        performAction("Sending Mail") {
            gameRepository.sendMail(recipientId, subject, body)
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
