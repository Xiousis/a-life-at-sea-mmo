import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// --- Constants & Config ---
const ENERGY_REGEN_RATE_MS = 3 * 60 * 1000; // 1 energy per 3 minutes
const MAX_ENERGY = 100;
const INVENTORY_CAPACITY = 20;
const TURN_TIMEOUT_MS = 60 * 1000; // 1 minute per turn
const HEALING_DURATION_MS = 2 * 60 * 1000; // 2 minutes
const TRAINING_DURATION_MS = 20 * 1000; // 20 seconds
const TRAINING_GOLD_COST = 50;

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
    "Sniper": "sniper",
    "MysticArts": "mysticArts",
    "Cooking": "cooking",
    "Navigating": "navigating",
    "TreasureHunting": "treasureHunting",
    "Blacksmith": "blacksmith",
    "Fishing": "fishing"
};

const LOCATION_DATA: Record<string, { x: number, y: number, region: string }> = {
    "Fogi Tail Island": { x: 0, y: 0, region: "East Blue" },
    "Ironcrest Isle": { x: 50, y: 20, region: "East Blue" },
    "Amber Reach": { x: -30, y: 40, region: "East Blue" },
    "Sunken Reef": { x: 10, y: 15, region: "East Blue" },
    "Tortuga Bay": { x: 10, y: -100, region: "South Blue" },
    "Pirate\u0027s Den": { x: 350, y: -350, region: "South Blue" },
    "Navy Outpost Aqua": { x: -80, y: -50, region: "South Blue" },
    "Navy Outpost Terra": { x: -300, y: 200, region: "Grand Line" },
    "Navy Outpost Ignis": { x: 400, y: 300, region: "Grand Line" },
    "Crystal Cove": { x: 120, y: 80, region: "Grand Line" },
    "Volcano Peak": { x: 200, y: 150, region: "Grand Line" },
    "Whispering Woods": { x: -150, y: 180, region: "Grand Line" },
};

function calculateTravelTime(from: string, to: string, speedMultiplier: number = 1.0): number {
    const start = LOCATION_DATA[from] || { x: 0, y: 0, region: "Unknown" };
    const end = LOCATION_DATA[to] || { x: 0, y: 0, region: "Unknown" };

    const dist = Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
    let baseTime = dist * 2000; // 1 unit = 2 seconds

    if (start.region !== end.region) {
        baseTime += 60000; // Extra minute for inter-region travel
    }

    return Math.max(10000, Math.floor(baseTime / speedMultiplier)); // Min 10 seconds
}

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

export function calculateCurrentEnergy(character: any): { energy: number, energyUpdatedAt: number } {
    const now = Date.now();
    const elapsed = now - character.energyUpdatedAt;
    const regenerated = Math.floor(elapsed / ENERGY_REGEN_RATE_MS);

    const currentMaxEnergy = character.maxEnergy || MAX_ENERGY;
    if (regenerated <= 0) return { energy: character.energy, energyUpdatedAt: character.energyUpdatedAt };

    const newEnergy = Math.min(currentMaxEnergy, character.energy + regenerated);
    const newTimestamp = character.energy + regenerated >= currentMaxEnergy ? now : character.energyUpdatedAt + (regenerated * ENERGY_REGEN_RATE_MS);

    return { energy: newEnergy, energyUpdatedAt: newTimestamp };
}

function checkLevelUp(character: any) {
    let { level, xp, stats, maxEnergy, energy, maxHp, hp } = character;
    let xpNeeded = level * 100;

    let leveledUp = false;
    while (xp >= xpNeeded) {
        level++;
        xp -= xpNeeded;
        maxEnergy += 100;
        energy = maxEnergy;
        stats.endurance += 1;
        maxHp += 100;
        hp = maxHp;

        stats.strength += 1;
        stats.agility += 1;
        stats.perception += 1;
        stats.willpower += 1;
        stats.luck += 1;

        xpNeeded = level * 100;
        leveledUp = true;
    }

    return { ...character, level, xp, stats, maxEnergy, energy, maxHp, hp, leveledUp };
}

function processHealing(character: any): any {
    if (character.healingState && character.healingState.endTime <= Date.now()) {
        return {
            ...character,
            hp: character.maxHp,
            healingState: null
        };
    }
    return character;
}

// --- Player Management ---

export const createCharacter = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, gender, race } = data;
    const userId = context.auth.uid;

    // Validate if character already exists for this user
    const playerDoc = await db.collection("players").doc(userId).get();
    if (playerDoc.exists) {
        throw new functions.https.HttpsError("already-exists", "You already have a character.");
    }

    // Name Validation
    const trimmedName = (name || "").trim();
    if (trimmedName.length < 3 || trimmedName.length > 16) {
        throw new functions.https.HttpsError("invalid-argument", "Name must be between 3 and 16 characters.");
    }
    if (!/^[a-zA-Z0-9_]+$/.test(trimmedName)) {
        throw new functions.https.HttpsError("invalid-argument", "Name contains invalid characters.");
    }

    const reservedNames = ["admin", "system", "moderator", "game-master", "gm"];
    if (reservedNames.includes(trimmedName.toLowerCase())) {
        throw new functions.https.HttpsError("invalid-argument", "This name is reserved.");
    }

    // Case-insensitive uniqueness check
    const nameLower = trimmedName.toLowerCase();
    const nameQuery = await db.collection("players").where("nameLower", "==", nameLower).get();
    if (!nameQuery.empty) {
        throw new functions.https.HttpsError("already-exists", "Character name is already taken.");
    }

    // Race/Gender Validation
    const allowedRaces = ["Human", "Abyssal", "Beastkin", "Celestian", "Automaton"];
    const allowedGenders = ["Male", "Female", "Other"];
    if (!allowedRaces.includes(race)) throw new functions.https.HttpsError("invalid-argument", "Invalid race.");
    if (!allowedGenders.includes(gender)) throw new functions.https.HttpsError("invalid-argument", "Invalid gender.");

    const character = {
        id: userId,
        name: trimmedName,
        nameLower: nameLower,
        gender: gender,
        race: race,
        level: 1,
        xp: 0,
        gold: 100,
        bounty: 0,
        infamy: 0,
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
        faction: "Neutral",
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
            sniper: 0,
            mysticArts: 0
        },
        professionStats: {
            cooking: 0,
            navigating: 0,
            treasureHunting: 0,
            blacksmith: 0,
            fishing: 0
        },
        inventory: [],
        equipment: {},
        travelState: null,
        combatState: null,
        learnedTechniques: ["bash"],
        healingState: null,
        ship: { id: "row_boat", name: "Row Boat", price: 0, speedMultiplier: 1.0 }
    };

    await db.collection("players").doc(userId).set(character);
    return { success: true };
});

export const joinFaction = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { faction } = data; // Navy, Pirate
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        if (character.faction !== "Neutral") {
            throw new functions.https.HttpsError("failed-precondition", "You already belong to a faction.");
        }

        if (faction === "Pirate") {
            if (character.currentLocation !== "Pirate\u0027s Den") {
                throw new functions.https.HttpsError("failed-precondition", "You must be at the Pirate\u0027s Den to join the Pirates.");
            }
            transaction.update(playerRef, { faction: "Pirate", title: "Rogue Sailor" });
            recordLog(transaction, userId, "JoinFaction", "Became a Pirate", 0, 0);
        } else if (faction === "Navy") {
            if (character.currentLocation !== "Navy Outpost Aqua") {
                throw new functions.https.HttpsError("failed-precondition", "You must be at the Navy Outpost Aqua to enlist in the Navy.");
            }
            transaction.update(playerRef, { faction: "Navy", title: "Navy Recruit" });
            recordLog(transaction, userId, "JoinFaction", "Enlisted in the Navy", 0, 0);
        } else {
            throw new functions.https.HttpsError("invalid-argument", "Invalid faction choice.");
        }

        return { success: true, faction };
    });
});

export const train = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { statType } = data;
    // Stronger mapping validation to prevent incorrect fields like 'martialarts'
    const mappedStat = STAT_MAPPING[statType];
    if (!mappedStat) {
        throw new functions.https.HttpsError("invalid-argument", `Invalid stat type: ${statType}. Allowed: ${Object.keys(STAT_MAPPING).join(", ")}`);
    }

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to train. Visit an infirmary.");
        }

        if (character.trainingState) {
             throw new functions.https.HttpsError("failed-precondition", "You are already training.");
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);

        if (energy < 10) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");
        if (character.gold < TRAINING_GOLD_COST) throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");

        const endTime = Date.now() + TRAINING_DURATION_MS;

        transaction.update(playerRef, {
            energy: energy - 10,
            energyUpdatedAt,
            gold: admin.firestore.FieldValue.increment(-TRAINING_GOLD_COST),
            trainingState: { endTime, statType }
        });

        recordLog(transaction, userId, "TrainStart", `Started training ${statType}`, -TRAINING_GOLD_COST, 0);

        return { success: true, endTime };
    });
});

export const finishTraining = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        const training = character.trainingState;
        if (!training) throw new functions.https.HttpsError("failed-precondition", "No active training session.");
        if (training.endTime > Date.now()) throw new functions.https.HttpsError("failed-precondition", "Training not yet complete.");

        const statType = training.statType;
        const mappedStat = STAT_MAPPING[statType];

        // Ensure stats object exists
        const stats = { ...character.stats };
        const pStats = { ...(character.professionStats || {}) };

        if (stats[mappedStat] !== undefined) {
            stats[mappedStat] = (stats[mappedStat] || 0) + 1;
        } else if (pStats[mappedStat] !== undefined) {
            pStats[mappedStat] = (pStats[mappedStat] || 0) + 1;
        } else {
            // Fallback for new stats
            const combatStats = ["swordsmanship", "brawling", "gunslinging", "spear", "martialArts", "sniper", "mysticArts"];
            if (combatStats.includes(mappedStat)) {
                stats[mappedStat] = (stats[mappedStat] || 0) + 1;
            } else {
                pStats[mappedStat] = (pStats[mappedStat] || 0) + 1;
            }
        }

        const updatedChar = { ...character, xp: character.xp + 5, stats, professionStats: pStats, trainingState: null };
        const finalChar = checkLevelUp(updatedChar);

        transaction.set(playerRef, finalChar);
        recordLog(transaction, userId, "TrainFinish", `Finished training ${statType}`, 0, 5);

        return { success: true };
    });
});

// --- Gameplay Mechanics ---

export const startTravel = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { destination } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to travel. Visit an infirmary.");
        }

        if (character.healingState) {
            throw new functions.https.HttpsError("failed-precondition", "You cannot travel while resting in the infirmary.");
        }

        if (character.travelState || character.combatState || character.trainingState) {
            throw new functions.https.HttpsError("failed-precondition", "Player is already busy.");
        }

        if (character.currentLocation === destination) {
            throw new functions.https.HttpsError("invalid-argument", "Already at destination.");
        }

        if (!LOCATION_DATA[destination]) {
            throw new functions.https.HttpsError("invalid-argument", "Invalid destination.");
        }

        const speedMultiplier = character.ship?.speedMultiplier || 1.0;
        const travelDuration = calculateTravelTime(character.currentLocation, destination, speedMultiplier);
        const arrivalTime = Date.now() + travelDuration;

        // Potential for random encounter here (Pirates, Monsters)
        if (travelDuration > 10000 && Math.random() < 0.25) {
            const enemy = generateEnemy(character.level);

            transaction.update(playerRef, {
                combatState: {
                    enemy: enemy,
                    playerTurn: true,
                    logs: [`While sailing to ${destination}, you encountered a ${enemy.name}!`],
                    isFinished: false,
                    playerWon: false,
                    turnCount: 0,
                    intendedDestination: destination,
                    intendedArrivalTime: arrivalTime,
                    intendedStartTime: Date.now()
                }
            });
            return { ambush: true, enemy };
        }

        transaction.update(playerRef, {
            travelState: { destination, arrivalTime, startTime: Date.now() }
        });
        return { success: true, arrivalTime, travelDuration };
    });
});

export const finishTravel = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

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

export const finishHealing = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        if (!character.healingState) throw new functions.https.HttpsError("failed-precondition", "No active healing state.");
        if (character.healingState.endTime > Date.now()) {
            throw new functions.https.HttpsError("failed-precondition", "Healing not yet complete.");
        }

        transaction.update(playerRef, {
            hp: character.maxHp,
            healingState: null
        });
        return { success: true };
    });
});

export const purchaseShip = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { shipId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    const SHIPS: Record<string, { name: string, price: number, speedMultiplier: number }> = {
        "row_boat": { name: "Row Boat", price: 0, speedMultiplier: 1.0 },
        "sloop": { name: "Sloop", price: 500, speedMultiplier: 1.5 },
        "caravel": { name: "Caravel", price: 2500, speedMultiplier: 2.0 },
        "galleon": { name: "Galleon", price: 10000, speedMultiplier: 3.0 }
    };

    const ship = SHIPS[shipId];
    if (!ship) throw new functions.https.HttpsError("invalid-argument", "Invalid ship ID.");

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        // Validate location has a shipyard
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasShipyard = location?.actions?.some((a: any) => a.type === "Shipyard");

        if (!hasShipyard) {
            throw new functions.https.HttpsError("failed-precondition", "There is no shipyard at your current location.");
        }

        if (character.gold < ship.price) throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");
        if (character.ship?.id === shipId) throw new functions.https.HttpsError("failed-precondition", "You already own this ship.");

        transaction.update(playerRef, {
            gold: character.gold - ship.price,
            ship: { id: shipId, ...ship }
        });

        recordLog(transaction, userId, "PurchaseShip", `Purchased ${ship.name}`, -ship.price, 0);
        return { success: true };
    });
});

// --- Combat Engine ---

interface CombatStats {
    hp: number;
    maxHp: number;
    strength: number;
    endurance: number;
    agility: number;
    perception: number;
    willpower: number;
    luck: number;
    swordsmanship?: number;
    brawling?: number;
    gunslinging?: number;
    spear?: number;
    martialArts?: number;
    sniper?: number;
    mysticArts?: number;
    defense: number;
    accuracy: number;
    dodge: number;
    critChance: number;
}

function calculateCombatStats(charOrEnemy: any, currentEffects: any[] = []): CombatStats {
    const stats = { ...(charOrEnemy.stats || {}) };
    const level = charOrEnemy.level || 1;

    // Apply equipment bonuses
    if (charOrEnemy.equipment) {
        for (const slot in charOrEnemy.equipment) {
            const item = charOrEnemy.equipment[slot];
            if (item && item.statBonus) {
                for (const stat in item.statBonus) {
                    if (stats[stat] !== undefined) {
                        stats[stat] += item.statBonus[stat];
                    }
                }
            }
        }
    }

    // Base stats
    let strength = stats.strength || 5;
    let endurance = stats.endurance || 5;
    let agility = stats.agility || 5;
    let perception = stats.perception || 5;
    let luck = stats.luck || 5;
    let willpower = stats.willpower || 5;

    // Derived stats
    let defense = Math.floor(endurance * 1.5 + level);
    let accuracy = 80 + agility * 0.5 + perception * 0.5;
    let dodge = agility * 0.8 + luck * 0.2;
    let critChance = 5 + luck * 0.5 + perception * 0.2;

    // Apply effects
    if (currentEffects.some(e => e.type === "Haste")) {
        dodge += 10;
        accuracy += 10;
    }

    return {
        hp: charOrEnemy.hp,
        maxHp: charOrEnemy.maxHp,
        strength,
        endurance,
        agility,
        perception,
        willpower,
        luck,
        swordsmanship: stats.swordsmanship || 0,
        brawling: stats.brawling || 0,
        gunslinging: stats.gunslinging || 0,
        spear: stats.spear || 0,
        martialArts: stats.martialArts || 0,
        sniper: stats.sniper || 0,
        mysticArts: stats.mysticArts || 0,
        defense,
        accuracy,
        dodge,
        critChance
    };
}

function generateEnemy(playerLevel: number) {
    const seaMobs = [
        { name: "Sea Serpent", minLevel: 1, maxLevel: 10, dropTableId: "basic_sea_loot" },
        { name: "Pirate Scout", minLevel: 3, maxLevel: 15, dropTableId: "basic_sea_loot" },
        { name: "Giant Squid", minLevel: 10, maxLevel: 25, dropTableId: "rare_sea_loot" },
        { name: "Ghost Pirate", minLevel: 15, maxLevel: 40, dropTableId: "rare_sea_loot" },
        { name: "Feral Crab", minLevel: 1, maxLevel: 5, dropTableId: "basic_sea_loot" },
        { name: "Rogue Sloop", minLevel: 5, maxLevel: 20, dropTableId: "basic_sea_loot" },
        { name: "Navy Enforcer", minLevel: 8, maxLevel: 30, dropTableId: "rare_sea_loot" }
    ];

    // Filter mobs by player level
    const possibleMobs = seaMobs.filter(m => playerLevel >= m.minLevel - 2); // Allow slightly higher level mobs
    const mobDef = possibleMobs.length > 0
        ? possibleMobs[Math.floor(Math.random() * possibleMobs.length)]
        : seaMobs[0];

    const level = Math.max(1, Math.min(mobDef.maxLevel, playerLevel + Math.floor(Math.random() * 3) - 1));

    const stats = {
        strength: 5 + level * 2,
        endurance: 5 + level * 2,
        agility: 5 + level,
        perception: 5 + level,
        willpower: 5 + level,
        luck: 5 + level / 2
    };

    const maxHp = 40 + (level * 15);

    return {
        name: mobDef.name,
        level,
        hp: maxHp,
        maxHp: maxHp,
        stats,
        goldReward: 20 * level,
        xpReward: 15 * level,
        dropTableId: mobDef.dropTableId
    };
}

async function processLoot(dropTableId: string): Promise<any[]> {
    if (!dropTableId) return [];

    const lootTableSnap = await db.collection("gameData").doc("world").collection("lootTables").doc(dropTableId).get();
    if (!lootTableSnap.exists) return [];

    const lootTable = lootTableSnap.data();
    const droppedItems: any[] = [];

    for (const entry of (lootTable?.entries || [])) {
        if (Math.random() <= entry.chance) {
            const itemSnap = await db.collection("gameData").doc("items").collection("all").doc(entry.itemId).get();
            if (itemSnap.exists) {
                const item = itemSnap.data();
                if (item) {
                    const amount = Math.floor(Math.random() * ((entry.maxAmount || 1) - (entry.minAmount || 1) + 1)) + (entry.minAmount || 1);
                    for (let i = 0; i < amount; i++) {
                        droppedItems.push({ ...item, id: `${item.id}_${Date.now()}_${Math.floor(Math.random() * 10000)}` });
                    }
                }
            }
        }
    }

    return droppedItems;
}

function processStatusEffects(character: any, effects: any[], logs: string[]): { character: any, activeEffects: any[] } {
    let updatedChar = { ...character };
    const activeEffects: any[] = [];

    for (const effect of effects) {
        if (effect.duration <= 0) continue;

        switch (effect.type) {
            case "Bleed":
            case "Burn":
                const damage = effect.magnitude || 5;
                updatedChar.hp = Math.max(0, updatedChar.hp - damage);
                logs.push(`${updatedChar.name || "You"} took ${damage} damage from ${effect.type}.`);
                break;
        }

        if (effect.duration > 1) {
            activeEffects.push({ ...effect, duration: effect.duration - 1 });
        }
    }

    return { character: updatedChar, activeEffects };
}

function calculateDamage(attackerStats: CombatStats, defenderStats: CombatStats, attackerEffects: any[], defenderEffects: any[], isCrit: boolean): number {
    let damage = attackerStats.strength * 2 + (attackerStats.swordsmanship || 0) * 1.5;

    // Apply Weaken effect
    if (attackerEffects.some(e => e.type === "Weaken")) {
        damage *= 0.7;
    }

    let defense = defenderStats.defense;
    // Apply Fortify effect
    if (defenderEffects.some(e => e.type === "Fortify")) {
        defense *= 1.5;
    }

    damage = Math.max(1, Math.floor(damage - defense * 0.5));
    damage = Math.floor(damage * (0.9 + Math.random() * 0.2));

    if (isCrit) {
        damage = Math.floor(damage * 2);
    }

    return damage;
}

export const combatAction = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { action, techniqueId, itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        let combat = character.combatState;
        if (!combat || combat.isFinished) throw new functions.https.HttpsError("failed-precondition", "No active combat.");

        // Turn Timeout Check
        const now = Date.now();
        if (combat.turnExpiresAt && now > combat.turnExpiresAt) {
            combat.logs.push(`${combat.playerTurn ? "You" : "Opponent"} took too long! Forfeiting turn.`);
            combat.playerTurn = !combat.playerTurn;
            combat.turnExpiresAt = now + TURN_TIMEOUT_MS;
            transaction.update(playerRef, { combatState: combat });
            return { success: true, timeout: true };
        }

        if (!combat.playerTurn) throw new functions.https.HttpsError("failed-precondition", "Not your turn.");

        const logs = [...(combat.logs || [])];
        let enemy = combat.enemy;
        let opponent: any = null;
        let opponentRef: admin.firestore.DocumentReference | null = null;

        if (combat.isPvP) {
            opponentRef = db.collection("players").doc(combat.opponentId);
            const opponentSnap = await transaction.get(opponentRef);
            if (!opponentSnap.exists) throw new functions.https.HttpsError("not-found", "Opponent not found.");
            opponent = opponentSnap.data();
        }

        let playerHp = character.hp;
        let playerEnergy = character.energy;
        let isFinished = false;
        let playerWon = false;

        // Process Player Effects at start of turn
        const pEffectResult = processStatusEffects({ ...character, hp: playerHp, name: "You" }, combat.playerEffects || [], logs);
        playerHp = pEffectResult.character.hp;
        let pActiveEffects = pEffectResult.activeEffects;

        if (playerHp <= 0) {
            isFinished = true;
            playerWon = false;
        }

        if (!isFinished) {
            const pStats = calculateCombatStats(character, pActiveEffects);
            const targetStats = combat.isPvP ? calculateCombatStats(opponent, opponent.combatState?.playerEffects || []) : calculateCombatStats(enemy, combat.enemyEffects || []);
            const targetEffects = combat.isPvP ? (opponent.combatState?.playerEffects || []) : (combat.enemyEffects || []);

            const isStunned = (pActiveEffects || []).some((e: any) => e.type === "Stun");

            if (!isStunned) {
                // --- Player Action ---
                if (action === "Attack") {
                    const hitRoll = Math.random() * 100;
                    const hitChance = pStats.accuracy - targetStats.dodge;

                    if (hitRoll < hitChance) {
                        const isCrit = Math.random() * 100 < pStats.critChance;
                        const damage = calculateDamage(pStats, targetStats, pActiveEffects, targetEffects, isCrit);

                        if (isCrit) logs.push(`CRITICAL! You strike for ${damage} damage!`);
                        else logs.push(`You hit for ${damage} damage.`);

                        if (combat.isPvP) opponent.hp = Math.max(0, opponent.hp - damage);
                        else enemy.hp = Math.max(0, enemy.hp - damage);
                    } else {
                        logs.push("You missed your attack.");
                    }
                } else if (action === "Technique") {
                    if (!techniqueId) throw new functions.https.HttpsError("invalid-argument", "Missing technique ID.");

                    // Technique validation
                    if (!character.learnedTechniques || !character.learnedTechniques.includes(techniqueId)) {
                        throw new functions.https.HttpsError("failed-precondition", "You have not learned this technique.");
                    }

                    const techSnap = await db.collection("gameData").doc("skills").collection("techniques").doc(techniqueId).get();
                    if (!techSnap.exists) throw new functions.https.HttpsError("not-found", "Technique not found.");

                    const tech = techSnap.data() as any;
                    if (playerEnergy < tech.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");
                    if ((combat.cooldowns || {})[techniqueId] > 0) throw new functions.https.HttpsError("failed-precondition", "Technique on cooldown.");

                    playerEnergy -= tech.energyCost;
                    const cooldowns = { ...(combat.cooldowns || {}) };
                    cooldowns[techniqueId] = tech.cooldown;
                    combat.cooldowns = cooldowns;

                    let techDamage = Math.floor(pStats.strength * tech.power * 2);
                    techDamage = Math.max(1, Math.floor(techDamage - targetStats.defense * 0.3));

                    if (combat.isPvP) {
                        opponent.hp = Math.max(0, opponent.hp - techDamage);
                        if (tech.effects) {
                            opponent.combatState.playerEffects = [...(opponent.combatState.playerEffects || []), ...tech.effects];
                        }
                    } else {
                        enemy.hp = Math.max(0, enemy.hp - techDamage);
                        if (tech.effects) {
                            combat.enemyEffects = [...(combat.enemyEffects || []), ...tech.effects];
                        }
                    }
                    logs.push(`You use ${tech.name}! Target takes ${techDamage} damage.`);
                } else if (action === "Defend") {
                    logs.push("You take a defensive stance.");
                    combat.defending = true;
                } else if (action === "Item") {
                    if (!itemId) throw new functions.https.HttpsError("invalid-argument", "Missing item ID.");
                    const inventory = character.inventory || [];
                    const itemIndex = inventory.findIndex((i: any) => i.id === itemId);
                    if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

                    const item = inventory[itemIndex];
                    if (item.type !== "Consumable") throw new functions.https.HttpsError("invalid-argument", "Item is not consumable.");

                    // Healing logic
                    const healAmount = item.healAmount || 30;
                    playerHp = Math.min(character.maxHp, playerHp + healAmount);
                    logs.push(`You used ${item.name} and recovered ${healAmount} HP.`);

                    // Remove item from inventory
                    character.inventory.splice(itemIndex, 1);
                } else if (action === "Flee") {
                    if (combat.isPvP) throw new functions.https.HttpsError("failed-precondition", "Cannot flee from a duel.");
                    const fleeChance = 40 + (pStats.agility - targetStats.agility) * 2;
                    if (Math.random() * 100 < fleeChance) {
                        transaction.update(playerRef, { combatState: null });
                        return { fled: true };
                    } else {
                        logs.push("You failed to flee!");
                    }
                }
            } else {
                logs.push("You are stunned and cannot move!");
            }

            // Check if win/loss
            const targetHp = combat.isPvP ? opponent.hp : enemy.hp;
            if (targetHp <= 0) {
                logs.push(combat.isPvP ? `You defeated ${opponent.name}!` : `You defeated ${enemy.name}!`);
                playerWon = true;
                isFinished = true;
            } else if (!combat.isPvP) {
                // --- PvE Enemy Turn ---
                const eStats = calculateCombatStats(enemy, combat.enemyEffects || []);
                const eEffectResult = processStatusEffects({ ...enemy }, combat.enemyEffects || [], logs);
                enemy = eEffectResult.character;
                let eActiveEffects = eEffectResult.activeEffects;
                combat.enemyEffects = eActiveEffects;

                if (enemy.hp <= 0) {
                    logs.push(`The ${enemy.name} succumbed to its wounds!`);
                    playerWon = true;
                    isFinished = true;
                } else {
                    const eIsStunned = (eActiveEffects || []).some((e: any) => e.type === "Stun");
                    if (!eIsStunned) {
                        // Enemy Ability Logic
                        const useAbility = Math.random() < 0.3;
                        if (useAbility) {
                            const abilityDamage = Math.floor(eStats.strength * 2.5);
                            playerHp = Math.max(0, playerHp - abilityDamage);
                            logs.push(`${enemy.name} uses a special ability and strikes you for ${abilityDamage} damage!`);
                        } else {
                            const eHitRoll = Math.random() * 100;
                            const eHitChance = eStats.accuracy - pStats.dodge;
                            if (eHitRoll < eHitChance) {
                                let eDamage = calculateDamage(eStats, pStats, eActiveEffects, pActiveEffects, false);
                                if (combat.defending) {
                                    eDamage = Math.floor(eDamage * 0.5);
                                    combat.defending = false;
                                }
                                playerHp = Math.max(0, playerHp - eDamage);
                                logs.push(`${enemy.name} hits you for ${eDamage} damage.`);
                            } else {
                                logs.push(`${enemy.name} missed its attack.`);
                            }
                        }
                    } else {
                        logs.push(`${enemy.name} is stunned!`);
                    }

                    if (playerHp <= 0) {
                        logs.push("You were defeated...");
                        isFinished = true;
                        playerWon = false;
                    }
                }
            }
        }

        // Update Cooldowns
        const updatedCooldowns: any = {};
        for (const [key, val] of Object.entries(combat.cooldowns || {})) {
            if ((val as number) > 1) updatedCooldowns[key] = (val as number) - 1;
        }

        if (isFinished) {
            if (combat.isPvP) {
                // PvP Finish Logic
                const winner = playerWon ? character : opponent;
                const loser = playerWon ? opponent : character;
                const winnerRef = playerWon ? playerRef : opponentRef!;
                const loserRef = playerWon ? opponentRef! : playerRef;

                const stealAmount = Math.floor(loser.gold * 0.15);
                const collectedBounty = loser.bounty || 0;
                const totalGoldGained = stealAmount + collectedBounty;

                // Update Winner
                const winnerUpdate: any = {
                    gold: winner.gold + totalGoldGained,
                    pvpWins: (winner.pvpWins || 0) + 1,
                    combatState: null,
                    hp: playerWon ? playerHp : winner.hp,
                    energy: playerWon ? playerEnergy : winner.energy
                };

                // Only Pirates increase their bounty through victory
                if (winner.faction === "Pirate") {
                    winnerUpdate.bounty = (winner.bounty || 0) + 100;
                }

                if (playerWon) winnerUpdate.inventory = character.inventory;
                transaction.update(winnerRef, winnerUpdate);

                // Update Winner's Crew Bounty
                if (winner.crewId && winner.faction === "Pirate") {
                    transaction.update(db.collection("crews").doc(winner.crewId), {
                        totalBounty: admin.firestore.FieldValue.increment(100)
                    });
                }

                // Update Loser
                const loserUpdate: any = {
                    gold: loser.gold - stealAmount,
                    bounty: 0, // Bounty is collected, reset it
                    pvpLosses: (loser.pvpLosses || 0) + 1,
                    combatState: null,
                    hp: playerWon ? 0 : playerHp, // Ensure loser ends with 0 HP if playerWon, otherwise take attacker's HP
                    energy: playerWon ? loser.energy : playerEnergy,
                    currentLocation: "Fogi Tail Island" // Respawn
                };
                if (!playerWon) loserUpdate.inventory = character.inventory;
                transaction.update(loserRef, loserUpdate);

                recordLog(transaction, winner.id, "PvPWin", `Defeated ${loser.name} and collected ${collectedBounty}B bounty`, totalGoldGained, 0);
                recordLog(transaction, loser.id, "PvPLoss", `Lost to ${winner.name}`, -stealAmount, 0);

                return { success: true, isFinished: true, playerWon, bountyCollected: collectedBounty };
            } else {
                // PvE Finish Logic
                let updatedChar = { ...character, hp: playerHp, energy: playerEnergy, combatState: null };
                const loot = playerWon && enemy.dropTableId ? await processLoot(enemy.dropTableId) : [];

                if (playerWon) {
                    updatedChar.gold += enemy.goldReward;
                    updatedChar.xp += enemy.xpReward;
                    if (loot.length > 0) {
                        const currentInv = updatedChar.inventory || [];
                        const freeSlots = INVENTORY_CAPACITY - currentInv.length;
                        const lootToAdd = loot.slice(0, Math.max(0, freeSlots));

                        updatedChar.inventory = [...currentInv, ...lootToAdd];
                        if (lootToAdd.length > 0) {
                            logs.push(`Loot found: ${lootToAdd.map((i: any) => i.name).join(", ")}`);
                        }
                        if (lootToAdd.length < loot.length) {
                            logs.push("Some loot was lost because your inventory is full!");
                        }
                    }
                    updatedChar = checkLevelUp(updatedChar);

                    // Ambush Restoration
                    if (combat.intendedDestination) {
                        updatedChar.travelState = {
                            destination: combat.intendedDestination,
                            arrivalTime: combat.intendedArrivalTime,
                            startTime: combat.intendedStartTime
                        };
                        logs.push(`You defeated the ambush and continue your journey to ${combat.intendedDestination}.`);
                    }

                    recordLog(transaction, userId, "CombatWin", `Defeated ${enemy.name}`, enemy.goldReward, enemy.xpReward);
                } else {
                    const goldLost = Math.floor(updatedChar.gold * 0.1);
                    updatedChar.gold -= goldLost;
                    updatedChar.currentLocation = "Fogi Tail Island";
                    updatedChar.hp = 0; // Set to 0 HP on defeat
                    updatedChar.energy = character.maxEnergy;
                    recordLog(transaction, userId, "CombatLoss", `Defeated by ${enemy.name}`, -goldLost, 0);
                }
                transaction.set(playerRef, updatedChar);
                return { success: true, isFinished: true, playerWon, logs };
            }
        } else {
            // Update combat state and swap turns if PvP
            if (combat.isPvP) {
                const nextLogs = [...logs, `It is now ${opponent.name}'s turn.`];
                const updatedEnemy = { ...combat.enemy, hp: opponent.hp };

                transaction.update(playerRef, {
                    hp: playerHp,
                    energy: playerEnergy,
                    inventory: character.inventory,
                    combatState: { ...combat, enemy: updatedEnemy, playerTurn: false, logs: nextLogs, cooldowns: updatedCooldowns, playerEffects: pActiveEffects, turnExpiresAt: now + TURN_TIMEOUT_MS }
                });

                const opponentCombat = {
                    ...opponent.combatState,
                    enemy: { ...opponent.combatState.enemy, hp: playerHp }, // Sync attacker's HP to defender
                    playerTurn: true,
                    logs: nextLogs,
                    turnExpiresAt: now + TURN_TIMEOUT_MS
                };
                transaction.update(opponentRef!, {
                    hp: opponent.hp,
                    combatState: opponentCombat
                });
            } else {
                transaction.update(playerRef, {
                    hp: playerHp,
                    energy: playerEnergy,
                    inventory: character.inventory,
                    combatState: { ...combat, enemy, logs, turnCount: (combat.turnCount || 0) + 1, cooldowns: updatedCooldowns, playerEffects: pActiveEffects }
                });
            }
            return { success: true, logs };
        }
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

        let attacker = attackerSnap.data() as any;
        attacker = processHealing(attacker);

        if (attacker.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to fight. Visit an infirmary.");
        }

        const defender = defenderSnap.data() as any;

        if (userId === defenderId) throw new functions.https.HttpsError("invalid-argument", "You cannot attack yourself.");

        // Block list check
        if (defender.blocked && defender.blocked.includes(userId)) {
            throw new functions.https.HttpsError("permission-denied", "You have been blocked by this player.");
        }

        if (!defender.isOnline) throw new functions.https.HttpsError("failed-precondition", "Target is currently offline.");

        if (attacker.currentLocation !== defender.currentLocation) {
            throw new functions.https.HttpsError("failed-precondition", "Target is not at your location.");
        }

        // Safe zone check
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(attacker.currentLocation));
        if (locationSnap.exists && locationSnap.data()?.isSafe) {
            throw new functions.https.HttpsError("failed-precondition", "PvP is not allowed in safe zones.");
        }

        if (attacker.combatState || attacker.travelState) throw new functions.https.HttpsError("failed-precondition", "You are already busy.");
        if (defender.combatState || defender.travelState) throw new functions.https.HttpsError("failed-precondition", "Target is already busy.");

        // Faction-based attack rules and infamy
        let infamyGain = 0;
        let kickFromFaction = false;
        let changeToPirate = false;

        if (attacker.faction === "Navy") {
            if (defender.faction === "Neutral") {
                infamyGain = 10;
            } else if (defender.faction === "Navy") {
                throw new functions.https.HttpsError("failed-precondition", "You cannot attack fellow Navy members.");
            }
            // Navy vs Pirate is fine
        } else if (attacker.faction === "Neutral") {
            if (defender.faction === "Navy") {
                infamyGain = 10;
            }
            // Neutral vs Pirate is fine, Neutral vs Neutral is fine (Pirates can attack anyone)
        }
        // Pirate vs anyone is fine

        if (infamyGain > 0) {
            attacker.infamy = (attacker.infamy || 0) + infamyGain;
            if (attacker.infamy >= 100) {
                attacker.infamy = 0;
                if (attacker.faction === "Navy") {
                    attacker.faction = "Neutral";
                    attacker.title = "Dishonored Sailor";
                } else if (attacker.faction === "Neutral") {
                    attacker.faction = "Pirate";
                    attacker.title = "Outlaw";
                }
            }
        }

        const attackerCombat = {
            opponentId: defenderId,
            isPvP: true,
            playerTurn: true,
            logs: [`You initiated a duel with ${defender.name}!`],
            enemy: {
                name: defender.name,
                level: defender.level,
                hp: defender.hp,
                maxHp: defender.maxHp,
                stats: defender.stats
            },
            isFinished: false,
            turnCount: 0,
            playerEffects: [],
            enemyEffects: [],
            cooldowns: {},
            turnExpiresAt: Date.now() + TURN_TIMEOUT_MS
        };

        const defenderCombat = {
            opponentId: userId,
            isPvP: true,
            playerTurn: false,
            logs: [`${attacker.name} has challenged you to a duel!`],
            enemy: {
                name: attacker.name,
                level: attacker.level,
                hp: attacker.hp,
                maxHp: attacker.maxHp,
                stats: attacker.stats
            },
            isFinished: false,
            turnCount: 0,
            playerEffects: [],
            enemyEffects: [],
            cooldowns: {},
            turnExpiresAt: Date.now() + TURN_TIMEOUT_MS
        };

        transaction.update(attackerRef, attacker); // Apply processHealing, infamy and faction changes
        transaction.update(attackerRef, { combatState: attackerCombat });
        transaction.update(defenderRef, { combatState: defenderCombat });

        return { success: true };
    });
});

export const startHealing = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        const character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        if (character.hp >= character.maxHp) throw new functions.https.HttpsError("failed-precondition", "You are already at full health.");
        if (character.healingState) throw new functions.https.HttpsError("failed-precondition", "You are already resting.");

        // Check if current location has an infirmary
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasInfirmary = location?.actions?.some((a: any) => a.type === "Infirmary");
        if (!hasInfirmary) throw new functions.https.HttpsError("failed-precondition", "There is no infirmary at your current location.");

        const endTime = Date.now() + HEALING_DURATION_MS;
        transaction.update(playerRef, { healingState: { endTime } });
        return { success: true, endTime };
    });
});

export const instantHeal = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        const character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        if (character.hp >= character.maxHp) throw new functions.https.HttpsError("failed-precondition", "You are already at full health.");
        if (character.gold < 50) throw new functions.https.HttpsError("failed-precondition", "Not enough gold for instant treatment.");

        // Check if current location has an infirmary
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasInfirmary = location?.actions?.some((a: any) => a.type === "Infirmary");
        if (!hasInfirmary) throw new functions.https.HttpsError("failed-precondition", "There is no infirmary at your current location.");

        transaction.update(playerRef, {
            hp: character.maxHp,
            gold: admin.firestore.FieldValue.increment(-50),
            healingState: null
        });
        recordLog(transaction, userId, "InstantHeal", "Paid for immediate treatment", -50, 0);
        return { success: true };
    });
});

// --- Existing Functions ---

export const completeMission = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { missionId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const missionRef = db.collection("gameData").doc("world").collection("missions").doc(missionId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, missionSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(missionRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!missionSnap.exists) throw new functions.https.HttpsError("not-found", "Mission not found.");

        let character = playerSnap.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to go on missions. Visit an infirmary.");
        }

        const mission = missionSnap.data() as any;

        if (character.level < (mission.minLevel || 1)) {
            throw new functions.https.HttpsError("failed-precondition", "Level too low for this mission.");
        }

        if (mission.factionRequirement && mission.factionRequirement !== "Neutral" && character.faction !== mission.factionRequirement) {
            throw new functions.https.HttpsError("failed-precondition", "You do not belong to the required faction.");
        }

        if (mission.locationId && character.currentLocation !== mission.locationId) {
            throw new functions.https.HttpsError("failed-precondition", "You are not at the required location for this mission.");
        }

        if (mission.statRequirement) {
            for (const [stat, value] of Object.entries(mission.statRequirement)) {
                const charStat = character.stats[STAT_MAPPING[stat] || stat];
                if ((charStat || 0) < (value as number)) {
                    throw new functions.https.HttpsError("failed-precondition", `Need ${value} ${stat} for this mission.`);
                }
            }
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < mission.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        let updatedChar = {
            ...character,
            energy: energy - mission.energyCost,
            energyUpdatedAt: energyUpdatedAt,
            gold: character.gold + (mission.goldReward || 0),
            xp: character.xp + (mission.xpReward || 0)
        };

        updatedChar = checkLevelUp(updatedChar);
        transaction.set(playerRef, updatedChar);
        recordLog(transaction, userId, "MissionCompleted", `Completed mission ${mission.title || missionId}`, mission.goldReward, mission.xpReward);

        return { success: true, rewards: { gold: mission.goldReward, xp: mission.xpReward } };
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
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) return { success: false };

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        transaction.update(playerRef, {
            ...character,
            lastOnline: Date.now(),
            isOnline: true
        });
        return { success: true };
    });
});

export const seedWorld = functions.https.onCall(async (data, context) => {
    // Basic admin check
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const batch = db.batch();

    const locations = [
        { name: "Fogi Tail Island", region: "East Blue", description: "A peaceful starting island with clear blue waters.", isSafe: true, weather: "Sunny", x: 0, y: 0, actions: [{ type: "Training", label: "Dojo", icon: "🥋" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Medical Clinic", icon: "🏥" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Ironcrest Isle", region: "East Blue", description: "A rocky island known for its iron mines and blacksmiths.", isSafe: true, weather: "Foggy", x: 50, y: 20, actions: [{ type: "Market", label: "Blacksmith", icon: "⚒" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Sunken Reef", region: "East Blue", description: "A shallow reef area teeming with colorful fish and hidden treasures.", isSafe: false, weather: "Clear", x: 10, y: 15, actions: [{ type: "Fishing", label: "Fishing Spot", icon: "🎣" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Tortuga Bay", region: "South Blue", description: "A bustling pirate haven filled with taverns and mystery.", isSafe: true, weather: "Tropical", x: 10, y: -100, actions: [{ type: "Tavern", label: "The Salty Dog", icon: "🍻" }, { type: "Market", label: "Bazaar", icon: "💰" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Pirate Doctor", icon: "🏥" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Pirate\u0027s Den", region: "South Blue", description: "An outlaw stronghold hidden within jagged cliffs.", isSafe: false, weather: "Stormy", x: 350, y: -350, actions: [{ type: "Arena", label: "Duel Pit", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Aqua", region: "South Blue", description: "A strictly regulated military base maintaining order.", isSafe: true, weather: "Clear", x: -80, y: -50, actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Navy Hospital", icon: "🏥" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Terra", region: "Grand Line", description: "A frontier navy post watching over the Grand Line entrance.", isSafe: true, weather: "Windy", x: -300, y: 200, actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Ignis", region: "Grand Line", description: "A strategic outpost near the volcanic islands.", isSafe: true, weather: "Hot", x: 400, y: 300, actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Crystal Cove", region: "Grand Line", description: "An island made of glowing crystals and mysterious energy.", isSafe: false, weather: "Shimmering", x: 120, y: 80, actions: [{ type: "BlackMarket", label: "Crystal Trader", icon: "💎" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Volcano Peak", region: "Grand Line", description: "An active volcano island with treacherous terrain.", isSafe: false, weather: "Ashy", x: 200, y: 150, actions: [{ type: "Training", label: "Extreme Training", icon: "🔥" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Whispering Woods", region: "Grand Line", description: "A dense forest where the trees seem to whisper secrets.", isSafe: false, weather: "Mist", x: -150, y: 180, actions: [{ type: "Cave", label: "Ancient Grotto", icon: "🕳" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] }
    ];

    for (const loc of locations) {
        const ref = db.collection("gameData").doc("world").collection("locations").doc(loc.name);
        batch.set(ref, { ...loc, id: loc.name });
    }

    const enemies = [
        { id: "sea_serpent", name: "Sea Serpent", minLevel: 1, maxLevel: 10, hp: 60, stats: { strength: 8, endurance: 8, agility: 5 }, goldRewardMin: 10, goldRewardMax: 30, xpReward: 20, dropTableId: "basic_sea_loot" },
        { id: "pirate_scout", name: "Pirate Scout", minLevel: 3, maxLevel: 15, hp: 80, stats: { strength: 10, endurance: 8, agility: 10 }, goldRewardMin: 20, goldRewardMax: 50, xpReward: 35, dropTableId: "basic_sea_loot" },
        { id: "giant_squid", name: "Giant Squid", minLevel: 10, maxLevel: 25, hp: 200, stats: { strength: 20, endurance: 20, agility: 5 }, goldRewardMin: 100, goldRewardMax: 200, xpReward: 100, dropTableId: "rare_sea_loot" },
        { id: "ghost_ship", name: "Ghost Pirate", minLevel: 15, maxLevel: 40, hp: 350, stats: { strength: 25, endurance: 25, agility: 15 }, goldRewardMin: 300, goldRewardMax: 600, xpReward: 250, dropTableId: "rare_sea_loot" }
    ];

    for (const enemy of enemies) {
        const ref = db.collection("gameData").doc("world").collection("enemies").doc(enemy.id);
        batch.set(ref, enemy);
    }

    const items = [
        { id: "fish_scales", name: "Fish Scales", description: "Shiny scales from a sea creature.", type: "Miscellaneous", rarity: "Common", price: 5 },
        { id: "sea_shell", name: "Sea Shell", description: "A pretty shell from the ocean floor.", type: "Miscellaneous", rarity: "Common", price: 10 },
        { id: "rusty_cutlass", name: "Rusty Cutlass", description: "An old, worn-out sword.", type: "Weapon", rarity: "Common", price: 50, levelRequirement: 1, statBonus: { strength: 2 } },
        { id: "old_boots", name: "Old Boots", description: "Waterlogged but still wearable.", type: "Armor", rarity: "Common", price: 40, levelRequirement: 1, statBonus: { endurance: 2 } },
        { id: "pearl", name: "Pearl", description: "A rare and valuable gem from a Giant Squid.", type: "Miscellaneous", rarity: "Rare", price: 200 }
    ];

    for (const item of items) {
        const ref = db.collection("gameData").doc("items").collection("all").doc(item.id);
        batch.set(ref, item);
    }

    const lootTables = [
        {
            id: "basic_sea_loot",
            entries: [
                { itemId: "fish_scales", chance: 0.6, minAmount: 1, maxAmount: 3 },
                { itemId: "sea_shell", chance: 0.4, minAmount: 1, maxAmount: 2 },
                { itemId: "rusty_cutlass", chance: 0.05, minAmount: 1, maxAmount: 1 }
            ]
        },
        {
            id: "rare_sea_loot",
            entries: [
                { itemId: "pearl", chance: 0.2, minAmount: 1, maxAmount: 1 },
                { itemId: "old_boots", chance: 0.1, minAmount: 1, maxAmount: 1 },
                { itemId: "fish_scales", chance: 0.5, minAmount: 2, maxAmount: 5 }
            ]
        }
    ];

    for (const table of lootTables) {
        const ref = db.collection("gameData").doc("world").collection("lootTables").doc(table.id);
        batch.set(ref, table);
    }

    await batch.commit();
    return { success: true, count: locations.length + enemies.length + items.length + lootTables.length };
});

export const sendMessage = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { message, channelId } = data;
    const userId = context.auth.uid;

    const playerSnap = await db.collection("players").doc(userId).get();
    if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
    const player = playerSnap.data() as any;

    if (player.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");
    if (player.mutedUntil && player.mutedUntil > Date.now()) {
        throw new functions.https.HttpsError("permission-denied", `You are muted until ${new Date(player.mutedUntil).toLocaleString()}. Reason: ${player.muteReason || "None"}`);
    }

    const chatRef = db.collection("chat").doc();
    await chatRef.set({
        senderId: userId,
        senderName: player.name,
        message: message,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        channelId: channelId || "global"
    });

    return { success: true };
});

// --- Admin Tools ---

export async function checkAdmin(context: functions.https.CallableContext) {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    // Check if user has admin claim
    if (context.auth.token.admin) return;

    // Fallback: Hardcoded check for "Sedna" as the primary admin
    const userId = context.auth.uid;
    const playerSnap = await db.collection("players").doc(userId).get();
    if (playerSnap.exists && (playerSnap.data() as any).nameLower === "sedna") {
        // Grant admin claim permanently for this user
        await admin.auth().setCustomUserClaims(userId, { admin: true });
        return;
    }

    throw new functions.https.HttpsError("permission-denied", "User is not an admin.");
}

export const adminAdjustGold = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

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
    await checkAdmin(context);

    const { userId, location } = data;
    await db.collection("players").doc(userId).update({ currentLocation: location, travelState: null });
    return { success: true };
});

export const adminSendAnnouncement = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { message } = data;
    await db.collection("announcements").add({
        message,
        timestamp: Date.now(),
        authorId: context.auth.uid
    });
    return { success: true };
});

export const adminMutePlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { userId, reason, durationHours } = data;
    const muteUntil = Date.now() + (durationHours * 3600000);

    await db.collection("players").doc(userId).update({
        mutedUntil: muteUntil,
        muteReason: reason
    });
    return { success: true };
});

export const adminBanPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { userId, reason } = data;

    await db.collection("players").doc(userId).update({
        isBanned: true,
        banReason: reason
    });
    return { success: true };
});

// --- Social Functions ---

export const explicitLogout = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).update({ isOnline: false });
    return { success: true };
});

export const sendFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    if (senderId === targetId) throw new functions.https.HttpsError("invalid-argument", "Cannot add yourself.");

    const targetSnap = await db.collection("players").doc(targetId).get();
    if (!targetSnap.exists) throw new functions.https.HttpsError("not-found", "Target player not found.");
    const target = targetSnap.data() as any;

    if (target.blocked && target.blocked.includes(senderId)) {
        throw new functions.https.HttpsError("permission-denied", "You have been blocked by this player.");
    }

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

export const declineFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { senderId } = data;
    const receiverId = context.auth.uid;
    const requestRef = db.collection("friendRequests").doc(`${senderId}_${receiverId}`);

    await requestRef.update({ status: "declined" });
    return { success: true };
});

export const removeFriend = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { friendId } = data;
    const userId = context.auth.uid;

    await db.runTransaction(async (transaction) => {
        transaction.update(db.collection("players").doc(userId), {
            friends: admin.firestore.FieldValue.arrayRemove(friendId)
        });
        transaction.update(db.collection("players").doc(friendId), {
            friends: admin.firestore.FieldValue.arrayRemove(userId)
        });
    });

    return { success: true };
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

export const unblockPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const userId = context.auth.uid;

    await db.collection("players").doc(userId).update({
        blocked: admin.firestore.FieldValue.arrayRemove(targetId)
    });

    return { success: true };
});

// --- Inventory & Equipment ---

export const equipItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId, slot } = data; // slot: Weapon, Armor, Accessory
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const inventory = character.inventory || [];
        const item = inventory.find((i: any) => i.id === itemId);

        if (!item) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");
        if (item.type !== slot) throw new functions.https.HttpsError("invalid-argument", `Item cannot be equipped in ${slot} slot.`);

        // Level Requirement Check
        if (character.level < (item.levelRequirement || 1)) {
            throw new functions.https.HttpsError("failed-precondition", "Level too low to equip this item.");
        }

        const equipment = character.equipment || {};
        equipment[slot] = item;

        transaction.update(playerRef, { equipment });
        return { success: true };
    });
});

export const unequipItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { slot } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const equipment = character.equipment || {};
        delete equipment[slot];

        transaction.update(playerRef, { equipment });
        return { success: true };
    });
});

export const purchaseItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId, shopId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const itemRef = db.collection("gameData").doc("items").collection("all").doc(itemId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, itemSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(itemRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!itemSnap.exists) throw new functions.https.HttpsError("not-found", "Item not found.");

        const character = playerSnap.data() as any;
        const item = itemSnap.data() as any;

        // Validate location has a market
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const marketAction = location?.actions?.find((a: any) => a.type === "Market" || a.type === "BlackMarket");

        if (!marketAction) {
            throw new functions.https.HttpsError("failed-precondition", "There is no market at your current location.");
        }

        // shopId validation (simplified for now, but enforces the concept)
        if (shopId === "BlackMarket" && marketAction.type !== "BlackMarket") {
             throw new functions.https.HttpsError("failed-precondition", "This item is only available in a Black Market.");
        }

        if (character.gold < item.price) {
            throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");
        }

        const inventory = character.inventory || [];
        if (inventory.length >= INVENTORY_CAPACITY) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory is full.");
        }

        const newInventory = [...inventory, { ...item, id: `${item.id}_${Date.now()}` }];

        transaction.update(playerRef, {
            gold: character.gold - item.price,
            inventory: newInventory
        });

        recordLog(transaction, userId, "PurchaseItem", `Purchased ${item.name}`, -item.price, 0);
        return { success: true };
    });
});

export const sellItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;

        // Protection: Check if item is equipped
        const isEquipped = Object.values(character.equipment || {}).some((i: any) => i && i.id === itemId);
        if (isEquipped) {
            throw new functions.https.HttpsError("failed-precondition", "Cannot sell equipped items. Unequip it first.");
        }

        const inventory = character.inventory || [];
        const itemIndex = inventory.findIndex((i: any) => i.id === itemId);

        if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

        const item = inventory[itemIndex];
        const sellPrice = Math.floor(item.price * 0.5);

        inventory.splice(itemIndex, 1);

        transaction.update(playerRef, {
            gold: admin.firestore.FieldValue.increment(sellPrice),
            inventory
        });

        recordLog(transaction, userId, "SellItem", `Sold ${item.name}`, sellPrice, 0);
        return { success: true, goldGained: sellPrice };
    });
});

export const useItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const inventory = character.inventory || [];
        const itemIndex = inventory.findIndex((i: any) => i.id === itemId);

        if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

        const item = inventory[itemIndex];
        if (item.type !== "Consumable") throw new functions.https.HttpsError("invalid-argument", "Item is not consumable.");

        let playerHp = character.hp;
        const healAmount = item.healAmount || 30;
        playerHp = Math.min(character.maxHp, playerHp + healAmount);

        inventory.splice(itemIndex, 1);

        transaction.update(playerRef, {
            hp: playerHp,
            inventory
        });

        recordLog(transaction, userId, "UseItem", `Used ${item.name}`, 0, 0);
        return { success: true, newHp: playerHp };
    });
});

// --- Crew System Completion ---

export const leaveCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const crewId = character.crewId;
        if (!crewId) throw new functions.https.HttpsError("failed-precondition", "Player is not in a crew.");

        const crewRef = db.collection("crews").doc(crewId);
        const crewSnap = await transaction.get(crewRef);
        if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");

        const crew = crewSnap.data() as any;
        if (crew.captainId === userId) {
            throw new functions.https.HttpsError("failed-precondition", "Captain cannot leave. Disband or promote someone else first.");
        }

        transaction.update(crewRef, {
            members: admin.firestore.FieldValue.arrayRemove(userId),
            totalBounty: admin.firestore.FieldValue.increment(-character.bounty)
        });
        transaction.update(playerRef, { crewId: null });

        return { success: true };
    });
});

export const inviteToCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    const senderSnap = await db.collection("players").doc(senderId).get();
    const sender = senderSnap.data() as any;
    if (!sender.crewId) throw new functions.https.HttpsError("failed-precondition", "You are not in a crew.");

    const inviteRef = db.collection("crewInvites").doc(`${sender.crewId}_${targetId}`);
    await inviteRef.set({
        crewId: sender.crewId,
        crewName: (await db.collection("crews").doc(sender.crewId).get()).data()?.name,
        senderId,
        targetId,
        status: "pending",
        timestamp: Date.now()
    });

    return { success: true };
});

export const respondToInvite = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { crewId, accept } = data;
    const userId = context.auth.uid;
    const inviteRef = db.collection("crewInvites").doc(`${crewId}_${userId}`);

    return db.runTransaction(async (transaction) => {
        const inviteSnap = await transaction.get(inviteRef);
        if (!inviteSnap.exists || inviteSnap.data()?.status !== "pending") {
            throw new functions.https.HttpsError("not-found", "Invite not found.");
        }

        if (!accept) {
            transaction.update(inviteRef, { status: "rejected" });
            return { success: true, accepted: false };
        }

        const playerRef = db.collection("players").doc(userId);
        const crewRef = db.collection("crews").doc(crewId);
        const [playerSnap, crewSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(crewRef)
        ]);

        const character = playerSnap.data() as any;
        const crew = crewSnap.data() as any;

        if (character.crewId) throw new functions.https.HttpsError("already-exists", "Already in a crew.");
        if (crew.members.length >= 20) throw new functions.https.HttpsError("resource-exhausted", "Crew is full.");

        transaction.update(crewRef, {
            members: admin.firestore.FieldValue.arrayUnion(userId),
            totalBounty: admin.firestore.FieldValue.increment(character.bounty)
        });
        transaction.update(playerRef, { crewId });
        transaction.update(inviteRef, { status: "accepted" });

        return { success: true, accepted: true };
    });
});

export const markMailAsRead = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).collection("mail").doc(mailId).update({ isRead: true });
    return { success: true };
});

export const deleteMail = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).collection("mail").doc(mailId).delete();
    return { success: true };
});

export const claimMailRewards = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const mailRef = playerRef.collection("mail").doc(mailId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, mailSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(mailRef)
        ]);

        if (!playerSnap.exists || !mailSnap.exists) throw new functions.https.HttpsError("not-found", "Not found.");
        const mail = mailSnap.data() as any;
        if (mail.claimed) throw new functions.https.HttpsError("failed-precondition", "Rewards already claimed.");
        if (!mail.rewards) throw new functions.https.HttpsError("failed-precondition", "No rewards to claim.");

        const character = playerSnap.data() as any;
        const rewards = mail.rewards;

        const updates: any = {
            gold: character.gold + (rewards.gold || 0),
            xp: character.xp + (rewards.xp || 0),
        };

        if (rewards.items) {
             const inventory = character.inventory || [];
             if (inventory.length + rewards.items.length > INVENTORY_CAPACITY) {
                  throw new functions.https.HttpsError("resource-exhausted", "Inventory full.");
             }
             updates.inventory = [...inventory, ...rewards.items.map((i: any) => ({ ...i, id: `${i.id}_${Date.now()}_${Math.random()}` }))];
        }

        transaction.update(playerRef, updates);
        transaction.update(mailRef, { claimed: true, isRead: true });

        return { success: true };
    });
});

export const promoteMember = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId, rank } = data; // rank: Officer
    const captainId = context.auth.uid;

    const playerSnap = await db.collection("players").doc(captainId).get();
    const captain = playerSnap.data() as any;
    const crewRef = db.collection("crews").doc(captain.crewId);

    return db.runTransaction(async (transaction) => {
        const crewSnap = await transaction.get(crewRef);
        const crew = crewSnap.data() as any;

        if (crew.captainId !== captainId) throw new functions.https.HttpsError("permission-denied", "Only Captain can promote.");

        const roles = crew.roles || {};
        roles[targetId] = rank;
        transaction.update(crewRef, { roles });

        return { success: true };
    });
});
