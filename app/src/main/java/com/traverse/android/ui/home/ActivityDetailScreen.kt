package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate

private val CardBackground = Color(0xFF1A1A1A)

/** iOS: `Color(red: 0.31, green: 0.76, blue: 0.97)` — frozen days. */
private val IceBlue = Color(0xFF4FC3F7)

private val DAY_LABELS = listOf("", "Mon", "", "Wed", "", "Fri", "")

/**
 * 1:1 port of the iOS `ActivityDetailView`: a full-screen 20-week activity heatmap with
 * active-day / total-solve summary stats and both a density and difficulty legend.
 *
 * Palette mapping from iOS: "Active Days" uses `color(at: 3)`, "Total Solves" uses
 * `color(at: 0)`; cells and legends use `color(at: 0/1/2)` for Easy/Medium/Hard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    solves: List<Solve>,
    frozenDates: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val easyColor = palette.colorAt(0)
    val mediumColor = palette.colorAt(1)
    val hardColor = palette.colorAt(2)
    val activeDaysColor = palette.colorAt(3)

    val frozenSet = remember(frozenDates) { frozenDates.toSet() }

    val activityByDate = remember(solves) {
        val map = HashMap<LocalDate, Pair<String, Int>>()
        solves.forEach { solve ->
            val date = parseLocalDate(solve.solvedAt) ?: return@forEach
            val existing = map[date]
            map[date] = if (existing == null) {
                solve.problem.difficulty to 1
            } else {
                harderDifficulty(existing.first, solve.problem.difficulty) to (existing.second + 1)
            }
        }
        map
    }

    val weeks = remember {
        val today = LocalDate.now()
        val daysFromSunday = today.dayOfWeek.value % 7
        val currentWeekStart = today.minusDays(daysFromSunday.toLong())
        (19 downTo 0).map { weekOffset ->
            val start = currentWeekStart.minusWeeks(weekOffset.toLong())
            (0..6).map { dayOffset -> start.plusDays(dayOffset.toLong()) }
        }
    }

    val totalActiveDays = activityByDate.size
    val totalSolves = activityByDate.values.sumOf { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                SummaryStat(value = "$totalActiveDays", label = "Active Days", color = activeDaysColor)
                SummaryStat(value = "$totalSolves", label = "Total Solves", color = easyColor)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Last 20 Weeks",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val available = maxWidth - 40.dp
                    val cell = (available - 57.dp) / 20
                    val labelSpacing = maxOf(cell * 0.2f, 2.dp)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(labelSpacing)) {
                            DAY_LABELS.forEach { label ->
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(cell),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            weeks.forEach { week ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    week.forEach { date ->
                                        Box(
                                            modifier = Modifier
                                                .size(cell)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    colorForDate(
                                                        date = date,
                                                        frozenDates = frozenSet,
                                                        activityByDate = activityByDate,
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
                }

                // Density legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf(
                        Color.Gray.copy(alpha = 0.15f),
                        easyColor,
                        mediumColor,
                        hardColor
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                // Difficulty legend
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(
                        easyColor to "Easy",
                        mediumColor to "Medium",
                        hardColor to "Hard"
                    ).forEach { (color, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color) {
    Column {
        Text(
            text = value,
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

private fun colorForDate(
    date: LocalDate,
    frozenDates: Set<String>,
    activityByDate: Map<LocalDate, Pair<String, Int>>,
    easyColor: Color,
    mediumColor: Color,
    hardColor: Color
): Color {
    if (frozenDates.contains(date.toString())) return IceBlue

    val difficulty = activityByDate[date]?.first ?: return Color.Gray.copy(alpha = 0.15f)
    return when (difficulty.lowercase()) {
        "easy" -> easyColor
        "medium" -> mediumColor
        "hard" -> hardColor
        else -> Color.Gray.copy(alpha = 0.3f)
    }
}
