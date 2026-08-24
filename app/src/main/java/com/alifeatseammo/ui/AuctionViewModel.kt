package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuctionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val auctionRepository: AuctionRepository
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
                delay(2000)
                if (_actionState.value is UIActionState.Success && (_actionState.value as UIActionState.Success).label == label) {
                    _actionState.value = UIActionState.Idle
                }
            } catch (e: Exception) {
                android.util.Log.e("AuctionViewModel", "Action $label failed", e)
                _actionState.value = UIActionState.Error(e.message ?: "Action failed")
                _errorMessage.value = e.message
            }
        }
    }

    private val currentUser = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    val character = currentUser.flatMapLatest { user ->
        if (user != null) gameRepository.getCharacter(user.uid)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val auctionListings: StateFlow<List<AuctionListing>> = auctionRepository.getAuctionListings()
        .catch { e ->
            android.util.Log.e("AuctionViewModel", "Error in auctionListings flow", e)
            _errorMessage.value = "Failed to load auctions: ${e.localizedMessage}"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun listAuctionItem(item: Item, price: Int) {
        if (price <= 0) {
            _errorMessage.value = "Price must be greater than 0"
            return
        }
        performAction("Listing ${item.name}") {
            auctionRepository.listAuctionItem(item.id, price)
        }
    }

    fun buyAuctionItem(listing: AuctionListing) {
        val char = character.value ?: return
        if (char.gold < listing.price) {
            _errorMessage.value = "Insufficient gold"
            return
        }
        if (char.id == listing.sellerId) {
            _errorMessage.value = "You cannot buy your own listing"
            return
        }
        performAction("Buying ${listing.item.name}") {
            auctionRepository.buyAuctionItem(listing.id)
        }
    }

    fun cancelAuctionListing(listing: AuctionListing) {
        performAction("Canceling Listing") {
            auctionRepository.cancelAuctionListing(listing.id)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
