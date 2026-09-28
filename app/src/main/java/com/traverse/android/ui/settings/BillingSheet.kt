package com.traverse.android.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.SubscriptionStatusResponse
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BillingSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val network = remember { NetworkService.getInstance(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<SubscriptionStatusResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        isLoading = true
        when (val result = network.getBillingStatus()) {
            is NetworkResult.Success -> status = result.data
            is NetworkResult.Error -> error = result.message
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { refresh() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Billing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                    Text("Your Traverse plan and renewal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Current plan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (isLoading) "Checking your plan…" else if (status?.isSubscriptionActive == true) status?.planName ?: "Traverse Pro" else "Traverse Free",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(planDescription(status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isLoading) CircularProgressIndicator()
            else if (status?.isSubscriptionActive == true && status?.canCancel == true) {
                OutlinedButton(onClick = { showCancelConfirmation = true }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isWorking) "Updating…" else "Cancel renewal")
                }
            } else if (status?.cancellationScheduled == true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Renewal cancelled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (status?.isSubscriptionActive != true) {
                Button(onClick = { openBilling(context) }, modifier = Modifier.fillMaxWidth()) { Text("Upgrade to Pro") }
            }
            HorizontalDivider()
            Text("Pro includes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text("Adaptive revisions, deeper progress analytics, AI code analysis, and shared streaks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { openBilling(context) }, modifier = Modifier.align(Alignment.Start)) { Text("Manage billing on the web") }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Cancel Pro renewal?") },
            text = { Text("Pro remains active until the end of your current billing period.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmation = false
                    scope.launch {
                        isWorking = true
                        when (val result = network.cancelSubscription()) {
                            is NetworkResult.Success -> { error = null; refresh() }
                            is NetworkResult.Error -> error = result.message
                        }
                        isWorking = false
                    }
                }) { Text("Cancel renewal", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirmation = false }) { Text("Keep Pro") } }
        )
    }
}

private fun planDescription(status: SubscriptionStatusResponse?): String {
    if (status?.isSubscriptionActive != true) return "Upgrade whenever you’re ready."
    if (status.cancellationScheduled) return "Renewal cancelled. Pro remains active through ${formatDate(status.activeUntil) ?: "this billing period"}."
    return status.activeUntil?.let { "Renews after ${formatDate(it)}." } ?: "Pro is active on your account."
}

private fun formatDate(raw: String): String? = runCatching {
    DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(raw))
}.getOrNull()

private fun openBilling(context: android.content.Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://traverses.tech/billing")))
}
