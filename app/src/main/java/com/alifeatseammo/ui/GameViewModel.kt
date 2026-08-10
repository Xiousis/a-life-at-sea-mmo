package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class GameViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val gameRepository: GameRepository = FirestoreGameRepository(),
    private val chatRepository: ChatRepository = FirestoreChatRepository()
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    val character: StateFlow<Character?> = currentUser
        .flatMapLatest { user ->
            if (user != null) gameRepository.getCharacter(user.uid)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val missions: List<Mission> = gameRepository.getAvailableMissions()

    val chatMessages: StateFlow<List<ChatMessage>> = chatRepository.getMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createCharacter(name: String, origin: String, style: CombatStyle) {
        val user = currentUser.value ?: return
        gameRepository.createCharacter(user.uid, name, origin, style)
    }

    fun train(statType: StatType) {
        val user = currentUser.value ?: return
        gameRepository.train(user.uid, statType)
    }

    fun completeMission(mission: Mission) {
        val user = currentUser.value ?: return
        gameRepository.completeMission(user.uid, mission)
    }

    fun startTravel(destination: String, arrivalTime: Long) {
        val user = currentUser.value ?: return
        gameRepository.startTravel(user.uid, destination, arrivalTime)
    }

    fun attackPlayer(target: Character) {
        val user = currentUser.value ?: return
        gameRepository.attackPlayer(user.uid, target.id)
    }

    fun sendMessage(text: String) {
        val user = currentUser.value ?: return
        val char = character.value ?: return
        chatRepository.sendMessage(char.name, text)
    }
}
