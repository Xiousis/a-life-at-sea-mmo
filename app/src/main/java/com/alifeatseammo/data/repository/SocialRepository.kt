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
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : SocialRepository {

    override fun getFriends(userId: String): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("players").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("SocialRepository", "Error fetching friends for: $userId", error)
                    return@addSnapshotListener
                }
                val friendIds = snapshot?.toObject<Character>()?.friends ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    // Fetch characters for these IDs
                    db.collection("players").whereIn("id", friendIds).get().addOnSuccessListener { querySnapshot ->
                        trySend(querySnapshot.documents.mapNotNull { it.toObject<Character>() })
                    }.addOnFailureListener { e ->
                        android.util.Log.e("SocialRepository", "Error fetching friend details", e)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPendingRequests(userId: String): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("friendRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("SocialRepository", "Error fetching pending requests for: $userId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val senderIds = snapshot.documents.mapNotNull { it.getString("senderId") }
                    if (senderIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        db.collection("players").whereIn("id", senderIds).get().addOnSuccessListener { querySnapshot ->
                            trySend(querySnapshot.documents.mapNotNull { it.toObject<Character>() })
                        }.addOnFailureListener { e ->
                            android.util.Log.e("SocialRepository", "Error fetching sender details", e)
                        }
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendFriendRequest(targetId: String): Boolean {
        val data = hashMapOf("targetId" to targetId)
        functions.getHttpsCallable("sendFriendRequest").call(data).await()
        return true
    }

    override suspend fun acceptFriendRequest(senderId: String): Boolean {
        val data = hashMapOf("senderId" to senderId)
        functions.getHttpsCallable("acceptFriendRequest").call(data).await()
        return true
    }

    override suspend fun declineFriendRequest(senderId: String): Boolean {
        val data = hashMapOf("senderId" to senderId)
        functions.getHttpsCallable("declineFriendRequest").call(data).await()
        return true
    }

    override suspend fun removeFriend(friendId: String): Boolean {
        val data = hashMapOf("friendId" to friendId)
        functions.getHttpsCallable("removeFriend").call(data).await()
        return true
    }

    override suspend fun blockPlayer(targetId: String): Boolean {
        val data = hashMapOf("targetId" to targetId)
        functions.getHttpsCallable("blockPlayer").call(data).await()
        return true
    }

    override suspend fun unblockPlayer(targetId: String): Boolean {
        val data = hashMapOf("targetId" to targetId)
        functions.getHttpsCallable("unblockPlayer").call(data).await()
        return true
    }

    override fun getBlockedPlayers(userId: String): Flow<List<String>> = callbackFlow {
        val subscription = db.collection("players").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("SocialRepository", "Error fetching blocked players for: $userId", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<Character>()?.blocked ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }
}
