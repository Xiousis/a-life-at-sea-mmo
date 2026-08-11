package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.Character
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface SocialRepository {
    fun getFriends(userId: String): Flow<List<Character>>
    fun getPendingRequests(userId: String): Flow<List<Character>>
    suspend fun sendFriendRequest(targetId: String): Boolean
    suspend fun acceptFriendRequest(senderId: String): Boolean
    suspend fun declineFriendRequest(senderId: String): Boolean
    suspend fun removeFriend(friendId: String): Boolean
    suspend fun blockPlayer(targetId: String): Boolean
    suspend fun unblockPlayer(targetId: String): Boolean
    fun getBlockedPlayers(userId: String): Flow<List<String>>
}

class FirestoreSocialRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : SocialRepository {

    override fun getFriends(userId: String): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("players").document(userId)
            .addSnapshotListener { snapshot, _ ->
                val friendIds = snapshot?.get("friends") as? List<String> ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    // Fetch characters for these IDs
                    db.collection("players").whereIn("id", friendIds).get().addOnSuccessListener { querySnapshot ->
                        trySend(querySnapshot.documents.mapNotNull { it.toObject<Character>() })
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPendingRequests(userId: String): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("friendRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val senderIds = snapshot.documents.mapNotNull { it.getString("senderId") }
                    if (senderIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        db.collection("players").whereIn("id", senderIds).get().addOnSuccessListener { querySnapshot ->
                            trySend(querySnapshot.documents.mapNotNull { it.toObject<Character>() })
                        }
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendFriendRequest(targetId: String): Boolean {
        return try {
            val data = hashMapOf("targetId" to targetId)
            functions.getHttpsCallable("sendFriendRequest").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun acceptFriendRequest(senderId: String): Boolean {
        return try {
            val data = hashMapOf("senderId" to senderId)
            functions.getHttpsCallable("acceptFriendRequest").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun declineFriendRequest(senderId: String): Boolean {
        return try {
            val data = hashMapOf("senderId" to senderId)
            functions.getHttpsCallable("declineFriendRequest").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeFriend(friendId: String): Boolean {
        return try {
            val data = hashMapOf("friendId" to friendId)
            functions.getHttpsCallable("removeFriend").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun blockPlayer(targetId: String): Boolean {
        return try {
            val data = hashMapOf("targetId" to targetId)
            functions.getHttpsCallable("blockPlayer").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unblockPlayer(targetId: String): Boolean {
        return try {
            val data = hashMapOf("targetId" to targetId)
            functions.getHttpsCallable("unblockPlayer").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getBlockedPlayers(userId: String): Flow<List<String>> = callbackFlow {
        val subscription = db.collection("players").document(userId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.get("blocked") as? List<String> ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }
}
