package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.traverse.android.data.Revision

// Pastel colors matching Android app's monochromish-pastel theme
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLAttemptSheet(
    revision: Revision,
    onDismiss: () -> Unit,
    onOpenProblem: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Smart Revisions",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Hero Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AccentPastel,
                        modifier = Modifier.size(56.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "LSTM Spaced Repetition",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Predicting your optimal review intervals",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // What is this section
                SectionCard(
                    title = "What is this?",
                    icon = Icons.Default.QuestionMark
                ) {
                    Text(
                        text = "This is an ML-powered spaced repetition system. Instead of fixed review schedules (1 day, 3 days, 7 days...), our LSTM neural network learns YOUR learning patterns and predicts the perfect time for your next review.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EasyPastel,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MAE: 1.78 days",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EasyPastel
                            )
                        )
                        Text(
                            text = " — within ~2 days of optimal",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Features Section
                SectionCard(
                    title = "7 Features We Track",
                    icon = Icons.Default.Analytics
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureRow(Icons.Default.Speed, "Problem difficulty", "Easy / Medium / Hard", AccentPastel)
                        FeatureRow(Icons.Default.Folder, "Category", "Arrays, Trees, DP, Graphs...", AccentPastel)
                        FeatureRow(Icons.Default.Numbers, "Attempt number", "1st, 2nd, 3rd review...", AccentPastel)
                        FeatureRow(Icons.Default.DateRange, "Days since last", "Time gap between reviews", AccentPastel)
                        FeatureRow(Icons.Default.CheckCircle, "Outcome", "Success or failure — critical!", AccentPastel)
                        FeatureRow(Icons.Default.Refresh, "Number of tries", "Submit attempts this session", AccentPastel)
                        FeatureRow(Icons.Default.AccessTime, "Time spent", "Minutes solving the problem", AccentPastel)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Every submission feeds into the model to improve predictions.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Technical Details Section
                SectionCard(
                    title = "Under the hood",
                    icon = Icons.Default.Computer
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TechRow("Architecture", "2-layer LSTM + BatchNorm")
                        TechRow("Hidden size", "128 units")
                        TechRow("Loss function", "Huber Loss")
                        TechRow("Training data", "15,321 records")
                        TechRow("Clusters", "5 learner patterns")
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        
                        Text(
                            text = "Exponential Decay Model",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "interval = -log(0.9) / exp(LSTM_output)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = AccentPastel
                                )
                            )
                        }
                        
                        Text(
                            text = "Recall probability decays exponentially. The LSTM learns your personal forgetting curve.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onOpenProblem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPastel),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Solve: ${revision.problem.title}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            ),
                            maxLines = 1
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            MediumPastel
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Got it",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MediumPastel
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentPastel,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun TechRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        )
    }
}