package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.Crew
import com.alifeatseammo.data.model.CrewInvite
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface CrewRepository {
    fun getCrew(crewId: String): Flow<Crew?>
    suspend fun createCrew(name: String, description: String): Boolean
    suspend fun joinCrew(crewId: String): Boolean
    suspend fun leaveCrew(): Boolean
    suspend fun inviteToCrew(targetId: String): Boolean
    suspend fun respondToInvite(crewId: String, accept: Boolean): Boolean
    suspend fun promoteMember(targetId: String, rank: String): Boolean
    suspend fun kickMember(targetId: String): Boolean
    suspend fun donateToCrew(amount: Int): Boolean
    suspend fun updateCrewSettings(description: String, isPublic: Boolean): Boolean
    suspend fun upgradeCrewPerk(perk: String): Boolean
    suspend fun toggleCrewPvP(enabled: Boolean): Boolean
    fun getTopCrews(limit: Int, sortBy: String = "level"): Flow<List<Crew>>
    fun getInvitesForUser(userId: String): Flow<List<CrewInvite>>
}

class FirestoreCrewRepository(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : CrewRepository {

    override fun getCrew(crewId: String): Flow<Crew?> = callbackFlow {
        val subscription = db.collection("crews").document(crewId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CrewRepository", "Error fetching crew: $crewId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObject(Crew::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createCrew(name: String, description: String): Boolean {
        val data = hashMapOf(
            "name" to name,
            "description" to description
        )
        functions.getHttpsCallable("createCrew").call(data).await()
        return true
    }

    override suspend fun joinCrew(crewId: String): Boolean {
        val data = hashMapOf("crewId" to crewId)
        functions.getHttpsCallable("joinCrew").call(data).await()
        return true
    }

    override suspend fun leaveCrew(): Boolean {
        functions.getHttpsCallable("leaveCrew").call().await()
        return true
    }

    override suspend fun inviteToCrew(targetId: String): Boolean {
        val data = hashMapOf("targetId" to targetId)
        functions.getHttpsCallable("inviteToCrew").call(data).await()
        return true
    }

    override suspend fun respondToInvite(crewId: String, accept: Boolean): Boolean {
        val data = hashMapOf("crewId" to crewId, "accept" to accept)
        functions.getHttpsCallable("respondToInvite").call(data).await()
        return true
    }

    override suspend fun promoteMember(targetId: String, rank: String): Boolean {
        val data = hashMapOf("targetId" to targetId, "rank" to rank)
        functions.getHttpsCallable("promoteMember").call(data).await()
        return true
    }

    override suspend fun kickMember(targetId: String): Boolean {
        val data = hashMapOf("targetId" to targetId)
        functions.getHttpsCallable("kickMember").call(data).await()
        return true
    }

    override suspend fun donateToCrew(amount: Int): Boolean {
        val data = hashMapOf("amount" to amount)
        functions.getHttpsCallable("donateToCrew").call(data).await()
        return true
    }

    override suspend fun updateCrewSettings(description: String, isPublic: Boolean): Boolean {
        val data = hashMapOf("description" to description, "isPublic" to isPublic)
        functions.getHttpsCallable("updateCrewSettings").call(data).await()
        return true
    }

    override suspend fun upgradeCrewPerk(perk: String): Boolean {
        val data = hashMapOf("perk" to perk)
        functions.getHttpsCallable("upgradeCrewPerk").call(data).await()
        return true
    }

    override suspend fun toggleCrewPvP(enabled: Boolean): Boolean {
        val data = hashMapOf("enabled" to enabled)
        functions.getHttpsCallable("toggleCrewPvP").call(data).await()
        return true
    }

    override fun getTopCrews(limit: Int, sortBy: String): Flow<List<Crew>> = callbackFlow {
        var query: com.google.firebase.firestore.Query = db.collection("crews")
        
        query = when (sortBy) {
            "pvpWins" -> query.orderBy("pvpWins", com.google.firebase.firestore.Query.Direction.DESCENDING)
            "totalBounty" -> query.orderBy("totalBounty", com.google.firebase.firestore.Query.Direction.DESCENDING)
            else -> query.orderBy("level", com.google.firebase.firestore.Query.Direction.DESCENDING)
                         .orderBy("experience", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }

        val subscription = query.limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CrewRepository", "Error fetching top crews", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Crew>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getInvitesForUser(userId: String): Flow<List<CrewInvite>> = callbackFlow {
        val subscription = db.collection("crewInvites")
            .whereEqualTo("targetId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CrewRepository", "Error fetching invites for: $userId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<CrewInvite>() })
                }
            }
        awaitClose { subscription.remove() }
    }
}
