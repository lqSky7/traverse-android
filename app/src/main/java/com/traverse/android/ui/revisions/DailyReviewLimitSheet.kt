package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.sp
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.RevisionTodayResponse
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlinx.coroutines.launch

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReviewLimitSheet(
    currentLimit: Int = 20,
    todaySummary: RevisionTodayResponse? = null,
    onDismiss: () -> Unit,
    onLimitUpdated: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkService = remember { NetworkService.getInstance(context) }

    var draftLimit by remember { mutableFloatStateOf(currentLimit.toFloat()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentLimit) {
        draftLimit = currentLimit.toFloat()
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
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
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AccentPastel,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Daily Revision Limit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = BelfastGroteskBlackFamily,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cap the number of ML revisions shown each day. Overflow rolls into the next days.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Slider & number display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max per day",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                        Text(
                            text = "${draftLimit.toInt()}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentPastel
                            )
                        )
                    }

                    Slider(
                        value = draftLimit,
                        onValueChange = { draftLimit = it },
                        valueRange = 1f..100f,
                        steps = 99,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPastel,
                            activeTrackColor = AccentPastel,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Queue stats row if available
            todaySummary?.let { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBox(
                        title = "Showing",
                        value = "${summary.revisions.size}",
                        color = EasyPastel,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Total Due",
                        value = "${summary.total}",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Overflow",
                        value = "${summary.overflow}",
                        color = HardPastel,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = EasyPastel,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null
                    successMessage = null
                    scope.launch {
                        val newLimit = draftLimit.toInt()
                        val result = networkService.updateProfile(maxDailyReviews = newLimit)
                        isSaving = false
                        if (result is NetworkResult.Success) {
                            successMessage = "Daily limit updated to $newLimit!"
                            onLimitUpdated(newLimit)
                        } else if (result is NetworkResult.Error) {
                            errorMessage = result.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPastel),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text(
                        text = "Save Limit",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}
