package com.traverse.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.theme.SheetPanelBackground
import com.traverse.android.ui.theme.CardBackground
import com.traverse.android.data.AuthSession
import com.traverse.android.data.AuthSessionsResponse
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.ui.theme.rememberPalette
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Panel surface *inside* a sheet — see the note in `RingGoalsSheet`.
 *
 * This was `#1A1A1A` on a `#121212` sheet, which happened to work by accident. The sheet now uses
 * [CardBackground] like every other sheet, so the panels lift off it with a translucent white
 * instead of repeating its colour.
 */
private val SessionCardBackground = SheetPanelBackground
private val SessionDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsSheet(
    networkService: NetworkService,
    onDismiss: () -> Unit
) {
    val palette = rememberPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var response by remember { mutableStateOf<AuthSessionsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingRevoke by remember { mutableStateOf<AuthSession?>(null) }
    var confirmRevokeOthers by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun loadSessions() {
        isLoading = true
        error = null
        when (val result = networkService.getAuthSessions()) {
            is NetworkResult.Success -> response = result.data
            is NetworkResult.Error -> error = result.message
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadSessions() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Devices, contentDescription = null, tint = palette.colorAt(1))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Active sessions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = response?.let { "${it.sessions.size} of ${it.maxSessions} devices" } ?: "Manage devices signed in to your account",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp
                    )
                }
                TextButton(
                    enabled = !isLoading && !isWorking,
                    onClick = { scope.launch { loadSessions() } }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh sessions")
                }
            }

            notice?.let {
                Text(it, color = palette.colorAt(1), fontSize = 14.sp)
            }
            error?.takeIf { response != null }?.let {
                Text(it, color = Color(0xFFFF6961), fontSize = 14.sp)
            }

            when {
                isLoading && response == null -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = palette.colorAt(1)
                )
                error != null && response == null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(error.orEmpty(), color = Color.White.copy(alpha = 0.75f))
                    Button(onClick = { scope.launch { loadSessions() } }) { Text("Try again") }
                }
                response?.sessions.isNullOrEmpty() -> Text(
                    "No active sessions were found.",
                    color = Color.White.copy(alpha = 0.7f)
                )
                else -> {
                    response?.sessions.orEmpty().forEach { session ->
                        SessionCard(
                            session = session,
                            enabled = !isWorking,
                            onRevoke = { pendingRevoke = session }
                        )
                    }

                    if (response?.sessions.orEmpty().any { !it.isCurrent }) {
                        TextButton(
                            enabled = !isWorking,
                            onClick = { confirmRevokeOthers = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Sign out other devices", color = Color(0xFFFF6961))
                        }
                    }
                }
            }
        }
    }

    pendingRevoke?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            title = { Text("Sign out this device?") },
            text = { Text("${session.deviceName} will need to sign in again.") },
            confirmButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        pendingRevoke = null
                        scope.launch {
                            isWorking = true
                            when (val result = networkService.revokeAuthSession(session.id)) {
                                is NetworkResult.Success -> {
                                    notice = "Device signed out."
                                    loadSessions()
                                }
                                is NetworkResult.Error -> error = result.message
                            }
                            isWorking = false
                        }
                    }
                ) { Text("Sign out", color = Color(0xFFFF6961)) }
            },
            dismissButton = { TextButton(onClick = { pendingRevoke = null }) { Text("Cancel") } }
        )
    }

    if (confirmRevokeOthers) {
        AlertDialog(
            onDismissRequest = { confirmRevokeOthers = false },
            title = { Text("Sign out other devices?") },
            text = { Text("This device will stay signed in. Other active sessions will be revoked.") },
            confirmButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        confirmRevokeOthers = false
                        scope.launch {
                            isWorking = true
                            when (val result = networkService.revokeOtherAuthSessions()) {
                                is NetworkResult.Success -> {
                                    notice = "${result.data.revokedCount ?: 0} other session(s) signed out."
                                    loadSessions()
                                }
                                is NetworkResult.Error -> error = result.message
                            }
                            isWorking = false
                        }
                    }
                ) { Text("Sign out others", color = Color(0xFFFF6961)) }
            },
            dismissButton = { TextButton(onClick = { confirmRevokeOthers = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SessionCard(
    session: AuthSession,
    enabled: Boolean,
    onRevoke: () -> Unit
) {
    val palette = rememberPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SessionCardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = palette.colorAt(if (session.isCurrent) 0 else 1),
                modifier = Modifier.padding(top = 2.dp).size(21.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        session.deviceName.ifBlank { "Unknown device" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (session.isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "THIS DEVICE",
                            color = palette.colorAt(0),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.colorAt(0).copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    "Last active ${formatSessionDate(session.lastSeenAt)}",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 12.sp
                )
                session.ipAddress?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }
        }
        if (!session.isCurrent) {
            TextButton(
                enabled = enabled,
                onClick = onRevoke,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Revoke", color = Color(0xFFFF6961))
            }
        }
    }
}

private fun formatSessionDate(value: String): String = runCatching {
    OffsetDateTime.parse(value)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(SessionDateFormat)
}.getOrDefault(value)
