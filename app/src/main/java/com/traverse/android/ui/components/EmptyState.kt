package com.traverse.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.theme.rememberPalette

private val EmptyStateCardBackground = Color(0xFF1A1A1A)

/**
 * Where a brand-new account has to go. Mirrors iOS `TraverseLinks`.
 *
 * Nothing in this app can be filled in from inside the app: every solve, attempt and
 * revision arrives from the browser extension. These are the only two URLs a zero state
 * ever needs.
 */
object TraverseLinks {
    /** The install-and-sign-in page. Explains the extension and links the store. */
    const val DOWNLOADS = "https://leet-feedback.vercel.app/downloads"

    /** Direct store listing, for a user who already knows what they want. */
    const val EXTENSION_STORE =
        "https://chromewebstore.google.com/detail/traverse/nnapafjmoelkehjedfgjchoeelgbiama"
}

/**
 * Icon, headline, one sentence of explanation, and an optional action. Mirrors iOS
 * `EmptyStateView`.
 *
 * Used wherever a list or chart legitimately has nothing to show. Deliberately not a
 * full-screen `ContentUnavailableView` equivalent: most of these sit inside a card or a
 * half-filled scrolling column, so the padding is sized for that.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    /** Tighter spacing and smaller type, for use inside a card rather than a whole screen. */
    compact: Boolean = false
) {
    val palette = rememberPalette()
    val accent = palette.primary
    val circleSize = if (compact) 52.dp else 72.dp
    val iconSize = if (compact) 21.dp else 30.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 12.dp else 24.dp, vertical = if (compact) 22.dp else 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(iconSize)
            )
        }

        Text(
            text = title,
            fontSize = if (compact) 15.sp else 19.sp,
            lineHeight = if (compact) 19.sp else 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = message,
            fontSize = if (compact) 13.sp else 14.sp,
            lineHeight = if (compact) 18.sp else 20.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        if (actionTitle != null && onAction != null) {
            Button(
                onClick = onAction,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = actionTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * What Home, Problems and Revisions show before the account has any data at all. Mirrors
 * iOS `GettingStartedEmptyState`.
 *
 * The three steps are in the order they have to happen, because the ordering is the part
 * that is not obvious: the extension has to be installed *before* the next problem is
 * solved, or that solve is lost and the user concludes the app is broken rather than that
 * they skipped a step.
 */
@Composable
fun GettingStartedEmptyState(
    modifier: Modifier = Modifier,
    title: String = "Nothing here yet",
    message: String = "Traverse records what you do in the browser and reports it back here. " +
        "Nothing to show until your first solve."
) {
    val palette = rememberPalette()
    val accent = palette.primary
    val uriHandler = LocalUriHandler.current

    val steps = listOf(
        Triple(
            Icons.Filled.Extension,
            "Install the extension",
            "It watches your LeetCode and GeeksforGeeks submissions in Chrome."
        ),
        Triple(
            Icons.Filled.Code,
            "Solve a problem as usual",
            "Your attempts, timing and code are captured while you work."
        ),
        Triple(
            Icons.Filled.Autorenew,
            "Come back here",
            "Your streak, revision load and analytics fill in from the first solve."
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Extension,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = title,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EmptyStateCardBackground)
                .padding(horizontal = 14.dp)
        ) {
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.first,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = step.second,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = step.third,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }

                if (index < steps.lastIndex) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { uriHandler.openUri(TraverseLinks.EXTENSION_STORE) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Install the Extension",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            TextButton(
                onClick = { uriHandler.openUri(TraverseLinks.DOWNLOADS) },
                shape = CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = accent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Set-up guide",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
