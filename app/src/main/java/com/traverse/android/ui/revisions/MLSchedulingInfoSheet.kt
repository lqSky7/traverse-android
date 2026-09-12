package com.traverse.android.ui.revisions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
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
private val FormulaBackground = Color(0xFF0D0D0D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLSchedulingInfoSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTechDetails by remember { mutableStateOf(false) }

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
                text = "FSRS-5 Spaced Repetition",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Power-Law Forgetting Curve Scheduling",
                style = MaterialTheme.typography.bodyMedium.copy(color = SecondaryText),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "What is this?" Section
            SectionHeader(icon = Icons.Default.Help, text = "What is this?")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This is an ML-powered spaced repetition system based on FSRS-5 (Free Spaced " +
                    "Repetition Scheduler). Instead of static intervals (1d, 3d, 7d...), the algorithm " +
                    "tracks item-level Memory Stability (S) and Difficulty (D) to schedule reviews " +
                    "right when your retrievability reaches 90%.",
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
                    text = "Target Recall: 90% (R = 0.9)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SwiftGreen
                    )
                )
                Text(
                    text = "— optimal spacing window",
                    style = MaterialTheme.typography.labelMedium.copy(color = SecondaryText)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "6 Quality Signals We Track" Section
            SectionHeader(icon = Icons.Default.TrendingUp, text = "6 Quality Signals We Track")

            Spacer(modifier = Modifier.height(8.dp))

            SignalRow(
                icon = Icons.Default.Schedule,
                text = "Time Spent Ratio (28%)",
                detail = "Ratio vs your personal median time per difficulty"
            )
            SignalRow(
                icon = Icons.Default.Speed,
                text = "Problem Difficulty (18%)",
                detail = "Intrinsic problem baseline (Easy / Medium / Hard)"
            )
            SignalRow(
                icon = Icons.Default.Warning,
                text = "Mistake Tags (18%)",
                detail = "Penalties for approach, TLE, syntax, or DS errors"
            )
            SignalRow(
                icon = Icons.Default.Refresh,
                text = "Number of Retries (15%)",
                detail = "Softly scaled runs (typos & code runs non-punitive)"
            )
            SignalRow(
                icon = Icons.Default.CalendarMonth,
                text = "Spacing Bonus (13%)",
                detail = "Logarithmic reward for long-gap successful recall"
            )
            SignalRow(
                icon = Icons.Default.Numbers,
                text = "Attempt Number (8%)",
                detail = "Review iteration expectation adjustment"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Signals compute a Quality Score (q ∈ [0, 1]) mapped to FSRS grades " +
                    "(Again, Hard, Good, Easy) to scale stability.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SecondaryText,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "Under the hood" Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTechDetails = !showTechDetails },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Under the hood",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Icon(
                            imageVector = if (showTechDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedVisibility(visible = showTechDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TechRow("Algorithm", "FSRS-5 (Free Spaced Repetition)")
                            TechRow("Curve Model", "Power-Law Forgetting")
                            TechRow("Key States", "Stability (S) & Difficulty (D)")
                            TechRow("Target Recall", "90% Retrievability (R = 0.9)")
                            TechRow("Clustering Prevention", "±10% Dynamic Interval Fuzzing")

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            FormulaBlock(
                                title = "Power-Law Forgetting Curve",
                                formula = "R(t, S) = (1 + 19/81 * (t / S))^(-0.5)",
                                explanation = "Retrievability R(t, S) represents recall probability after " +
                                    "t days. At t = S, recall probability is exactly 90%."
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            FormulaBlock(
                                title = "Stability Recall Growth",
                                formula = "S' = S * e^(w8) * (11 - D) * S^(-w9) * (e^(w10*(1-R)) - 1)",
                                explanation = "Successful recall expands stability S according to the " +
                                    "spacing effect, while lapse/failure resets stability."
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            FormulaBlock(
                                title = "Next Review Interval",
                                formula = "I = (S / (19/81)) * (0.9^(-2) - 1) ≈ S",
                                explanation = "Reviews are scheduled right before memory retrievability " +
                                    "drops below 90%, preventing item decay."
                            )
                        }
                    }
                }
            }

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

@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        )
    }
}

@Composable
private fun FormulaBlock(title: String, formula: String, explanation: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = FormulaBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formula,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = palettePrimary,
                modifier = Modifier.padding(8.dp)
            )
        }
        Text(
            text = explanation,
            style = MaterialTheme.typography.labelSmall.copy(
                color = SecondaryText,
                lineHeight = 16.sp
            )
        )
    }
}
