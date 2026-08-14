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
    suspend fun createCharacter(name: String, gender: Gender, race: Race): Boolean
    suspend fun train(statType: StatType): Boolean
    suspend fun finishTraining(): Boolean
    suspend fun completeMission(missionId: String): Boolean
    fun getAvailableMissions(): Flow<List<Mission>>
    suspend fun startTravel(destination: String): Boolean
    suspend fun finishTravel(): Boolean
    suspend fun combatAction(action: CombatAction, techniqueId: String? = null, itemId: String? = null): Boolean
    suspend fun attackPlayer(defenderId: String): Boolean
    suspend fun equipItem(itemId: String, slot: String): Boolean
    suspend fun unequipItem(slot: String): Boolean
    suspend fun purchaseItem(itemId: String, shopId: String): Boolean
    suspend fun purchaseShip(shipId: String): Boolean
    suspend fun sellItem(itemId: String): Boolean
    suspend fun useItem(itemId: String): Boolean
    suspend fun joinFaction(faction: Faction): Boolean
    suspend fun heartbeat(): Boolean
    suspend fun explicitLogout(): Boolean
    suspend fun startHealing(): Boolean
    suspend fun finishHealing(): Boolean
    suspend fun instantHeal(): Boolean
    fun getPlayersAtLocation(location: String): Flow<List<Character>>
    fun getTopPlayers(limit: Int, faction: Faction? = null): Flow<List<Character>>
    fun getPlayerProfile(playerId: String): Flow<Character?>
    fun getLocations(): Flow<List<LocationDef>>
    fun getEnemyDefs(): Flow<List<EnemyDef>>
    fun getMissionDefs(): Flow<List<Mission>>
    fun getMailMessages(userId: String): Flow<List<MailMessage>>
    suspend fun markMailAsRead(mailId: String): Boolean
    suspend fun deleteMail(mailId: String): Boolean
    suspend fun claimMailRewards(mailId: String): Boolean
    fun getMarketItems(): Flow<List<Item>>
    suspend fun catchFish(fishId: String): Boolean
    suspend fun cookFish(itemId: String): Boolean
    suspend fun purchaseMedicalLicense(): Boolean
    suspend fun healPlayer(targetPlayerId: String): Boolean
    suspend fun startMonsterHunt(): Boolean
    suspend fun rollMythicArt(): Boolean
    suspend fun adminGrantTestItems(): Boolean
}

class FirestoreGameRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) : GameRepository {
    
    override fun getCharacter(userId: String): Flow<Character?> = callbackFlow {
        val docRef = db.collection("players").document(userId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreGameRepository", "Error fetching character for $userId", error)
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot?.exists() == true) {
                val character = snapshot.toObject<Character>()
                if (character != null) {
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

    override suspend fun createCharacter(name: String, gender: Gender, race: Race): Boolean {
        val data = hashMapOf(
            "name" to name,
            "gender" to gender.name,
            "race" to race.name
        )
        functions.getHttpsCallable("createCharacter").call(data).await()
        return true
    }

    override suspend fun train(statType: StatType): Boolean {
        val data = hashMapOf("statType" to statType.name)
        functions.getHttpsCallable("train").call(data).await()
        return true
    }

    override suspend fun finishTraining(): Boolean {
        functions.getHttpsCallable("finishTraining").call().await()
        return true
    }

    override suspend fun completeMission(missionId: String): Boolean {
        val data = hashMapOf("missionId" to missionId)
        functions.getHttpsCallable("completeMission").call(data).await()
        return true
    }

    override fun getAvailableMissions(): Flow<List<Mission>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("missions")
            .orderBy("difficulty")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching available missions", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Mission>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun startTravel(destination: String): Boolean {
        val data = hashMapOf("destination" to destination)
        functions.getHttpsCallable("startTravel").call(data).await()
        return true
    }

    override suspend fun finishTravel(): Boolean {
        functions.getHttpsCallable("finishTravel").call().await()
        return true
    }

    override suspend fun combatAction(action: CombatAction, techniqueId: String?, itemId: String?): Boolean {
        val data = hashMapOf(
            "action" to action.name,
            "techniqueId" to techniqueId,
            "itemId" to itemId
        )
        functions.getHttpsCallable("combatAction").call(data).await()
        return true
    }

    override suspend fun attackPlayer(defenderId: String): Boolean {
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

    override suspend fun joinFaction(faction: Faction): Boolean {
        val data = hashMapOf("faction" to faction.name)
        functions.getHttpsCallable("joinFaction").call(data).await()
        return true
    }

    override suspend fun heartbeat(): Boolean {
        functions.getHttpsCallable("heartbeat").call().await()
        return true
    }

    override suspend fun explicitLogout(): Boolean {
        functions.getHttpsCallable("explicitLogout").call().await()
        return true
    }

    override suspend fun startHealing(): Boolean {
        functions.getHttpsCallable("startHealing").call().await()
        return true
    }

    override suspend fun finishHealing(): Boolean {
        functions.getHttpsCallable("finishHealing").call().await()
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching players at location: $location", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getTopPlayers(limit: Int, faction: Faction?): Flow<List<Character>> = callbackFlow {
        var query: com.google.firebase.firestore.Query = db.collection("players")
        
        if (faction != null) {
            query = query.whereEqualTo("faction", faction.name)
            if (faction == Faction.Pirate) {
                query = query.orderBy("bounty", Query.Direction.DESCENDING)
            } else {
                query = query.orderBy("level", Query.Direction.DESCENDING)
                             .orderBy("xp", Query.Direction.DESCENDING)
            }
        } else {
            query = query.orderBy("level", Query.Direction.DESCENDING)
                         .orderBy("xp", Query.Direction.DESCENDING)
        }

        val subscription = query.limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching top players (faction=$faction)", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPlayerProfile(playerId: String): Flow<Character?> = callbackFlow {
        val subscription = db.collection("players").document(playerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching player profile: $playerId", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<Character>())
            }
        awaitClose { subscription.remove() }
    }

    override fun getLocations(): Flow<List<LocationDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("locations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching locations", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<LocationDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getEnemyDefs(): Flow<List<EnemyDef>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("enemies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching enemy defs", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<EnemyDef>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMissionDefs(): Flow<List<Mission>> = callbackFlow {
        val subscription = db.collection("gameData").document("world").collection("missions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching mission defs", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Mission>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMailMessages(userId: String): Flow<List<MailMessage>> = callbackFlow {
        val subscription = db.collection("players").document(userId).collection("mail")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching mail for $userId", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<MailMessage>()?.copy(id = doc.id) })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun markMailAsRead(mailId: String): Boolean {
        functions.getHttpsCallable("markMailAsRead").call(hashMapOf("mailId" to mailId)).await()
        return true
    }

    override suspend fun deleteMail(mailId: String): Boolean {
        functions.getHttpsCallable("deleteMail").call(hashMapOf("mailId" to mailId)).await()
        return true
    }

    override suspend fun claimMailRewards(mailId: String): Boolean {
        functions.getHttpsCallable("claimMailRewards").call(hashMapOf("mailId" to mailId)).await()
        return true
    }

    override fun getMarketItems(): Flow<List<Item>> = callbackFlow {
        val subscription = db.collection("gameData").document("items").collection("all")
            .whereNotEqualTo("type", "Artifact")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching market items", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Item>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun catchFish(fishId: String): Boolean {
        functions.getHttpsCallable("catchFish").call(hashMapOf("fishId" to fishId)).await()
        return true
    }

    override suspend fun cookFish(itemId: String): Boolean {
        functions.getHttpsCallable("cookFish").call(hashMapOf("itemId" to itemId)).await()
        return true
    }

    override suspend fun purchaseMedicalLicense(): Boolean {
        functions.getHttpsCallable("purchaseMedicalLicense").call().await()
        return true
    }

    override suspend fun adminGrantTestItems(): Boolean {
        functions.getHttpsCallable("adminGrantTestItems").call().await()
        return true
    }

    override suspend fun healPlayer(targetPlayerId: String): Boolean {
        functions.getHttpsCallable("healPlayer").call(hashMapOf("targetPlayerId" to targetPlayerId)).await()
        return true
    }

    override suspend fun startMonsterHunt(): Boolean {
        functions.getHttpsCallable("startMonsterHunt").call().await()
        return true
    }

    override suspend fun rollMythicArt(): Boolean {
        functions.getHttpsCallable("rollMythicArt").call().await()
        return true
    }
}
