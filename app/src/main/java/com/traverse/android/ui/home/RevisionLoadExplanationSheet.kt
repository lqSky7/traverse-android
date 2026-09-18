package com.traverse.android.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Explains the Revision Load figure. Reached from the info button on the load detail screen, in
 * the same slot Apple uses for its training-load explainer. 1:1 port of the iOS
 * `RevisionLoadExplanationSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionLoadExplanationSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Mirrors the iOS inline navigation bar: "Done" leading, title centred.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(72.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = "Revision Load",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(72.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Compares how much revision work you did in the last 7 days against your " +
                        "own 28-day baseline. It is a relative measure — it only ever compares you " +
                        "to you.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Explainer(
                    title = "Load",
                    body = "Every completed revision counts, and every problem you work on counts. " +
                        "Harder problems count for more than easier ones, the same way a longer " +
                        "workout counts for more training load."
                )

                Explainer(
                    title = "7-Day vs. 28-Day",
                    body = "Your daily average over the last 7 days is measured against your daily " +
                        "average over the last 28. Staying between 80% and 130% of baseline is the " +
                        "sweet spot — enough to keep memory strong, not so much that you burn out."
                )

                Explainer(
                    title = "Bands",
                    body = "Well Below and Below mean you are tapering and retention will start to " +
                        "slip. Optimal means you are holding steady. Above and Well Above mean a " +
                        "sharp ramp — expect gains, but take a lighter day if revisions start " +
                        "failing."
                )

                Explainer(
                    title = "Revision Score",
                    body = "The score in the footer is separate: it tracks revision consistency and " +
                        "memory retention over the past 7 days, and is never penalised for " +
                        "struggling on hard problems."
                )
            }
        }
    }
}

@Composable
private fun Explainer(title: String, body: String) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = body,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
