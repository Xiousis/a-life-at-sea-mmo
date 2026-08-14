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
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val currentUser = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    private val character = currentUser.flatMapLatest { user ->
        if (user != null) gameRepository.getCharacter(user.uid)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val chatMessages: StateFlow<List<ChatMessage>> = chatRepository.getMessages("global")
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
                if (char != null && char.faction == Faction.Navy && char.infamy >= 100 && char.crewId != null) {
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
        viewModelScope.launch {
            try {
                crewRepository.createCrew(name, description)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun joinCrew(crewId: String) {
        val char = character.value ?: return
        viewModelScope.launch {
            try {
                val crew = crewRepository.getCrew(crewId).firstOrNull()
                if (crew != null && crew.faction != char.faction) {
                    _errorMessage.value = "You can only join crews of your own faction."
                    return@launch
                }
                crewRepository.joinCrew(crewId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun leaveCrew() {
        viewModelScope.launch {
            try {
                crewRepository.leaveCrew()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun inviteToCrew(targetId: String) {
        viewModelScope.launch {
            try {
                crewRepository.inviteToCrew(targetId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun respondToInvite(crewId: String, accept: Boolean) {
        if (!accept) {
            viewModelScope.launch {
                try {
                    crewRepository.respondToInvite(crewId, false)
                } catch (e: Exception) {
                    _errorMessage.value = e.message
                }
            }
            return
        }

        val char = character.value ?: return
        viewModelScope.launch {
            try {
                val crew = crewRepository.getCrew(crewId).firstOrNull()
                if (crew != null && crew.faction != char.faction) {
                    _errorMessage.value = "You cannot join a crew of a different faction."
                    crewRepository.respondToInvite(crewId, false)
                    return@launch
                }
                crewRepository.respondToInvite(crewId, accept)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun promoteMember(targetId: String, rank: String) {
        viewModelScope.launch {
            try {
                crewRepository.promoteMember(targetId, rank)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun addFriend(targetId: String) {
        viewModelScope.launch {
            try {
                socialRepository.sendFriendRequest(targetId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun blockPlayer(targetId: String) {
        viewModelScope.launch {
            try {
                socialRepository.blockPlayer(targetId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
