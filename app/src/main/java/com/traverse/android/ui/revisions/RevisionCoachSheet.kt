package com.traverse.android.ui.revisions

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.CacheManager
import com.traverse.android.data.GeminiService
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Revision
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val PurplePastel = Color(0xFFD1C4E9)
private val CardBackground = Color(0xFF141414)

private val TagColors = listOf(
    Color(0xFFFFB6C1),
    Color(0xFFB6E3FF),
    Color(0xFFFFE4B6),
    Color(0xFFB6FFD8),
    Color(0xFFE6B6FF),
    Color(0xFFFFF0B6)
)

private val loadingSteps = listOf(
    "Fetching backend data...",
    "Loading model & parameters...",
    "Analyzing code execution patterns...",
    "Formulating AI revision hint..."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionCoachSheet(
    revision: Revision,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cacheManager = remember { CacheManager.getInstance(context) }
    val networkService = remember { NetworkService.getInstance(context) }
    val geminiService = remember { GeminiService.getInstance() }

    var fullRevision by remember { mutableStateOf(revision) }
    var apiKey by remember { mutableStateOf(cacheManager.getGeminiApiKey() ?: "") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var inputKey by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var aiFeedbackText by remember { mutableStateOf<String?>(null) }
    var selectedTooltipTag by remember { mutableStateOf<String?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }

    // Fetch full revision details & run AI hint flow
    LaunchedEffect(revision.id) {
        // Step 1: Check cached/saved API key
        val storedKey = cacheManager.getGeminiApiKey()
        if (storedKey.isNullOrBlank()) {
            isLoading = false
            showApiKeyDialog = true
            return@LaunchedEffect
        }
        apiKey = storedKey

        // Step 2: Fetch details if attempts aren't loaded
        scope.launch {
            val result = networkService.getRevisionDetails(revision.id)
            if (result is NetworkResult.Success) {
                fullRevision = result.data.revision
            }
        }

        // Step 3: Run loading step cycle
        val stepJob = scope.launch {
            while (isLoading) {
                delay(1600)
                currentStepIndex = (currentStepIndex + 1) % loadingSteps.size
            }
        }

        // Step 4: Call Gemini
        scope.launch {
            val attempts = fullRevision.solve?.attempts ?: emptyList()
            val mistakeTags = fullRevision.solve?.mistakeTags ?: emptyList()
            val isCompleted = fullRevision.isCompleted

            val result = if (isCompleted) {
                geminiService.generateRevisionComparisonForCompleted(
                    apiKey = apiKey,
                    problemTitle = fullRevision.problem.title,
                    difficulty = fullRevision.problem.difficulty,
                    previousAttempts = attempts.dropLast(1),
                    todayAttempts = attempts.takeLast(1),
                    mistakeTags = mistakeTags
                )
            } else {
                geminiService.generateRevisionHintForPending(
                    apiKey = apiKey,
                    problemTitle = fullRevision.problem.title,
                    difficulty = fullRevision.problem.difficulty,
                    attempts = attempts,
                    mistakeTags = mistakeTags,
                    aiAnalysis = fullRevision.solve?.aiAnalysis
                )
            }

            isLoading = false
            stepJob.cancel()

            result.onSuccess { hint ->
                aiFeedbackText = hint
            }.onFailure { err ->
                aiFeedbackText = "Could not generate hint: ${err.localizedMessage ?: "Unknown error"}. Check your Gemini API key."
            }
        }
    }

    val mistakeTags = remember(fullRevision) {
        val tags = fullRevision.solve?.mistakeTags ?: emptyList()
        if (tags.isEmpty()) listOf("Logic Bug", "Edge Case", "Time Limit Exceeded") else tags
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 32.dp)
        ) {
            // Ambient Bottom Glow (reproducing iOS bottom ambient light)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.BottomCenter)
                    .blur(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF6200EA).copy(alpha = 0.25f),
                                Color(0xFF9C27B0).copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = fullRevision.problem.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = BelfastGroteskBlackFamily,
                            color = Color.White
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key",
                            tint = AccentPastel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Organic Floating Icons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Code History Button
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .width(62.dp)
                            .clickable { showHistorySheet = true },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = AccentPastel,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Mistake Tag Chips
                    mistakeTags.forEachIndexed { idx, tag ->
                        val isSelected = selectedTooltipTag == tag
                        val tagColor = TagColors[idx % TagColors.size]
                        val icon = getTagIcon(tag)

                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .clickable {
                                    selectedTooltipTag = if (selectedTooltipTag == tag) null else tag
                                },
                            shape = RoundedCornerShape(if (idx % 2 == 0) 24.dp else 16.dp),
                            color = if (isSelected) tagColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) tagColor else Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tagColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Tooltip Banner for active mistake tag
                AnimatedVisibility(
                    visible = selectedTooltipTag != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    selectedTooltipTag?.let { tag ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1E2C),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MediumPastel,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "RECORDED MISTAKE PATTERN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { selectedTooltipTag = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Central AI Sparkles Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C4DFF).copy(alpha = 0.18f))
                        .border(1.5.dp, Color(0xFFB388FF).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Hint",
                        tint = Color(0xFFD1C4E9),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = if (fullRevision.isCompleted) "AI ATTEMPT COMPARISON" else "AI REVISION HINT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Silky smooth morphing text area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.85f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PurplePastel
                                )
                                Text(
                                    text = loadingSteps[currentStepIndex],
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = aiFeedbackText ?: "No feedback generated.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    lineHeight = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Code History Sheet
    if (showHistorySheet) {
        AttemptCodeHistorySheet(
            revision = fullRevision,
            onDismiss = { showHistorySheet = false }
        )
    }

    // Gemini API Key Input Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "To enable AI Revision Coach hints, enter your free Gemini API key from Google AI Studio.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Get a free Gemini API Key", color = AccentPastel)
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = AccentPastel,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputKey.isNotBlank()) {
                            cacheManager.cacheGeminiApiKey(inputKey.trim())
                            apiKey = inputKey.trim()
                            showApiKeyDialog = false
                            // Restart flow
                            isLoading = true
                            aiFeedbackText = null
                        }
                    }
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getTagIcon(tag: String): ImageVector {
    val lower = tag.lowercase()
    return when {
        lower.contains("syntax") || lower.contains("type") -> Icons.Default.Code
        lower.contains("logic") || lower.contains("bug") -> Icons.Default.BugReport
        lower.contains("boundary") || lower.contains("edge") || lower.contains("corner") -> Icons.Default.Warning
        lower.contains("time") || lower.contains("tle") || lower.contains("timeout") -> Icons.Default.HourglassEmpty
        lower.contains("memory") || lower.contains("space") || lower.contains("mle") -> Icons.Default.Memory
        lower.contains("approach") || lower.contains("algo") -> Icons.Default.Psychology
        else -> Icons.Default.Tag
    }
}
