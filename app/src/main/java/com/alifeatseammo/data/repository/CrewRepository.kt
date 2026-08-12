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
    fun getInvitesForUser(userId: String): Flow<List<CrewInvite>>
}

class FirestoreCrewRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) : CrewRepository {

    override fun getCrew(crewId: String): Flow<Crew?> = callbackFlow {
        val subscription = db.collection("crews").document(crewId)
            .addSnapshotListener { snapshot, _ ->
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

    override fun getInvitesForUser(userId: String): Flow<List<CrewInvite>> = callbackFlow {
        val subscription = db.collection("crewInvites")
            .whereEqualTo("targetId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<CrewInvite>() })
                }
            }
        awaitClose { subscription.remove() }
    }
}
