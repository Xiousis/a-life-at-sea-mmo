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
class SocialViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val chatRepository: ChatRepository,
    private val crewRepository: CrewRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _actionState = MutableStateFlow<UIActionState>(UIActionState.Idle)
    val actionState: StateFlow<UIActionState> = _actionState.asStateFlow()

    private fun performAction(label: String, block: suspend () -> Unit) {
        viewModelScope.launchUIAction(label, _actionState, _errorMessage, block = block)
    }

    private val currentUser = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    private val character = currentUser.flatMapLatest { user ->
        if (user != null) gameRepository.getCharacter(user.uid)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val chatMessages: StateFlow<List<ChatMessage>> = chatRepository.getMessages("global")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val friends: StateFlow<List<Character>> = currentUser
        .flatMapLatest { user ->
            if (user != null) socialRepository.getFriends(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pendingRequests: StateFlow<List<Character>> = currentUser
        .flatMapLatest { user ->
            if (user != null) socialRepository.getPendingRequests(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val crewChatMessages: StateFlow<List<ChatMessage>> = character
        .map { it?.crewId }
        .distinctUntilChanged()
        .flatMapLatest { crewId ->
            if (crewId != null) chatRepository.getMessages(crewId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val crewInvites: StateFlow<List<CrewInvite>> = currentUser
        .flatMapLatest { user ->
            if (user != null) crewRepository.getInvitesForUser(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            character.collectLatest { char ->
                if (char != null && (char.faction == Faction.Navy) && (char.infamy >= 100) && (char.crewId != null)) {
                    leaveCrew()
                    _errorMessage.value = "You have been kicked from the Navy and your crew due to high infamy!"
                }
            }
        }
    }

    fun sendMessage(text: String, channelId: String = "global") {
        val char = character.value ?: return
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(char.name, text, channelId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun createCrew(name: String, description: String) {
        val char = character.value ?: return
        if (char.faction == Faction.Neutral) {
            _errorMessage.value = "Only Navy or Pirates can create crews."
            return
        }
        performAction("Creating Crew") {
            crewRepository.createCrew(name, description)
        }
    }

    fun joinCrew(crewId: String) {
        val char = character.value ?: return
        performAction("Joining Crew") {
            val crew = crewRepository.getCrew(crewId).firstOrNull()
            if (crew != null && crew.faction != char.faction) {
                throw Exception("You can only join crews of your own faction.")
            }
            crewRepository.joinCrew(crewId)
        }
    }

    fun leaveCrew() {
        performAction("Leaving Crew") {
            crewRepository.leaveCrew()
        }
    }

    fun inviteToCrew(targetId: String) {
        performAction("Sending Invite") {
            crewRepository.inviteToCrew(targetId)
        }
    }

    fun respondToInvite(crewId: String, accept: Boolean) {
        if (!accept) {
            performAction("Declining Invite") {
                crewRepository.respondToInvite(crewId, accept = false)
            }
            return
        }

        val char = character.value ?: return
        performAction("Accepting Invite") {
            val crew = crewRepository.getCrew(crewId).firstOrNull()
            if (crew != null && crew.faction != char.faction) {
                crewRepository.respondToInvite(crewId, accept = false)
                throw Exception("You cannot join a crew of a different faction.")
            }
            crewRepository.respondToInvite(crewId, accept = accept)
        }
    }

    fun promoteMember(targetId: String, rank: String) {
        performAction("Setting Rank to $rank") {
            crewRepository.promoteMember(targetId, rank)
        }
    }

    fun kickMember(targetId: String) {
        val char = character.value ?: return
        performAction("Kicking Member") {
            val crewId = char.crewId ?: throw Exception("You are not in a crew.")
            val crew = crewRepository.getCrew(crewId).firstOrNull() ?: throw Exception("Crew not found.")
            val myRole = crew.roles[char.id] ?: CrewRole.Member
            if (myRole != CrewRole.Captain && myRole != CrewRole.CoCaptain) {
                throw Exception("Only Captains or Co-Captains can kick members.")
            }
            crewRepository.kickMember(targetId)
        }
    }

    fun donateGold(amount: Long) {
        if (amount <= 0) {
            _errorMessage.value = "Amount must be greater than 0"
            return
        }
        val char = character.value ?: return
        if (char.gold < amount) {
            _errorMessage.value = "Insufficient gold"
            return
        }
        performAction("Donating $amount Gold") {
            crewRepository.donateToCrew(amount)
        }
    }

    fun updateCrewSettings(description: String, isPublic: Boolean) {
        val char = character.value ?: return
        performAction("Updating Crew Settings") {
            val crewId = char.crewId ?: throw Exception("You are not in a crew.")
            val crew = crewRepository.getCrew(crewId).firstOrNull() ?: throw Exception("Crew not found.")
            if (crew.captainId != char.id) {
                throw Exception("Only the Captain can update crew settings.")
            }
            crewRepository.updateCrewSettings(description, isPublic)
        }
    }

    fun toggleCrewPvP(enabled: Boolean) {
        val char = character.value ?: return
        performAction(if (enabled) "Enabling Crew PvP" else "Disabling Crew PvP") {
            val crewId = char.crewId ?: throw Exception("You are not in a crew.")
            val crew = crewRepository.getCrew(crewId).firstOrNull() ?: throw Exception("Crew not found.")
            val myRole = crew.roles[char.id] ?: CrewRole.Member
            if (myRole != CrewRole.Captain && myRole != CrewRole.CoCaptain) {
                throw Exception("Only the Captain or Co-Captain can toggle Crew PvP.")
            }
            crewRepository.toggleCrewPvP(enabled)
        }
    }

    fun upgradeCrewPerk(perk: String) {
        performAction("Upgrading Perk: $perk") {
            crewRepository.upgradeCrewPerk(perk)
        }
    }

    fun addFriend(targetId: String) {
        performAction("Sending Friend Request") {
            socialRepository.sendFriendRequest(targetId)
        }
    }

    fun acceptFriendRequest(senderId: String) {
        performAction("Accepting Friend Request") {
            socialRepository.acceptFriendRequest(senderId)
        }
    }

    fun declineFriendRequest(senderId: String) {
        performAction("Declining Friend Request") {
            socialRepository.declineFriendRequest(senderId)
        }
    }

    fun removeFriend(friendId: String) {
        performAction("Removing Friend") {
            socialRepository.removeFriend(friendId)
        }
    }

    fun blockPlayer(targetId: String) {
        performAction("Blocking Player") {
            socialRepository.blockPlayer(targetId)
        }
    }

    fun unblockPlayer(targetId: String) {
        performAction("Unblocking Player") {
            socialRepository.unblockPlayer(targetId)
        }
    }

    fun getPrivateMessages(otherUserId: String): Flow<List<ChatMessage>> {
        val myId = currentUser.value?.uid ?: return flowOf(emptyList())
        val channelId = if (myId < otherUserId) "pm_${myId}_$otherUserId" else "pm_${otherUserId}_$myId"
        return chatRepository.getMessages(channelId)
    }

    fun sendPrivateMessage(otherUserId: String, text: String) {
        val myId = currentUser.value?.uid ?: return
        val char = character.value ?: return
        val channelId = if (myId < otherUserId) "pm_${myId}_$otherUserId" else "pm_${otherUserId}_$myId"
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(char.name, text, channelId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
