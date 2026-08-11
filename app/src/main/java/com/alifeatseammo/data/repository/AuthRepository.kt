package com.alifeatseammo.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Loading : AuthResult()
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    val currentUser: StateFlow<FirebaseUser?>
    suspend fun signInAnonymously(): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String, username: String): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    suspend fun upgradeGuestAccount(email: String, password: String): AuthResult
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    private val _currentUser = MutableStateFlow(auth.currentUser)
    override val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener {
            _currentUser.value = it.currentUser
        }
    }

    override suspend fun signInAnonymously(): AuthResult {
        return try {
            auth.signInAnonymously().await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Anonymous sign-in failed")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Login failed")
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = username
            }
            user?.updateProfile(profileUpdates)?.await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to send reset email")
        }
    }

    override suspend fun upgradeGuestAccount(email: String, password: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("No active guest session")
        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(credential).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to upgrade account")
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
