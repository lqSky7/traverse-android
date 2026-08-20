package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.Revision
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import kotlinx.coroutines.launch

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 Kotlin port of iOS Reschedule Revision Sheet.
 * Allows users to defer a pending revision by presets (1d, 3d, 7d, 14d, 30d) or custom days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleSheet(
    revision: Revision,
    onDismiss: () -> Unit,
    onRescheduled: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkService = remember { NetworkService.getInstance(context) }

    var selectedDays by remember { mutableIntStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetDays = listOf(1, 3, 7, 14, 30)

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
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reschedule Revision",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = revision.problem.title,
                fontSize = 14.sp,
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDays.forEach { days ->
                    val isSelected = selectedDays == days
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDays = days },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF242424),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Text(
                            text = when (days) {
                                1 -> "+1d"
                                3 -> "+3d"
                                7 -> "+1w"
                                14 -> "+2w"
                                30 -> "+1m"
                                else -> "+${days}d"
                            },
                            color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.8f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Days Stepper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF242424), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (selectedDays > 1) selectedDays-- },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$selectedDays Days",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "from today",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = { if (selectedDays < 365) selectedDays++ },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = networkService.rescheduleRevision(revision.id, selectedDays)
                        isLoading = false
                        if (result is NetworkResult.Success) {
                            onRescheduled()
                            onDismiss()
                        } else if (result is NetworkResult.Error) {
                            errorMessage = result.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Confirm Reschedule (+$selectedDays Days)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
