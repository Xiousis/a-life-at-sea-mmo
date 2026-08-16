package com.alifeatseammo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.FirestoreGameRepository
import com.alifeatseammo.data.repository.GameRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PvPConcurrencyTest : FirebaseEmulatorTest() {

    private lateinit var gameRepository: GameRepository

    @Before
    fun setupRepo() {
        gameRepository = FirestoreGameRepository(firestore, functions)
    }

    @Test
    fun testAttackPlayerInSafeZone_Fails() = runBlocking {
        val defEmail = "safe_def_${System.currentTimeMillis()}@test.com"
        val defUid = createTestUser(defEmail)
        loginTestUser(defEmail)
        gameRepository.createCharacter("SafeDef", Gender.Male, Race.Human)
        
        val attEmail = "safe_att_${System.currentTimeMillis()}@test.com"
        createTestUser(attEmail)
        loginTestUser(attEmail)
        gameRepository.createCharacter("SafeAtt", Gender.Male, Race.Human)

        // Force location to be safe
        firestore.collection("gameData").document("world").collection("locations").document("Fogi Tail Island")
            .set(mapOf("isSafe" to true)).await()
        
        try {
            gameRepository.attackPlayer(defUid)
            fail("Should not be able to attack in a safe zone")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("safe zones") == true)
        }
    }

    @Test
    fun testAttackHealingPlayer_Fails() = runBlocking {
        val defEmail = "healer_${System.currentTimeMillis()}@test.com"
        val defUid = createTestUser(defEmail)
        loginTestUser(defEmail)
        gameRepository.createCharacter("Healer", Gender.Male, Race.Human)
        
        // Set defender to healing state
        firestore.collection("players").document(defUid).update("healingState", mapOf("endTime" to System.currentTimeMillis() + 100000)).await()
        
        val attEmail = "att_h_${System.currentTimeMillis()}@test.com"
        createTestUser(attEmail)
        loginTestUser(attEmail)
        gameRepository.createCharacter("AttackerH", Gender.Male, Race.Human)

        // Force location to be unsafe
        firestore.collection("gameData").document("world").collection("locations").document("Fogi Tail Island")
            .set(mapOf("isSafe" to false)).await()
        
        try {
            gameRepository.attackPlayer(defUid)
            fail("Should not be able to attack a healing player")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("busy or resting") == true)
        }
    }

    @Test
    fun testAttackBusyPlayer_Fails() = runBlocking {
        val defEmail = "busy_def_${System.currentTimeMillis()}@test.com"
        val defUid = createTestUser(defEmail)
        loginTestUser(defEmail)
        gameRepository.createCharacter("BusyDef", Gender.Male, Race.Human)
        
        // Set defender to busy (e.g. traveling)
        firestore.collection("players").document(defUid).update("travelState", mapOf("destination" to "Somewhere", "arrivalTime" to Long.MAX_VALUE)).await()
        
        val attEmail = "busy_att_${System.currentTimeMillis()}@test.com"
        createTestUser(attEmail)
        loginTestUser(attEmail)
        gameRepository.createCharacter("BusyAtt", Gender.Male, Race.Human)

        firestore.collection("gameData").document("world").collection("locations").document("Fogi Tail Island")
            .set(mapOf("isSafe" to false)).await()
        
        try {
            gameRepository.attackPlayer(defUid)
            fail("Should not be able to attack a busy player")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("busy or resting") == true)
        }
    }
}
