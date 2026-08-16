package com.alifeatseammo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val channelId: String = "global"
)

interface ChatRepository {
    fun getMessages(channelId: String = "global"): Flow<List<ChatMessage>>
    suspend fun sendMessage(senderName: String, text: String, channelId: String = "global"): Boolean
}

class FirestoreChatRepository(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : ChatRepository {

    override fun getMessages(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = db.collection("chat")
            .whereEqualTo("channelId", channelId)
            .orderBy("timestamp", Query.Direction.ASCENDING) // Change to Ascending for easier auto-scroll logic
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepository", "Error fetching messages for $channelId", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<ChatMessage>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendMessage(senderName: String, text: String, channelId: String): Boolean {
        return try {
            val data = hashMapOf(
                "message" to text,
                "channelId" to channelId
            )
            functions.getHttpsCallable("sendMessage").call(data).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to send message", e)
            throw e
        }
    }
}
