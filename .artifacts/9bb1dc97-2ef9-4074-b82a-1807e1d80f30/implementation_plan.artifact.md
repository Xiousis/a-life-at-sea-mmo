# Implementation Plan - HUB Redesign

Redesign the main HUB (Dashboard) to feel like the player is standing in their current port, moving training to a separate area and focusing on immersion.

## User Review Required

> [!IMPORTANT]
> The Training grid will be moved off the main HUB as requested. A new "Training" screen or dialog will be needed eventually, but for this task, I will implement the "🥊 TRAINING" button in the Action Grid which can navigate to a training view.

## Proposed Changes

### Data Layer

#### [MODIFY] [Models.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/model/Models.kt)
- Add `Location` data class with fields: `name`, `region`, `isSafe`, `description`, `weather`, `actions`.
- Add `LocationAction` enum or data class for the 2x3 grid.

### UI Layer

#### [MODIFY] [GameViewModel.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/GameViewModel.kt)
- Add a `currentLocationInfo` StateFlow that provides `Location` details based on `character.currentLocation`.
- Add mock data for locations like "Port Haven", "Blacktooth Island", etc.

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/screens/DashboardScreen.kt)
- Implement `PlayerCard` (Top section).
- Implement `LocationCenterpiece` (Island info).
- Implement `MissionBoard` (Big button).
- Implement `ActionGrid` (2x3 grid of location actions).
- Implement `WorldActivity` feed.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/MainActivity.kt)
- Update `NavigationBar` to: HUB, SEA, CREW, CHAT.
- Add "Training" to the `Screen` sealed class for navigation from the HUB.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- Unit test for `GameViewModel` to ensure `currentLocationInfo` updates correctly when character location changes.

### Manual Verification
- Deploy the app and verify the new layout matches the provided wireframe.
- Verify that clicking "MISSION BOARD" navigates to the Missions screen.
- Verify that the Action Grid buttons are correctly displayed for "Port Haven".
