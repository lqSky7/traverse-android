package com.traverse.android.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.AchievementDetail
import com.traverse.android.data.AwardSection
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.components.MedalBadge
import com.traverse.android.ui.components.MedalProgressBar
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * One award shelf, opened. A three-up grid of badges — the layout Apple uses for
 * "Close Your Rings" and "Monthly Challenges" in the Fitness app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwardsSectionScreen(
    section: AwardSection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<AchievementDetail?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (section.achievements.isEmpty()) {
            // A shelf with no badges on it yet. The section supplies its own `emptyCopy`; the icon
            // used to be a bare "\u25C7" glyph, which read as a rendering artefact rather than an
            // intentional empty state.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.EmojiEvents,
                    title = section.title,
                    message = section.emptyCopy.ifBlank {
                        "No awards on this shelf yet. They unlock as you keep solving."
                    }
                )
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (section.subtitle.isNotEmpty()) {
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            items(section.achievements, key = { it.id }) { award ->
                AwardGridCell(award = award, onClick = { selected = award })
            }
        }
    }

    selected?.let { award ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
            containerColor = Color(0xFF0C0C0C)
        ) {
            AwardDetailSheetContent(award = award)
        }
    }
}

// MARK: - Grid cell

@Composable
private fun AwardGridCell(
    award: AchievementDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val progress = award.progress
    val showsProgress = !award.unlocked && (progress?.fraction ?: 0f) > 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            contentAlignment = Alignment.Center
        ) {
            MedalBadge(
                medal = award.medalAsset,
                unlocked = award.unlocked,
                size = 72.dp
            )
        }

        Text(
            text = award.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (award.unlocked) Color.White else Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        if (showsProgress && progress != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = progress.caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MedalProgressBar(
                    fraction = progress.fraction,
                    width = 72.dp,
                    tint = palette.colorAt(1)
                )
            }
        } else {
            Text(
                text = awardCaption(award),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// MARK: - Badge detail

/**
 * Tapping a badge opens it: a large badge you can hold and drag to tilt.
 */
@Composable
private fun AwardDetailSheetContent(award: AchievementDetail) {
    val palette = rememberPalette()
    val progress = award.progress

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            MedalBadge(
                medal = award.medalAsset,
                unlocked = award.unlocked,
                size = 240.dp
            )
        }

        Text(
            text = "Hold and drag the badge to tilt it",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f)
        )

        Text(
            text = award.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = award.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )

        if (progress != null && !award.unlocked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = progress.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                MedalProgressBar(
                    fraction = progress.fraction,
                    width = 160.dp,
                    tint = palette.colorAt(1)
                )
            }
        } else if (award.unlocked) {
            shortDate(award.unlockedAt)?.let { stamp ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = palette.colorAt(1),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Earned $stamp",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.colorAt(1)
                    )
                }
            }
        }
    }
}

// MARK: - Formatting

private val isoDayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yy")
private val isoYear: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy")

private fun parseIso(value: String?): Instant? {
    if (value.isNullOrEmpty()) return null
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {
                null
            }
        }
    }
}

private fun shortDate(value: String?): String? =
    parseIso(value)?.atZone(ZoneId.systemDefault())?.format(isoDayMonthYear)

private fun shortYear(value: String?): String? =
    parseIso(value)?.atZone(ZoneId.systemDefault())?.format(isoYear)

/**
 * The line under a badge in a grid cell: progress while it's in flight, otherwise the unlock
 * stamp, otherwise the goal description.
 */
internal fun awardCaption(award: AchievementDetail): String {
    val progress = award.progress

    if (!award.unlocked && progress != null) return progress.caption

    if (award.unlocked) {
        // Monthly challenges are stamped with their year, the way Apple does it.
        if (award.key.startsWith("monthly_challenge_")) {
            shortYear(award.unlockedAt)?.let { return it }
        }
        shortDate(award.unlockedAt)?.let { return it }
    }

    progress?.let { return it.caption }
    return award.description
}
