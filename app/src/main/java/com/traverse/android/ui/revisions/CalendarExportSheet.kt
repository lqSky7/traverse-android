package com.traverse.android.ui.revisions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import com.traverse.android.ui.theme.palettePrimary

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 Kotlin port of iOS Calendar Feed Export Sheet.
 * Generates personal webcal feed URL for Google Calendar / Apple Calendar / Outlook integration.
 *
 * **The feed is gated by the *calendar* token, not the session token.** The
 * server checks `?token=` against `getCalendarToken(userId)` — an HMAC of the
 * user id that only the server can mint (`getCalendarToken` in `lib/auth.ts`).
 * This sheet used to build the URL from `TokenManager.getToken()`, the auth JWT,
 * which the feed endpoint rejects with a 401 — so every link the app handed out
 * was dead on arrival. It now reads the real token off the signed-in user.
 *
 * That puts a fetch behind the sheet, so it has three states rather than one:
 * asking, no link, and the link. The middle one is a real state and gets a real
 * empty state. Showing an empty box with a copy button under it would be worse
 * than saying nothing — the reader would copy nothing and blame their calendar
 * app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarExportSheet(
    username: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val networkService = remember { NetworkService.getInstance(context) }

    var calendarToken by remember(username) { mutableStateOf<String?>(null) }
    var isLoading by remember(username) { mutableStateOf(true) }
    // Bumped by "Try again"; part of the effect key, so it re-runs the fetch.
    var loadAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(username, loadAttempt) {
        isLoading = true
        calendarToken = when (val result = networkService.getCurrentUser()) {
            is NetworkResult.Success -> result.data.calendarToken
            is NetworkResult.Error -> null
        }
        isLoading = false
    }

    // A blank token is as useless as a missing one — the server would 401 either
    // way — so both collapse to "no link".
    val calendarFeedUrl = calendarToken
        ?.takeIf { it.isNotBlank() }
        ?.let { networkService.calendarFeedURL(username, it) }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = palettePrimary,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Calendar Feed Sync",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Subscribe to your personalized iCal feed to see upcoming problem revisions automatically on Google Calendar, Apple Calendar, or Outlook.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ordered so the link branch is a *positive* `!= null` check: Kotlin
            // smart-casts that for a local val without argument, whereas leaning
            // on the `else` of a subject-less `when` to imply non-null is the
            // kind of thing that costs a CI round trip to find out.
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = palettePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Getting your feed link…",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                calendarFeedUrl != null -> CalendarFeedLinkContent(
                    feedUrl = calendarFeedUrl,
                    onCopy = { copyFeedLink(context, calendarFeedUrl) }
                )

                // A real state, not a dead link. The token is minted server-side,
                // so if it did not arrive there is nothing to copy — and an empty
                // box with a copy button under it would send the reader off to
                // paste nothing into their calendar app, then blame the app.
                else -> {
                    EmptyStateView(
                        icon = Icons.Default.Link,
                        title = "Feed link unavailable",
                        message = "We couldn't get your calendar token. Check your connection and try again.",
                        actionTitle = "Try again",
                        onAction = { loadAttempt += 1 },
                        compact = true
                    )
                }
            }
        }
    }
}

/**
 * The link itself: the URL with its copy affordance, the how-to, and the big copy
 * button.
 *
 * Split out so the sheet's body stays a flat three-way branch. Inlining it put a
 * screenful of markup four levels deep inside a `when`, which is how the empty
 * and loading states end up being the ones nobody reads.
 */
@Composable
private fun CalendarFeedLinkContent(
    feedUrl: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // URL Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF242424),
            border = BorderStroke(1.dp, palettePrimary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = palettePrimary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = feedUrl,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = palettePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Instructions Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF242424)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How to Subscribe:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                InstructionStep(step = "1", text = "Copy the webcal feed link above.")
                InstructionStep(step = "2", text = "Open Google Calendar on Web -> Other calendars (+) -> From URL.")
                InstructionStep(step = "3", text = "Paste the link and click Add calendar.")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCopy,
            colors = ButtonDefaults.buttonColors(containerColor = palettePrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copy Feed Link",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/** Shared by the inline copy icon and the full-width button, so they cannot drift. */
private fun copyFeedLink(context: Context, feedUrl: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Traverse Calendar Feed", feedUrl))
    Toast.makeText(context, "Feed URL copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
private fun InstructionStep(step: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(palettePrimary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = palettePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
