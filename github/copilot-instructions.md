# Traverse Android - Copilot Instructions

## Architecture Overview
- **Framework**: Jetpack Compose with Material3 Design System
- **Pattern**: MVVM (Model-View-ViewModel) with single-directional data flow
- **Networking**: Retrofit + OkHttp + kotlinx.serialization
- **State Management**: StateFlow for reactive UI updates
- **Caching**: Centralized SharedPreferences-based system with TTL expiration
- **Image Loading**: Coil library with local file caching
- **Navigation**: Jetpack Navigation Compose

## Project Structure
```
app/src/main/java/com/traverse/android/
├── data/
│   ├── AuthModels.kt         # User, login/register models
│   ├── HomeModels.kt         # Stats, solves, achievements
│   ├── RevisionModels.kt     # Revision scheduling models
│   ├── FriendsModels.kt      # Friends, requests, profiles, subscription
│   ├── NetworkService.kt     # Retrofit API interface
│   └── CacheManager.kt       # Centralized caching system
├── viewmodel/
│   ├── AuthViewModel.kt      # Auth state, login, register, profile image
│   ├── HomeViewModel.kt      # Home tab data fetching
│   ├── RevisionsViewModel.kt # Revisions with ML mode + subscription check
│   └── FriendsViewModel.kt   # Friends, requests, leaderboard
├── ui/
│   ├── auth/                 # Login, register screens
│   ├── home/                 # Home tab UI
│   ├── revisions/            # Revisions tab UI
│   ├── friends/              # Friends tab UI
│   ├── settings/             # Settings, profile edit, freeze shop
│   ├── navigation/           # Bottom tab navigation
│   └── theme/                # Colors, typography, shapes
└── MainActivity.kt           # Entry point
```

## Design System

### Colors (Monochromatic Pastel Palette)
```kotlin
EasyPastel = Color(0xFFA8E6CF)    // Easy difficulty
MediumPastel = Color(0xFFFFD3B6)  // Medium difficulty
HardPastel = Color(0xFFFFAAA5)    // Hard difficulty
AccentPastel = Color(0xFFB8D4E3)  // Accents
GoldColor = Color(0xFFFFD700)     // Achievements
Peach = Color(0xFFFFB6A3)         // CTAs and highlights
```

### Typography
- **BelfastGroteskBlackFamily**: Headlines, stats, large numbers
- **RingiftFamily**: Page titles, tab navigation
- **RoundedCornerShape(24.dp)**: Standard for inputs, buttons, cards

### UI Patterns
- **Floating Tab Bar**: Rounded bottom bar with 4 tabs (Home, Revisions, Friends, Settings)
- **Spinning Refresh Icons**: In top bar instead of overlay loading bars
- **Cache-First Loading**: Show cached data immediately, refresh in background
- **Sheet Modals**: `skipPartiallyExpanded = true` for full-height sheets
- **No Tab Animations**: Instant transitions between tabs (no slide animations)

## Key Features

### 1. Authentication
- **Login/Register**: JWT tokens stored in HTTP-only cookies
- **Profile Images**: Random cat images from TheCatAPI on first login
- **Local Caching**: Profile images downloaded and cached as files in `filesDir`
- **Persistent Auth**: Token validated on app launch

### 2. Home Tab
- **User Stats**: Current streak, total XP, solves count
- **Recent Solves**: Last 50 submissions with difficulty indicators
- **Achievements**: Progress tracking with visual indicators
- **Freeze Dates**: Calendar of used streak freezes

### 3. Revisions Tab
- **Smart Scheduling**: Normal (Ebbinghaus curve) and ML-based modes
- **Premium Check**: ML mode requires active subscription
- **Grouping**: "Due Now", "Due Soon", "Upcoming"
- **Actions**: Complete revision, delete, record attempts
- **Subscription Dialog**: Shows when non-premium user tries ML mode

### 4. Friends Tab
- **Leaderboard**: Friends ranked by total XP with Belfast typography
- **Friend Requests**: Send, accept, reject
- **User Search**: Debounced search with cached results
- **Friend Profiles**: Solves, achievements, gift freeze option
- **Gift Freeze**: Costs 70 XP, sends 1 freeze to friend

### 5. Settings Tab
- **Profile Management**: Edit username, email (rounded 24.dp inputs)
- **Password Change**: Secure with rounded 24.dp inputs
- **Freeze Shop**: Purchase freezes with XP
- **Logout**: Clears all cache including local image files
- **Delete Account**: Soft delete with 7-day recovery period

## Caching Strategy

### TTL Values
- **Short (15 min)**: User stats, solves, revisions, friends
- **Long (1 hour)**: Achievements, freeze dates
- **Permanent**: Revision mode preference, profile image file paths
- **Daily Check**: Subscription status (24-hour cache)

### Cache Keys
```kotlin
// Home
KEY_USER_STATS, KEY_SOLVE_STATS, KEY_RECENT_SOLVES
KEY_ACHIEVEMENT_STATS, KEY_ALL_ACHIEVEMENTS, KEY_FREEZE_DATES

// Revisions
KEY_REVISION_GROUPS_normal, KEY_REVISION_GROUPS_ml
KEY_REVISION_STATS_normal, KEY_REVISION_STATS_ml
KEY_REVISION_MODE

// Friends
KEY_FRIENDS, KEY_RECEIVED_REQUESTS, KEY_SENT_REQUESTS
KEY_USER_PROFILE_{username}, KEY_FRIEND_SOLVES_{username}
KEY_FRIEND_ACHIEVEMENTS_{username}

// Subscription
KEY_SUBSCRIPTION_STATUS, KEY_SUBSCRIPTION_LAST_CHECK

// Profile Images
KEY_PROFILE_IMAGE, KEY_PROFILE_IMAGE_file
```

### Cache Invalidation
- **Logout**: `clearAllCache()` - deletes everything including local image files
- **Submission**: Invalidates home stats and solves
- **Revision Complete**: Invalidates revision cache for current mode
- **Friend Action**: Invalidates friends cache

## API Integration

### Base URL
```
https://traverse-backend-api.azurewebsites.net/api/
```

### Authentication
- JWT token in HTTP-only cookies (`auth_token`)
- OkHttp interceptor handles cookie persistence
- Automatic retry with token refresh (if needed)

### Key Endpoints
```
POST /auth/login
POST /auth/register
GET  /auth/me
PATCH /auth/profile
POST /auth/change-password
DELETE /auth/account

GET  /auth/me/stats
GET  /solves/stats/summary
GET  /solves
GET  /achievements

GET  /revisions/grouped?type=normal|ml
GET  /revisions/stats?type=normal|ml
POST /revisions/{id}/complete
DELETE /revisions/{id}

GET  /friends
GET  /friends/requests/received
POST /friends/request
POST /friends/accept
POST /friends/reject
DELETE /friends/{username}

GET  /users/search?query=...
GET  /users/{username}
GET  /friends/{username}/solves
GET  /friends/{username}/achievements

GET  /users/me/freezes
POST /users/me/freezes/purchase
POST /users/{username}/freezes/gift

GET  /subscription/status
```

## Critical Implementation Rules

### 1. Subscription & Premium Features
- **ALWAYS** check `isSubscriptionActive` before enabling ML revision mode
- Cache subscription status for 24 hours, force-check when toggling ML
- Show upgrade dialog (`showProUpgradeDialog`) if user tries ML without subscription
- If ML is enabled but subscription expires, auto-switch to normal mode

### 2. Profile Image Handling
```kotlin
// First login: Fetch cat image and save locally
AuthViewModel.fetchRandomCatImage()
  → Downloads from TheCatAPI
  → Saves to context.filesDir
  → Caches file path in SharedPreferences
  → Uses Dispatchers.IO for background work

// Subsequent loads: Instant from local file
AsyncImage(
  model = localFilePath ?: imageUrl,
  contentDescription = null,
  modifier = Modifier.size(120.dp).clip(CircleShape)
)
```

### 3. Sheet Modals
```kotlin
val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true  // ALWAYS true for full height
)
```

### 4. Loading States
- **Initial Load**: Show loading indicator
- **Cached Load**: Show data immediately, refresh in background
- **Refresh**: Spinning icon in top bar, NOT overlay bars
- **Callback Pattern**: `onSave: (String?) -> Unit` to properly stop loading

### 5. Error Handling
```kotlin
when (val result = networkService.someCall()) {
    is NetworkResult.Success -> {
        // Update cache
        cacheManager.cacheSomeData(result.data)
        // Update UI state
        _uiState.update { it.copy(data = result.data) }
    }
    is NetworkResult.Error -> {
        _uiState.update { it.copy(errorMessage = result.message) }
    }
}
```

### 6. Navigation
- **No Animations**: Remove all `enterTransition`, `exitTransition` from NavHost
- **State Preservation**: `saveState = true`, `restoreState = true`
- **Single Top**: `launchSingleTop = true` to prevent duplicate screens

## Data Flow Patterns

### ViewModel → UI
```kotlin
// ViewModel
private val _uiState = MutableStateFlow(SomeUiState())
val uiState: StateFlow<SomeUiState> = _uiState.asStateFlow()

// Composable
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### User Action → Network → Cache → UI
```kotlin
fun doAction() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        
        when (val result = networkService.performAction()) {
            is NetworkResult.Success -> {
                cacheManager.cacheData(result.data)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        data = result.data
                    ) 
                }
            }
            is NetworkResult.Error -> {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = result.message
                    ) 
                }
            }
        }
    }
}
```

### Cache-First Loading
```kotlin
fun loadData(forceRefresh: Boolean = false) {
    if (!forceRefresh) {
        val cached = cacheManager.getCachedData()
        if (cached != null) {
            _uiState.update { it.copy(data = cached, isFromCache = true) }
            refreshInBackground()  // Update in background
            return
        }
    }
    
    // No cache or force refresh - load from network
    loadFromNetwork()
}
```

## Common Pitfalls to Avoid

### ❌ Don't Do This
```kotlin
// Missing subscription check
fun toggleMLMode() {
    _uiState.update { it.copy(useMLMode = !it.useMLMode) }
}

// Not using Dispatchers.IO for file operations
fun downloadImage(url: String) {
    val data = URL(url).readBytes()  // Blocks main thread!
}

// Not clearing local files on logout
fun logout() {
    cacheManager.clearAllCache()  // Missing file cleanup
}

// Animating tab switches (causes weird left/right slides)
NavHost(
    enterTransition = { slideInHorizontally() }
)
```

### ✅ Do This Instead
```kotlin
// Check subscription before enabling ML
fun toggleMLMode() {
    if (!uiState.value.useMLMode && !uiState.value.isSubscribed) {
        checkSubscriptionAndEnableML()
    } else {
        disableMLMode()
    }
}

// Use Dispatchers.IO for network/file operations
suspend fun downloadImage(url: String) = withContext(Dispatchers.IO) {
    val data = java.net.URL(url).readBytes()
    // ...
}

// Clear local files on logout
fun clearAllCache() {
    getProfileImageFile()?.let { java.io.File(it).delete() }
    prefs.edit().clear().apply()
}

// No animations for tab navigation
NavHost(
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None }
)
```

## Testing & Debugging

### Common Issues
1. **"ML mode not working"** → Check subscription status cached correctly
2. **"Profile image loads slowly"** → Verify local file caching is working
3. **"Sheets don't open fully"** → Ensure `skipPartiallyExpanded = true`
4. **"Tab animation glitches"** → Remove all transitions from NavHost
5. **"Loading never stops"** → Check callback pattern with `onComplete: (String?) -> Unit`

### Debug Checklist
- [ ] Subscription status checked before enabling ML mode
- [ ] Profile images cached as local files, not just URLs
- [ ] All cache cleared on logout (including files)
- [ ] Modal sheets use `skipPartiallyExpanded = true`
- [ ] No animations in tab navigation
- [ ] Rounded corners (24.dp) on all inputs
- [ ] Belfast font for headlines, Ringift for page titles
- [ ] Spinning refresh icon in top bar, not overlay

## Environment
- **Min SDK**: 24
- **Target SDK**: 34
- **Kotlin**: 1.9+
- **Compose**: 1.5+
- **Coil**: 2.6.0

## Dependencies
```gradle
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("androidx.navigation:navigation-compose:2.7.+")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.+")
```

---

**Remember**: This is a premium LeetCode tracking app. Security, performance, and UX polish are critical. Always check subscription status for premium features, cache aggressively for instant loading, and maintain the clean monochromatic pastel aesthetic throughout.
