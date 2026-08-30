package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.*
import com.google.firebase.functions.FirebaseFunctionsException
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
        viewModelScope.launchUIAction(
            label = label,
            actionState = _actionState,
            errorState = _errorMessage,
            onError = { e ->
                if (e is FirebaseFunctionsException) {
                    android.util.Log.e("AuctionViewModel", "Function failed: code=${e.code}, details=${e.details}", e)
                    when (e.code) {
                        FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                            "Permissions Denied. Please ensure your App Check debug token is registered. Search Logcat for 'DebugAppCheckProvider' to find it."
                        FirebaseFunctionsException.Code.UNAUTHENTICATED -> "You must be logged in to use the Auction House."
                        else -> e.message ?: "${e.code}: Auction action failed"
                    }
                } else {
                    android.util.Log.e("AuctionViewModel", "Action $label failed", e)
                    e.message ?: "Action failed"
                }
            },
            block = block
        )
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

    fun listAuctionItem(item: Item, price: Long) {
        if (price <= 0) {
            _errorMessage.value = "Price must be greater than 0"
            return
        }
        android.util.Log.d("AuctionViewModel", "Listing item: ${item.name} (ID: ${item.id}) for $price")
        performAction("Listing ${item.name}") {
            auctionRepository.listAuctionItem(item.id, price)
        }
    }

    fun buyAuctionItem(listing: AuctionListing) {
        val char = character.value
        if (char == null) {
            _errorMessage.value = "Character data not loaded. Please wait."
            return
        }
        if (char.gold < listing.price) {
            _errorMessage.value = "Insufficient gold"
            return
        }
        if (char.id == listing.sellerId) {
            _errorMessage.value = "You cannot buy your own listing"
            return
        }
        if (char.inventory.size >= char.calculateMaxCapacity()) {
            _errorMessage.value = "Backpack Full! Sell or unequip items to make room."
            return
        }
        android.util.Log.d("AuctionViewModel", "Buying listing: ${listing.id} for ${listing.price}")
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
