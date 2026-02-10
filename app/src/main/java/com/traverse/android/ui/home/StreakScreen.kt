package com.traverse.android.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.FriendStreak
import com.traverse.android.data.StreakGoalPreferences
import com.traverse.android.ui.theme.RingiftFamily
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CardBackground = Color(0xFF1A1A1A)
private val AccentPastel = Color(0xFFB8D4E3)
private val StreakOrange = Color(0xFFFFB74D)
private val StreakRed = Color(0xFFFF8A65)
private val FreezeBlueDark = Color(0xFF42A5F5)
private val FreezeBlueLight = Color(0xFF90CAF9)
private val SolvedGreen = Color(0xFFA8E6CF)
private val TodayHighlight = Color(0xFFB8D4E3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(
    currentStreak: Int,
    totalStreakDays: Int,
    solvedToday: Boolean,
    solveDates: List<String>,
    frozenDates: List<String>,
    friendStreaks: List<FriendStreak>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("PERSONAL", "FRIENDS")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Streak Stats",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
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
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentPastel else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(AccentPastel)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> PersonalStreakTab(
                    currentStreak = currentStreak,
                    totalStreakDays = totalStreakDays,
                    solvedToday = solvedToday,
                    solveDates = solveDates,
                    frozenDates = frozenDates
                )
                1 -> FriendsStreakTab(
                    friendStreaks = friendStreaks
                )
            }

            // Bottom spacing for floating tab bar
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PersonalStreakTab(
    currentStreak: Int,
    totalStreakDays: Int,
    solvedToday: Boolean,
    solveDates: List<String>,
    frozenDates: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val streakGoalPrefs = remember { StreakGoalPreferences.getInstance(context) }
    var streakGoal by remember { mutableIntStateOf(streakGoalPrefs.getStreakGoal()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big Streak Counter
        Text(
            text = "$currentStreak",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 80.sp,
                fontFamily = RingiftFamily,
                brush = Brush.linearGradient(
                    colors = listOf(StreakOrange, StreakRed)
                )
            )
        )
        Text(
            text = "day streak!",
            style = MaterialTheme.typography.titleMedium.copy(
                color = StreakOrange,
                fontFamily = RingiftFamily
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Monthly Calendar
        StreakCalendar(
            solveDates = solveDates,
            frozenDates = frozenDates
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Streak Goal section
        Text(
            text = "Streak Goal",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        StreakGoalCard(
            totalStreakDays = totalStreakDays,
            streakGoal = streakGoal,
            onGoalChanged = { newGoal ->
                streakGoal = newGoal
                streakGoalPrefs.setStreakGoal(newGoal)
            }
        )
    }
}

@Composable
private fun StreakCalendar(
    solveDates: List<String>,
    frozenDates: List<String>,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val today = LocalDate.now()

    val solveDateSet = remember(solveDates) { solveDates.toSet() }
    val frozenDateSet = remember(frozenDates) { frozenDates.toSet() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Month header
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val dayNames = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                dayNames.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val lastDayOfMonth = currentMonth.atEndOfMonth()
            // Sunday = 0
            val startDayOffset = firstDayOfMonth.dayOfWeek.value % 7
            val totalDays = currentMonth.lengthOfMonth()

            // Build weeks
            var dayCounter = 1
            var weekStarted = false

            // Calculate number of weeks needed
            val totalCells = startDayOffset + totalDays
            val numWeeks = (totalCells + 6) / 7

            for (week in 0 until numWeeks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0 until 7) {
                        val cellIndex = week * 7 + dayOfWeek

                        if (cellIndex < startDayOffset || dayCounter > totalDays) {
                            // Empty cell
                            Box(modifier = Modifier.size(36.dp))
                        } else {
                            val date = currentMonth.atDay(dayCounter)
                            val dateString = date.toString()
                            val isSolvedDay = solveDateSet.contains(dateString)
                            val isFrozenDay = frozenDateSet.contains(dateString)
                            val isToday = date == today
                            val isFuture = date.isAfter(today)

                            CalendarDayCell(
                                day = dayCounter,
                                isSolved = isSolvedDay,
                                isFrozen = isFrozenDay,
                                isToday = isToday,
                                isFuture = isFuture
                            )
                            dayCounter++
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Solved legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(SolvedGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Solved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                // Freeze legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AcUnit,
                        contentDescription = null,
                        tint = FreezeBlueDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Freeze",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                // Today legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .border(2.dp, TodayHighlight, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSolved: Boolean,
    isFrozen: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isFrozen -> FreezeBlueDark.copy(alpha = 0.25f)
            isSolved -> SolvedGreen.copy(alpha = 0.25f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "dayBg"
    )

    val textColor = when {
        isFuture -> Color.White.copy(alpha = 0.2f)
        isSolved || isFrozen -> Color.White
        else -> Color.White.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (isToday) Modifier.border(2.dp, TodayHighlight, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFrozen) {
            // Show freeze icon (blue flame / snowflake)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = "Streak Freeze",
                    tint = FreezeBlueDark,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$day",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FreezeBlueLight,
                        fontSize = 8.sp
                    )
                )
            }
        } else if (isSolved) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Solved",
                    tint = StreakOrange,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$day",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SolvedGreen,
                        fontSize = 8.sp
                    )
                )
            }
        } else {
            Text(
                text = "$day",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun StreakGoalCard(
    totalStreakDays: Int,
    streakGoal: Int,
    onGoalChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val progress = totalStreakDays.toFloat() / streakGoal.toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Progress bar with editable goal badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    // Background track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // Progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(StreakOrange, StreakRed)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Editable goal badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$streakGoal",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$totalStreakDays / $streakGoal DAYS",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }

    // Edit streak goal dialog
    if (showEditDialog) {
        var goalText by remember { mutableStateOf(streakGoal.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Set Streak Goal")
            },
            text = {
                Column {
                    Text(
                        text = "Choose your streak goal (${StreakGoalPreferences.MIN_GOAL}–${StreakGoalPreferences.MAX_GOAL} days)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { value ->
                            goalText = value.filter { it.isDigit() }
                            val parsed = goalText.toIntOrNull()
                            isError = parsed == null || parsed < StreakGoalPreferences.MIN_GOAL || parsed > StreakGoalPreferences.MAX_GOAL
                        },
                        label = { Text("Goal (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isError
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = goalText.toIntOrNull()
                        if (parsed != null && parsed in StreakGoalPreferences.MIN_GOAL..StreakGoalPreferences.MAX_GOAL) {
                            onGoalChanged(parsed)
                            showEditDialog = false
                        }
                    },
                    enabled = !isError && goalText.isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FriendsStreakTab(
    friendStreaks: List<FriendStreak>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (friendStreaks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No friend streaks yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a streak with a friend from the Friends tab!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.3f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            friendStreaks.forEach { streak ->
                FriendStreakRow(streak = streak)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FriendStreakRow(
    streak: FriendStreak,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentPastel.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = streak.friend.username.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentPastel
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = streak.friend.username,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Longest: ${streak.longestStreak} days",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }

            // Streak count with fire icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (streak.currentStreak > 0) StreakOrange else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${streak.currentStreak}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (streak.currentStreak > 0) StreakOrange else Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
