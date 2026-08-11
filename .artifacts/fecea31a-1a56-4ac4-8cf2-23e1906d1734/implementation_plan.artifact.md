# Server-Authoritative Logic & Combat Engine Expansion

Transition the game logic to be fully server-authoritative by expanding Cloud Functions for travel, combat, and PvP. Strengthen the progression system and build a more complex combat engine.

## User Review Required

> [!IMPORTANT]
> The PvP implementation will follow a "forced turn-based" model where the attacker initiates a combat state that the defender must eventually respond to or be defeated by auto-turns. This is a significant change from the current instantaneous result.

> [!NOTE]
> Stat mapping and level-up logic are already mostly server-side in `functions/src/index.ts`, but I will refine them to ensure no client-side calculations are leaked.

## Proposed Changes

### Backend (Cloud Functions)

#### [MODIFY] [index.ts](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/functions/src/index.ts)
- **Combat Stats**: Update `calculateCombatStats` to include equipment bonuses.
- **Combat Engine**:
    - Enhance `combatAction` to support various status effects (Stun, Weaken, Fortify, etc.).
    - Implement real `Item` usage in combat by validating and removing items from inventory.
    - Add `Enemy abilities` (enemies can use techniques too).
    - Improve `Defend` behavior to provide a temporary defense buff.
- **PvP**:
    - Update `attackPlayer` to initialize a `combatState` for both participants instead of calculating an instant result.
    - Add `pvpAction` to handle player responses in PvP combat.
- **Progression**:
    - Robust server-side validation for mission requirements (minLevel, location, faction).
    - Fix stat mapping to prevent incorrect field names (e.g., `martialarts`).

### Frontend (Android App)

#### [MODIFY] [GameRepository.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/repository/GameRepository.kt)
- Update `attackPlayer` to handle the new `combatState` return/trigger.
- Ensure all repository methods are simple wrappers for Cloud Function calls.

#### [MODIFY] [GameViewModel.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/GameViewModel.kt)
- Adjust view model to handle potential new states from Cloud Functions (e.g., waiting for opponent in PvP).

## Verification Plan

### Automated Tests
- Run `npm run build` in the `functions` directory to ensure no TypeScript errors.
- (Manual) Verify Cloud Function logic through logs and Firestore emulator.

### Manual Verification
- Deploy functions and test:
    - **Travel**: Verify travel completion is handled by `finishTravel` triggered by the client.
    - **Combat**: Test techniques, cooldowns, and status effects in a random encounter.
    - **PvP**: Verify attacking another player starts a turn-based session.
    - **Training**: Verify stats are updated correctly with the new mapping.
