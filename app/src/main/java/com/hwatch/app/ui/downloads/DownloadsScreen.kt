package com.hwatch.app.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hwatch.app.R
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.ui.common.EmptyState
import com.hwatch.app.ui.common.FullScreenErrorState
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.LabeledValue
import com.hwatch.app.ui.common.LoadingState
import com.hwatch.app.ui.common.LocalSnackbarHostState
import com.hwatch.app.ui.common.VideoThumbnail
import com.hwatch.app.ui.common.toVideoCardUiModel
import com.hwatch.app.util.ByteFormatter

/** 下载页：任务（REST + SSE）+ 未下载（discovered）子页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    baseUrl: String,
    onPlayVideo: (VideoDto) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var deleteTarget by remember { mutableStateOf<VideoDto?>(null) }
    var showDownloadAll by remember { mutableStateOf(false) }

    LifecycleStartEffect(Unit) {
        viewModel.activate()
        onStopOrDispose { viewModel.deactivate() }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && state.discovered.isEmpty() && state.discoveredError == null) {
            viewModel.loadDiscovered(refresh = true)
        }
    }

    val message = state.message
    val messageText = message?.let { downloadsMessageText(it) }
    LaunchedEffect(message?.id) {
        if (messageText != null) snackbarHostState.showSnackbar(messageText)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.downloads_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DownloadsSummary(state, onTogglePause = viewModel::toggleGlobalPause)
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.downloads_tab_tasks)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.downloads_tab_discovered)) },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    TasksTab(
                        state = state,
                        baseUrl = baseUrl,
                        onRefresh = viewModel::refresh,
                        onRetry = viewModel::retry,
                        onPlayVideo = onPlayVideo,
                        onAction = viewModel::onAction,
                        onDeleteRequest = { deleteTarget = it },
                    )
                } else {
                    DiscoveredTab(
                        state = state,
                        baseUrl = baseUrl,
                        onLoadMore = { viewModel.loadDiscovered(refresh = false) },
                        onRetry = { viewModel.loadDiscovered(refresh = true) },
                        onDownload = { viewModel.onAction(it.videoId, DownloadAction.DOWNLOAD) },
                        onDownloadAll = { showDownloadAll = true },
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.downloads_delete_confirm_title)) },
            text = { Text(stringResource(R.string.downloads_delete_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(target.videoId, DownloadAction.DELETE)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.download_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showDownloadAll) {
        AlertDialog(
            onDismissRequest = { showDownloadAll = false },
            title = { Text(stringResource(R.string.downloads_download_all_confirm_title)) },
            text = { Text(stringResource(R.string.downloads_download_all_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.downloadAll()
                    showDownloadAll = false
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadAll = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun DownloadsSummary(state: DownloadsUiState, onTogglePause: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(HWatchSpacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HWatchSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(connectionColor(state.connectionState)),
                )
                Text(
                    text = stringResource(connectionStateRes(state.connectionState)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onTogglePause, enabled = !state.isTogglingPause) {
                    Text(
                        stringResource(
                            if (state.isGloballyPaused) {
                                R.string.downloads_resume_all
                            } else {
                                R.string.downloads_pause_all
                            },
                        ),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xxl)) {
                LabeledValue(
                    labelRes = R.string.downloads_speed,
                    value = ByteFormatter.formatDownloadSpeed(state.downloadSpeed),
                )
                LabeledValue(
                    labelRes = R.string.downloads_connections,
                    value = state.connections.toString(),
                )
            }
        }
    }
}

@Composable
private fun connectionColor(state: com.hwatch.app.data.events.ConnectionState) =
    when (state) {
        com.hwatch.app.data.events.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        com.hwatch.app.data.events.ConnectionState.CONNECTING,
        com.hwatch.app.data.events.ConnectionState.RECONNECTING,
        -> MaterialTheme.colorScheme.tertiary
        com.hwatch.app.data.events.ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksTab(
    state: DownloadsUiState,
    baseUrl: String,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onPlayVideo: (VideoDto) -> Unit,
    onAction: (String, DownloadAction) -> Unit,
    onDeleteRequest: (VideoDto) -> Unit,
) {
    when {
        state.isInitialLoading -> LoadingState()
        state.initialError != null && state.tasks.isEmpty() -> FullScreenErrorState(
            icon = Icons.Filled.CloudOff,
            title = stringResource(R.string.downloads_load_error),
            message = stringResource(downloadsErrorRes(state.initialError)),
            onRetry = onRetry,
        )
        state.tasks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Filled.Download,
                title = stringResource(R.string.downloads_empty_tasks),
            )
        }
        else -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(HWatchSpacing.md),
                verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
            ) {
                items(items = state.tasks, key = { it.videoId }) { video ->
                    DownloadTaskCard(
                        video = video,
                        baseUrl = baseUrl,
                        pending = video.videoId in state.pendingActions,
                        onPlay = { onPlayVideo(video) },
                        onAction = { action -> onAction(video.videoId, action) },
                        onDeleteRequest = { onDeleteRequest(video) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveredTab(
    state: DownloadsUiState,
    baseUrl: String,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDownload: (VideoDto) -> Unit,
    onDownloadAll: () -> Unit,
) {
    when {
        state.discoveredLoading -> LoadingState()
        state.discoveredError != null && state.discovered.isEmpty() -> FullScreenErrorState(
            icon = Icons.Filled.CloudOff,
            title = stringResource(R.string.downloads_load_error),
            message = stringResource(downloadsErrorRes(state.discoveredError)),
            onRetry = onRetry,
        )
        state.discovered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Filled.Download,
                title = stringResource(R.string.downloads_empty_discovered),
            )
        }
        else -> {
            val listState = rememberLazyListState()
            val shouldLoadMore by remember {
                derivedStateOf {
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    val total = listState.layoutInfo.totalItemsCount
                    total > 0 && last >= total - 3
                }
            }
            LaunchedEffect(shouldLoadMore, state.discovered.size) {
                if (shouldLoadMore && state.discoveredHasMore) onLoadMore()
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = onDownloadAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HWatchSpacing.md, vertical = HWatchSpacing.xs),
                ) {
                    Text(stringResource(R.string.downloads_download_all))
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(HWatchSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
                ) {
                    items(items = state.discovered, key = { it.videoId }) { video ->
                        DiscoveredRow(
                            video = video,
                            baseUrl = baseUrl,
                            pending = video.videoId in state.pendingActions,
                            onDownload = { onDownload(video) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredRow(
    video: VideoDto,
    baseUrl: String,
    pending: Boolean,
    onDownload: () -> Unit,
) {
    val ui = remember(video) { video.toVideoCardUiModel() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(HWatchSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoThumbnail(
                baseUrl = baseUrl,
                videoId = ui.videoId,
                contentDescription = ui.primaryTitle,
                modifier = Modifier.width(120.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ui.primaryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ui.creator?.let { creator ->
                    Text(
                        text = creator,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onDownload, enabled = !pending) {
                Text(stringResource(R.string.download_action_download))
            }
        }
    }
}

@Composable
private fun downloadsMessageText(message: DownloadsMessage): String = when (message.kind) {
    DownloadsMessage.Kind.ACTION_SUBMITTED -> stringResource(R.string.downloads_action_submitted)
    DownloadsMessage.Kind.ACTION_FAILED -> stringResource(R.string.downloads_action_failed)
    DownloadsMessage.Kind.REFRESH_FAILED -> stringResource(R.string.snackbar_refresh_failed)
    DownloadsMessage.Kind.DOWNLOAD_ALL_ACCEPTED ->
        stringResource(R.string.downloads_download_all_accepted, message.count)
    DownloadsMessage.Kind.DOWNLOAD_ALL_FAILED -> stringResource(R.string.downloads_action_failed)
}

private fun downloadsErrorRes(error: DownloadsError): Int = when (error) {
    DownloadsError.UNREACHABLE -> R.string.error_unreachable
    DownloadsError.TIMEOUT -> R.string.error_timeout
    DownloadsError.HTTP -> R.string.error_http
    DownloadsError.PARSE -> R.string.error_parse
}
