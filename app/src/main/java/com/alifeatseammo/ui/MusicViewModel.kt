package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.GameRepository
import com.alifeatseammo.util.MusicManager
import com.alifeatseammo.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val musicManager: MusicManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTrack: StateFlow<Int> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(R.raw.life_at_sea_menu_sound)
            } else {
                combine(
                    gameRepository.getCharacter(user.uid),
                    gameRepository.getLocations()
                ) { character, locations ->
                    if (character == null) {
                        R.raw.life_at_sea_menu_sound
                    } else {
                        val location = locations.find { it.name.equals(character.currentLocation, ignoreCase = true) }
                        when {
                            character.travelState != null -> R.raw.life_at_sea_traveling_music
                            location?.name?.startsWith("Navy Outpost", ignoreCase = true) == true -> R.raw.navy_outpost_music
                            location?.name?.equals("Pirate's Den", ignoreCase = true) == true -> R.raw.pirate_den_music
                            else -> R.raw.life_at_sea_menu_sound
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, R.raw.life_at_sea_menu_sound)

    init {
        currentTrack.onEach { trackResId ->
            musicManager.play(trackResId)
        }.launchIn(viewModelScope)
    }
}
