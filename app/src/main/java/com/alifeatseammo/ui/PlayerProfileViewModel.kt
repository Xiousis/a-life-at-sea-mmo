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
    private val _crewId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playerProfile: StateFlow<Character?> = _playerId
        .flatMapLatest { id ->
            if (id != null) gameRepository.getPlayerProfile(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playerCrew: StateFlow<Crew?> = combine(
        playerProfile.map { it?.crewId }.distinctUntilChanged(),
        _crewId
    ) { fromProfile, direct ->
        direct ?: fromProfile
    }.flatMapLatest { crewId ->
        if (crewId != null) crewRepository.getCrew(crewId)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        _crewId.value = null
    }

    fun loadCrew(crewId: String) {
        _crewId.value = crewId
        _playerId.value = null
    }
}
