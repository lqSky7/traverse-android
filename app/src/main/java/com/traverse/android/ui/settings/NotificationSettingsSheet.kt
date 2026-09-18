package com.traverse.android.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traverse.android.data.NotificationType
import com.traverse.android.data.NotificationTypePreference
import com.traverse.android.data.QuietHours
import com.traverse.android.data.UpdateTypePreference
import com.traverse.android.ui.theme.rememberPalette
import com.traverse.android.viewmodel.NotificationsViewModel
import kotlinx.coroutines.launch

private val CardBg = Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSheet(
    viewModel: NotificationsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = rememberPalette()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        viewModel.loadPreferences()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(48.dp))

                Text(
                    text = "Notifications",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            // 1. Permission status card (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Turn on notifications",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Get told when a friend request lands, an award unlocks, or you are one solve from closing your rings.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Allow", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. Push unavailable notice
            if (!uiState.pushConfigured) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Push delivery is not switched on for this build yet. Your inbox, badge and these settings all work — banners will start arriving once it is enabled.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Types Section
            Text(
                text = "WHAT TO NOTIFY ME ABOUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
            ) {
                val types = uiState.preferences.types
                types.forEachIndexed { index, entry ->
                    TypePreferenceRow(
                        preference = entry,
                        onPushChange = { pushVal ->
                            viewModel.updatePreferences(
                                types = listOf(UpdateTypePreference(type = entry.type, push = pushVal))
                            )
                        },
                        onInAppChange = { inAppVal ->
                            viewModel.updatePreferences(
                                types = listOf(UpdateTypePreference(type = entry.type, inApp = inAppVal))
                            )
                        }
                    )
                    if (index < types.size - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = 50.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Quiet Hours Section
            Text(
                text = "QUIET HOURS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            val quiet = uiState.preferences.quietHours
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hold notifications overnight",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "They arrive when the window ends rather than being dropped.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = quiet.enabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updatePreferences(
                                quietHours = quiet.copy(enabled = isChecked)
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.primary,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                if (quiet.enabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HourPicker(
                            title = "From",
                            selectedHour = quiet.start,
                            onSelectHour = { newStart ->
                                viewModel.updatePreferences(
                                    quietHours = quiet.copy(start = newStart)
                                )
                            }
                        )

                        HourPicker(
                            title = "Until",
                            selectedHour = quiet.end,
                            onSelectHour = { newEnd ->
                                viewModel.updatePreferences(
                                    quietHours = quiet.copy(end = newEnd)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Currently ${quiet.displayRange} in your timezone.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypePreferenceRow(
    preference: NotificationTypePreference,
    onPushChange: (Boolean) -> Unit,
    onInAppChange: (Boolean) -> Unit
) {
    val palette = rememberPalette()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = preference.knownType?.icon ?: NotificationType.iconFor(preference.type),
            contentDescription = null,
            tint = palette.colorAt(0),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preference.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = preference.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Two switches: Push and In-app
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Push",
                    fontSize = 11.sp,
                    color = if (preference.userConfigurable) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = preference.push,
                    onCheckedChange = onPushChange,
                    enabled = preference.userConfigurable,
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.primary,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "In-app",
                    fontSize = 11.sp,
                    color = if (preference.userConfigurable) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = preference.inApp,
                    onCheckedChange = onInAppChange,
                    enabled = preference.userConfigurable,
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.primary,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

@Composable
private fun HourPicker(
    title: String,
    selectedHour: Int,
    onSelectHour: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = QuietHours.hourLabel(selectedHour),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(CardBg)
            ) {
                (0..23).forEach { h ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = QuietHours.hourLabel(h),
                                color = if (h == selectedHour) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (h == selectedHour) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelectHour(h)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
