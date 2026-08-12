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
    suspend fun createCharacter(userId: String, name: String, gender: Gender, race: Race): Boolean
    suspend fun train(userId: String, statType: StatType): Boolean
    suspend fun completeMission(userId: String, missionId: String): Boolean
    fun getAvailableMissions(): Flow<List<Mission>>
    suspend fun startTravel(userId: String, destination: String): Boolean
    suspend fun finishTravel(): Boolean
    suspend fun combatAction(userId: String, action: CombatAction, techniqueId: String? = null, itemId: String? = null): Boolean
    suspend fun attackPlayer(attackerId: String, defenderId: String): Boolean
    suspend fun equipItem(itemId: String, slot: String): Boolean
    suspend fun unequipItem(slot: String): Boolean
    suspend fun purchaseItem(itemId: String, shopId: String): Boolean
    suspend fun purchaseShip(shipId: String): Boolean
    suspend fun sellItem(itemId: String): Boolean
    suspend fun useItem(itemId: String): Boolean
    suspend fun joinFaction(userId: String, faction: Faction): Boolean
    suspend fun startHealing(): Boolean
    suspend fun instantHeal(): Boolean
    fun getPlayersAtLocation(location: String): Flow<List<Character>>
    fun getTopPlayers(limit: Int): Flow<List<Character>>
    fun getPlayerProfile(playerId: String): Flow<Character?>
    fun getLocations(): Flow<List<LocationDef>>
    fun getEnemyDefs(): Flow<List<EnemyDef>>
    fun getMissionDefs(): Flow<List<Mission>>
    fun getMailMessages(userId: String): Flow<List<MailMessage>>
    fun getMarketItems(): Flow<List<Item>>
}

class FirestoreGameRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) : GameRepository {
    
    override fun getCharacter(userId: String): Flow<Character?> = callbackFlow {
        val docRef = db.collection("players").document(userId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Log the error and don't send anything, or send a specific error state if needed
                // For now, sending null might trigger "NoCharacter", so we should be careful.
                // However, without a dedicated Error state in the Flow, logging is our best bet.
                android.util.Log.e("FirestoreGameRepository", "Error fetching character", error)
                return@addSnapshotListener
            }
            
            if (snapshot?.exists() == true) {
                val character = snapshot.toObject<Character>()
                if (character != null) {
                    // Server-authoritative travel finish check
                    val travel = character.travelState
                    if (travel != null && travel.arrivalTime <= System.currentTimeMillis()) {
                        // Notify server to finalize travel
                        functions.getHttpsCallable("finishTravel").call()
                    }
                    trySend(character)
                } else {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun createCharacter(userId: String, name: String, gender: Gender, race: Race): Boolean {
        val data = hashMapOf(
            "name" to name,
            "gender" to gender.name,
            "race" to race.name
        )
        functions.getHttpsCallable("createCharacter").call(data).await()
        return true
    }

    override suspend fun train(userId: String, statType: StatType): Boolean {
        val data = hashMapOf("statType" to statType.name)
        functions.getHttpsCallable("train").call(data).await()
        return true
    }

    override suspend fun completeMission(userId: String, missionId: String): Boolean {
        val data = hashMapOf("missionId" to missionId)
        functions.getHttpsCallable("completeMission").call(data).await()
        return true
    }

    override fun getAvailableMissions(): Flow<List<Mission>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("missions")
            .orderBy("difficulty")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Mission>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun startTravel(userId: String, destination: String): Boolean {
        val data = hashMapOf("destination" to destination)
        functions.getHttpsCallable("startTravel").call(data).await()
        return true
    }

    override suspend fun finishTravel(): Boolean {
        functions.getHttpsCallable("finishTravel").call().await()
        return true
    }

    override suspend fun combatAction(userId: String, action: CombatAction, techniqueId: String?, itemId: String?): Boolean {
        val data = hashMapOf(
            "action" to action.name,
            "techniqueId" to techniqueId,
            "itemId" to itemId
        )
        functions.getHttpsCallable("combatAction").call(data).await()
        return true
    }

    override suspend fun attackPlayer(attackerId: String, defenderId: String): Boolean {
        val data = hashMapOf("defenderId" to defenderId)
        functions.getHttpsCallable("attackPlayer").call(data).await()
        return true
    }

    override suspend fun equipItem(itemId: String, slot: String): Boolean {
        val data = hashMapOf("itemId" to itemId, "slot" to slot)
        functions.getHttpsCallable("equipItem").call(data).await()
        return true
    }

    override suspend fun unequipItem(slot: String): Boolean {
        val data = hashMapOf("slot" to slot)
        functions.getHttpsCallable("unequipItem").call(data).await()
        return true
    }

    override suspend fun purchaseItem(itemId: String, shopId: String): Boolean {
        val data = hashMapOf("itemId" to itemId, "shopId" to shopId)
        functions.getHttpsCallable("purchaseItem").call(data).await()
        return true
    }

    override suspend fun purchaseShip(shipId: String): Boolean {
        val data = hashMapOf("shipId" to shipId)
        functions.getHttpsCallable("purchaseShip").call(data).await()
        return true
    }

    override suspend fun sellItem(itemId: String): Boolean {
        val data = hashMapOf("itemId" to itemId)
        functions.getHttpsCallable("sellItem").call(data).await()
        return true
    }

    override suspend fun useItem(itemId: String): Boolean {
        val data = hashMapOf("itemId" to itemId)
        functions.getHttpsCallable("useItem").call(data).await()
        return true
    }

    override suspend fun joinFaction(userId: String, faction: Faction): Boolean {
        val data = hashMapOf("faction" to faction.name)
        functions.getHttpsCallable("joinFaction").call(data).await()
        return true
    }

    override suspend fun startHealing(): Boolean {
        functions.getHttpsCallable("startHealing").call().await()
        return true
    }

    override suspend fun instantHeal(): Boolean {
        functions.getHttpsCallable("instantHeal").call().await()
        return true
    }

    override fun getPlayersAtLocation(location: String): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("players")
            .whereEqualTo("currentLocation", location)
            .limit(10)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getTopPlayers(limit: Int): Flow<List<Character>> = callbackFlow {
        val subscription = db.collection("players")
            .orderBy("level", Query.Direction.DESCENDING)
            .orderBy("xp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Character>() })
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

    override fun getLocations(): Flow<List<LocationDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("locations")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<LocationDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getEnemyDefs(): Flow<List<EnemyDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("enemies")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<EnemyDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMissionDefs(): Flow<List<Mission>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("missions")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Mission>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMailMessages(userId: String): Flow<List<MailMessage>> = callbackFlow {
        val subscription = db.collection("players").document(userId).collection("mail")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<MailMessage>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMarketItems(): Flow<List<Item>> = callbackFlow {
        val subscription = db.collection("gameData").document("items").collection("all")
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Item>() })
                }
            }
        awaitClose { subscription.remove() }
    }
}

@Suppress("unused")
class MockGameRepository : GameRepository {
    private val _character = MutableStateFlow<Character?>(null)
    
    override fun getCharacter(userId: String): Flow<Character?> = _character.asStateFlow()

    override suspend fun createCharacter(userId: String, name: String, gender: Gender, race: Race): Boolean {
        _character.value = Character(
            id = userId,
            name = name,
            gender = gender,
            race = race
        )
        return true
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
            Mission("1", "Scout the Shore", "Check for any suspicious activity on Fogi Tail Island's beach.", 10, 1, 50, 20, 1),
            Mission("2", "Deliver Message", "Take a letter to the merchant on Ironcrest Isle.", 15, 1, 75, 30, 2),
            Mission("3", "Clear Pests", "Help an Amber Reach farmer clear giant crabs from his field.", 25, 2, 150, 60, 3)
        )
    )

    override suspend fun startTravel(userId: String, destination: String): Boolean = true
    override suspend fun finishTravel(): Boolean = true
    override suspend fun combatAction(userId: String, action: CombatAction, techniqueId: String?, itemId: String?): Boolean = true
    override suspend fun attackPlayer(attackerId: String, defenderId: String): Boolean = true
    override suspend fun equipItem(itemId: String, slot: String): Boolean = true
    override suspend fun unequipItem(slot: String): Boolean = true
    override suspend fun purchaseItem(itemId: String, shopId: String): Boolean = true
    override suspend fun purchaseShip(shipId: String): Boolean = true
    override suspend fun sellItem(itemId: String): Boolean {
        _character.update { char ->
            char?.let {
                val item = it.inventory.find { i -> i.id == itemId }
                if (item != null) {
                    it.copy(
                        inventory = it.inventory.filter { i -> i.id != itemId },
                        gold = it.gold + (item.price / 2)
                    )
                } else it
            }
        }
        return true
    }

    override suspend fun useItem(itemId: String): Boolean {
        _character.update { char ->
            char?.let {
                val item = it.inventory.find { i -> i.id == itemId }
                if (item != null && item.type == ItemType.Consumable) {
                    it.copy(
                        inventory = it.inventory.filter { i -> i.id != itemId },
                        hp = (it.hp + 30).coerceAtMost(it.maxHp)
                    )
                } else it
            }
        }
        return true
    }
    override suspend fun joinFaction(userId: String, faction: Faction): Boolean = true
    override suspend fun startHealing(): Boolean = true
    override suspend fun instantHeal(): Boolean = true

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

    override fun getLocations(): Flow<List<LocationDef>> = flowOf(emptyList())
    override fun getEnemyDefs(): Flow<List<EnemyDef>> = flowOf(emptyList())
    override fun getMissionDefs(): Flow<List<Mission>> = flowOf(emptyList())
    override fun getMailMessages(userId: String): Flow<List<MailMessage>> = flowOf(emptyList())
    override fun getMarketItems(): Flow<List<Item>> = flowOf(emptyList())
}
