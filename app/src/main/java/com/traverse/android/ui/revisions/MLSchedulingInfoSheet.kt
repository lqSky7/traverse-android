package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.paletteColorAt
import com.traverse.android.ui.theme.palettePrimary

private val CardBackground = Color(0xFF1A1A1A)
private val SwiftGreen = Color(0xFF34C759)
private val SecondaryText = Color(0x99EBEBF5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLSchedulingInfoSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Header — iOS navigationTitle("Smart Revisions")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Smart Revisions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily,
                        color = Color.White
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Section
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = palettePrimary,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Spaced Repetition",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Reviews timed for when you're about to forget",
                style = MaterialTheme.typography.bodyMedium.copy(color = SecondaryText),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "What is this?" Section
            SectionHeader(icon = Icons.Default.Help, text = "What is this?")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                // NOTE: this used to say "ML-powered", and later explained the algorithm
                // (FSRS-5, stability/difficulty, the 85% retrievability target). Scheduling
                // internals are deliberately not surfaced to users — see the
                // traverse-revision-scheduling skill. Keep this plain-language.
                text = "Traverse follows how each problem goes for you and schedules the next " +
                    "revision for when you're about to forget it. Recall a problem well and the " +
                    "next review moves further out; struggle with it and it comes back sooner.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SecondaryText,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = SwiftGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Adapts to you",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SwiftGreen
                    )
                )
                Text(
                    text = "— no fixed 1d / 3d / 7d ladder",
                    style = MaterialTheme.typography.labelMedium.copy(color = SecondaryText)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "What Shapes Your Schedule" Section
            // Deliberately qualitative — the factor weights are internal and are not shown to
            // users. They have drifted before (a sixth "Mistake Tags (18%)" row that was never a
            // real factor; the attempt weight listed as 8% when it is 4%), and a percentage
            // invites users to reconstruct the scoring function. Keep this list descriptive,
            // with no numbers.
            SectionHeader(icon = Icons.Default.TrendingUp, text = "What Shapes Your Schedule")

            Spacer(modifier = Modifier.height(8.dp))

            SignalRow(
                icon = Icons.Default.Schedule,
                text = "Time Spent Ratio",
                detail = "How your solve time compares with your personal median for that difficulty"
            )
            SignalRow(
                icon = Icons.Default.Speed,
                text = "Problem Difficulty",
                detail = "Intrinsic problem baseline (Easy / Medium / Hard)"
            )
            SignalRow(
                icon = Icons.Default.Refresh,
                text = "Number of Retries",
                detail = "Softly scaled runs (typos & code runs non-punitive)"
            )
            SignalRow(
                icon = Icons.Default.CalendarMonth,
                text = "Spacing",
                detail = "Rewards a successful recall after a longer gap"
            )
            SignalRow(
                icon = Icons.Default.Numbers,
                text = "Attempt Number",
                detail = "How many times you have revised this problem"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = paletteColorAt(2))
            ) {
                Text(
                    text = "Got it",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = SecondaryText
            )
        )
    }
}

@Composable
private fun SignalRow(
    icon: ImageVector,
    text: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palettePrimary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
            )
        }
    }
}
