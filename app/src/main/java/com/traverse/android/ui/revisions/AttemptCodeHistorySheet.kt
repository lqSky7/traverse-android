package com.traverse.android.ui.revisions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.CodeAttempt
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Revision
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import kotlinx.coroutines.launch

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 Kotlin port of iOS AttemptCodeHistorySheet.swift.
 * Displays timeline of raw attempt code snippets, execution metrics, and outcomes for a problem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttemptCodeHistorySheet(
    revision: Revision,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkService = remember { NetworkService.getInstance(context) }

    var isLoading by remember { mutableStateOf(true) }
    var previousAttempts by remember { mutableStateOf<List<CodeAttempt>>(emptyList()) }
    var todayAttempts by remember { mutableStateOf<List<CodeAttempt>>(emptyList()) }

    LaunchedEffect(revision.id) {
        isLoading = true
        val result = networkService.getRevisionDetails(revision.id)
        if (result is NetworkResult.Success) {
            val solveAttempts = result.data.revision.solve?.attempts ?: emptyList()
            // Partition by today vs previous
            todayAttempts = solveAttempts.filter { it.runNumber != null || it.timestamp.contains("T") }
            previousAttempts = solveAttempts.filterNot { todayAttempts.contains(it) }
        } else {
            // Fallback to locally cached attempts
            val localAttempts = revision.solve?.attempts ?: emptyList()
            todayAttempts = localAttempts
        }
        isLoading = false
    }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attempt History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = revision.problem.title,
                        fontSize = 13.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDismiss) {
                    Text("Close", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00E676), modifier = Modifier.size(32.dp))
                }
            } else if (todayAttempts.isEmpty() && previousAttempts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No code submissions recorded for this problem.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                if (todayAttempts.isNotEmpty()) {
                    Text(
                        text = "Session Attempts (${todayAttempts.size})",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    todayAttempts.forEachIndexed { index, attempt ->
                        CodeAttemptCard(index = index + 1, attempt = attempt, isToday = true)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (previousAttempts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Previous Attempts (${previousAttempts.size})",
                        color = Color(0xFF7C4DFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    previousAttempts.forEachIndexed { index, attempt ->
                        CodeAttemptCard(index = index + 1, attempt = attempt, isToday = false)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeAttemptCard(
    index: Int,
    attempt: CodeAttempt,
    isToday: Boolean
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }
    val isSuccess = attempt.successful != false

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF242424),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSuccess) Color(0xFF00E676).copy(alpha = 0.3f) else Color(0xFFFF9100).copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF9100),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${if (isToday) "Session" else "Prev"} Attempt #$index",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "(${attempt.language ?: attempt.type ?: "code"})",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Copy Code Button
                if (!attempt.code.isNullOrEmpty()) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Attempt Code", attempt.code))
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (!attempt.code.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF121212), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = attempt.code,
                                color = Color(0xFFE6EDF3),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Text(
                            text = "No code text saved for this attempt.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
