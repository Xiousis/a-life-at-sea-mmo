package com.alifeatseammo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alifeatseammo.data.model.Gender
import com.alifeatseammo.data.model.Race
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.AuthResult
import com.alifeatseammo.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult.asStateFlow()

    private val _createCharacterResult = MutableStateFlow<AuthResult?>(null)
    val createCharacterResult: StateFlow<AuthResult?> = _createCharacterResult.asStateFlow()

    fun signIn() {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signInAnonymously()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signIn(email, password)
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.signUp(email, password, username)
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.sendPasswordResetEmail(email)
        }
    }

    fun upgradeGuestAccount(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            _authResult.value = authRepository.upgradeGuestAccount(email, password)
        }
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                gameRepository.explicitLogout()
            } catch (_: Exception) {}
            authRepository.signOut()
            _authResult.value = null
        }
    }

    fun createCharacter(name: String, gender: Gender, race: Race) {
        val userId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            _createCharacterResult.value = AuthResult.Loading
            try {
                gameRepository.createCharacter(userId, name, gender, race)
                delay(2000)
                _createCharacterResult.value = null
            } catch (e: Exception) {
                val message = if (e is com.google.firebase.functions.FirebaseFunctionsException && e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND) {
                    "Function not found. Please ensure backend is deployed and on Blaze plan."
                } else {
                    e.message ?: "Failed to create character. Name may be taken."
                }
                _createCharacterResult.value = AuthResult.Error(message)
            }
        }
    }

    fun clearCreateCharacterResult() {
        _createCharacterResult.value = null
    }
}
