# A Life at Sea MMO

An Android-based Massive Multiplayer Online (MMO) game set in a maritime world. Built with modern Android technologies and powered by Firebase.

## 🚀 Features

*   **Modern UI**: Built entirely with **Jetpack Compose** for a reactive and fluid user experience.
*   **Real-time Backend**: Powered by **Firebase Firestore** for game state and player data synchronization.
*   **Authentication**: Secure player login via **Firebase Auth**.
*   **AI Integration**: Utilizing **Firebase AI** (Vertex AI for Firebase) for intelligent game features.
*   **Analytics**: Integrated **Firebase Analytics** to track player engagement and game performance.
*   **Navigation**: Uses **Navigation 3** for robust multi-pane and adaptive layouts.

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Architecture**: MVVM / Clean Architecture
*   **Dependency Injection**: (e.g., Hilt/Koin if applicable, but currently visible in gradle)
*   **Backend**: Firebase (Auth, Firestore, Analytics, AI)
*   **Build System**: Kotlin DSL (Gradle) with Version Catalogs

## 📥 Getting Started

### Prerequisites

*   Android Studio Ladybug (or newer)
*   JDK 17+
*   A Firebase project (see [Firebase Setup](#firebase-setup))

### Firebase Setup

1.  Create a new project in the [Firebase Console](https://console.firebase.google.com/).
2.  Add an Android app with the package name `com.alifeatseammo`.
3.  Download the `google-services.json` file and place it in the `app/` directory.
4.  Enable the following services in the console:
    *   Authentication (Email/Password or other providers)
    *   Cloud Firestore
    *   Vertex AI for Firebase (if using AI features)

## 🏗️ Building

Clone the repository and open it in Android Studio.

```bash
git clone https://github.com/Xiousis/a-life-at-sea-mmo.git
cd a-life-at-sea-mmo
./gradlew assembleDebug
```

## 📄 License and Terms

*   **License**: This project is proprietary. See the [LICENSE](LICENSE) file for details.
*   **Terms of Service**: By using this software, you agree to the [Terms of Service](TERMS_OF_SERVICE.md).
