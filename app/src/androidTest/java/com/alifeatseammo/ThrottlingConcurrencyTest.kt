package com.alifeatseammo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alifeatseammo.data.model.*
import com.alifeatseammo.data.repository.FirestoreChatRepository
import com.alifeatseammo.data.repository.FirestoreGameRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThrottlingConcurrencyTest : FirebaseEmulatorTest() {

    private lateinit var gameRepository: FirestoreGameRepository
    private lateinit var chatRepository: FirestoreChatRepository

    @Before
    fun setupRepo() {
        gameRepository = FirestoreGameRepository(firestore, functions)
        chatRepository = FirestoreChatRepository(firestore, functions)
    }

    @Test
    fun testSimultaneousChat_RateLimitsCorrectly() = runBlocking {
        val email = "chat_test_${System.currentTimeMillis()}@test.com"
        createTestUser(email)
        loginTestUser(email)
        
        gameRepository.createCharacter("ChatBot", Gender.Male, Race.Human)
        
        // Try to send 10 messages simultaneously
        val deferreds = (1..10).map { i ->
            async {
                try {
                    chatRepository.sendMessage("ChatBot", "Message $i")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferreds.awaitAll()
        val successCount = results.count { it }
        
        // Only 1 should succeed due to 2s rate limit
        assertEquals("Only one message should succeed under simultaneous stress", 1, successCount)
    }

    @Test
    fun testSimultaneousMail_RateLimitsCorrectly() = runBlocking {
        val senderEmail = "sender_${System.currentTimeMillis()}@test.com"
        val receiverEmail = "receiver_${System.currentTimeMillis()}@test.com"
        
        createTestUser(senderEmail)
        val receiverId = createTestUser(receiverEmail)
        
        loginTestUser(senderEmail)
        gameRepository.createCharacter("Sender", Gender.Male, Race.Human)
        
        logout()
        loginTestUser(receiverEmail)
        gameRepository.createCharacter("Receiver", Gender.Male, Race.Human)
        
        logout()
        loginTestUser(senderEmail)
        
        // Try to send 10 mails simultaneously
        val deferreds = (1..10).map { i ->
            async {
                try {
                    gameRepository.sendMail(receiverId, "Subject $i", "Body $i")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        val results = deferreds.awaitAll()
        val successCount = results.count { it }
        
        // Only 1 should succeed due to 5s rate limit
        assertEquals("Only one mail should succeed under simultaneous stress", 1, successCount)
    }
}
