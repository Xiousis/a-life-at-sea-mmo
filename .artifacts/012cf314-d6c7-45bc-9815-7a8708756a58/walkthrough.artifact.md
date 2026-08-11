# Player Profile System Walkthrough

The player profile system is now fully integrated into the "A Life At Sea" MMO. This foundation allows players to inspect each other, paving the way for social interactions and crew management.

## Key Accomplishments

- **Enhanced Data Model**: The `Character` model now tracks `title`, `pvpWins`, and `pvpLosses`.
- **Dynamic Data Fetching**: A new `PlayerProfileViewModel` fetches both player details and crew information in real-time.
- **Pirate-Themed UI**: The `ProfileScreen` has been overhauled to provide a clear, bold overview of a pirate's achievements.
- **Seamless Navigation**: Players can now be inspected from the Dashboard, Leaderboard, and PvP screens.

## Visual Overview

The profile screen now looks like this:

☠ **RAZOR**
Level 31
Human

**Bounty:** 3,482,900
**Crew:** Black Tide
**Title:** Sea Devil

**PvP:** 81W / 24L

[ATTACK]
[MESSAGE]
[VIEW CREW]
[ADD FRIEND]

## Implementation Details

### Updated Character Model
```kotlin
data class Character(
    // ... existing fields
    val title: String = "Novice Sailor",
    val pvpWins: Int = 0,
    val pvpLosses: Int = 0,
    // ...
)
```

### PlayerProfileViewModel
The ViewModel ensures that when you click a player, their latest stats and their crew's name are fetched and displayed.

```kotlin
class PlayerProfileViewModel(...) : ViewModel() {
    val playerProfile: StateFlow<Character?> = ...
    val playerCrew: StateFlow<Crew?> = ...
    fun loadPlayer(playerId: String) { ... }
}
```

## Next Steps

1.  **Crew Screen**: Implement the `VIEW CREW` functionality to show all members of a crew.
2.  **Messaging System**: Build the `MESSAGE` screen to allow direct communication between players.
3.  **Friend System**: Implement `ADD FRIEND` logic and a friend list.
4.  **Bounty Hunters**: Use the new bounty field to create a dedicated bounty board where players can hunt high-value targets.
