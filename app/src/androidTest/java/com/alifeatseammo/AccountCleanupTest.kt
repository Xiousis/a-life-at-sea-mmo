package com.alifeatseammo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.FirestoreGameRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountCleanupTest : FirebaseEmulatorTest() {

    private lateinit var gameRepository: FirestoreGameRepository

    @Before
    fun setupRepo() {
        gameRepository = FirestoreGameRepository(firestore, functions)
    }

    @Test
    fun testAccountDeletion_CleansUpData() = runBlocking {
        val email = "delete_test_${System.currentTimeMillis()}@test.com"
        val uid = createTestUser(email)
        loginTestUser(email)
        
        gameRepository.createCharacter("ToDeleted", Gender.Male, Race.Human)
        
        // 1. Create some data to clean up
        // Auction
        val auctionRef = firestore.collection("auctions").document()
        auctionRef.set(mapOf("sellerId" to uid, "sellerName" to "ToDeleted")).await()
        
        // Mail
        val mailRef = firestore.collection("players").document(uid).collection("mail").document()
        mailRef.set(mapOf("subject" to "Test Mail")).await()
        
        // Friend Request
        val frRef = firestore.collection("friendRequests").document("${uid}_someone")
        frRef.set(mapOf("senderId" to uid, "receiverId" to "someone", "status" to "pending")).await()

        // 2. Delete account
        auth.currentUser?.delete()?.await()
        
        // Give time for trigger to run (emulator can be slow)
        kotlinx.coroutines.delay(3000)
        
        // 3. Verify cleanup
        val playerSnap = firestore.collection("players").document(uid).get().await()
        assertFalse("Player document should be deleted", playerSnap.exists())
        
        val auctionSnap = firestore.collection("auctions").whereEqualTo("sellerId", uid).get().await()
        assertTrue("Auctions should be deleted", auctionSnap.isEmpty)
        
        val frSnap = firestore.collection("friendRequests").document("${uid}_someone").get().await()
        assertFalse("Friend request should be deleted", frSnap.exists())
        
        // Check subcollection (mail)
        val mailSnap = firestore.collection("players").document(uid).collection("mail").get().await()
        assertTrue("Mail subcollection should be empty", mailSnap.isEmpty)
    }

    @Test
    fun testCaptainDeletion_DisbandsCrewCorrectly() = runBlocking {
        val captainEmail = "captain_${System.currentTimeMillis()}@test.com"
        val memberEmail = "member_${System.currentTimeMillis()}@test.com"
        
        val captainId = createTestUser(captainEmail)
        val memberId = createTestUser(memberEmail)
        
        loginTestUser(captainEmail)
        gameRepository.createCharacter("Captain", Gender.Male, Race.Human)
        
        logout()
        loginTestUser(memberEmail)
        gameRepository.createCharacter("Member", Gender.Male, Race.Human)
        
        // Manually setup crew in firestore for speed
        val crewId = "test_crew_${System.currentTimeMillis()}"
        val crewRef = firestore.collection("crews").document(crewId)
        crewRef.set(mapOf(
            "id" to crewId,
            "captainId" to captainId,
            "members" to listOf(captainId, memberId),
            "name" to "Cleanup Crew"
        )).await()
        
        firestore.collection("players").document(captainId).update("crewId", crewId).await()
        firestore.collection("players").document(memberId).update("crewId", crewId, "crewRank", "Officer").await()
        
        // Delete captain
        logout()
        loginTestUser(captainEmail)
        auth.currentUser?.delete()?.await()
        
        kotlinx.coroutines.delay(3000)
        
        // Verify crew disbanded and member updated
        val crewSnap = firestore.collection("crews").document(crewId).get().await()
        assertFalse("Crew document should be deleted", crewSnap.exists())
        
        val memberSnap = firestore.collection("players").document(memberId).get().await()
        assertNull("Member crewId should be cleared", memberSnap.getString("crewId"))
        assertNull("Member crewRank should be cleared", memberSnap.getString("crewRank"))
    }
}
