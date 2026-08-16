package com.alifeatseammo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.FirestoreGameRepository
import com.alifeatseammo.data.repository.GameRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EconomyConcurrencyTest : FirebaseEmulatorTest() {

    private lateinit var gameRepository: GameRepository

    @Before
    fun setupRepo() {
        gameRepository = FirestoreGameRepository(firestore, functions)
        
        runBlocking {
            // Seed Game Data
            firestore.collection("gameData").document("world").collection("locations").document("Fogi Tail Island")
                .set(mapOf("actions" to listOf(mapOf("type" to "Market")))).await()
            
            firestore.collection("gameData").document("items").collection("all").document("test_sword")
                .set(mapOf(
                    "id" to "test_sword",
                    "name" to "Test Sword",
                    "price" to 10,
                    "type" to "Weapon"
                )).await()
        }
    }

    @Test
    fun testSimultaneousPurchase_DeductsGoldCorrectly() = runBlocking {
        val email = "economy_test_${System.currentTimeMillis()}@test.com"
        val uid = createTestUser(email)
        loginTestUser(email)
        
        gameRepository.createCharacter("TestEco", Gender.Male, Race.Human)
        
        // Ensure character has enough gold (Default is 100, we need 30)
        
        // Try to buy 3 swords simultaneously (10 gold each)
        val deferreds = (1..3).map {
            async { 
                try {
                    gameRepository.purchaseItem("test_sword", "Market")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferreds.awaitAll()
        assertTrue("At least one purchase should succeed", results.any { it })
        
        // Check final gold and inventory
        val character = firestore.collection("players").document(uid).get().await().toObject(Character::class.java)
        assertNotNull(character)
        
        val successCount = results.count { it }
        assertEquals("Gold deduction should match successes", 100 - (successCount * 10), character?.gold)
        assertEquals("Inventory size should match successes", successCount, character?.inventory?.size)
    }

    @Test
    fun testPurchaseBeyondCapacity_FailsGracefully() = runBlocking {
        val email = "capacity_test_${System.currentTimeMillis()}@test.com"
        val uid = createTestUser(email)
        loginTestUser(email)
        
        gameRepository.createCharacter("TestCap", Gender.Male, Race.Human)
        
        // Set capacity to 2
        firestore.collection("players").document(uid).update("inventoryCapacity", 2).await()
        
        // Try to buy 4 swords simultaneously
        val deferreds = (1..4).map {
            async { 
                try {
                    gameRepository.purchaseItem("test_sword", "Market")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferreds.awaitAll()
        val successCount = results.count { it }
        
        // Only 2 should succeed
        assertEquals("Should not exceed capacity", 2, successCount)
        
        val character = firestore.collection("players").document(uid).get().await().toObject(Character::class.java)
        assertEquals(2, character?.inventory?.size)
        assertEquals(80, character?.gold) // 100 - 20 = 80
    }

    @Test
    fun testSimultaneousSell_DoesNotDoublePay() = runBlocking {
        val email = "sell_test_${System.currentTimeMillis()}@test.com"
        val uid = createTestUser(email)
        loginTestUser(email)
        
        gameRepository.createCharacter("TestSell", Gender.Male, Race.Human)
        
        // Give one item with a unique ID (mimicking how the server creates them)
        val serverItemId = "test_sword_unique_123"
        val item = Item(id = serverItemId, name = "Test Sword", price = 100, type = ItemType.Weapon)
        firestore.collection("players").document(uid).update("inventory", listOf(item)).await()
        
        // Try to sell it twice simultaneously
        val deferreds = (1..2).map {
            async {
                try {
                    gameRepository.sellItem(serverItemId)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferreds.awaitAll()
        val successCount = results.count { it }
        
        // Only 1 should succeed
        assertEquals("Item should only be sold once", 1, successCount)
        
        val character = firestore.collection("players").document(uid).get().await().toObject(Character::class.java)
        assertEquals(0, character?.inventory?.size)
        // Default gold 100 + 50 (50% of 100) = 150
        assertEquals(150, character?.gold)
    }
}
