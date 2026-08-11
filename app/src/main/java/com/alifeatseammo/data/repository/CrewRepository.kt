package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.Crew
import com.google.firebase.firestore.FirebaseFirestore
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
}

class FirestoreCrewRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
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
        return try {
            val data = hashMapOf(
                "name" to name,
                "description" to description
            )
            functions.getHttpsCallable("createCrew").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun joinCrew(crewId: String): Boolean {
        return try {
            val data = hashMapOf("crewId" to crewId)
            functions.getHttpsCallable("joinCrew").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun leaveCrew(): Boolean {
        return try {
            functions.getHttpsCallable("leaveCrew").call().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
