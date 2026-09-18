package com.traverse.android.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.RingGoals
import com.traverse.android.data.RingProgress
import com.traverse.android.ui.components.ActivityRings
import com.traverse.android.ui.theme.rememberPalette
import kotlinx.coroutines.launch

private val SheetCardBg = Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingGoalsSheet(
    rings: RingProgress?,
    onDismiss: () -> Unit,
    onSaveGoals: suspend (solveGoal: Int, revisionGoal: Int) -> Result<Unit>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val initialRings = rings ?: RingProgress.empty()
    var solveGoal by remember { mutableIntStateOf(initialRings.configuredSolveGoal.coerceIn(RingGoals.MINIMUM, RingGoals.MAXIMUM)) }
    var revisionGoal by remember { mutableIntStateOf(initialRings.configuredRevisionGoal.coerceIn(RingGoals.MINIMUM, RingGoals.MAXIMUM)) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasChanges = solveGoal != initialRings.configuredSolveGoal || revisionGoal != initialRings.configuredRevisionGoal
    val differsFromToday = solveGoal != initialRings.solveGoal || revisionGoal != initialRings.revisionGoal

    val palette = rememberPalette()
    val solveColor = palette.colorAt(0)
    val revisionColor = palette.colorAt(1)

    val previewSolveFraction = if (solveGoal <= 0) 0f else minOf(initialRings.solves.toFloat() / solveGoal.toFloat(), 1f)
    val previewRevisionFraction = if (revisionGoal <= 0) 0f else minOf(initialRings.revisions.toFloat() / revisionGoal.toFloat(), 1f)

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
        ) {
            // Header: Cancel, Title, Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                }

                Text(
                    text = "Your rings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        if (hasChanges && !isSaving) {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                val result = onSaveGoals(solveGoal, revisionGoal)
                                isSaving = false
                                if (result.isSuccess) {
                                    sheetState.hide()
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to save goals"
                                }
                            }
                        }
                    },
                    enabled = hasChanges && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = palette.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasChanges) palette.primary else Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 1. Live Preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SheetCardBg)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActivityRings(
                    solveFraction = previewSolveFraction,
                    revisionFraction = previewRevisionFraction,
                    solveColor = solveColor,
                    revisionColor = revisionColor,
                    diameter = 132.dp,
                    strokeWidth = 14.dp,
                    ringGap = 5.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val solvedMet = initialRings.solves >= solveGoal
                val revisedMet = initialRings.revisions >= revisionGoal
                val summary = when {
                    solvedMet && revisedMet -> "You would already have both rings closed today."
                    solvedMet && !revisedMet -> "Solves done. ${revisionGoal - initialRings.revisions} more to review."
                    !solvedMet && revisedMet -> "Revisions done. ${solveGoal - initialRings.solves} more to solve."
                    else -> {
                        val rem = (solveGoal - initialRings.solves) + (revisionGoal - initialRings.revisions)
                        "$rem more ${if (rem == 1) "action" else "actions"} to close both."
                    }
                }

                Text(
                    text = summary,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Explainer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SheetCardBg)
                    .padding(14.dp)
            ) {
                Text(
                    text = "What is a ring?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A ring is one thing you owe today. Close both and the day counts. " +
                        "The outer ring is new solves, the inner one is revisions, and both " +
                        "are measured in your own timezone — so a solve at 1am belongs to the " +
                        "day you were still awake for.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Goal Steppers
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GoalStepperRow(
                    title = "New solves",
                    subtitle = "Problems solved for the first time",
                    color = solveColor,
                    value = solveGoal,
                    doneToday = initialRings.solves,
                    onValueChange = { solveGoal = it }
                )

                GoalStepperRow(
                    title = "Revisions",
                    subtitle = "Scheduled reviews completed",
                    color = revisionColor,
                    value = revisionGoal,
                    doneToday = initialRings.revisions,
                    onValueChange = { revisionGoal = it }
                )
            }

            // 4. Error banner
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE65100).copy(alpha = 0.15f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Could not save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = error,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 5. Tomorrow Note
            if (differsFromToday && hasChanges) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SheetCardBg)
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
                        text = "Today's rings keep the goals they were opened with. A change applies " +
                            "from tomorrow, so raising a goal never takes away a ring you have " +
                            "already earned.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GoalStepperRow(
    title: String,
    subtitle: String,
    color: Color,
    value: Int,
    doneToday: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SheetCardBg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val canDecrease = value > RingGoals.MINIMUM
            val canIncrease = value < RingGoals.MAXIMUM

            IconButton(
                onClick = { if (canDecrease) onValueChange(value - 1) },
                enabled = canDecrease,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (canDecrease) 0.12f else 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (canDecrease) Color.White else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "$value",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp)
            )

            IconButton(
                onClick = { if (canIncrease) onValueChange(value + 1) },
                enabled = canIncrease,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (canIncrease) 0.12f else 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (canIncrease) Color.White else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$doneToday today",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.45f),
            textAlign = TextAlign.End,
            modifier = Modifier.width(54.dp)
        )
    }
}
