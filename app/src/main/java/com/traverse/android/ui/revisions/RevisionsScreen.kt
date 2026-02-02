package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traverse.android.data.RevisionStatsResponse
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.RevisionsViewModel

// Pastel colors matching Android app's monochromish-pastel theme
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionsScreen(
    viewModel: RevisionsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Revisions",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = CardBackground,
                            modifier = Modifier.width(220.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = uiState.showCompletedRevisions,
                                            onCheckedChange = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Show Completed",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = BelfastGroteskBlackFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.toggleShowCompleted()
                                    showMenu = false
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = uiState.useMLMode,
                                            onCheckedChange = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "ML-Based Scheduling",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = BelfastGroteskBlackFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.toggleMLMode()
                                    showMenu = false
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            )
        }) { padding ->
        PullToRefreshBox(
            onRefresh = { viewModel.refresh() },
            isRefreshing = uiState.isLoading,
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (uiState.stats != null) 90.dp else 16.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loading State
                if (uiState.isLoading && uiState.revisionGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Error State
                else if (uiState.errorMessage != null && uiState.revisionGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.errorMessage ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.refresh() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                // Empty State
                else if (uiState.revisionGroups.isEmpty()) {
                    item {
                        EmptyRevisionsState(modifier = Modifier.fillParentMaxSize())
                    }
                }

                // List State
                else {
                    items(
                        items = uiState.revisionGroups,
                        key = { it.date }
                    ) { group ->
                        RevisionGroupCard(
                            group = group,
                            useMLMode = uiState.useMLMode,
                            completingId = uiState.completingRevisionId,
                            onComplete = { viewModel.completeRevision(it) },
                            onDelete = { viewModel.deleteRevision(it) }
                        )
                    }
                }
            }

            // Floating Stats Toolbar
            uiState.stats?.let { stats ->
                FloatingStatsToolbar(
                    stats = stats,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }

        // Show Pro Upgrade Sheet when needed
        if (uiState.showProUpgradeDialog) {
            ProUpgradeSheet(
                onDismiss = {
                    viewModel.dismissProUpgradeDialog()
                }
            )
        }
    }
}

@Composable
private fun FloatingStatsToolbar(
    stats: RevisionStatsResponse,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StatBadge(
            title = "Due Today",
            value = stats.dueToday.toString(),
            color = AccentPastel
        )
        StatBadge(
            title = "Overdue",
            value = stats.overdue.toString(),
            color = HardPastel
        )
        StatBadge(
            title = "Done",
            value = "${stats.completionRate}%",
            color = EasyPastel
        )
    }
}

@Composable
private fun StatBadge(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun EmptyRevisionsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = "No Revisions Scheduled",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Text(
                text = "Complete problems to schedule revisions",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}
