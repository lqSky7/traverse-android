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

/** Verbatim copy of the iOS `RevisionScoreExplanationSheet` body. */
internal const val REVISION_SCORE_EXPLANATION =
    "Tracks your overall revision consistency and memory retention health over the past 7 days.\n\n" +
        "\u2022 Memory Retention: Measures how effectively your review habit reinforces learned DSA " +
        "concepts to maintain strong long-term recall.\n\n" +
        "\u2022 Outcome-Independent: Focuses purely on engagement and recall effort \u2014 it is not " +
        "penalized when you struggle on difficult problems."

/**
 * 1:1 port of the iOS `RevisionScoreExplanationSheet`.
 *
 * Presented as a medium-detent sheet titled "Revision Score" with a "Done" action and the
 * same explanatory copy, triggered by tapping the revision score card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScoreExplanationSheet(
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
                    text = "Revision Score",
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

            Text(
                text = REVISION_SCORE_EXPLANATION,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 22.sp
                )
            )
        }
    }
}
