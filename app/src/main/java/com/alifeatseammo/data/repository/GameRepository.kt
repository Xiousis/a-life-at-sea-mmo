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
    suspend fun upgradeShip(upgradeType: String): Boolean
    suspend fun sellItem(itemId: String): Boolean
    suspend fun useItem(itemId: String): Boolean
    suspend fun joinFaction(faction: Faction): Boolean
    suspend fun heartbeat(): Boolean
    suspend fun explicitLogout(): Boolean
    suspend fun startHealing(): Boolean
    suspend fun finishHealing(): Boolean
    suspend fun instantHeal(): Boolean
    fun getPlayersAtLocation(location: String): Flow<List<Character>>
    fun getTopPlayers(limit: Int, faction: Faction? = null, sortBy: String = "level"): Flow<List<Character>>
    fun getPlayerProfile(playerId: String): Flow<Character?>
    fun getCharacters(ids: List<String>): Flow<List<Character>>
    fun getLocations(): Flow<List<LocationDef>>
    fun getEnemyDefs(): Flow<List<EnemyDef>>
    fun getTechniques(): Flow<List<Technique>>
    fun getMissionDefs(): Flow<List<Mission>>
    fun getMailMessages(userId: String): Flow<List<MailMessage>>
    suspend fun markMailAsRead(mailId: String): Boolean
    suspend fun deleteMail(mailId: String): Boolean
    suspend fun claimMailRewards(mailId: String): Boolean
    suspend fun sendMail(recipientId: String, subject: String, body: String): Boolean
    fun getMarketItems(category: String? = null): Flow<List<Item>>
    suspend fun startFishing(): String?
    suspend fun catchFish(): Boolean
    suspend fun cookFish(itemId: String): Boolean
    suspend fun purchaseMedicalLicense(): Boolean
    suspend fun healPlayer(targetPlayerId: String): Boolean
    suspend fun startMonsterHunt(): Boolean
    suspend fun rollMythicArt(): Boolean
    suspend fun challengeHighestRank(targetId: String): Boolean
    suspend fun adminGrantTestItems(): Boolean
}

class FirestoreGameRepository(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
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

    override suspend fun upgradeShip(upgradeType: String): Boolean {
        val data = hashMapOf("upgradeType" to upgradeType)
        functions.getHttpsCallable("upgradeShip").call(data).await()
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

    override fun getTopPlayers(limit: Int, faction: Faction?, sortBy: String): Flow<List<Character>> = callbackFlow {
        var query: com.google.firebase.firestore.Query = db.collection("players")
        
        if (faction != null) {
            query = query.whereEqualTo("faction", faction.name)
        }

        query = when (sortBy) {
            "bounty" -> query.orderBy("bounty", Query.Direction.DESCENDING)
            "infamy" -> query.orderBy("infamy", Query.Direction.DESCENDING)
            "gold" -> query.orderBy("gold", Query.Direction.DESCENDING)
            else -> query.orderBy("level", Query.Direction.DESCENDING)
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

    override fun getCharacters(ids: List<String>): Flow<List<Character>> = callbackFlow {
        if (ids.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Firestore whereIn limit is 10 or 30 depending on version, but crew limit is 20.
        // We'll assume whereIn works for up to 30 as per recent Firebase updates.
        val subscription = db.collection("players")
            .whereIn("id", ids)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreGameRepository", "Error fetching characters: $ids", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> doc.toObject<Character>() })
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getLocations(): Flow<List<LocationDef>> = flow {
        try {
            val snapshot = db.collection("gameData").document("world").collection("locations").get().await()
            emit(snapshot.documents.mapNotNull { it.toObject<LocationDef>() })
        } catch (e: Exception) {
            android.util.Log.e("FirestoreGameRepository", "Error fetching locations", e)
            emit(emptyList())
        }
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

    override fun getTechniques(): Flow<List<Technique>> = flow {
        try {
            val snapshot = db.collection("gameData").document("skills").collection("techniques").get().await()
            emit(snapshot.documents.mapNotNull { it.toObject<Technique>() })
        } catch (e: Exception) {
            android.util.Log.e("FirestoreGameRepository", "Error fetching techniques", e)
            emit(emptyList())
        }
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

    override suspend fun sendMail(recipientId: String, subject: String, body: String): Boolean {
        val data = hashMapOf(
            "recipientId" to recipientId,
            "subject" to subject,
            "body" to body
        )
        functions.getHttpsCallable("sendMail").call(data).await()
        return true
    }

    override fun getMarketItems(category: String?): Flow<List<Item>> = callbackFlow {
        var query = db.collection("gameData").document("items").collection("all")
            .whereNotEqualTo("type", "Artifact")
        
        if (category != null) {
            query = query.whereEqualTo("weaponCategory", category)
        }

        val subscription = query.limit(20)
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

    override suspend fun startFishing(): String? {
        val result = functions.getHttpsCallable("startFishing").call().await()
        val data = result.data as? Map<String, Any>
        return data?.get("fishId") as? String
    }

    override suspend fun catchFish(): Boolean {
        functions.getHttpsCallable("catchFish").call().await()
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

    override suspend fun challengeHighestRank(targetId: String): Boolean {
        functions.getHttpsCallable("challengeHighestRank").call(hashMapOf("targetId" to targetId)).await()
        return true
    }
}
