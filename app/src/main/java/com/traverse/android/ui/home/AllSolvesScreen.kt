package com.traverse.android.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.Solve

private val CardBackground = Color(0xFF1A1A1A)
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AIPastel = Color(0xFFE6B6FF)
private val TagPastel = Color(0xFFB6E3FF)
private val NotePastel = Color(0xFFFFE4B6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSolvesScreen(
    solves: List<Solve>,
    onBack: () -> Unit
) {
    var expandedSolveId by remember { mutableStateOf<Int?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Solves") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(solves, key = { it.id }) { solve ->
                ExpandableSolveCard(
                    solve = solve,
                    isExpanded = expandedSolveId == solve.id,
                    onClick = {
                        expandedSolveId = if (expandedSolveId == solve.id) null else solve.id
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpandableSolveCard(
    solve: Solve,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val difficultyColor = when (solve.problem.difficulty.lowercase()) {
        "easy" -> EasyPastel
        "medium" -> MediumPastel
        "hard" -> HardPastel
        else -> Color.White.copy(alpha = 0.5f)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = solve.problem.title,
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            text = solve.problem.difficulty.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(color = difficultyColor)
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                        )
                        Text(
                            text = solve.problem.platform.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "+${solve.xpAwarded}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MediumPastel)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MediumPastel,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = formatDate(solve.solvedAt),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Expandable details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Language
                    DetailRow(
                        icon = Icons.Default.Code,
                        iconColor = EasyPastel,
                        text = "Language: ${solve.submission.language.replaceFirstChar { it.uppercase() }}"
                    )
                    
                    // Number of tries
                    solve.submission.numberOfTries?.let { tries ->
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            icon = Icons.Default.Refresh,
                            iconColor = MediumPastel,
                            text = "Attempts: $tries"
                        )
                    }
                    
                    // Time taken
                    solve.submission.timeTaken?.let { time ->
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            icon = Icons.Default.Schedule,
                            iconColor = HardPastel,
                            text = "Time: ${formatTime(time)}"
                        )
                    }
                    
                    // AI Analysis
                    val analysis = solve.aiAnalysis ?: solve.submission.aiAnalysis
                    if (!analysis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AIPastel,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Analysis",
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Render markdown-like formatting
                        Text(
                            text = analysis.replace("**", "").replace("*", ""), // Basic cleanup
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                    
                    // Mistake Tags
                    val tags = solve.mistakeTags ?: solve.submission.mistakeTags
                    if (!tags.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = TagPastel,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mistake Tags",
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            tags.forEach { tag ->
                                TagChip(tag = tag, color = TagPastel)
                            }
                        }
                    }
                    
                    // Highlight (user note)
                    solve.highlight?.let { highlight ->
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = NotePastel,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Note",
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = highlight.note,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        
                        if (highlight.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                highlight.tags.forEach { tag ->
                                    TagChip(tag = tag, color = NotePastel)
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
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
        )
    }
}

@Composable
private fun TagChip(tag: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall.copy(color = color)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}

private fun formatDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString.take(10))
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
        date.format(formatter)
    } catch (e: Exception) {
        dateString.take(10)
    }
}
