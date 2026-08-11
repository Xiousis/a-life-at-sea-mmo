package com.alifeatseammo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ChatMessage(
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val channelId: String = "global"
)

interface ChatRepository {
    fun getMessages(channelId: String = "global"): Flow<List<ChatMessage>>
    fun sendMessage(senderName: String, text: String, channelId: String = "global")
}

class FirestoreChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChatRepository {

    override fun getMessages(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = db.collection("chat")
            .whereEqualTo("channelId", channelId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<ChatMessage>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun sendMessage(senderName: String, text: String, channelId: String) {
        val message = ChatMessage(senderName, text, channelId = channelId)
        db.collection("chat").add(message)
    }
}
