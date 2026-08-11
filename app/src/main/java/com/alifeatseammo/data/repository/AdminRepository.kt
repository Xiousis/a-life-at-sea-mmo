package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AdminRepository {
    fun searchPlayers(query: String): Flow<List<Character>>
    fun getPlayerLogs(userId: String): Flow<List<TransactionLog>>
    suspend fun mutePlayer(userId: String, reason: String, durationHours: Int): Boolean
    suspend fun banPlayer(userId: String, reason: String): Boolean
    suspend fun teleportPlayer(userId: String, location: String): Boolean
    suspend fun adjustGold(userId: String, amount: Int, reason: String): Boolean
    suspend fun sendGlobalAnnouncement(message: String): Boolean
}

class FirestoreAdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : AdminRepository {

    override fun searchPlayers(query: String): Flow<List<Character>> = callbackFlow {
        // Simple prefix search for name
        val subscription = db.collection("players")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPlayerLogs(userId: String): Flow<List<TransactionLog>> = callbackFlow {
        val subscription = db.collection("logs")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<TransactionLog>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun mutePlayer(userId: String, reason: String, durationHours: Int): Boolean {
        return try {
            val data = hashMapOf("userId" to userId, "reason" to reason, "durationHours" to durationHours)
            functions.getHttpsCallable("adminMutePlayer").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun banPlayer(userId: String, reason: String): Boolean {
        return try {
            val data = hashMapOf("userId" to userId, "reason" to reason)
            functions.getHttpsCallable("adminBanPlayer").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun teleportPlayer(userId: String, location: String): Boolean {
        return try {
            val data = hashMapOf("userId" to userId, "location" to location)
            functions.getHttpsCallable("adminTeleportPlayer").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun adjustGold(userId: String, amount: Int, reason: String): Boolean {
        return try {
            val data = hashMapOf("userId" to userId, "amount" to amount, "reason" to reason)
            functions.getHttpsCallable("adminAdjustGold").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendGlobalAnnouncement(message: String): Boolean {
        return try {
            val data = hashMapOf("message" to message)
            functions.getHttpsCallable("adminSendAnnouncement").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
