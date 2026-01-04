package jp.co.screentime.slackreporter.ui.screens.home

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.screentime.slackreporter.R
import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.presentation.home.HomeUiState
import jp.co.screentime.slackreporter.presentation.home.HomeViewModel
import jp.co.screentime.slackreporter.presentation.model.UiAppUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToExclusions: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onUsageAccessGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToExclusions) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = stringResource(R.string.nav_exclusions)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                !uiState.hasUsageAccess -> {
                    UsageAccessRequiredContent(
                        onOpenSettings = onOpenUsageAccessSettings,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    HomeContent(
                        uiState = uiState,
                        onSendNow = viewModel::onClickSendNow
                    )
                }
            }
        }
    }
}

@Composable
private fun formatMinutes(context: Context, minutes: Int): String {
    return when {
        minutes < 1 -> context.getString(R.string.time_format_less_than_minute)
        minutes < 60 -> context.getString(R.string.time_format_minutes, minutes)
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) {
                context.getString(R.string.time_format_hours_minutes, hours, mins)
            } else {
                context.getString(R.string.time_format_hours_minutes, hours, 0) // e.g., "1時間0分"
            }
        }
    }
}

@Composable
private fun UsageAccessRequiredContent(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_usage_access_required_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_usage_access_required_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.home_grant_permission))
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onSendNow: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TotalTimeCard(
                totalTime = formatMinutes(context, uiState.totalMinutes),
                hasUsage = uiState.hasUsage
            )
        }

        item {
            SendStatusCard(
                status = uiState.sendStatus,
                lastSentTime = uiState.lastSentTimeFormatted,
                isSending = uiState.isSending,
                error = uiState.sendError,
                onSendNow = onSendNow
            )
        }

        if (uiState.topApps.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_top_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.topApps) { app ->
                AppUsageItem(app = app)
            }

            if (uiState.otherMinutes > 0) {
                item {
                    OtherAppsItem(duration = formatMinutes(context, uiState.otherMinutes))
                }
            }
        }

        if (!uiState.hasUsage) {
            item {
                Text(
                    text = stringResource(R.string.home_no_usage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TotalTimeCard(
    totalTime: String,
    hasUsage: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_total_time),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasUsage) totalTime else stringResource(R.string.home_total_time_zero),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SendStatusCard(
    status: SendStatus,
    lastSentTime: String?,
    isSending: Boolean,
    error: String?,
    onSendNow: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (status) {
                            SendStatus.NOT_SENT -> stringResource(R.string.home_send_status_not_sent)
                            SendStatus.SUCCESS -> stringResource(R.string.home_send_status_success)
                            SendStatus.FAILED -> stringResource(R.string.home_send_status_failed)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (status) {
                            SendStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                            SendStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (lastSentTime != null) {
                        Text(
                            text = stringResource(R.string.home_last_sent_at, lastSentTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Button(onClick = onSendNow, enabled = !isSending) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.home_sending))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.home_send_now))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageItem(app: UiAppUsage) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon = app.icon, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = app.formattedDuration(context),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OtherAppsItem(duration: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_other_apps),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon != null) {
        Image(
            bitmap = icon.toBitmap(width = 80, height = 80).asImageBitmap(),
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
