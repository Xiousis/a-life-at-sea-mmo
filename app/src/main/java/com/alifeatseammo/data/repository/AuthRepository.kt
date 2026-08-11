package com.alifeatseammo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    val currentUser: StateFlow<FirebaseUser?>
    fun signInAnonymously(onComplete: (Boolean) -> Unit)
    fun signIn(email: String, password: String, onComplete: (Boolean) -> Unit)
    fun signUp(email: String, password: String, username: String, onComplete: (Boolean) -> Unit)
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

    override fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        auth.signInAnonymously().addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    override fun signIn(email: String, password: String, onComplete: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    override fun signUp(email: String, password: String, username: String, onComplete: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = username
                }
                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    onComplete(true)
                }
            } else {
                onComplete(false)
            }
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
