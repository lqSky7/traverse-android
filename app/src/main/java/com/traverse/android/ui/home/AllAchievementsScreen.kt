package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.AchievementDetail
import com.traverse.android.data.AchievementStatsData
import com.traverse.android.data.AwardSection
import com.traverse.android.ui.components.MedalBadge
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily

private val CardBackground = Color(0xFF1C1C1C)

/**
 * The Awards shelf — Apple Fitness' award page, rebuilt for Traverse.
 *
 * Layout mirrors the Fitness app: an in-progress challenge card leads, the first shelf gets a
 * full-width hero card, and the remaining shelves sit in a two-up grid. A shelf with nothing
 * on it yet spans the full width so its empty-state copy has room to breathe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAchievementsScreen(
    achievements: List<AchievementDetail>,
    stats: AchievementStatsData?,
    sections: List<AwardSection>,
    featured: AchievementDetail?,
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Servers that predate the award shelves still return a flat catalogue.
    val shelves = remember(sections, achievements) {
        if (sections.isNotEmpty()) sections else fallbackSections(achievements)
    }
    val lead = featured ?: shelves.firstNotNullOfOrNull { it.hero }

    Scaffold(
        modifier = modifier,
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Awards",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = floatingBottomBarContentInset() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stats != null && stats.total > 0) {
                Text(
                    text = "${stats.unlocked} of ${stats.total} earned",
                    style = MaterialTheme.typography.subheadline,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            lead?.let { award ->
                FeaturedAwardCard(
                    award = award,
                    onClick = { onOpenSection(award.section ?: shelves.firstOrNull()?.id ?: "workouts") }
                )
            }

            shelfRows(shelves).forEach { row ->
                if (row.size == 1) {
                    ShelfCard(
                        section = row[0],
                        onClick = { onOpenSection(row[0].id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        row.forEach { section ->
                            ShelfCard(
                                section = section,
                                onClick = { onOpenSection(section.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rows of shelves: full-width heroes, and pairs for the two-up grid. The first shelf leads
 * the page; an empty shelf needs the full width for its copy.
 */
private fun shelfRows(sections: List<AwardSection>): List<List<AwardSection>> {
    val rows = mutableListOf<List<AwardSection>>()
    var pending: AwardSection? = null

    sections.forEachIndexed { index, section ->
        val wide = index == 0 || section.total == 0
        if (wide) {
            pending?.let { rows.add(listOf(it)); pending = null }
            rows.add(listOf(section))
        } else if (pending != null) {
            rows.add(listOf(pending!!, section))
            pending = null
        } else {
            pending = section
        }
    }
    pending?.let { rows.add(listOf(it)) }
    return rows
}

/**
 * The wide card that leads the page: the challenge currently in flight, shown with its badge
 * drained back so it reads as "not yet earned".
 */
@Composable
private fun FeaturedAwardCard(
    award: AchievementDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MedalBadge(
            medal = award.medalAsset,
            unlocked = award.unlocked,
            size = 64.dp,
            interactive = false
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = award.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = award.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            award.progress?.takeIf { !award.unlocked }?.let { progress ->
                Text(
                    text = progress.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** One award shelf on the hub. Leads with the newest badge the user earned on that shelf. */
@Composable
private fun ShelfCard(
    section: AwardSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = section.hero
    val isEmpty = section.total == 0 || hero == null

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        if (isEmpty) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.height(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u25C7",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White.copy(alpha = 0.16f)
                    )
                }
                Text(
                    text = section.emptyCopy,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else if (hero != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp),
                contentAlignment = Alignment.Center
            ) {
                MedalBadge(
                    medal = hero.medalAsset,
                    unlocked = hero.unlocked,
                    size = 116.dp,
                    interactive = false
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = hero.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (hero.unlocked) Color.White else Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = awardCaption(hero),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // The little row of recent badges under the hero — Apple's "and these too" flourish.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.spacedBy((-12).dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                section.stack.forEach { award ->
                    MedalBadge(
                        medal = award.medalAsset,
                        unlocked = award.unlocked,
                        size = 30.dp,
                        interactive = false
                    )
                }
            }
        }
    }
}

// MARK: - Fallback for servers that predate the award shelves

private val fallbackOrder = listOf("rings", "monthly", "workouts", "competitions", "limited")

private val fallbackTitles = mapOf(
    "rings" to Triple("Close Your Rings", "Awards for keeping your streak alive.", "Keep a streak going to earn awards."),
    "monthly" to Triple("Monthly Challenges", "Awards for the challenges set each month.", "Solve problems this month to earn the award."),
    "workouts" to Triple("Workouts", "Awards for your training volume and records.", "Log some solving workouts to earn awards."),
    "competitions" to Triple("Competitions", "Awards for competing with friends.", "Complete competitions to earn awards."),
    "limited" to Triple("Limited Edition", "Limited edition awards.", "Special awards appear here when available.")
)

internal fun fallbackSections(achievements: List<AchievementDetail>): List<AwardSection> {
    val grouped = achievements.groupBy { it.section ?: "workouts" }
    return fallbackOrder.mapNotNull { id ->
        val items = grouped[id] ?: return@mapNotNull null
        val meta = fallbackTitles[id] ?: return@mapNotNull null
        AwardSection(
            id = id,
            title = meta.first,
            subtitle = meta.second,
            emptyCopy = meta.third,
            unlocked = items.count { it.unlocked },
            total = items.size,
            achievements = items.sortedBy { it.sortOrder ?: 0 }
        )
    }
}
