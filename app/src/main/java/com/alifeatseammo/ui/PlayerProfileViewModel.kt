package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Crew
import com.alifeatseammo.data.repository.CrewRepository
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val crewRepository: CrewRepository
) : ViewModel() {

    private val _playerId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playerProfile: StateFlow<Character?> = _playerId
        .filterNotNull()
        .flatMapLatest { id -> gameRepository.getPlayerProfile(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playerCrew: StateFlow<Crew?> = playerProfile
        .map { it?.crewId }
        .distinctUntilChanged()
        .flatMapLatest { crewId ->
            if (crewId != null) crewRepository.getCrew(crewId)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val crewMembers: StateFlow<List<Character>> = playerCrew
        .map { it?.members ?: emptyList() }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            gameRepository.getCharacters(ids)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadPlayer(playerId: String) {
        _playerId.value = playerId
    }
}
