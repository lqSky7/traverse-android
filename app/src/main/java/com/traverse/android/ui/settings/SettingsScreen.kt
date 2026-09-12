package com.traverse.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.traverse.android.BuildConfig
import com.traverse.android.data.*
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.ColorPaletteManager
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import kotlinx.coroutines.launch

// SwiftUI system colours, used wherever iOS reaches for a literal rather than the palette
// (e.g. `.green` for the used-freeze checkmark, `.orange` for the insufficient-XP warning).
private val SwiftGreen = Color(0xFF34C759)
private val SwiftOrange = Color(0xFFFF9500)
private val SwiftRed = Color(0xFFFF3B30)
private val CardBackground = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }
    val cacheManager = remember { CacheManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val palette = rememberPalette()
    
    // State
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Dialog states
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showChangePasswordSheet by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFreezeShopSheet by remember { mutableStateOf(false) }
    var showCheckUpdatesDialog by remember { mutableStateOf(false) }
    var showGeminiKeyDialog by remember { mutableStateOf(false) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }
    var geminiKeyInput by remember { mutableStateOf(cacheManager.getGeminiApiKey() ?: "") }

    // Palette selector state (mirrors the iOS `SettingsView` @State block)
    var showImportPalette by remember { mutableStateOf(false) }
    var showHuePicker by remember { mutableStateOf(false) }
    var paletteInput by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    
    // Load user data and profile image
    LaunchedEffect(Unit) {
        isLoading = true
        // Get cached profile image (prefer local file)
        profileImageUrl = cacheManager.getProfileImageFile() ?: cacheManager.getProfileImage()
        
        when (val result = networkService.getCurrentUser()) {
            is NetworkResult.Success -> {
                user = result.data
            }
            is NetworkResult.Error -> {
                errorMessage = result.message
            }
        }
        isLoading = false
    }
    
    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "User",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Profile Card
            user?.let { currentUser ->
                UserProfileCard(
                    user = currentUser,
                    profileImageUrl = profileImageUrl
                )
            } ?: run {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Bento Settings Grid
            BentoSettingsGrid(
                onEditProfile = { showEditProfileSheet = true },
                onChangePassword = { showChangePasswordSheet = true },
                onLogout = { showLogoutDialog = true },
                onCheckUpdates = { showCheckUpdatesDialog = true },
                onDeleteAccount = { showDeleteAccountDialog = true },
                onFreezeShop = { showFreezeShopSheet = true },
                onCalendarSubscription = {
                    user?.let { u ->
                        val url = networkService.calendarFeedURL(u.username, u.calendarToken ?: "")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                },
                onGeminiApiKey = {
                    geminiKeyInput = cacheManager.getGeminiApiKey() ?: ""
                    showGeminiKeyDialog = true
                },
                onImportPalette = { showImportPalette = true },
                onHuePicker = { showHuePicker = true }
            )
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // Edit Profile Sheet
    if (showEditProfileSheet) {
        EditProfileSheet(
            user = user,
            onDismiss = { showEditProfileSheet = false },
            onSave = { email, visibility, maxDailyReviews, onComplete ->
                scope.launch {
                    when (val result = networkService.updateProfile(email, null, visibility, maxDailyReviews)) {
                        is NetworkResult.Success -> {
                            // Refresh user data
                            when (val userResult = networkService.getCurrentUser()) {
                                is NetworkResult.Success -> user = userResult.data
                                is NetworkResult.Error -> {}
                            }
                            onComplete(null)
                            showEditProfileSheet = false
                        }
                        is NetworkResult.Error -> {
                            onComplete(result.message)
                        }
                    }
                }
            }
        )
    }
    
    // Change Password Sheet
    if (showChangePasswordSheet) {
        ChangePasswordSheet(
            onDismiss = { showChangePasswordSheet = false },
            onSave = { currentPassword, newPassword, onComplete ->
                scope.launch {
                    when (val result = networkService.changePassword(currentPassword, newPassword)) {
                        is NetworkResult.Success -> {
                            // Sheet shows the success message and dismisses after 1.5s (iOS parity)
                            onComplete(null)
                        }
                        is NetworkResult.Error -> {
                            onComplete(result.message)
                        }
                    }
                }
            }
        )
    }
    
    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            networkService.logout()
                            cacheManager.clearAllCache()
                            showLogoutDialog = false
                            onLogout()
                        }
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Account Sheet
    if (showDeleteAccountDialog) {
        DeleteAccountSheet(
            errorMessage = errorMessage,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password ->
                scope.launch {
                    when (val result = networkService.deleteAccount(password)) {
                        is NetworkResult.Success -> {
                            cacheManager.clearAllCache()
                            showDeleteAccountDialog = false
                            onLogout()
                        }
                        is NetworkResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                }
            }
        )
    }
    
    // Check for Updates Dialog
    var latestReleaseUrl by remember { mutableStateOf<String?>(null) }
    if (showCheckUpdatesDialog) {
        AlertDialog(
            onDismissRequest = {
                showCheckUpdatesDialog = false
                updateCheckMessage = null
                latestReleaseUrl = null
            },
            title = { Text("Check for Updates") },
            text = {
                if (updateCheckMessage == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.colorAt(1))
                    }
                } else {
                    Text(updateCheckMessage!!)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (updateCheckMessage?.contains("available") == true) {
                            // Open GitHub release page
                            val url = latestReleaseUrl ?: "https://github.com/lqSky7/traverse-android/releases/latest"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        showCheckUpdatesDialog = false
                        updateCheckMessage = null
                        latestReleaseUrl = null
                    }
                ) {
                    Text(if (updateCheckMessage?.contains("available") == true) "Download" else "Done")
                }
            }
        )
        
        // Fetch latest version when dialog opens
        LaunchedEffect(showCheckUpdatesDialog) {
            if (showCheckUpdatesDialog && updateCheckMessage == null) {
                try {
                    val currentVersion = BuildConfig.VERSION_NAME
                    when (val result = networkService.getLatestRelease()) {
                        is NetworkResult.Success -> {
                            val latestVersion = result.data.version
                            latestReleaseUrl = result.data.htmlUrl
                            val isUpdateAvailable = isVersionNewer(latestVersion, currentVersion)
                            updateCheckMessage = if (isUpdateAvailable) {
                                "New version $latestVersion available\n\nYou are currently on version $currentVersion.\n\nTap Download to get the latest update from GitHub."
                            } else {
                                "You are on version $currentVersion (latest)\n\nNo updates available."
                            }
                        }
                        is NetworkResult.Error -> {
                            updateCheckMessage = "Unable to check for updates: ${result.message}\n\nPlease try again later."
                        }
                    }
                } catch (e: Exception) {
                    updateCheckMessage = "Unable to check for updates.\n\nPlease check your connection and try again."
                }
            }
        }
    }
    
    // Freeze Shop Sheet
    if (showFreezeShopSheet) {
        FreezeShopSheet(
            userXp = user?.totalXp ?: 0,
            onDismiss = { showFreezeShopSheet = false },
            onXpUpdated = { newXp ->
                // Update user XP locally
                user = user?.copy(totalXp = newXp)
            }
        )
    }

    // Gemini API Key Dialog
    if (showGeminiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGeminiKeyDialog = false },
            title = {
                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Used to power the AI Revision Coach & Hints on revision cards.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Get free Gemini API Key", color = palette.colorAt(1))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = palette.colorAt(1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cacheManager.cacheGeminiApiKey(geminiKeyInput.trim())
                        showGeminiKeyDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGeminiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Palette Sheet (iOS `showingImportPalette` → `ImportPaletteView`)
    if (showImportPalette) {
        ImportPaletteSheet(
            paletteInput = paletteInput,
            onPaletteInputChange = { paletteInput = it },
            importError = importError,
            onImport = {
                if (ColorPaletteManager.importPalette(paletteInput)) {
                    showImportPalette = false
                    paletteInput = ""
                    importError = null
                } else {
                    importError =
                        "Invalid palette format. Please provide a coolors.co URL or SCSS colors."
                }
            },
            onDismiss = { showImportPalette = false }
        )
    }

    // Hue Picker Sheet (iOS `showingDreamPicker` → `HuePicker`)
    if (showHuePicker) {
        HuePickerSheet(onDismiss = { showHuePicker = false })
    }
    
    // Error Snackbar
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }
    }
}

@Composable
private fun UserProfileCard(
    user: User,
    profileImageUrl: String?,
    modifier: Modifier = Modifier
) {
    // iOS `SettingsView` tints "Day Streak" with `color(at: 0)` and "Total XP" with `color(at: 1)`.
    val palette = rememberPalette()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Profile image background (cat pic)
            if (profileImageUrl != null) {
                AsyncImage(
                    model = if (profileImageUrl!!.startsWith("/")) {
                        // Local file path
                        java.io.File(profileImageUrl!!)
                    } else {
                        // URL
                        profileImageUrl
                    },
                    contentDescription = "Profile background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Gray.copy(alpha = 0.3f),
                                    Color.Gray.copy(alpha = 0.1f)
                                )
                            )
                        )
                )
            }
            
            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // User info
                Column {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = BelfastGroteskBlackFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                    Text(
                        text = user.email ?: "No email",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    StatColumn(
                        value = "${user.currentStreak}",
                        label = "Day Streak",
                        color = palette.colorAt(0)
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    StatColumn(
                        value = "${user.totalXp}",
                        label = "Total XP",
                        color = palette.colorAt(1)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = BelfastGroteskBlackFamily,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun BentoSettingsGrid(
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDeleteAccount: () -> Unit,
    onFreezeShop: () -> Unit,
    onCalendarSubscription: () -> Unit,
    onGeminiApiKey: () -> Unit,
    onImportPalette: () -> Unit,
    onHuePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Every tile icon follows the selected palette exactly like `paletteManager.color(at:)` on iOS.
    val palette = rememberPalette()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column {
            // Row 1: Palette | Hue Picker
            Row(modifier = Modifier.height(140.dp)) {
                PaletteTile(modifier = Modifier.weight(1f))

                VerticalDivider()

                BentoCell(
                    icon = Icons.Default.Palette,
                    iconColor = palette.colorAt(1),
                    title = "Hue Picker",
                    subtitle = "Pick a vibe",
                    onClick = onHuePicker,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 2: Import | Profile
            Row(modifier = Modifier.height(140.dp)) {
                BentoCell(
                    icon = Icons.Default.ArrowCircleDown,
                    iconColor = palette.colorAt(2),
                    title = "Import",
                    subtitle = "Custom palette",
                    onClick = onImportPalette,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider()

                BentoCell(
                    icon = Icons.Default.AccountCircle,
                    iconColor = palette.colorAt(3),
                    title = "Profile",
                    subtitle = "Edit details",
                    onClick = onEditProfile,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 3: Security | Logout
            Row(modifier = Modifier.height(140.dp)) {
                BentoCell(
                    icon = Icons.Default.Lock,
                    iconColor = palette.colorAt(4),
                    title = "Security",
                    subtitle = "Change password",
                    onClick = onChangePassword,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider()

                BentoCell(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconColor = Color.Red,
                    title = "Logout",
                    subtitle = "Sign out",
                    onClick = onLogout,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 4: Freeze Shop (full width)
            BentoCellWide(
                icon = Icons.Default.AcUnit,
                iconColor = palette.colorAt(0),
                title = "Freeze Shop",
                subtitle = "Protect your streak with freezes",
                onClick = onFreezeShop
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 5: Calendar Subscription (full width)
            BentoCellWide(
                icon = Icons.Default.CalendarMonth,
                iconColor = palette.colorAt(1),
                title = "Calendar Subscription",
                subtitle = "Sync revisions to your Calendar app",
                onClick = onCalendarSubscription
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 6: AI Revision Coach (Android-only, full width)
            BentoCellWide(
                icon = Icons.Default.AutoAwesome,
                iconColor = palette.colorAt(3),
                title = "AI Revision Coach",
                subtitle = "Configure Gemini API key for hints",
                onClick = onGeminiApiKey
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 7: Check for Updates (Android-only, full width)
            BentoCellWide(
                icon = Icons.Default.Download,
                iconColor = palette.colorAt(1),
                title = "Check for Updates",
                subtitle = "Update to the latest version",
                onClick = onCheckUpdates
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Row 8: Delete Account (full width, danger)
            BentoDeleteCell(onClick = onDeleteAccount)
        }
    }
}

@Composable
private fun RowScope.VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.1f))
    )
}

@Composable
private fun BentoCell(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun BentoCellWide(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = iconColor.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Palette tile, mirroring the iOS bento grid's "Palette" cell exactly: the icon slot holds a
 * preview of the selected palette's first four colours (22dp circles, 1dp white-20% stroke,
 * 6dp apart) and the label shows "Palette" plus the selected palette's name. Tapping opens the
 * same `Menu` of [ColorPaletteManager.allAvailablePalettes] — each entry is the palette name
 * with a checkmark on the current selection.
 */
@Composable
private fun PaletteTile(modifier: Modifier = Modifier) {
    val palette = rememberPalette()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // BentoCell's icon slot — the four-colour swatch preview
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                palette.swiftUIColors.take(4).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }

            // BentoCell's label slot
            Column {
                Text(
                    text = "Palette",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = palette.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ColorPaletteManager.allAvailablePalettes.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = option.name, color = Color.White)
                            if (option.id == palette.id) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        ColorPaletteManager.selectPalette(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Full-width destructive row, mirroring iOS `BentoSettingsGrid`'s "Delete Account" cell:
 * a 20dp trash icon, a single red subheadline label (no subtitle) and a red chevron.
 */
@Composable
private fun BentoDeleteCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = "Delete Account",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color.Red
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Red.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (email: String?, visibility: String?, maxDailyReviews: Int?, onComplete: (String?) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf(user?.email ?: "") }
    var visibility by remember { mutableStateOf(user?.visibility ?: "PUBLIC") }
    var maxDailyReviews by remember { mutableFloatStateOf(user?.maxDailyReviews?.toFloat() ?: 20f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // iOS `ProfileEditView` tints the shade-tone value, slider and Save button with `primary`.
    val palette = rememberPalette()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val roundedShape = RoundedCornerShape(24.dp)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(rememberSheetOverscrollClamper())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = roundedShape
            )
            
            // Visibility selector — iOS `ProfileEditView` tags: "public" / "private" / "friends"
            Column {
                Text(
                    text = "Profile Visibility",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // These tags are sent verbatim to `PATCH /auth/profile`, which only accepts the
                    // lowercase forms — see `canonicalVisibility`.
                    listOf(
                        "public" to "Public",
                        "private" to "Private",
                        "friends" to "Friends"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = visibility == value,
                            onClick = { visibility = value },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Max Daily Reviews Target
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Review Limit",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${maxDailyReviews.toInt()} problems",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    )
                }
                Slider(
                    value = maxDailyReviews,
                    onValueChange = { maxDailyReviews = it },
                    valueRange = 5f..50f,
                    steps = 8
                )
            }

            // Theme Preferences — iOS `ProfileEditView` Section("Theme Preferences")
            Text(
                text = "Theme Preferences",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shade Tone",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = vibrancyLabel(ColorPaletteManager.vibrancy),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pastel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                    Slider(
                        value = ColorPaletteManager.vibrancy.toFloat(),
                        onValueChange = { ColorPaletteManager.vibrancy = it.toDouble() },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Bright",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = SwiftRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = SwiftGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    onSave(
                        email.ifBlank { null },
                        visibility,
                        maxDailyReviews.toInt()
                    ) { error ->
                        isLoading = false
                        if (error == null) {
                            // iOS: shows "Profile updated successfully" then dismisses after 1.5s
                            successMessage = "Profile updated successfully"
                            scope.launch {
                                kotlinx.coroutines.delay(1500)
                                onDismiss()
                            }
                        } else {
                            errorMessage = error
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = Color.Black
                ),
                shape = roundedShape
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text("Save Changes")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** iOS `ChangePasswordView.passwordStrength`: up to 4 points for length, uppercase, digit, symbol. */
private fun passwordStrengthScore(password: String): Int {
    var strength = 0
    if (password.length >= 8) strength += 1
    if (password.any { it.isUpperCase() }) strength += 1
    if (password.any { it.isDigit() }) strength += 1
    if (password.any { it in "!@#$%^&*()_+-=[]{}|;:,.<>?" }) strength += 1
    return strength
}

/** iOS `ChangePasswordView.strengthColor`. */
private fun passwordStrengthColor(strength: Int): Color = when (strength) {
    0, 1 -> SwiftRed
    2 -> SwiftOrange
    3 -> Color(0xFFFFCC00) // iOS `.yellow`
    4 -> SwiftGreen
    else -> Color.Gray
}

/** iOS `ChangePasswordView.strengthText`. */
private fun passwordStrengthText(strength: Int): String = when (strength) {
    0, 1 -> "Weak"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Strong"
    else -> ""
}

/** iOS `ProfileEditView.vibrancyLabel`. */
private fun vibrancyLabel(vibrancy: Double): String = when {
    vibrancy < 0.35 -> "Pastel"
    vibrancy < 0.70 -> "Balanced"
    else -> "Bright"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(
    onDismiss: () -> Unit,
    onSave: (currentPassword: String, newPassword: String, onComplete: (String?) -> Unit) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // iOS `ChangePasswordView` tints the submit button with `color(at: 2)`.
    val palette = rememberPalette()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val roundedShape = RoundedCornerShape(24.dp)

    // iOS isFormValid: all three non-empty, new == confirm, and new has >= 8 characters.
    val isFormValid = currentPassword.isNotEmpty() &&
        newPassword.isNotEmpty() &&
        confirmPassword.isNotEmpty() &&
        newPassword == confirmPassword &&
        newPassword.length >= 8

    val strength = passwordStrengthScore(newPassword)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(rememberSheetOverscrollClamper())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = roundedShape,
                visualTransformation = PasswordVisualTransformation()
            )
            
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = roundedShape,
                visualTransformation = PasswordVisualTransformation()
            )
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = roundedShape,
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
            )

            // Password Strength Indicator — iOS renders this only when newPassword is non-empty
            if (newPassword.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Password Strength",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index < strength) passwordStrengthColor(strength)
                                    else Color.Gray.copy(alpha = 0.3f)
                                )
                        )
                    }
                    Text(
                        text = passwordStrengthText(strength),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = passwordStrengthColor(strength)
                        )
                    )
                }
            }
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = SwiftRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = SwiftGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    onSave(currentPassword, newPassword) { error ->
                        isLoading = false
                        if (error == null) {
                            // iOS: successMessage then dismiss after 1.5s
                            successMessage = "Password changed successfully"
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            scope.launch {
                                kotlinx.coroutines.delay(1500)
                                onDismiss()
                            }
                        } else {
                            errorMessage = error
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.colorAt(2),
                    contentColor = Color.Black
                ),
                shape = roundedShape
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text("Change Password")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * iOS `DeleteAccountView`: red warning header, "What happens when you delete your
 * account" list, password + type-DELETE gates, "Delete My Account" button, recovery
 * footer, and a final "Final Confirmation" alert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountSheet(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var password by remember { mutableStateOf("") }
    var confirmationText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showFinalConfirmation by remember { mutableStateOf(false) }

    // iOS isFormValid: !password.isEmpty && confirmationText.uppercased() == "DELETE"
    val isFormValid = password.isNotEmpty() && confirmationText.uppercase() == "DELETE"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(rememberSheetOverscrollClamper())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header — iOS navigationTitle("Delete Account")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily,
                        color = Color.White
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Warning header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SwiftRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SwiftRed
                    )
                )
            }

            Text(
                text = "This action cannot be undone. Your account will be marked for " +
                    "deletion and you have 7 days to recover it.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )
            )

            // "What happens when you delete your account:"
            Text(
                text = "What happens when you delete your account:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            DeleteWarningRow(Icons.Default.Lock, "Your account will be inaccessible immediately")
            DeleteWarningRow(Icons.Default.Schedule, "You have 7 days to recover your account")
            DeleteWarningRow(Icons.Default.Delete, "After 7 days, all data will be permanently deleted")

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // "Confirm Your Password"
            Text(
                text = "Confirm Your Password",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Enter your password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            // "Type 'DELETE' to confirm"
            Text(
                text = "Type 'DELETE' to confirm",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            OutlinedTextField(
                value = confirmationText,
                onValueChange = { confirmationText = it.uppercase() },
                placeholder = { Text("Type DELETE") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(color = SwiftRed)
                )
            }

            Button(
                onClick = { showFinalConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = isFormValid && !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SwiftRed,
                    disabledContainerColor = SwiftRed.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Delete My Account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            // Recovery footer
            Text(
                text = "Need to recover your account?",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "You can recover within 7 days by logging in again",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }

    // Final confirmation — iOS .alert("Final Confirmation")
    if (showFinalConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinalConfirmation = false },
            title = { Text("Final Confirmation") },
            text = {
                Text(
                    "Are you absolutely sure you want to delete your account? This cannot " +
                        "be undone and you will lose all your data after 7 days."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinalConfirmation = false
                        isLoading = true
                        onConfirm(password)
                    }
                ) {
                    Text("Delete Account", color = SwiftRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeleteWarningRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SwiftRed,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreezeShopSheet(
    userXp: Int,
    onDismiss: () -> Unit,
    onXpUpdated: (Int) -> Unit
) {
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }
    val scope = rememberCoroutineScope()
    // iOS `FreezeShopSheet` tints the snowflake, XP balance, info button, purchase button,
    // quantity selector and info rows with `selectedPalette.primary`.
    val palette = rememberPalette()
    
    var freezeInfo by remember { mutableStateOf<FreezeInfoResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPurchasing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var selectedCount by remember { mutableIntStateOf(1) }
    var currentXp by remember { mutableIntStateOf(userXp) }
    var showInfo by remember { mutableStateOf(false) }
    
    val freezeCost = freezeInfo?.costs?.purchase ?: 100
    val totalCost = selectedCount * freezeCost
    val canAfford = currentXp >= totalCost
    
    // Load freeze info
    LaunchedEffect(Unit) {
        isLoading = true
        when (val result = networkService.getFreezeInfo()) {
            is NetworkResult.Success -> {
                freezeInfo = result.data
            }
            is NetworkResult.Error -> {
                errorMessage = result.message
            }
        }
        isLoading = false
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(rememberSheetOverscrollClamper())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Freeze Shop",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = BelfastGroteskBlackFamily,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
            
            // Freeze Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Column {
                                Text(
                                    text = "${freezeInfo?.availableFreezes ?: 0}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = BelfastGroteskBlackFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Available Freezes",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currentXp",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = BelfastGroteskBlackFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                            )
                            Text(
                                text = "XP Balance",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    
                    // Used freezes info
                    if ((freezeInfo?.usedFreezes ?: 0) > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SwiftGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${freezeInfo?.usedFreezes} freeze${if ((freezeInfo?.usedFreezes ?: 0) == 1) "" else "s"} used to save your streak",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
            
            // Purchase Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Purchase Freezes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        
                        IconButton(onClick = { showInfo = !showInfo }) {
                            Icon(
                                imageVector = if (showInfo) Icons.Default.Info else Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = palette.primary
                            )
                        }
                    }
                    
                    // Expandable info section
                    if (showInfo) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FreezeInfoRow(
                                icon = Icons.Default.CalendarToday,
                                text = "Freezes are automatically used when you miss a day"
                            )
                            FreezeInfoRow(
                                icon = Icons.Default.LocalFireDepartment,
                                text = "Your streak is preserved instead of resetting to 0"
                            )
                            FreezeInfoRow(
                                icon = Icons.Default.CardGiftcard,
                                text = "Gift freezes to friends for 70 XP each"
                            )
                        }
                    }
                    
                    // Quantity selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..5).forEach { count ->
                            QuantityButton(
                                count = count,
                                isSelected = selectedCount == count,
                                onClick = { selectedCount = count }
                            )
                        }
                    }
                    
                    // Purchase button
                    Button(
                        onClick = {
                            scope.launch {
                                isPurchasing = true
                                errorMessage = null
                                successMessage = null
                                
                                when (val result = networkService.purchaseFreezes(selectedCount)) {
                                    is NetworkResult.Success -> {
                                        successMessage = result.data.message
                                        currentXp = result.data.remainingXp
                                        onXpUpdated(result.data.remainingXp)
                                        // Refresh freeze info
                                        when (val refreshResult = networkService.getFreezeInfo()) {
                                            is NetworkResult.Success -> freezeInfo = refreshResult.data
                                            is NetworkResult.Error -> {}
                                        }
                                    }
                                    is NetworkResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                isPurchasing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canAfford && !isPurchasing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) palette.primary else Color.Gray,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPurchasing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Purchase for $totalCost XP",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    // Not enough XP warning
                    if (!canAfford) {
                        Text(
                            text = "Not enough XP. You need ${totalCost - currentXp} more XP.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SwiftOrange
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Success/Error messages
                    successMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SwiftGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SwiftRed
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuantityButton(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // iOS `circleQuantityButton` — primary-20% fill plus a 2pt primary stroke when selected.
    val palette = rememberPalette()

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) palette.primary.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) palette.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = BelfastGroteskBlackFamily,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) palette.primary else Color.White
            )
        )
    }
}

@Composable
private fun FreezeInfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = rememberPalette().primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.7f)
            )
        )
    }
}
/**
 * Compare two semantic versions to determine if versionNew is newer than versionCurrent
 * e.g., isVersionNewer("1.5", "1.4") returns true
 */
private fun isVersionNewer(versionNew: String, versionCurrent: String): Boolean {
    return try {
        val cleanNew = versionNew.split("-")[0].removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = versionCurrent.split("-")[0].removePrefix("v").removePrefix("V").trim()
        val newParts = cleanNew.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        
        // Compare version parts
        val maxLen = maxOf(newParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val newPart = newParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            
            when {
                newPart > currentPart -> return true
                newPart < currentPart -> return false
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}