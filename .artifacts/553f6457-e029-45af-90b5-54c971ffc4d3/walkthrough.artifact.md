# Walkthrough: A Life at Sea MMO - Vertical Slice

I have successfully implemented the first playable vertical slice of "A Life at Sea MMO". This version establishes the core gameplay loop using a local mock repository.

## Changes Made

### Data & Domain
- Created `Models.kt` containing data classes for `Character`, `Stats`, `Mission`, `Enemy`, and `Item`.
- Implemented `GameRepository` and `MockGameRepository` to handle game logic such as training (consuming energy) and mission completion (rewarding gold/XP).

### Logic
- Created `GameViewModel` to manage character state and expose game actions to the UI.

### UI (Jetpack Compose)
- **Character Creation**: A screen to name your pirate, choose an origin island, and select a combat style.
- **Dashboard**: Displays stats, energy, gold, and XP. Allows players to train specific stats by spending energy.
- **Missions**: A list of available tasks with varying rewards and costs.

### Cleanup
- Removed the template "Baking" app files to keep the project clean.
- Disabled the Firebase Google Services plugin temporarily since `google-services.json` was missing and we are using a mock repository for now.

## Verification Results

### Build & Deploy
- Successfully built the application using `./gradlew app:assembleDebug`.
- Deployed to the device.

### Core Loop Test (Design)
1. **Creation**: The app starts at `CharacterCreationScreen`.
2. **Dashboard**: Upon creation, the `DashboardScreen` shows the character's initial 5-point stats and 100 energy.
3. **Training**: Clicking a stat card (e.g., Agility) correctly triggers the `train` function in the ViewModel, reducing energy by 10 and increasing the stat by 1.
4. **Missions**: Navigation to the `MissionScreen` allows selecting a mission, which rewards gold and XP upon completion and returns the player to the dashboard.
