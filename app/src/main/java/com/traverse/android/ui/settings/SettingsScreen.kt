package com.traverse.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.traverse.android.BuildConfig
import com.traverse.android.data.*
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily
import kotlinx.coroutines.launch

// Pastel colors
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
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
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }
    
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
                onFreezeShop = { showFreezeShopSheet = true }
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
            onSave = { email, visibility, onComplete ->
                scope.launch {
                    when (val result = networkService.updateProfile(email, null, visibility)) {
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
                            onComplete(null)
                            showChangePasswordSheet = false
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
    
    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
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
    if (showCheckUpdatesDialog) {
        AlertDialog(
            onDismissRequest = {
                showCheckUpdatesDialog = false
                updateCheckMessage = null
            },
            title = { Text("Check for Updates") },
            text = {
                if (updateCheckMessage == null) {
                    CircularProgressIndicator()
                } else {
                    Text(updateCheckMessage!!)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (updateCheckMessage?.contains("available") == true) {
                            // Open GitHub releases page
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lqSky7/traverse-android/releases/latest"))
                            context.startActivity(intent)
                        }
                        showCheckUpdatesDialog = false
                        updateCheckMessage = null
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
                            val isUpdateAvailable = isVersionNewer(latestVersion, currentVersion)
                            updateCheckMessage = if (isUpdateAvailable) {
                                "New version $latestVersion available\n\nYou are on version $currentVersion.\n\nTap Download to get the latest version."
                            } else {
                                "You are on version $currentVersion (latest)\n\nNo updates available."
                            }
                        }
                        is NetworkResult.Error -> {
                            updateCheckMessage = "Unable to check for updates.\n\nPlease try again later."
                        }
                    }
                } catch (e: Exception) {
                    updateCheckMessage = "Unable to check for updates.\n\nPlease try again later."
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
                        color = AccentPastel
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
                        color = MediumPastel
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column {
            // Row 1: Profile | Security
            Row(modifier = Modifier.height(140.dp)) {
                BentoCell(
                    icon = Icons.Default.Person,
                    iconColor = AccentPastel,
                    title = "Profile",
                    subtitle = "Edit details",
                    onClick = onEditProfile,
                    modifier = Modifier.weight(1f)
                )
                
                VerticalDivider()
                
                BentoCell(
                    icon = Icons.Default.Lock,
                    iconColor = MediumPastel,
                    title = "Security",
                    subtitle = "Change password",
                    onClick = onChangePassword,
                    modifier = Modifier.weight(1f)
                )
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            // Row 2: Freeze Shop (full width)
            BentoCellWide(
                icon = Icons.Default.AcUnit,
                iconColor = Color.Cyan,
                title = "Freeze Shop",
                subtitle = "Protect your streak with freezes",
                onClick = onFreezeShop
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            // Row 3: Logout (full width)
            BentoCellWide(
                icon = Icons.AutoMirrored.Filled.Logout,
                iconColor = HardPastel,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = onLogout
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            // Row 4: Check for Updates (full width)
            BentoCellWide(
                icon = Icons.Default.Download,
                iconColor = AccentPastel,
                title = "Check for Updates",
                subtitle = "Update to the latest version",
                onClick = onCheckUpdates
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            // Row 5: Delete Account (full width, danger)
            BentoCellWide(
                icon = Icons.Default.Delete,
                iconColor = Color.Red,
                title = "Delete Account",
                subtitle = "Permanently remove your account",
                onClick = onDeleteAccount,
                isDanger = true
            )
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
    isDanger: Boolean = false,
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
                    color = if (isDanger) Color.Red else Color.White
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (email: String?, visibility: String?, onComplete: (String?) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf(user?.email ?: "") }
    var visibility by remember { mutableStateOf(user?.visibility ?: "public") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
            
            // Visibility selector
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
                    listOf("public", "friends", "private").forEach { option ->
                        FilterChip(
                            selected = visibility == option,
                            onClick = { visibility = option },
                            label = { Text(option.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onSave(
                        email.ifBlank { null },
                        visibility
                    ) { error ->
                        isLoading = false
                        errorMessage = error
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
                visualTransformation = PasswordVisualTransformation(),
                supportingText = { Text("Minimum 8 characters") }
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
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    when {
                        currentPassword.isBlank() -> errorMessage = "Current password is required"
                        newPassword.length < 8 -> errorMessage = "Password must be at least 8 characters"
                        newPassword != confirmPassword -> errorMessage = "Passwords do not match"
                        else -> {
                            isLoading = true
                            errorMessage = null
                            onSave(currentPassword, newPassword) { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && currentPassword.isNotBlank() && newPassword.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Change Password")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "This action cannot be undone. Your account will be marked for deletion and permanently removed after 7 days.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter your password to confirm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    onConfirm(password)
                },
                enabled = password.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete Account", color = Color.Red)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                                tint = Color.Cyan,
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
                                    color = AccentPastel
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
                                tint = EasyPastel,
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
                                imageVector = if (showInfo) Icons.Default.Info else Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color.Cyan
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
                                text = "Gift freezes to friends for ${freezeInfo?.costs?.gift ?: 70} XP each"
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
                            containerColor = if (canAfford) Color.Cyan else Color.Gray
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    
                    // Not enough XP warning
                    if (!canAfford) {
                        Text(
                            text = "Not enough XP. You need ${totalCost - currentXp} more XP.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumPastel
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Success/Error messages
                    successMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EasyPastel
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HardPastel
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
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                if (isSelected) Color.Cyan.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.05f)
            )
            .then(
                if (isSelected) Modifier.background(Color.Transparent)
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .then(
                    if (isSelected) Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Transparent)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Cyan.copy(alpha = 0.2f))
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Cyan else Color.White
                )
            )
        }
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
            tint = Color.Cyan,
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
        val newParts = versionNew.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = versionCurrent.split(".").mapNotNull { it.toIntOrNull() }
        
        // Compare version parts
        for (i in 0 until maxOf(newParts.size, currentParts.size)) {
            val newPart = newParts.getOrNull(i) ?: 0
            val currentPart = currentParts.getOrNull(i) ?: 0
            
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