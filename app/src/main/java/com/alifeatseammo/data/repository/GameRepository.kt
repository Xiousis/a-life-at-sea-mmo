package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

interface GameRepository {
    fun getCharacter(userId: String): Flow<Character?>
    fun createCharacter(userId: String, name: String, gender: Gender, race: Race)
    suspend fun train(userId: String, statType: StatType): Boolean
    suspend fun completeMission(userId: String, missionId: String): Boolean
    fun getAvailableMissions(): Flow<List<Mission>>
    fun startTravel(userId: String, destination: String, arrivalTime: Long)
    fun combatAction(userId: String, action: CombatAction)
    fun attackPlayer(attackerId: String, defenderId: String)
    fun getPlayersAtLocation(location: String): Flow<List<Character>>
    fun getTopPlayers(limit: Int): Flow<List<Character>>
    fun getPlayerProfile(playerId: String): Flow<Character?>
    fun getCrew(crewId: String): Flow<Crew?>
    suspend fun createCrew(name: String, description: String): String?
    suspend fun joinCrew(crewId: String): Boolean
    fun getLocations(): Flow<List<LocationDef>>
    fun getEnemyDefs(): Flow<List<EnemyDef>>
    fun getMissionDefs(): Flow<List<MissionDef>>
}

class FirestoreGameRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : GameRepository {
    
    override fun getCharacter(userId: String): Flow<Character?> = callbackFlow {
        val docRef = db.collection("players").document(userId)
        val subscription = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val character = snapshot.toObject<Character>()
                if (character != null) {
                    // Travel Arrival check - The client notifies the server when travel is complete
                    val travel = character.travelState
                    if (travel != null && travel.arrivalTime <= System.currentTimeMillis()) {
                        // We emit the arrival state optimistically, but the server must confirm it
                        trySend(character.copy(currentLocation = travel.destination, travelState = null))
                        
                        // Call Cloud Function to persist arrival
                        functions.getHttpsCallable("finishTravel").call()
                    } else {
                        trySend(character)
                    }
                } else {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    override fun createCharacter(userId: String, name: String, gender: Gender, race: Race) {
        val data = hashMapOf(
            "name" to name,
            "gender" to gender.name,
            "race" to race.name
        )
        functions.getHttpsCallable("createCharacter").call(data)
    }

    override suspend fun train(userId: String, statType: StatType): Boolean {
        return try {
            val data = hashMapOf("statType" to statType.name)
            functions.getHttpsCallable("train").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun completeMission(userId: String, missionId: String): Boolean {
        return try {
            val data = hashMapOf("missionId" to missionId)
            functions.getHttpsCallable("completeMission").call(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAvailableMissions(): Flow<List<Mission>> = callbackFlow {
        val subscription = db.collection("missions")
            .orderBy("difficulty")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<Mission>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun startTravel(userId: String, destination: String, arrivalTime: Long) {
        val data = hashMapOf(
            "destination" to destination,
            "arrivalTime" to arrivalTime
        )
        functions.getHttpsCallable("startTravel").call(data)
    }

    override fun combatAction(userId: String, action: CombatAction) {
        val data = hashMapOf("action" to action.name)
        functions.getHttpsCallable("combatAction").call(data)
    }

    override fun attackPlayer(attackerId: String, defenderId: String) {
        val data = hashMapOf("defenderId" to defenderId)
        functions.getHttpsCallable("attackPlayer").call(data)
    }

    override fun getPlayersAtLocation(location: String): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("players")
            .whereEqualTo("currentLocation", location)
            .limit(10)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getTopPlayers(limit: Int): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("players")
            .orderBy("xp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPlayerProfile(playerId: String): Flow<Character?> = callbackFlow {
        val subscription = db.collection("players").document(playerId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject<Character>())
            }
        awaitClose { subscription.remove() }
    }

    override fun getCrew(crewId: String): Flow<Crew?> = callbackFlow {
        val subscription = db.collection("crews").document(crewId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject<Crew>())
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createCrew(name: String, description: String): String? {
        return try {
            val data = hashMapOf("name" to name, "description" to description)
            val result = functions.getHttpsCallable("createCrew").call(data).await()
            val resultData = result.data as? Map<*, *>
            resultData?.get("crewId") as? String
        } catch (e: Exception) {
            null
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

    override fun getLocations(): Flow<List<LocationDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("locations")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<LocationDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getEnemyDefs(): Flow<List<EnemyDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("enemies")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<EnemyDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMissionDefs(): Flow<List<MissionDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("missions")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject<MissionDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }
}

@Suppress("unused")
class MockGameRepository : GameRepository {
    private val _character = MutableStateFlow<Character?>(null)
    
    override fun getCharacter(userId: String): Flow<Character?> = _character.asStateFlow()

    override fun createCharacter(userId: String, name: String, gender: Gender, race: Race) {
        _character.value = Character(
            id = userId,
            name = name,
            gender = gender,
            race = race
        )
    }

    override suspend fun train(userId: String, statType: StatType): Boolean {
        _character.update { char ->
            char?.let {
                if (it.energy >= 10) {
                    val updatedStats = when (statType) {
                        StatType.Strength -> it.stats.copy(strength = it.stats.strength + 1)
                        StatType.Endurance -> it.stats.copy(endurance = it.stats.endurance + 1)
                        StatType.Agility -> it.stats.copy(agility = it.stats.agility + 1)
                        StatType.Perception -> it.stats.copy(perception = it.stats.perception + 1)
                        StatType.Willpower -> it.stats.copy(willpower = it.stats.willpower + 1)
                        StatType.Luck -> it.stats.copy(luck = it.stats.luck + 1)
                        StatType.Swordsmanship -> it.stats.copy(swordsmanship = it.stats.swordsmanship + 1)
                        StatType.Brawling -> it.stats.copy(brawling = it.stats.brawling + 1)
                        StatType.Gunslinging -> it.stats.copy(gunslinging = it.stats.gunslinging + 1)
                        StatType.Spear -> it.stats.copy(spear = it.stats.spear + 1)
                        StatType.MartialArts -> it.stats.copy(martialArts = it.stats.martialArts + 1)
                        StatType.DualBlades -> it.stats.copy(dualBlades = it.stats.dualBlades + 1)
                    }
                    it.copy(
                        stats = updatedStats,
                        energy = it.energy - 10,
                        xp = it.xp + 5
                    ).checkLevelUp()
                } else it
            }
        }
        return true
    }

    override suspend fun completeMission(userId: String, missionId: String): Boolean {
        _character.update { char ->
            char?.let {
                // Mock logic: assumes mission exists and costs 10 energy
                if (it.energy >= 10) {
                    it.copy(
                        energy = it.energy - 10,
                        gold = it.gold + 50,
                        xp = it.xp + 20
                    ).checkLevelUp()
                } else it
            }
        }
        return true
    }

    override fun getAvailableMissions(): Flow<List<Mission>> = flowOf(
        listOf(
            Mission("1", "Scout the Shore", "Check for any suspicious activity on Fogi Tail Island's beach.", 10, 1, Reward(50, 20), 1),
            Mission("2", "Deliver Message", "Take a letter to the merchant on Ironcrest Isle.", 15, 1, Reward(75, 30), 2),
            Mission("3", "Clear Pests", "Help an Amber Reach farmer clear giant crabs from his field.", 25, 2, Reward(150, 60), 3)
        )
    )

    override fun startTravel(userId: String, destination: String, arrivalTime: Long) {}
    override fun combatAction(userId: String, action: CombatAction) {}
    override fun attackPlayer(attackerId: String, defenderId: String) {}

    override fun getPlayersAtLocation(location: String): Flow<List<Character>> = flowOf(emptyList())
    override fun getTopPlayers(limit: Int): Flow<List<Character>> = flowOf(emptyList())

    override fun getPlayerProfile(playerId: String): Flow<Character?> = flowOf(
        Character(
            id = playerId,
            name = "RAZOR",
            level = 31,
            race = Race.Human,
            bounty = 3482900,
            title = "Sea Devil",
            pvpWins = 81,
            pvpLosses = 24
        )
    )

    override fun getCrew(crewId: String): Flow<Crew?> = flowOf(
        Crew(id = crewId, name = "Black Tide")
    )

    override suspend fun createCrew(name: String, description: String): String? = "mock-crew-id"
    override suspend fun joinCrew(crewId: String): Boolean = true
    override fun getLocations(): Flow<List<LocationDef>> = flowOf(emptyList())
    override fun getEnemyDefs(): Flow<List<EnemyDef>> = flowOf(emptyList())
    override fun getMissionDefs(): Flow<List<MissionDef>> = flowOf(emptyList())
}
