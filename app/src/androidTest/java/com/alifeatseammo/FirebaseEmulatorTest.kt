package com.alifeatseammo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import org.junit.Before

open class FirebaseEmulatorTest {
    protected lateinit var auth: FirebaseAuth
    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var functions: FirebaseFunctions

    @Before
    fun setUpEmulators() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance("us-central1")

        // Point to emulators (10.0.2.2 is the host machine from Android Emulator)
        try {
            auth.useEmulator("10.0.2.2", 9099)
            
            // Firestore requires settings for emulator
            val settings = FirebaseFirestoreSettings.Builder()
                .setHost("10.0.2.2:8080")
                .setSslEnabled(false)
                .setPersistenceEnabled(false)
                .build()
            firestore.firestoreSettings = settings
            
            functions.useEmulator("10.0.2.2", 5001)
        } catch (e: IllegalStateException) {
            // Already initialized in a previous test
        }
    }

    suspend fun createTestUser(email: String): String {
        val result = auth.createUserWithEmailAndPassword(email, "password123").await()
        return result.user?.uid ?: throw Exception("Failed to create test user")
    }

    suspend fun loginTestUser(email: String) {
        auth.signInWithEmailAndPassword(email, "password123").await()
    }

    fun logout() {
        auth.signOut()
    }
}
