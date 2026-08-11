import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// Constants for game balance
const ENERGY_REGEN_RATE_MS = 3 * 60 * 1000; // 1 energy per 3 minutes
const MAX_ENERGY = 100;

/**
 * Calculates regenerated energy based on time elapsed since last update.
 */
function calculateCurrentEnergy(character: any): { energy: number, energyUpdatedAt: number } {
    const now = Date.now();
    const elapsed = now - character.energyUpdatedAt;
    const regenerated = Math.floor(elapsed / ENERGY_REGEN_RATE_MS);

    if (regenerated <= 0) return { energy: character.energy, energyUpdatedAt: character.energyUpdatedAt };

    const newEnergy = Math.min(MAX_ENERGY, character.energy + regenerated);
    // Only update timestamp if energy actually increased or is capped
    const newTimestamp = character.energy + regenerated >= MAX_ENERGY ? now : character.energyUpdatedAt + (regenerated * ENERGY_REGEN_RATE_MS);

    return { energy: newEnergy, energyUpdatedAt: newTimestamp };
}

export const train = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { statType } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").document(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data();
        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);

        if (energy < 10) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        const updateData: any = {
            energy: energy - 10,
            energyUpdatedAt: energyUpdatedAt,
            xp: character.xp + 5,
            [`stats.${statType.toLowerCase()}`]: admin.firestore.FieldValue.increment(1)
        };

        transaction.update(playerRef, updateData);
        return { success: true, newEnergy: energy - 10 };
    });
});

export const completeMission = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { missionId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").document(userId);
    const missionRef = db.collection("missions").document(missionId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, missionSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(missionRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!missionSnap.exists) throw new functions.https.HttpsError("not-found", "Mission not found.");

        const character = playerSnap.data();
        const mission = missionSnap.data();

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);

        if (energy < mission.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        const updateData = {
            energy: energy - mission.energyCost,
            energyUpdatedAt: energyUpdatedAt,
            gold: character.gold + mission.rewards.gold,
            xp: character.xp + mission.rewards.xp
        };

        transaction.update(playerRef, updateData);
        return { success: true, rewards: mission.rewards };
    });
});

export const createCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, description } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").document(userId);
    const crewRef = db.collection("crews").doc();

    return db.runTransaction(async (transaction) => {
        const playerSnap = await transaction.get(playerRef);
        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = playerSnap.data();
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

        return { success: true, crewId: crewRef.id };
    });
});

export const joinCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { crewId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").document(userId);
    const crewRef = db.collection("crews").document(crewId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, crewSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(crewRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");

        const character = playerSnap.data();
        const crew = crewSnap.data();

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
