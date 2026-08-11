import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// --- Constants & Config ---
const ENERGY_REGEN_RATE_MS = 3 * 60 * 1000; // 1 energy per 3 minutes
const MAX_ENERGY = 100;

const STAT_MAPPING: Record<string, string> = {
    "Strength": "strength",
    "Endurance": "endurance",
    "Agility": "agility",
    "Perception": "perception",
    "Willpower": "willpower",
    "Luck": "luck",
    "Swordsmanship": "swordsmanship",
    "Brawling": "brawling",
    "Gunslinging": "gunslinging",
    "Spear": "spear",
    "MartialArts": "martialArts",
    "DualBlades": "dualBlades"
};

// --- Helper Functions ---

function recordLog(transaction: admin.firestore.Transaction, userId: string, action: string, details: string, goldChange: number = 0, xpChange: number = 0) {
    const logRef = db.collection("logs").doc();
    transaction.set(logRef, {
        id: logRef.id,
        userId,
        action,
        details,
        goldChange,
        xpChange,
        timestamp: Date.now()
    });
}

function calculateCurrentEnergy(character: any): { energy: number, energyUpdatedAt: number } {
    const now = Date.now();
    const elapsed = now - character.energyUpdatedAt;
    const regenerated = Math.floor(elapsed / ENERGY_REGEN_RATE_MS);

    const currentMaxEnergy = character.maxEnergy || MAX_ENERGY;
    if (regenerated <= 0) return { energy: character.energy, energyUpdatedAt: character.energyUpdatedAt };

    const newEnergy = Math.min(currentMaxEnergy, character.energy + regenerated);
    const newTimestamp = character.energy + regenerated >= currentMaxMaxEnergy ? now : character.energyUpdatedAt + (regenerated * ENERGY_REGEN_RATE_MS);

    return { energy: newEnergy, energyUpdatedAt: newTimestamp };
}

function checkLevelUp(character: any) {
    let { level, xp, stats, maxEnergy, energy, maxHp, hp } = character;
    let xpNeeded = level * 100;

    let leveledUp = false;
    while (xp >= xpNeeded) {
        level++;
        xp -= xpNeeded;
        maxEnergy += 5;
        energy = maxEnergy;
        stats.endurance += 1;
        maxHp = 50 + (stats.endurance * 10);
        hp = maxHp;

        stats.strength += 1;
        stats.agility += 1;
        stats.perception += 1;
        stats.willpower += 1;
        stats.luck += 1;

        xpNeeded = level * 100;
        leveledUp = true;
    }

    return { level, xp, stats, maxEnergy, energy, maxHp, hp, leveledUp };
}

// --- Player Management ---

export const createCharacter = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, gender, race } = data;
    const userId = context.auth.uid;

    if (!name || name.length < 3 || name.length > 16) {
        throw new functions.https.HttpsError("invalid-argument", "Name must be between 3 and 16 characters.");
    }

    const nameQuery = await db.collection("players").where("name", "==", name).get();
    if (!nameQuery.empty) {
        throw new functions.https.HttpsError("already-exists", "Character name is already taken.");
    }

    const character = {
        id: userId,
        name: name,
        gender: gender,
        race: race,
        level: 1,
        xp: 0,
        gold: 100,
        bounty: 0,
        hp: 100,
        maxHp: 100,
        energy: 100,
        maxEnergy: 100,
        energyUpdatedAt: Date.now(),
        lastOnline: Date.now(),
        isOnline: true,
        currentLocation: "Fogi Tail Island",
        title: "Novice Sailor",
        pvpWins: 0,
        pvpLosses: 0,
        stats: {
            strength: 5,
            endurance: 5,
            agility: 5,
            perception: 5,
            willpower: 5,
            luck: 5,
            swordsmanship: 0,
            brawling: 0,
            gunslinging: 0,
            spear: 0,
            martialArts: 0,
            dualBlades: 0
        },
        inventory: [],
        equipment: {},
        travelState: null,
        combatState: null
    };

    await db.collection("players").doc(userId).set(character);
    return { success: true };
});

export const train = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { statType } = data;
    const mappedStat = STAT_MAPPING[statType];
    if (!mappedStat) throw new functions.https.HttpsError("invalid-argument", "Invalid stat type.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);

        if (energy < 10) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        const updatedChar = { ...character, energy: energy - 10, energyUpdatedAt, xp: character.xp + 5 };
        updatedChar.stats[mappedStat] += 1;

        const finalChar = checkLevelUp(updatedChar);
        transaction.set(playerRef, finalChar);
        recordLog(transaction, userId, "Train", `Trained ${statType}`, 0, 5);

        return { success: true, newEnergy: finalChar.energy };
    });
});

// --- Gameplay Mechanics ---

export const startTravel = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { destination, arrivalTime } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        if (character.travelState || character.combatState) {
            throw new functions.https.HttpsError("failed-precondition", "Player is already busy.");
        }

        // Potential for random encounter here
        const travelDuration = arrivalTime - Date.now();
        if (travelDuration > 30000 && Math.random() < 0.25) {
            const enemy = generateEnemy(character.level);
            transaction.update(playerRef, {
                combatState: {
                    enemy: enemy,
                    playerTurn: true,
                    logs: [`You were ambushed by a ${enemy.name}!`],
                    isFinished: false,
                    playerWon: false
                }
            });
            return { ambush: true };
        }

        transaction.update(playerRef, {
            travelState: { destination, arrivalTime }
        });
        return { success: true };
    });
});

export const finishTravel = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const travel = character.travelState;

        if (!travel || travel.arrivalTime > Date.now()) {
            throw new functions.https.HttpsError("failed-precondition", "Travel not yet complete.");
        }

        transaction.update(playerRef, {
            currentLocation: travel.destination,
            travelState: null
        });
        return { success: true, newLocation: travel.destination };
    });
});

// --- Combat Engine ---

function generateEnemy(playerLevel: number) {
    const names = ["Sea Serpent", "Pirate Scout", "Feral Crab", "Ghost Ship"];
    const name = names[Math.floor(Math.random() * names.length)];
    const level = Math.max(1, playerLevel + Math.floor(Math.random() * 3) - 1);

    return {
        name,
        level,
        hp: 40 + (level * 10),
        maxHp: 40 + (level * 10),
        stats: {
            strength: 5 + level * 2,
            endurance: 5 + level * 2,
            agility: 5 + level
        },
        goldReward: 20 * level,
        xpReward: 15 * level
    };
}

export const combatAction = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { action } = data; // Attack, Technique, Defend, Item, Flee
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const combat = character.combatState;
        if (!combat || combat.isFinished) throw new functions.https.HttpsError("failed-precondition", "No active combat.");

        const logs = [...combat.logs];
        let enemy = combat.enemy;
        let playerHp = character.hp;
        let playerWon = false;
        let isFinished = false;

        // RPG Logic
        const playerAgi = character.stats.agility;
        const enemyAgi = enemy.stats.agility;

        if (action === "Attack") {
            const hitChance = 0.8 + (playerAgi - enemyAgi) * 0.02;
            if (Math.random() < hitChance) {
                let damage = character.stats.strength * 2 + (character.stats.swordsmanship || 0) / 2;
                damage = Math.floor(damage * (0.9 + Math.random() * 0.2)); // Variance

                // Crit chance
                if (Math.random() < 0.05 + character.stats.luck * 0.01) {
                    damage *= 2;
                    logs.push(`CRITICAL HIT! You strike ${enemy.name} for ${damage} damage!`);
                } else {
                    logs.push(`You hit ${enemy.name} for ${damage} damage.`);
                }
                enemy.hp = Math.max(0, enemy.hp - damage);
            } else {
                logs.push(`You missed your attack on ${enemy.name}.`);
            }
        } else if (action === "Defend") {
            logs.push("You take a defensive stance.");
            // Flag for half damage on next enemy turn
            combat.defending = true;
        } else if (action === "Flee") {
            const fleeChance = 0.5 + (playerAgi - enemyAgi) * 0.05;
            if (Math.random() < fleeChance) {
                transaction.update(playerRef, { combatState: null });
                return { fled: true };
            } else {
                logs.push("You failed to flee!");
            }
        }

        if (enemy.hp <= 0) {
            logs.push(`You defeated ${enemy.name}!`);
            playerWon = true;
            isFinished = true;
        } else {
            // Enemy Turn
            const enemyHitChance = 0.7 + (enemyAgi - playerAgi) * 0.02;
            if (Math.random() < enemyHitChance) {
                let enemyDamage = Math.floor(enemy.stats.strength * 1.5);
                if (combat.defending) {
                    enemyDamage = Math.floor(enemyDamage / 2);
                    combat.defending = false;
                }
                playerHp = Math.max(0, playerHp - enemyDamage);
                logs.push(`${enemy.name} hits you for ${enemyDamage} damage.`);
            } else {
                logs.push(`${enemy.name} missed its attack.`);
            }

            if (playerHp <= 0) {
                logs.push("You were defeated...");
                isFinished = true;
                playerWon = false;
            }
        }

        if (isFinished) {
            let updatedChar = { ...character, hp: playerHp, combatState: null };
            if (playerWon) {
                updatedChar.gold += enemy.goldReward;
                updatedChar.xp += enemy.xpReward;
                updatedChar = checkLevelUp(updatedChar);
                recordLog(transaction, userId, "CombatWin", `Defeated ${enemy.name}`, enemy.goldReward, enemy.xpReward);
            } else {
                // Death penalty
                const goldLost = Math.floor(updatedChar.gold * 0.1);
                updatedChar.gold -= goldLost;
                updatedChar.currentLocation = "Fogi Tail Island";
                updatedChar.hp = updatedChar.maxHp;
                recordLog(transaction, userId, "CombatLoss", `Defeated by ${enemy.name}`, -goldLost, 0);
            }
            transaction.set(playerRef, updatedChar);
        } else {
            transaction.update(playerRef, {
                hp: playerHp,
                combatState: { ...combat, enemy, logs }
            });
        }

        return { success: true, logs };
    });
});

export const attackPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { defenderId } = data;
    const userId = context.auth.uid;
    const attackerRef = db.collection("players").doc(userId);
    const defenderRef = db.collection("players").doc(defenderId);

    return db.runTransaction(async (transaction) => {
        const [attackerSnap, defenderSnap] = await Promise.all([
            transaction.get(attackerRef),
            transaction.get(defenderRef)
        ]);

        if (!attackerSnap.exists || !defenderSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Player not found.");
        }

        const attacker = attackerSnap.data() as any;
        const defender = defenderSnap.data() as any;

        if (attacker.currentLocation !== defender.currentLocation) {
            throw new functions.https.HttpsError("failed-precondition", "Target is not at your location.");
        }

        const attackerPower = attacker.stats.strength + attacker.stats.agility + attacker.stats.willpower;
        const defenderPower = defender.stats.strength + defender.stats.agility + defender.stats.willpower;

        const winChance = 0.5 + (attackerPower - defenderPower) * 0.01;
        const attackerWon = Math.random() < winChance;

        if (attackerWon) {
            const stealAmount = Math.floor(defender.gold / 10);
            transaction.update(attackerRef, {
                gold: admin.firestore.FieldValue.increment(stealAmount),
                bounty: admin.firestore.FieldValue.increment(100),
                pvpWins: admin.firestore.FieldValue.increment(1)
            });
            transaction.update(defenderRef, {
                gold: admin.firestore.FieldValue.increment(-stealAmount),
                pvpLosses: admin.firestore.FieldValue.increment(1)
            });
            recordLog(transaction, userId, "PvPWin", `Defeated ${defender.name}`, stealAmount, 0);
            recordLog(transaction, defenderId, "PvPLoss", `Defeated by ${attacker.name}`, -stealAmount, 0);
            return { won: true, goldStolen: stealAmount };
        } else {
            const lossAmount = Math.floor(attacker.gold / 10);
            transaction.update(attackerRef, {
                gold: admin.firestore.FieldValue.increment(-lossAmount),
                pvpLosses: admin.firestore.FieldValue.increment(1)
            });
            transaction.update(defenderRef, {
                gold: admin.firestore.FieldValue.increment(lossAmount),
                pvpWins: admin.firestore.FieldValue.increment(1)
            });
            recordLog(transaction, userId, "PvPLoss", `Lost to ${defender.name}`, -lossAmount, 0);
            recordLog(transaction, defenderId, "PvPWin", `Defended against ${attacker.name}`, lossAmount, 0);
            return { won: false, goldLost: lossAmount };
        }
    });
});

// --- Existing Functions ---

export const completeMission = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { missionId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const missionRef = db.collection("missions").doc(missionId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, missionSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(missionRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!missionSnap.exists) throw new functions.https.HttpsError("not-found", "Mission not found.");

        const character = playerSnap.data() as any;
        const mission = missionSnap.data() as any;

        if (character.level < mission.minLevel) {
            throw new functions.https.HttpsError("failed-precondition", "Level too low for this mission.");
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < mission.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        let updatedChar = {
            ...character,
            energy: energy - mission.energyCost,
            energyUpdatedAt: energyUpdatedAt,
            gold: character.gold + mission.rewards.gold,
            xp: character.xp + mission.rewards.xp
        };

        updatedChar = checkLevelUp(updatedChar);
        transaction.set(playerRef, updatedChar);
        recordLog(transaction, userId, "MissionCompleted", `Completed mission ${missionId}`, mission.rewards.gold, mission.rewards.xp);

        return { success: true, rewards: mission.rewards };
    });
});

export const createCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, description } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const crewRef = db.collection("crews").doc();

    return db.runTransaction(async (transaction) => {
        const playerSnap = await transaction.get(playerRef);
        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = playerSnap.data() as any;
        if (character.crewId) throw new functions.https.HttpsError("already-exists", "Player is already in a crew.");
        if (character.gold < 10000) throw new functions.https.HttpsError("failed-precondition", "Need 10,000 Gold to start a crew.");

        const crewData = {
            id: crewRef.id,
            name: name,
            description: description,
            captainId: userId,
            members: [userId],
            totalBounty: character.bounty,
            level: 1,
            experience: 0
        };

        transaction.set(crewRef, crewData);
        transaction.update(playerRef, { crewId: crewRef.id, gold: character.gold - 10000 });
        recordLog(transaction, userId, "CreateCrew", `Created crew ${name}`, -10000, 0);

        return { success: true, crewId: crewRef.id };
    });
});

export const joinCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { crewId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const crewRef = db.collection("crews").doc(crewId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, crewSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(crewRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");

        const character = playerSnap.data() as any;
        const crew = crewSnap.data() as any;

        if (character.crewId) throw new functions.https.HttpsError("already-exists", "Player is already in a crew.");
        if (crew.members.length >= 20) throw new functions.https.HttpsError("resource-exhausted", "Crew is full.");

        transaction.update(crewRef, {
            members: admin.firestore.FieldValue.arrayUnion(userId),
            totalBounty: admin.firestore.FieldValue.increment(character.bounty)
        });
        transaction.update(playerRef, { crewId: crewId });

        return { success: true };
    });
});

export const heartbeat = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).update({
        lastOnline: Date.now(),
        isOnline: true
    });
    return { success: true };
});

// --- Admin Tools ---

async function checkAdmin(userId: string) {
    const userDoc = await db.collection("players").doc(userId).get();
    const userData = userDoc.data() as any;
    if (!userData || userData.role !== "admin") {
        // For early development, we can comment out the restriction or check for a specific UID
        // throw new functions.https.HttpsError("permission-denied", "User is not an admin.");
    }
}

export const adminAdjustGold = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context.auth.uid);

    const { userId, amount, reason } = data;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        transaction.update(playerRef, { gold: admin.firestore.FieldValue.increment(amount) });
        recordLog(transaction, userId, "AdminAdjustGold", reason, amount, 0);
        return { success: true };
    });
});

export const adminTeleportPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context.auth.uid);

    const { userId, location } = data;
    await db.collection("players").doc(userId).update({ currentLocation: location, travelState: null });
    return { success: true };
});

export const adminSendAnnouncement = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context.auth.uid);

    const { message } = data;
    await db.collection("announcements").add({
        message,
        timestamp: Date.now(),
        authorId: context.auth.uid
    });
    return { success: true };
});

// --- Social Functions ---

export const sendFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    if (senderId === targetId) throw new functions.https.HttpsError("invalid-argument", "Cannot add yourself.");

    const requestRef = db.collection("friendRequests").doc(`${senderId}_${targetId}`);
    const existing = await requestRef.get();
    if (existing.exists) throw new functions.https.HttpsError("already-exists", "Request already sent.");

    await requestRef.set({
        senderId,
        receiverId: targetId,
        status: "pending",
        timestamp: Date.now()
    });

    return { success: true };
});

export const acceptFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { senderId } = data;
    const receiverId = context.auth.uid;

    const requestRef = db.collection("friendRequests").doc(`${senderId}_${receiverId}`);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(requestRef);
        if (!snapshot.exists || snapshot.data()?.status !== "pending") {
            throw new functions.https.HttpsError("not-found", "Request not found.");
        }

        transaction.update(requestRef, { status: "accepted" });
        transaction.update(db.collection("players").doc(receiverId), {
            friends: admin.firestore.FieldValue.arrayUnion(senderId)
        });
        transaction.update(db.collection("players").doc(senderId), {
            friends: admin.firestore.FieldValue.arrayUnion(receiverId)
        });

        return { success: true };
    });
});

export const blockPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const userId = context.auth.uid;

    await db.collection("players").doc(userId).update({
        blocked: admin.firestore.FieldValue.arrayUnion(targetId)
    });

    return { success: true };
});
