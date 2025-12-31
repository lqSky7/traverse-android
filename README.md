# Traverse

A modern Android app for tracking coding practice with glassmorphic UI design.

## Features

- **Glassmorphic UI** - Beautiful frosted glass effects using Haze library
- **Dual Theme Support** - Light (pink) and Dark (black/white) themes
- **Authentication** - Login and registration with JWT tokens
- **Home Dashboard** - Streak tracking, XP stats, and problem completion progress
- **Bottom Navigation** - Animated tab bar with Home, Revisions, Friends, and Settings

## Screenshots

*Coming soon*

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Glassmorphism**: [Haze](https://github.com/chrisbanes/haze) 1.0.0
- **Networking**: Retrofit 2.11.0
- **Animations**: Lottie 6.6.2
- **Navigation**: Compose Navigation
- **Min SDK**: 24
- **Target SDK**: 36

## Architecture

```
app/src/main/java/com/example/traverse2/
├── data/
│   ├── api/          # Retrofit API interfaces
│   └── model/        # Data classes
├── ui/
│   ├── components/   # Reusable UI components
│   │   ├── AnimatedGradientBackground.kt
│   │   ├── GlassBottomBar.kt
│   │   ├── GlassButton.kt
│   │   ├── GlassCard.kt
│   │   └── GlassTextField.kt
│   ├── screens/      # App screens
│   │   ├── LoginScreen.kt
│   │   ├── MainScreen.kt
│   │   ├── HomeContent.kt
│   │   └── PlaceholderScreens.kt
│   └── theme/        # Theme configuration
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

## Backend

This app connects to the [Traverse Backend](https://traverse-backend-api.azurewebsites.net) API.

**API Endpoints:**
- `/api/auth` - Authentication (login, register, logout)
- `/api/users` - User profile and stats
- `/api/solves` - Problem solving records
- `/api/revisions` - Spaced repetition system
- `/api/friends` - Social features
- `/api/achievements` - User achievements

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device/emulator (API 24+)

## Building

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

## Theme System

The app supports two themes toggled via a button on the login screen:

**Light Theme (Pink)**
- Soft pink gradients with floating orbs
- White glassmorphic cards
- Pink accent colors

**Dark Theme (Black/White)**
- Grayscale floating orbs
- Dark glassmorphic cards with white text
- Monochromatic design

## Roadmap

- [ ] API integration for Home stats
- [ ] Revisions screen with spaced repetition
- [ ] Friends list and social features
- [ ] Settings and preferences
- [ ] Push notifications
- [ ] Offline support

## License

MIT License
