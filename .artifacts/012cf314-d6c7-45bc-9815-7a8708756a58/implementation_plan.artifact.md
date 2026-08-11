# Game Engine Hardening and Backend Migration Plan

This plan outlines the migration of critical game logic from the Android client to Firebase Cloud Functions and the hardening of the Firestore security rules.

## User Review Required

> [!IMPORTANT]
> - **Breaking Changes**: The client will no longer be able to directly modify player stats, gold, xp, or travel/combat state.
> - **Cloud Functions**: New functions for `createCharacter`, `startTravel`, `finishTravel`, `combatAction`, and `attackPlayer` will be introduced.
> - **Combat Engine**: A new server-side combat engine will handle accuracy, dodge, critical hits, and damage variance.

## Proposed Changes

### Backend (Cloud Functions & Rules)

#### [MODIFY] [firestore.rules](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/firestore.rules)
- Tighten rules to deny client-side updates to `hp`, `currentLocation`, `travelState`, `crewId`, and `equipment`.
- Deny client-side creation of player documents; enforce creation via Cloud Function.

#### [MODIFY] [index.ts](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/functions/src/index.ts)
- Implement `createCharacter` with unique name checks.
- Fix `train` stat mapping bug.
- Implement server-side level-up logic.
- Implement `startTravel` and `finishTravel` (arrival) logic.
- Implement a robust server-side Combat Engine for `combatAction` and `attackPlayer`.
- Enforce mission/location requirements and cooldowns.

#### [NEW] [firebase.json](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/firebase.json)
- Add Firebase deployment configuration for Functions and Firestore.

### Android Client

#### [MODIFY] [GameRepository.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/repository/GameRepository.kt)
- Update `createCharacter`, `startTravel`, `combatAction`, and `attackPlayer` to use `functions.getHttpsCallable()`.
- Remove local transaction-based logic for these actions.

#### [NEW] [InventoryScreen.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/screens/InventoryScreen.kt)
- Build a full Inventory and Equipment UI.

#### [NEW] [CrewScreen.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/screens/CrewScreen.kt)
- Build a full Crew management UI (Create, Join, Members, Ranks).

## Verification Plan

### Automated Tests
- Deploy functions to the Firebase Emulator.
- Run unit tests for Cloud Functions (if applicable) or verify via manual triggers.
- `gradle_build(":app:assembleDebug")`

### Manual Verification
1.  Attempt to create a character with an existing name (should fail).
2.  Perform a travel action and verify the arrival is handled by the server.
3.  Engage in combat and verify the logs show the new RPG mechanics (dodge, crit, etc.).
4.  Try to manually edit `hp` or `gold` via the client (should fail via Firestore rules).
5.  Test the new Inventory and Crew UI flows.
