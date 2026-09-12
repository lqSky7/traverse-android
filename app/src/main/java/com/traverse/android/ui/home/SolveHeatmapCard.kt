package com.traverse.android.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate

private val CardBackground = Color(0xFF1A1A1A)

/** iOS: `Color(red: 0.31, green: 0.76, blue: 0.97)` — frozen days. */
private val IceBlue = Color(0xFF4FC3F7)

private const val CELL_SIZE_DP = 16
private const val CELL_SPACING_DP = 3

/**
 * 1:1 port of the iOS `SolveHeatmapCard`: a compact 7-week activity grid where each cell is
 * tinted by the hardest problem solved that day (frozen days are ice blue), with a
 * three-dot difficulty legend. Tapping opens the full activity detail screen.
 *
 * Palette mapping from iOS: header icon and count use `color(at: 3)`; the heatmap cells and
 * legend dots use `color(at: 0/1/2)` for Easy/Medium/Hard.
 */
@Composable
fun SolveHeatmapCard(
    solves: List<Solve>,
    frozenDates: List<String>,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val headerColor = palette.colorAt(3)
    val easyColor = palette.colorAt(0)
    val mediumColor = palette.colorAt(1)
    val hardColor = palette.colorAt(2)

    val frozenSet = remember(frozenDates) { frozenDates.toSet() }

    val hardestByDate = remember(solves) {
        val map = HashMap<LocalDate, String>()
        solves.forEach { solve ->
            val date = parseLocalDate(solve.solvedAt) ?: return@forEach
            val existing = map[date]
            map[date] = if (existing == null) {
                solve.problem.difficulty
            } else {
                harderDifficulty(existing, solve.problem.difficulty)
            }
        }
        map
    }

    val weeks = remember {
        val today = LocalDate.now()
        // DayOfWeek: MONDAY=1 … SUNDAY=7 — normalise so weeks start on Sunday like iOS.
        val daysFromSunday = today.dayOfWeek.value % 7
        val currentWeekStart = today.minusDays(daysFromSunday.toLong())
        (6 downTo 0).map { weekOffset ->
            val start = currentWeekStart.minusWeeks(weekOffset.toLong())
            (0..6).map { dayOffset -> start.plusDays(dayOffset.toLong()) }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${hardestByDate.size}",
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING_DP.dp)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(CELL_SPACING_DP.dp)) {
                            week.forEach { date ->
                                Box(
                                    modifier = Modifier
                                        .size(CELL_SIZE_DP.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            colorForDate(
                                                date = date,
                                                frozenDates = frozenSet,
                                                hardestByDate = hardestByDate,
                                                easyColor = easyColor,
                                                mediumColor = mediumColor,
                                                hardColor = hardColor
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(easyColor, mediumColor, hardColor).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}

private fun colorForDate(
    date: LocalDate,
    frozenDates: Set<String>,
    hardestByDate: Map<LocalDate, String>,
    easyColor: Color,
    mediumColor: Color,
    hardColor: Color
): Color {
    if (frozenDates.contains(date.toString())) return IceBlue

    val difficulty = hardestByDate[date] ?: return Color.Gray.copy(alpha = 0.15f)
    return when (difficulty.lowercase()) {
        "easy" -> easyColor
        "medium" -> mediumColor
        "hard" -> hardColor
        else -> Color.Gray.copy(alpha = 0.3f)
    }
}

internal fun harderDifficulty(a: String, b: String): String {
    val order = mapOf("easy" to 0, "medium" to 1, "hard" to 2)
    val aValue = order[a.lowercase()] ?: 0
    val bValue = order[b.lowercase()] ?: 0
    return if (aValue >= bValue) a else b
}

internal fun parseLocalDate(raw: String): LocalDate? = try {
    LocalDate.parse(raw.take(10))
} catch (_: Exception) {
    null
}
