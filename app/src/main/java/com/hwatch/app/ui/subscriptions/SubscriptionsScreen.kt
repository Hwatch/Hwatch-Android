package com.hwatch.app.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hwatch.app.R
import com.hwatch.app.data.model.SubscriptionDto
import com.hwatch.app.ui.common.EmptyState
import com.hwatch.app.ui.common.FullScreenErrorState
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.LocalSnackbarHostState
import com.hwatch.app.ui.common.LoadingState
import com.hwatch.app.ui.common.SubscriptionAvatar
import com.hwatch.app.util.ProgressCalc
import com.hwatch.app.util.TimeUtils

/** 订阅页：列表 + 启停 + 检查 + 删除 + 新增。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    addViewModel: AddSubscriptionViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = state
    val addState by addViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    var deleteTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    val message = state.message
    val messageText = message?.let { subscriptionsMessageText(it) }
    LaunchedEffect(message?.id) {
        if (messageText != null) snackbarHostState.showSnackbar(messageText)
    }

    LaunchedEffect(addState.created) {
        if (addState.created) {
            showAdd = false
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscriptions_title)) },
                actions = {
                    if (state.isCheckingAll) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = HWatchSpacing.xs))
                    } else {
                        IconButton(onClick = viewModel::checkAll) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.subscriptions_check_all),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.subscriptions_add))
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> LoadingState()
                snapshot.error != null && snapshot.items.isEmpty() -> FullScreenErrorState(
                    icon = Icons.Filled.CloudOff,
                    title = stringResource(R.string.subscriptions_load_error),
                    message = stringResource(subscriptionsErrorRes(snapshot.error)),
                    onRetry = viewModel::retry,
                )
                snapshot.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.Subscriptions,
                        title = stringResource(R.string.subscriptions_empty),
                    )
                }
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(HWatchSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
                    ) {
                        items(items = state.items, key = { it.slug }) { subscription ->
                            SubscriptionCard(
                                subscription = subscription,
                                pending = subscription.slug in state.pendingActions,
                                onEnabledChange = { enabled -> viewModel.setEnabled(subscription, enabled) },
                                onCheck = { viewModel.checkOne(subscription) },
                                onDelete = { deleteTarget = subscription },
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.subscriptions_delete_confirm_title)) },
            text = { Text(stringResource(R.string.subscriptions_delete_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.subscriptions_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showAdd) {
        AddSubscriptionSheet(
            state = addState,
            onQueryChanged = addViewModel::onQueryChanged,
            onPlatformChanged = addViewModel::onPlatformChanged,
            onUrlChanged = addViewModel::onUrlChanged,
            onDownloadExistingChanged = addViewModel::onDownloadExistingChanged,
            onSelectCreator = addViewModel::selectCreator,
            onCreate = addViewModel::create,
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun SubscriptionCard(
    subscription: SubscriptionDto,
    pending: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HWatchSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubscriptionAvatar(
                    name = subscription.name.ifBlank { subscription.slug },
                    platform = subscription.platform,
                    avatarUrl = subscription.avatarUrl,
                )
                Spacer(modifier = Modifier.size(HWatchSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name.ifBlank { subscription.slug },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subscription.platform,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = subscription.enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !pending,
                )
            }
            val lastChecked = subscription.lastCheckedAt.takeIf { it.isNotBlank() }?.let {
                TimeUtils.formatDisplayFromRfc3339(it, "UTC").getOrNull()
            }
            Text(
                text = if (lastChecked != null) {
                    stringResource(R.string.dashboard_last_checked, lastChecked)
                } else {
                    stringResource(R.string.dashboard_time_unknown)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subscription.lastError.isNotBlank()) {
                Text(
                    text = subscription.lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            subscription.progress?.let { progress ->
                val fraction = ProgressCalc.fraction(progress.completed, progress.total)
                if (fraction != null) {
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(
                            R.string.dashboard_subscription_progress,
                            progress.completed,
                            progress.total,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.dashboard_subscription_no_progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
                TextButton(onClick = onCheck, enabled = !pending) {
                    Text(stringResource(R.string.subscriptions_check))
                }
                TextButton(onClick = onDelete, enabled = !pending) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.subscriptions_delete),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(HWatchSpacing.xxs))
                    Text(stringResource(R.string.subscriptions_delete))
                }
            }
        }
    }
}

@Composable
private fun subscriptionsMessageText(message: SubscriptionsMessage): String = when (message.kind) {
    SubscriptionsMessage.Kind.CHECK_SUBMITTED -> stringResource(R.string.subscriptions_check_submitted)
    SubscriptionsMessage.Kind.ACTION_FAILED -> stringResource(R.string.subscriptions_action_failed)
    SubscriptionsMessage.Kind.REFRESH_FAILED -> stringResource(R.string.snackbar_refresh_failed)
}

internal fun subscriptionsErrorRes(error: SubscriptionsError): Int = when (error) {
    SubscriptionsError.UNREACHABLE -> R.string.error_unreachable
    SubscriptionsError.TIMEOUT -> R.string.error_timeout
    SubscriptionsError.HTTP -> R.string.error_http
    SubscriptionsError.PARSE -> R.string.error_parse
}
