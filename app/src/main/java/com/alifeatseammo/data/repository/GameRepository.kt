package com.alifeatseammo.data.repository

import com.alifeatseammo.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

interface GameRepository {
    fun getCharacter(userId: String): Flow<Character?>
    fun createCharacter(userId: String, name: String, origin: String, style: CombatStyle)
    fun train(userId: String, statType: StatType)
    fun completeMission(userId: String, mission: Mission)
    fun getAvailableMissions(): List<Mission>
    fun startTravel(userId: String, destination: String, arrivalTime: Long)
    fun attackPlayer(attackerId: String, defenderId: String)
}

class FirestoreGameRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : GameRepository {
    
    override fun getCharacter(userId: String): Flow<Character?> = callbackFlow {
        val docRef = db.collection("players").document(userId)
        val subscription = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject<Character>())
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    override fun createCharacter(userId: String, name: String, origin: String, style: CombatStyle) {
        val character = Character(
            id = userId,
            name = name,
            originIsland = origin,
            style = style
        )
        db.collection("players").document(userId).set(character)
    }

    override fun train(userId: String, statType: StatType) {
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            
            if (character.energy >= 10) {
                val newStats = when (statType) {
                    StatType.Strength -> character.stats.copy(strength = character.stats.strength + 1)
                    StatType.Endurance -> character.stats.copy(endurance = character.stats.endurance + 1)
                    StatType.Agility -> character.stats.copy(agility = character.stats.agility + 1)
                    StatType.Perception -> character.stats.copy(perception = character.stats.perception + 1)
                    StatType.Willpower -> character.stats.copy(willpower = character.stats.willpower + 1)
                    StatType.Luck -> character.stats.copy(luck = character.stats.luck + 1)
                }
                transaction.update(docRef, "stats", newStats)
                transaction.update(docRef, "energy", character.energy - 10)
            }
        }
    }

    override fun completeMission(userId: String, mission: Mission) {
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            
            if (character.energy >= mission.energyCost) {
                transaction.update(docRef, "energy", character.energy - mission.energyCost)
                transaction.update(docRef, "gold", character.gold + mission.rewards.gold)
                transaction.update(docRef, "xp", character.xp + mission.rewards.xp)
            }
        }
    }

    override fun getAvailableMissions(): List<Mission> {
        return listOf(
            Mission("1", "Scout the Shore", "Check for any suspicious activity on the beach.", 10, 1, Reward(50, 20), 1),
            Mission("2", "Deliver Message", "Take a letter to the local merchant.", 15, 1, Reward(75, 30), 2),
            Mission("3", "Clear Pests", "Help a farmer clear giant crabs from his field.", 25, 2, Reward(150, 60), 3)
        )
    }

    override fun startTravel(userId: String, destination: String, arrivalTime: Long) {
        val docRef = db.collection("players").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val character = snapshot.toObject<Character>() ?: return@runTransaction
            
            if (character.travelState == null) {
                transaction.update(docRef, "travelState", TravelState(destination, arrivalTime))
            }
        }
    }

    override fun attackPlayer(attackerId: String, defenderId: String) {
        // Simple PvP logic: Attacker wins if total stats are higher
        // In a real game, this would be more complex and server-side
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
}

class MockGameRepository : GameRepository {
    private val _character = MutableStateFlow<Character?>(null)
    
    override fun getCharacter(userId: String): Flow<Character?> = _character.asStateFlow()

    override fun createCharacter(userId: String, name: String, origin: String, style: CombatStyle) {
        _character.value = Character(
            name = name,
            originIsland = origin,
            style = style
        )
    }

    override fun train(userId: String, statType: StatType) {
        _character.update { char ->
            char?.let {
                if (it.energy >= 10) {
                    val newStats = when (statType) {
                        StatType.Strength -> it.stats.copy(strength = it.stats.strength + 1)
                        StatType.Endurance -> it.stats.copy(endurance = it.stats.endurance + 1)
                        StatType.Agility -> it.stats.copy(agility = it.stats.agility + 1)
                        StatType.Perception -> it.stats.copy(perception = it.stats.perception + 1)
                        StatType.Willpower -> it.stats.copy(willpower = it.stats.willpower + 1)
                        StatType.Luck -> it.stats.copy(luck = it.stats.luck + 1)
                    }
                    it.copy(stats = newStats, energy = it.energy - 10)
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
                    )
                } else it
            }
        }
    }

    override fun getAvailableMissions(): List<Mission> {
        return listOf(
            Mission("1", "Scout the Shore", "Check for any suspicious activity on the beach.", 10, 1, Reward(50, 20), 1),
            Mission("2", "Deliver Message", "Take a letter to the local merchant.", 15, 1, Reward(75, 30), 2),
            Mission("3", "Clear Pests", "Help a farmer clear giant crabs from his field.", 25, 2, Reward(150, 60), 3)
        )
    }

    override fun startTravel(userId: String, destination: String, arrivalTime: Long) {}
    override fun attackPlayer(attackerId: String, defenderId: String) {}
}
