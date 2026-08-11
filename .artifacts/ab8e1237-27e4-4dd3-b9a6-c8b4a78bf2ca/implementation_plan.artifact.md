# Implementation Plan - Server-Authoritative Systems & RPG Engine Expansion

This plan outlines the steps to move game logic to the server, expand the combat engine, implement full inventory/equipment systems, and complete the crew features.

## User Review Required

> [!IMPORTANT]
> **Cloud Functions Overhaul**: This will significantly change how the client interacts with the server. All logic previously calculated on the client (like travel arrival times or combat damage) will now be strictly server-authoritative.

> [!WARNING]
> **Data Migration**: Changes to the `Character` and `CombatState` models might require careful handling if there is existing player data in Firestore.

## Proposed Changes

### [Backend] Cloud Functions (`functions/src/index.ts`)

#### [MODIFY] [index.ts](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/functions/src/index.ts)
- **Travel**:
    - Update `startTravel` to calculate `arrivalTime` on the server based on a distance map between locations.
    - Remove `arrivalTime` from input parameters.
- **Combat Engine**:
    - Expand `CombatState` to include `playerStats`, `enemyStats`, `turnCount`, `statusEffects`, and `cooldowns`.
    - Implement `startCombat` (PvE) and `startPvP` (PvP) functions.
    - Update `combatAction` to handle:
        - Accuracy, Dodge, Crit, and Defense calculations.
        - Techniques with energy costs and cooldowns.
        - Consumable item usage from inventory.
        - Status effects (e.g., Poison, Stun).
- **Progression**:
    - Update `train` to use the explicit `STAT_MAPPING` (already partially there, but ensure it's robust).
    - Add validation to `completeMission`: check if player is at the required mission location.
    - Centralize `checkLevelUp` logic and ensure it's the only place level-ups happen.
- **Inventory & Equipment**:
    - Add `equipItem`, `unequipItem`, and `useItem` Cloud Functions.
    - Implement `lootTable` logic for combat rewards and mission completions.
    - Add `purchaseItem` for the shop.
- **Crew System**:
    - Implement `leaveCrew`, `inviteToCrew`, `respondToInvite`, `kickFromCrew`, and `promoteMember`.
    - Add rank logic (Captain, Officer, Member).

### [Frontend] Models & Repositories

#### [MODIFY] [Models.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/model/Models.kt)
- Remove `checkLevelUp` from the `Character` class (moving to server).
- Expand `CombatState`, `Enemy`, `Item`, and `Technique` models to match the new server-side logic.
- Add `CrewMember` and `CrewInvite` models.

#### [MODIFY] [GameRepository.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/repository/GameRepository.kt)
- Update interface and `FirestoreGameRepository` to match new Cloud Function signatures.
- Remove client-side optimistic `finishTravel` logic.

#### [MODIFY] [CrewRepository.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/repository/CrewRepository.kt)
- Add new crew management methods.

### [Frontend] UI

#### [NEW] Crew Management Screens
- Build the UI for invites, member management, and ranks.

#### [NEW] Inventory & Equipment Screen
- Build a functional inventory list and equipment slots.
- Implement the "Equip/Unequip/Use" interactions.

#### [MODIFY] Combat Screen
- Update to support new actions (Techniques, Items) and display status effects/detailed logs.

## Verification Plan

### Automated Tests
- I will add unit tests for the updated `Character` and `CombatState` models.
- Since Cloud Functions are difficult to test locally without an emulator, I will focus on ensuring the client-side repository correctly marshals data.

### Manual Verification
- Deploy Cloud Functions.
- Verify travel starts and ends correctly without client-side timing manipulation.
- Verify combat actions (Techniques, Items) work and persist state correctly.
- Test the full crew flow: Invite -> Join -> Promote -> Kick -> Leave.
- Verify equipment bonuses are applied to player stats in combat.
