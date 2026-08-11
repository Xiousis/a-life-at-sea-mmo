package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

interface GameRepository {
    fun getCharacter(userId: String): Flow<Character?>
    fun createCharacter(userId: String, name: String, gender: Gender, race: Race)
    fun train(userId: String, statType: StatType)
    fun completeMission(userId: String, mission: Mission)
    fun getAvailableMissions(): List<Mission>
    fun startTravel(userId: String, destination: String, arrivalTime: Long)
    fun combatAction(userId: String, action: CombatAction)
    fun attackPlayer(attackerId: String, defenderId: String)
    fun getPlayersAtLocation(location: String): Flow<List<Character>>
    fun getTopPlayers(limit: Int): Flow<List<Character>>
}

class FirestoreGameRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : GameRepository {
    
    override fun getCharacter(userId: String): Flow<Character?> = callbackFlow {
        val docRef = db.collection("players").document(userId)
        val subscription = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val character = snapshot.toObject<Character>()
                if (character != null) {
                    // Lazy Travel Arrival check
                    val travel = character.travelState
                    if (travel != null && travel.arrivalTime <= System.currentTimeMillis()) {
                        // Optimistically emit the arrived state
                        trySend(character.copy(currentLocation = travel.destination, travelState = null))
                        
                        // Persist to Firestore
                        db.runTransaction { transaction ->
                            transaction.update(docRef, "currentLocation", travel.destination)
                            transaction.update(docRef, "travelState", null)
                        }
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
        val character = Character(
            id = userId,
            name = name,
            gender = gender,
            race = race
        )
        db.collection("players").document(userId).set(character)
    }

    override fun train(userId: String, statType: StatType) {
        // TODO: Move to Cloud Function for production
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            
            if (character.energy >= 10) {
                val updatedStats = when (statType) {
                    StatType.Strength -> character.stats.copy(strength = character.stats.strength + 1)
                    StatType.Endurance -> character.stats.copy(endurance = character.stats.endurance + 1)
                    StatType.Agility -> character.stats.copy(agility = character.stats.agility + 1)
                    StatType.Perception -> character.stats.copy(perception = character.stats.perception + 1)
                    StatType.Willpower -> character.stats.copy(willpower = character.stats.willpower + 1)
                    StatType.Luck -> character.stats.copy(luck = character.stats.luck + 1)
                    StatType.Swordsmanship -> character.stats.copy(swordsmanship = character.stats.swordsmanship + 1)
                    StatType.Brawling -> character.stats.copy(brawling = character.stats.brawling + 1)
                    StatType.Gunslinging -> character.stats.copy(gunslinging = character.stats.gunslinging + 1)
                    StatType.Spear -> character.stats.copy(spear = character.stats.spear + 1)
                    StatType.MartialArts -> character.stats.copy(martialArts = character.stats.martialArts + 1)
                    StatType.DualBlades -> character.stats.copy(dualBlades = character.stats.dualBlades + 1)
                }
                
                // Add 5 XP for training
                val updatedCharacter = character.copy(
                    stats = updatedStats,
                    energy = character.energy - 10,
                    xp = character.xp + 5
                ).checkLevelUp()

                transaction.set(docRef, updatedCharacter)
            }
        }
    }

    override fun completeMission(userId: String, mission: Mission) {
        // TODO: Move to Cloud Function for production
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            
            if (character.energy >= mission.energyCost) {
                val updatedCharacter = character.copy(
                    energy = character.energy - mission.energyCost,
                    gold = character.gold + mission.rewards.gold,
                    xp = character.xp + mission.rewards.xp
                ).checkLevelUp()
                
                transaction.set(docRef, updatedCharacter)
            }
        }
    }

    override fun getAvailableMissions(): List<Mission> {
        return listOf(
            Mission("1", "Scout the Shore", "Check for any suspicious activity on Fogi Tail Island's beach.", 10, 1, Reward(50, 20), 1),
            Mission("2", "Deliver Message", "Take a letter to the merchant on Ironcrest Isle.", 15, 1, Reward(75, 30), 2),
            Mission("3", "Clear Pests", "Help an Amber Reach farmer clear giant crabs from his field.", 25, 2, Reward(150, 60), 3)
        )
    }

    override fun startTravel(userId: String, destination: String, arrivalTime: Long) {
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            
            if (character.travelState == null && character.combatState == null) {
                // Encounter logic (25% chance if travel time > 30s)
                val travelDuration = arrivalTime - System.currentTimeMillis()
                val ambush = if (travelDuration > 30000 && (1..100).random() <= 25) {
                    generateRandomEnemy(character.level)
                } else null

                if (ambush != null) {
                    transaction.update(docRef, "combatState", CombatState(enemy = ambush, logs = listOf("You were ambushed by a ${ambush.name}!")))
                } else {
                    transaction.update(docRef, "travelState", TravelState(destination, arrivalTime))
                }
            }
        }
    }

    override fun combatAction(userId: String, action: CombatAction) {
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            val combatState = character.combatState ?: return@runTransaction
            if (combatState.isFinished) return@runTransaction

            val logs = combatState.logs.toMutableList()
            var currentEnemy = combatState.enemy
            var currentPlayerHp = character.hp
            var isFinished = false
            var playerWon = false

            when (action) {
                CombatAction.Attack -> {
                    val damage = (character.stats.strength * 2) + (character.stats.swordsmanship / 2)
                    currentEnemy = currentEnemy.copy(hp = (currentEnemy.hp - damage).coerceAtLeast(0))
                    logs.add("You attacked ${currentEnemy.name} for $damage damage!")
                }
                CombatAction.Technique -> {
                    // Placeholder for future Technique system
                    logs.add("Techniques are not yet available.")
                }
                CombatAction.Item -> {
                    // Placeholder for future Item system
                    logs.add("Your inventory is currently empty.")
                }
                CombatAction.Defend -> {
                    logs.add("You take a defensive stance.")
                }
                CombatAction.Flee -> {
                    if ((1..100).random() <= 50) {
                        transaction.update(docRef, "combatState", null)
                        return@runTransaction
                    } else {
                        logs.add("You failed to flee!")
                    }
                }
            }

            if (currentEnemy.hp <= 0) {
                logs.add("You defeated ${currentEnemy.name}!")
                isFinished = true
                playerWon = true
            } else {
                // Enemy Turn
                val enemyDamage = (currentEnemy.stats.strength * 1.5).toInt()
                currentPlayerHp = (currentPlayerHp - enemyDamage).coerceAtLeast(0)
                logs.add("${currentEnemy.name} attacked you for $enemyDamage damage!")
                
                if (currentPlayerHp <= 0) {
                    logs.add("You were defeated...")
                    isFinished = true
                    playerWon = false
                }
            }

            if (isFinished) {
                val updatedChar = if (playerWon) {
                    character.copy(
                        hp = currentPlayerHp,
                        gold = character.gold + currentEnemy.goldReward,
                        xp = character.xp + currentEnemy.xpReward,
                        combatState = null
                    ).checkLevelUp()
                } else {
                    character.copy(
                        hp = character.maxHp,
                        gold = (character.gold * 0.9).toInt(),
                        currentLocation = "Fogi Tail Island",
                        combatState = null
                    )
                }
                transaction.set(docRef, updatedChar)
            } else {
                transaction.update(docRef, "hp", currentPlayerHp)
                transaction.update(docRef, "combatState", combatState.copy(enemy = currentEnemy, logs = logs))
            }
        }
    }

    private fun generateRandomEnemy(playerLevel: Int): Enemy {
        val names = listOf("Sea Serpent", "Pirate Scout", "Feral Crab", "Ghost Ship")
        val name = names.random()
        val level = (playerLevel + (-1..1).random()).coerceAtLeast(1)
        val stats = Stats(
            strength = 5 + level * 2,
            endurance = 5 + level * 2,
            agility = 5 + level
        )
        return Enemy(
            name = name,
            level = level,
            maxHp = 40 + (level * 10),
            hp = 40 + (level * 10),
            stats = stats,
            goldReward = 20 * level,
            xpReward = 15 * level
        )
    }

    override fun attackPlayer(attackerId: String, defenderId: String) {
        // Simple PvP logic: Attacker wins if total stats are higher
        // TODO: Move to Cloud Function for production
        val attackerRef = db.collection("players").document(attackerId)
        val defenderRef = db.collection("players").document(defenderId)
        
        db.runTransaction { transaction ->
            val attacker = transaction.get(attackerRef).toObject<Character>() ?: return@runTransaction
            val defender = transaction.get(defenderRef).toObject<Character>() ?: return@runTransaction
            
            val attackerPower = attacker.stats.strength + attacker.stats.agility + attacker.stats.willpower
            val defenderPower = defender.stats.strength + defender.stats.agility + defender.stats.willpower
            
            if (attackerPower > defenderPower) {
                transaction.update(attackerRef, "gold", attacker.gold + (defender.gold / 10))
                transaction.update(defenderRef, "gold", defender.gold - (defender.gold / 10))
                transaction.update(attackerRef, "bounty", attacker.bounty + 100)
            } else {
                transaction.update(attackerRef, "gold", attacker.gold - (attacker.gold / 10))
                transaction.update(defenderRef, "gold", defender.gold + (attacker.gold / 10))
            }
        }
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

    override fun train(userId: String, statType: StatType) {
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
    }

    override fun completeMission(userId: String, mission: Mission) {
        _character.update { char ->
            char?.let {
                if (it.energy >= mission.energyCost) {
                    it.copy(
                        energy = it.energy - mission.energyCost,
                        gold = it.gold + mission.rewards.gold,
                        xp = it.xp + mission.rewards.xp
                    ).checkLevelUp()
                } else it
            }
        }
    }

    override fun getAvailableMissions(): List<Mission> {
        return listOf(
            Mission("1", "Scout the Shore", "Check for any suspicious activity on Fogi Tail Island's beach.", 10, 1, Reward(50, 20), 1),
            Mission("2", "Deliver Message", "Take a letter to the merchant on Ironcrest Isle.", 15, 1, Reward(75, 30), 2),
            Mission("3", "Clear Pests", "Help an Amber Reach farmer clear giant crabs from his field.", 25, 2, Reward(150, 60), 3)
        )
    }

    override fun startTravel(userId: String, destination: String, arrivalTime: Long) {}
    override fun combatAction(userId: String, action: CombatAction) {}
    override fun attackPlayer(attackerId: String, defenderId: String) {}

    override fun getPlayersAtLocation(location: String): Flow<List<Character>> = flowOf(emptyList())
    override fun getTopPlayers(limit: Int): Flow<List<Character>> = flowOf(emptyList())
}
