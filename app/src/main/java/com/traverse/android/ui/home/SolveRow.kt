package com.traverse.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette
import java.time.Duration
import java.time.LocalDateTime

private val RowBackground = Color(0xFF1A1A1A)

/** SwiftUI `Color.blue` (#007AFF), used for highlight tag chips on iOS. */
private val HighlightTagBlue = Color(0xFF007AFF)

/**
 * 1:1 port of the iOS `SolveRow`: a tappable row that expands (spring animated) to reveal
 * language, attempts, time, AI analysis, mistake tags and the user's note / highlight.
 *
 * Palette mapping from iOS: difficulty → `color(at: 0/1/2)`, XP → `color(at: 1)`,
 * language → `color(at: 0)`, attempts → `color(at: 1)`, time → `color(at: 2)`,
 * AI analysis → `color(at: 3)`, mistake tags → `color(at: 5)`, note → `color(at: 4)`.
 */
@Composable
fun SolveRow(
    solve: Solve,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val easyColor = palette.colorAt(0)
    val mediumColor = palette.colorAt(1)
    val hardColor = palette.colorAt(2)
    val aiColor = palette.colorAt(3)
    val noteColor = palette.colorAt(4)
    val tagColor = palette.colorAt(5)

    var isExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "solveRowChevron"
    )

    val difficultyColor = when (solve.problem.difficulty.lowercase()) {
        "easy" -> easyColor
        "medium" -> mediumColor
        "hard" -> hardColor
        else -> Color.Gray
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RowBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = solve.problem.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = solve.problem.difficulty.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(color = difficultyColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = solve.problem.platform.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                val topic = solve.problem.topic
                if (!topic.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TopicChip(
                            text = solve.problem.displayTopic ?: topic,
                            background = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White.copy(alpha = 0.6f),
                            weight = FontWeight.Medium
                        )
                        val subtopic = solve.problem.subtopic
                        if (!subtopic.isNullOrEmpty()) {
                            TopicChip(
                                text = subtopic,
                                background = Color.White.copy(alpha = 0.06f),
                                contentColor = Color.White.copy(alpha = 0.4f),
                                weight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+${solve.xpAwarded}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = mediumColor
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = mediumColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = formatRelativeDate(solve.solvedAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                DetailLine(
                    icon = Icons.Default.Code,
                    iconColor = easyColor,
                    text = "Language: ${solve.submission.language.replaceFirstChar { it.uppercase() }}"
                )

                solve.submission.numberOfTries?.let { tries ->
                    DetailLine(
                        icon = Icons.Default.Refresh,
                        iconColor = mediumColor,
                        text = "Attempts: $tries"
                    )
                }

                solve.submission.timeTaken?.let { time ->
                    DetailLine(
                        icon = Icons.Default.Schedule,
                        iconColor = hardColor,
                        text = "Time: ${formatDuration(time)}"
                    )
                }

                val analysis = solve.aiAnalysis ?: solve.submission.aiAnalysis
                if (!analysis.isNullOrEmpty()) {
                    Section(
                        icon = Icons.Default.AutoAwesome,
                        iconColor = aiColor,
                        title = "AI Analysis"
                    ) {
                        Text(
                            text = stripMarkdown(analysis),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                val tags = solve.mistakeTags ?: solve.submission.mistakeTags
                if (!tags.isNullOrEmpty()) {
                    Section(
                        icon = Icons.Default.Label,
                        iconColor = tagColor,
                        title = "Mistake Tags"
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            tags.forEach { tag ->
                                TagChip(
                                    text = tag,
                                    background = tagColor.copy(alpha = 0.2f),
                                    contentColor = tagColor
                                )
                            }
                        }
                    }
                }

                solve.highlight?.let { highlight ->
                    Section(
                        icon = Icons.Default.Notes,
                        iconColor = noteColor,
                        title = "Your Note"
                    ) {
                        if (highlight.note.isNotEmpty()) {
                            Text(
                                text = stripMarkdown(highlight.note),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )
                            )
                        }

                        if (highlight.content.isNotEmpty() && highlight.content != highlight.note) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = highlight.content,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(8.dp)
                            )
                        }

                        if (highlight.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                highlight.tags.forEach { tag ->
                                    TagChip(
                                        text = tag,
                                        background = HighlightTagBlue.copy(alpha = 0.2f),
                                        contentColor = HighlightTagBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun DetailLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
        )
    }
}

@Composable
private fun TopicChip(
    text: String,
    background: Color,
    contentColor: Color,
    weight: FontWeight
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = weight,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TagChip(
    text: String,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = contentColor
        )
    }
}

internal fun formatRelativeDate(dateString: String): String {
    val parsed = try {
        LocalDateTime.parse(dateString.take(19))
    } catch (_: Exception) {
        null
    } ?: return "via Chrome"

    val now = LocalDateTime.now()
    val duration = Duration.between(parsed, now)
    val days = duration.toDays()
    val hours = duration.toHours()
    val minutes = duration.toMinutes()

    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

internal fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

internal fun stripMarkdown(text: String): String =
    text.replace("**", "").replace("*", "")
