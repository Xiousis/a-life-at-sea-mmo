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
    suspend fun finishTraining(): Boolean
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

    override suspend fun finishTraining(): Boolean {
        functions.getHttpsCallable("finishTraining").call().await()
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
            .addSnapshotListener { snapshot, _ ->
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
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
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
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
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
                    val updatedChar = when (statType) {
                        StatType.Strength -> it.copy(stats = it.stats.copy(strength = it.stats.strength + 1))
                        StatType.Endurance -> it.copy(stats = it.stats.copy(endurance = it.stats.endurance + 1))
                        StatType.Agility -> it.copy(stats = it.stats.copy(agility = it.stats.agility + 1))
                        StatType.Perception -> it.copy(stats = it.stats.copy(perception = it.stats.perception + 1))
                        StatType.Willpower -> it.copy(stats = it.stats.copy(willpower = it.stats.willpower + 1))
                        StatType.Luck -> it.copy(stats = it.stats.copy(luck = it.stats.luck + 1))
                        StatType.Swordsmanship -> it.copy(stats = it.stats.copy(swordsmanship = it.stats.swordsmanship + 1))
                        StatType.Brawling -> it.copy(stats = it.stats.copy(brawling = it.stats.brawling + 1))
                        StatType.Gunslinging -> it.copy(stats = it.stats.copy(gunslinging = it.stats.gunslinging + 1))
                        StatType.Spear -> it.copy(stats = it.stats.copy(spear = it.stats.spear + 1))
                        StatType.MartialArts -> it.copy(stats = it.stats.copy(martialArts = it.stats.martialArts + 1))
                        StatType.Sniper -> it.copy(stats = it.stats.copy(sniper = it.stats.sniper + 1))
                        StatType.MysticArts -> it.copy(stats = it.stats.copy(mysticArts = it.stats.mysticArts + 1))
                        StatType.Cooking -> it.copy(professionStats = it.professionStats.copy(cooking = it.professionStats.cooking + 1))
                        StatType.Navigating -> it.copy(professionStats = it.professionStats.copy(navigating = it.professionStats.navigating + 1))
                        StatType.TreasureHunting -> it.copy(professionStats = it.professionStats.copy(treasureHunting = it.professionStats.treasureHunting + 1))
                        StatType.Blacksmith -> it.copy(professionStats = it.professionStats.copy(blacksmith = it.professionStats.blacksmith + 1))
                        StatType.Fishing -> it.copy(professionStats = it.professionStats.copy(fishing = it.professionStats.fishing + 1))
                    }
                    updatedChar.copy(
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
    override suspend fun purchaseItem(itemId: String, shopId: String): Boolean {
        _character.update { char ->
            char?.let {
                // Find item in market (mocking it by just checking ID)
                val item = listOf(
                    Item("rod_1", "Old Fishing Rod", "A simple wooden rod. Good enough to catch basic fish.", ItemType.Tool, Rarity.Common, 100),
                    Item("potion_1", "Health Potion", "Restores 30 HP.", ItemType.Consumable, Rarity.Common, 50),
                    Item("sword_1", "Iron Cutlass", "A standard pirate sword.", ItemType.Weapon, Rarity.Common, 250, Stats(strength = 5)),
                    Item("armor_1", "Leather Vest", "Provides basic protection.", ItemType.Armor, Rarity.Common, 200, Stats(endurance = 3)),
                    Item("bread_1", "Stale Bread", "Hard as a rock, but it's food.", ItemType.Food, Rarity.Common, 10)
                ).find { it.id == itemId }

                if (item != null && it.gold >= item.price) {
                    it.copy(
                        inventory = it.inventory + item,
                        gold = it.gold - item.price
                    )
                } else it
            }
        }
        return true
    }
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
    override suspend fun heartbeat(): Boolean = true
    override suspend fun explicitLogout(): Boolean = true
    override suspend fun startHealing(): Boolean = true
    override suspend fun finishHealing(): Boolean = true
    override suspend fun finishTraining(): Boolean = true
    override suspend fun instantHeal(): Boolean = true

    override fun getPlayersAtLocation(location: String): Flow<List<Character>> = flowOf(emptyList())
    override fun getTopPlayers(limit: Int, faction: Faction?): Flow<List<Character>> = flowOf(emptyList())

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

    override fun getLocations(): Flow<List<LocationDef>> = flowOf(
        listOf(
            LocationDef(
                id = "fogi_tail",
                name = "Fogi Tail Island",
                region = "East Blue",
                description = "A peaceful starting island with a small village.",
                actions = listOf(
                    ActionDef(ActionType.Docks, "Docks", "⚓"),
                    ActionDef(ActionType.Tavern, "Tavern", "🍺"),
                    ActionDef(ActionType.Kitchen, "Kitchen", "🍳"),
                    ActionDef(ActionType.Training, "Training", "🥋"),
                    ActionDef(ActionType.Market, "General Store", "🛍️"),
                    ActionDef(ActionType.Fishing, "Fishing", "🎣")
                ),
                x = 0, y = 0
            ),
            LocationDef(
                id = "ironcrest",
                name = "Ironcrest Isle",
                region = "East Blue",
                description = "A rocky island known for its rich iron mines and shipyards.",
                actions = listOf(
                    ActionDef(ActionType.Docks, "Docks", "⚓"),
                    ActionDef(ActionType.Shipyard, "Shipyard", "🔨"),
                    ActionDef(ActionType.Forge, "Forge", "🔥"),
                    ActionDef(ActionType.Market, "Ironcrest Market", "💰")
                ),
                x = 200, y = -100
            ),
            LocationDef(
                id = "amber_reach",
                name = "Amber Reach",
                region = "East Blue",
                description = "A dense forest island with ancient ruins hidden within.",
                actions = listOf(
                    ActionDef(ActionType.Docks, "Docks", "⚓"),
                    ActionDef(ActionType.Expedition, "Expedition", "🗺️"),
                    ActionDef(ActionType.Market, "Amber Trading Post", "🏹"),
                    ActionDef(ActionType.Fishing, "Fishing", "🎣"),
                    ActionDef(ActionType.Work, "Work", "⚓")
                ),
                x = -150, y = 300
            ),
            LocationDef(
                id = "starry_peak",
                name = "Starry Peak",
                region = "East Blue",
                description = "A high mountain peak with a clear view of the stars.",
                actions = listOf(
                    ActionDef(ActionType.Docks, "Docks", "⚓"),
                    ActionDef(ActionType.Observatory, "Observatory", "🔭"),
                    ActionDef(ActionType.Training, "Training", "🥋")
                ),
                x = 400, y = 400
            )
        )
    )
    override fun getEnemyDefs(): Flow<List<EnemyDef>> = flowOf(emptyList())
    override fun getMissionDefs(): Flow<List<Mission>> = flowOf(emptyList())
    override fun getMailMessages(userId: String): Flow<List<MailMessage>> = flowOf(emptyList())
    override suspend fun markMailAsRead(mailId: String): Boolean = true
    override suspend fun deleteMail(mailId: String): Boolean = true
    override suspend fun claimMailRewards(mailId: String): Boolean = true
    override fun getMarketItems(): Flow<List<Item>> = flowOf(
        listOf(
            Item("rod_1", "Old Fishing Rod", "A simple wooden rod. Good enough to catch basic fish.", ItemType.Tool, Rarity.Common, 100),
            Item("potion_1", "Health Potion", "Restores 30 HP.", ItemType.Consumable, Rarity.Common, 50),
            Item("sword_1", "Iron Cutlass", "A standard pirate sword.", ItemType.Weapon, Rarity.Common, 250, Stats(strength = 5)),
            Item("armor_1", "Leather Vest", "Provides basic protection.", ItemType.Armor, Rarity.Common, 200, Stats(endurance = 3)),
            Item("bread_1", "Stale Bread", "Hard as a rock, but it's food.", ItemType.Food, Rarity.Common, 10)
        )
    )

    override suspend fun catchFish(fishId: String): Boolean {
        _character.update { char ->
            char?.let {
                val fishItem = Item(
                    id = "fish_${System.currentTimeMillis()}",
                    name = fishId.replaceFirstChar { it.uppercase() },
                    description = "A fresh fish caught from the sea.",
                    type = ItemType.Fish,
                    price = 20
                )
                it.copy(
                    inventory = it.inventory + fishItem,
                    professionStats = it.professionStats.copy(fishing = it.professionStats.fishing + 1)
                ).checkLevelUp()
            }
        }
        return true
    }

    override suspend fun cookFish(itemId: String): Boolean {
        _character.update { char ->
            char?.let {
                if (it.professionStats.cooking < 5) return@let it // Requires level 5 cooking
                
                val fishIndex = it.inventory.indexOfFirst { item -> item.id == itemId && item.type == ItemType.Fish }
                if (fishIndex != -1) {
                    val newInventory = it.inventory.toMutableList()
                    val fish = newInventory[fishIndex]
                    newInventory[fishIndex] = Item(
                        id = "cooked_${fish.id}",
                        name = "Cooked ${fish.name}",
                        description = "Deliciously prepared ${fish.name}.",
                        type = ItemType.Food,
                        price = fish.price * 2
                    )
                    it.copy(
                        inventory = newInventory,
                        professionStats = it.professionStats.copy(cooking = it.professionStats.cooking + 1)
                    )
                } else it
            }
        }
        return true
    }
}
