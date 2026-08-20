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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.traverse.android.data.CacheManager
import com.traverse.android.data.GeminiService
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Revision
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val PurplePastel = Color(0xFFD1C4E9)

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

private data class IconStyle(
    val width: Int,
    val height: Int,
    val shape: Shape,
    val yOffset: Int
)

private fun getIconStyle(index: Int): IconStyle {
    return when (index % 6) {
        0 -> IconStyle(62, 48, RoundedCornerShape(24.dp), -8)
        1 -> IconStyle(54, 54, CircleShape, 8)
        2 -> IconStyle(48, 58, RoundedCornerShape(16.dp), -10)
        3 -> IconStyle(60, 46, RoundedCornerShape(12.dp), 8)
        4 -> IconStyle(52, 52, RoundedCornerShape(18.dp), -10)
        else -> IconStyle(56, 44, RoundedCornerShape(22.dp), 8)
    }
}

/**
 * Dedicated full-screen Revision Coach View reproducing iOS RevisionCoachSheet.swift 1:1.
 * Pure black background (#000000) with zero gradients, floating organic icons, and smooth text morph.
 */
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

    // Intro entrance animations
    var isIntroVisible by remember { mutableStateOf(false) }
    var isTextVisible by remember { mutableStateOf(true) }

    // Fetch full revision details & run AI hint flow
    LaunchedEffect(revision.id) {
        // Trigger intro animation
        delay(120)
        isIntroVisible = true

        // Step 1: Check cached/saved API key
        val storedKey = cacheManager.getGeminiApiKey()
        if (storedKey.isNullOrBlank()) {
            isLoading = false
            showApiKeyDialog = true
            return@LaunchedEffect
        }
        apiKey = storedKey

        // Step 2: Fetch full revision details if attempts aren't loaded
        scope.launch {
            val result = networkService.getRevisionDetails(revision.id)
            if (result is NetworkResult.Success) {
                fullRevision = result.data.revision
            }
        }

        // Step 3: Run loading step cycle with smooth text morph
        val stepJob = scope.launch {
            while (isLoading) {
                delay(1800)
                if (!isLoading) break
                isTextVisible = false
                delay(200)
                currentStepIndex = (currentStepIndex + 1) % loadingSteps.size
                isTextVisible = true
            }
        }

        // Step 4: Call Gemini (using gemini-flash-latest)
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

            isTextVisible = false
            delay(200)
            isLoading = false
            stepJob.cancel()

            result.onSuccess { hint ->
                aiFeedbackText = hint
            }.onFailure { err ->
                aiFeedbackText = "Could not generate hint: ${err.localizedMessage ?: "Unknown error"}. Check your Gemini API key."
            }
            isTextVisible = true
        }
    }

    val mistakeTags = remember(fullRevision) {
        val tags = fullRevision.solve?.mistakeTags ?: emptyList()
        if (tags.isEmpty()) listOf("Logic Bug", "Edge Case", "Time Limit Exceeded") else tags
    }

    // Render as a dedicated full-screen view
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Navigation Bar (matching iOS NavigationStack inline style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(onClick = { showApiKeyDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Gemini API Key",
                                tint = AccentPastel
                            )
                        }
                    }

                    // Spacer pushing content cluster to the lower-center / bottom (matching iOS)
                    Spacer(modifier = Modifier.weight(1f))

                    // Content Cluster with smooth entrance animation
                    AnimatedVisibility(
                        visible = isIntroVisible,
                        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                            initialOffsetY = { 60 },
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Organic Floating Icons Row (Code History + Mistake Tags)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Code History Button (Translucent Glass Capsule)
                                Box(
                                    modifier = Modifier
                                        .offset(y = (-8).dp)
                                        .size(width = 62.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                        .clickable { showHistorySheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Code History",
                                        tint = AccentPastel,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Mistake Tag Icons (Distinct alternating shapes & active tinting)
                                mistakeTags.forEachIndexed { idx, tag ->
                                    val style = getIconStyle(idx + 1)
                                    val isSelected = selectedTooltipTag == tag
                                    val tagColor = TagColors[idx % TagColors.size]
                                    val icon = getTagIcon(tag)

                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.12f else 1.0f,
                                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                        label = "tagScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .offset(y = style.yOffset.dp)
                                            .scale(scale)
                                            .size(width = style.width.dp, height = style.height.dp)
                                            .clip(style.shape)
                                            .background(
                                                if (isSelected) tagColor.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) tagColor else Color.White.copy(alpha = 0.15f),
                                                style.shape
                                            )
                                            .clickable {
                                                selectedTooltipTag = if (selectedTooltipTag == tag) null else tag
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tag,
                                            tint = tagColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Liquid Glass Tooltip Banner (Shown when a mistake tag is clicked)
                            AnimatedVisibility(
                                visible = selectedTooltipTag != null,
                                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                            ) {
                                selectedTooltipTag?.let { tag ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF1C1C1E))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Central AI Sparkles Icon (Clean glass circle)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Sparkles",
                                    tint = PurplePastel,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 4. Subtitle Label
                            Text(
                                text = if (fullRevision.isCompleted) "AI ATTEMPT COMPARISON" else "AI REVISION HINT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.35f)
                                )
                            )

                            // 5. Morphing Text Area (No heavy card boxes/neon gradients, clean typography)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp, max = 180.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val textAlpha by animateFloatAsState(
                                    targetValue = if (isTextVisible) 1f else 0f,
                                    animationSpec = tween(durationMillis = 220),
                                    label = "textAlpha"
                                )
                                val textBlur by animateDpAsState(
                                    targetValue = if (isTextVisible) 0.dp else 4.dp,
                                    animationSpec = tween(durationMillis = 220),
                                    label = "textBlur"
                                )

                                Text(
                                    text = if (isLoading) {
                                        loadingSteps[currentStepIndex % loadingSteps.size]
                                    } else {
                                        aiFeedbackText ?: "Preparing AI analysis..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = if (isLoading) FontWeight.Medium else FontWeight.Normal,
                                        lineHeight = 22.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .blur(textBlur)
                                        .graphicsLayer {
                                            alpha = textAlpha
                                        }
                                )
                            }
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
        lower.contains("boundary") || lower.contains("edge") || lower.contains("index") -> Icons.Default.Warning
        lower.contains("time") || lower.contains("tle") || lower.contains("timeout") -> Icons.Default.HourglassEmpty
        lower.contains("memory") || lower.contains("space") || lower.contains("mle") -> Icons.Default.Memory
        lower.contains("approach") || lower.contains("algo") -> Icons.Default.Psychology
        else -> Icons.Default.Tag
    }
}
