# Player Profile System Implementation Plan

This plan outlines the steps taken to implement a comprehensive player profile system for the social text MMO.

## User Review Required

> [!NOTE]
> The profile layout has been updated to match the requested design:
> ☠ RAZOR
> Level 31
> Human
> Bounty: 3,482,900
> Crew: Black Tide
> Title: Sea Devil
> PvP: 81W / 24L
> [ATTACK] [MESSAGE] [VIEW CREW] [ADD FRIEND]

## Proposed Changes

### Data Models

#### [MODIFY] [Models.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/model/Models.kt)
- Added `title`, `pvpWins`, and `pvpLosses` fields to the `Character` data class.

### Repositories

#### [MODIFY] [GameRepository.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/data/repository/GameRepository.kt)
- Added `getPlayerProfile(playerId: String)` and `getCrew(crewId: String)` to the `GameRepository` interface.
- Implemented these methods in `FirestoreGameRepository` using Firestore snapshots.
- Updated `MockGameRepository` for local development and testing.

### ViewModels

#### [NEW] [PlayerProfileViewModel.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/PlayerProfileViewModel.kt)
- Created a dedicated ViewModel to manage fetching and state of a specific player's profile and their associated crew.

### UI Screens

#### [MODIFY] [ProfileScreen.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/screens/ProfileScreen.kt)
- Redesigned the screen to match the requested pirate-themed aesthetic.
- Added support for displaying bounty (formatted), crew name, title, and PvP stats.
- Added action buttons for Attack, Message, View Crew, and Add Friend.

#### [MODIFY] [LeaderboardScreen.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/screens/LeaderboardScreen.kt)
- Added `onPlayerClick` callback to allow navigating to a player's profile from the leaderboard.

#### [MODIFY] [PvPScreen.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/ui/screens/PvPScreen.kt)
- Added `onPlayerClick` callback to allow viewing a target's profile before attacking.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/buggm/AndroidStudioProjects/AlifeatSeaMMO/app/src/main/java/com/alifeatseammo/MainActivity.kt)
- Integrated `PlayerProfileViewModel`.
- Updated navigation logic to use `selectedPlayerId` and fetch the full profile via the ViewModel when entering the Character screen.
- Wired up `onPlayerClick` from all relevant screens (Dashboard, Leaderboard, PvP).

## Verification Plan

### Automated Tests
- Build the project to ensure all new dependencies and method signatures are correct.
- `gradle_build(":app:assembleDebug")`

### Manual Verification
1.  Open the app and navigate to the Dashboard.
2.  Click on a player in the "Players Nearby" row.
3.  Verify the profile displays correctly: Name, Level, Race, Bounty, Crew, Title, PvP Stats.
4.  Verify the action buttons (Attack, Message, etc.) are present.
5.  Navigate to the Leaderboard and click a player to verify the profile opens.
6.  Navigate to the PvP screen and click a target's card to verify the profile opens.
