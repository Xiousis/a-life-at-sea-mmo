package com.alifeatseammo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    fun sendMessage(senderName: String, text: String, channelId: String = "global")
}

class FirestoreChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : ChatRepository {

    override fun getMessages(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = db.collection("chat")
            .whereEqualTo("channelId", channelId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<ChatMessage>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun sendMessage(senderName: String, text: String, channelId: String) {
        val data = hashMapOf(
            "message" to text,
            "channelId" to channelId
        )
        functions.getHttpsCallable("sendMessage").call(data)
    }
}
