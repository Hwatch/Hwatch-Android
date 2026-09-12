package com.hwatch.app.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hwatch.app.R
import com.hwatch.app.data.model.DashboardCountsDto
import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.data.model.SubscriptionDto
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.ui.common.EmptyState
import com.hwatch.app.ui.common.FullScreenErrorState
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.LabeledValue
import com.hwatch.app.ui.common.LoadingState
import com.hwatch.app.ui.common.LocalSnackbarHostState
import com.hwatch.app.ui.common.SectionHeader
import com.hwatch.app.ui.common.StatCard
import com.hwatch.app.ui.common.StatusIndicator
import com.hwatch.app.ui.common.SubscriptionAvatar
import com.hwatch.app.ui.common.VideoThumbnail
import com.hwatch.app.ui.common.dashboardErrorRes
import com.hwatch.app.ui.common.toVideoCardUiModel
import com.hwatch.app.util.ByteFormatter
import com.hwatch.app.util.DiskStatsCalculator
import com.hwatch.app.util.ProgressCalc
import com.hwatch.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    baseUrl: String,
    onVideoClick: (VideoDto) -> Unit,
    onSeeAllLibrary: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) }) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val current = state) {
                DashboardUiState.Loading -> LoadingState()
                is DashboardUiState.Error -> FullScreenErrorState(
                    icon = Icons.Filled.CloudOff,
                    title = stringResource(R.string.dashboard_load_error),
                    message = stringResource(dashboardErrorRes(current.error)),
                    onRetry = viewModel::retry,
                )
                is DashboardUiState.Content -> {
                    val refreshFailedText = stringResource(R.string.snackbar_refresh_failed)
                    LaunchedEffect(current.refreshErrorId) {
                        if (current.refreshErrorId > 0) {
                            snackbarHostState.showSnackbar(refreshFailedText)
                        }
                    }
                    DashboardContent(
                        content = current,
                        baseUrl = baseUrl,
                        onRefresh = viewModel::refresh,
                        onVideoClick = onVideoClick,
                        onSeeAllLibrary = onSeeAllLibrary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    content: DashboardUiState.Content,
    baseUrl: String,
    onRefresh: () -> Unit,
    onVideoClick: (VideoDto) -> Unit,
    onSeeAllLibrary: () -> Unit,
) {
    val dashboard = content.dashboard
    val timezone = dashboard.timezone

    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= 840.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(HWatchSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sectionGap),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.md)) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sectionGap),
                        ) {
                            ServerStatusCard(dashboard)
                            StatsSection(dashboard.counts)
                            StorageTrafficCard(dashboard)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sectionGap),
                        ) {
                            VideoCarouselSection(
                                titleRes = R.string.dashboard_recent_added,
                                emptyRes = R.string.dashboard_empty_recent_added,
                                videos = dashboard.videos,
                                baseUrl = baseUrl,
                                timezone = timezone,
                                onVideoClick = onVideoClick,
                                onSeeAll = onSeeAllLibrary,
                            )
                            VideoCarouselSection(
                                titleRes = R.string.dashboard_recent_played,
                                emptyRes = R.string.dashboard_empty_recent_played,
                                videos = dashboard.recentlyPlayed,
                                baseUrl = baseUrl,
                                timezone = timezone,
                                onVideoClick = onVideoClick,
                                showTime = false,
                            )
                            SubscriptionsSection(dashboard.subscriptions, timezone)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(HWatchSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sectionGap),
                ) {
                    item { ServerStatusCard(dashboard) }
                    item { StatsSection(dashboard.counts) }
                    item { StorageTrafficCard(dashboard) }
                    item {
                        VideoCarouselSection(
                            titleRes = R.string.dashboard_recent_added,
                            emptyRes = R.string.dashboard_empty_recent_added,
                            videos = dashboard.videos,
                            baseUrl = baseUrl,
                            timezone = timezone,
                            onVideoClick = onVideoClick,
                            onSeeAll = onSeeAllLibrary,
                        )
                    }
                    item {
                        VideoCarouselSection(
                            titleRes = R.string.dashboard_recent_played,
                            emptyRes = R.string.dashboard_empty_recent_played,
                            videos = dashboard.recentlyPlayed,
                            baseUrl = baseUrl,
                            timezone = timezone,
                            onVideoClick = onVideoClick,
                            showTime = false,
                        )
                    }
                    item { SubscriptionsSection(dashboard.subscriptions, timezone) }
                }
            }
        }
    }
}

@Composable
private fun ServerStatusCard(dashboard: DashboardDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HWatchSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
        ) {
            StatusIndicator(
                active = dashboard.aria2Online,
                activeText = stringResource(R.string.dashboard_aria2_online),
                inactiveText = stringResource(R.string.dashboard_aria2_offline),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xxl)) {
                LabeledValue(
                    labelRes = R.string.dashboard_download_speed,
                    value = ByteFormatter.formatDownloadSpeed(dashboard.currentDownloadSpeed),
                )
                LabeledValue(
                    labelRes = R.string.dashboard_connections,
                    value = dashboard.currentConnections.toString(),
                )
            }
        }
    }
}

@Composable
private fun StatsSection(counts: DashboardCountsDto?) {
    val items = listOf(
        R.string.dashboard_stat_total to (counts?.videos ?: 0L),
        R.string.dashboard_stat_active to (counts?.active ?: 0L),
        R.string.dashboard_stat_waiting to (counts?.waiting ?: 0L),
        R.string.dashboard_stat_completed to (counts?.completed ?: 0L),
        R.string.dashboard_stat_discovered to (counts?.discovered ?: 0L),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        Column(verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
            items.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
                    rowItems.forEach { (labelRes, value) ->
                        StatCard(labelRes = labelRes, value = value, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageTrafficCard(dashboard: DashboardDto) {
    val used = DiskStatsCalculator.usedBytes(dashboard.diskTotal, dashboard.diskFree)
    val fraction = DiskStatsCalculator.usedFraction(dashboard.diskTotal, dashboard.diskFree)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HWatchSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
        ) {
            SectionHeader(stringResource(R.string.dashboard_storage))
            if (used == null || fraction == null) {
                Text(
                    text = stringResource(R.string.dashboard_storage_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.lg)) {
                    LabeledValue(
                        labelRes = R.string.dashboard_storage_used,
                        value = ByteFormatter.formatFileSize(used),
                        modifier = Modifier.weight(1f),
                    )
                    LabeledValue(
                        labelRes = R.string.dashboard_storage_free,
                        value = ByteFormatter.formatFileSize(dashboard.diskFree),
                        modifier = Modifier.weight(1f),
                    )
                    LabeledValue(
                        labelRes = R.string.dashboard_storage_total,
                        value = ByteFormatter.formatFileSize(dashboard.diskTotal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.lg)) {
                LabeledValue(
                    labelRes = R.string.dashboard_storage_library,
                    value = ByteFormatter.formatFileSize(dashboard.libraryBytes),
                    modifier = Modifier.weight(1f),
                )
                LabeledValue(
                    labelRes = R.string.dashboard_traffic,
                    value = ByteFormatter.formatFileSize(dashboard.trafficBytes),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VideoCarouselSection(
    @StringRes titleRes: Int,
    @StringRes emptyRes: Int,
    videos: List<VideoDto>,
    baseUrl: String,
    timezone: String,
    onVideoClick: (VideoDto) -> Unit,
    showTime: Boolean = true,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
        SectionHeader(
            title = stringResource(titleRes),
            actionLabel = onSeeAll?.let { stringResource(R.string.common_see_all) },
            onAction = onSeeAll,
        )
        if (videos.isEmpty()) {
            EmptyState(icon = Icons.Filled.Movie, title = stringResource(emptyRes))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.sm)) {
                items(items = videos, key = { it.videoId }) { video ->
                    DashboardVideoCard(
                        video = video,
                        baseUrl = baseUrl,
                        timezone = timezone,
                        showTime = showTime,
                        onClick = { onVideoClick(video) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardVideoCard(
    video: VideoDto,
    baseUrl: String,
    timezone: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp,
    showTime: Boolean = true,
) {
    val ui = remember(video, timezone) { video.toVideoCardUiModel(timezone) }
    Card(modifier = modifier.width(width).clickable(onClick = onClick)) {
        Column {
            VideoThumbnail(
                baseUrl = baseUrl,
                videoId = ui.videoId,
                contentDescription = ui.primaryTitle,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 92.dp)
                    .padding(HWatchSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xxs),
            ) {
                Text(
                    text = ui.primaryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ui.secondaryTitle?.let { secondary ->
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ui.creator?.let { creator ->
                    Text(
                        text = creator,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showTime) {
                    ui.time?.let { time ->
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsSection(subscriptions: List<SubscriptionDto>, timezone: String) {
    Column(verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
        SectionHeader(stringResource(R.string.dashboard_subscriptions))
        if (subscriptions.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Subscriptions,
                title = stringResource(R.string.dashboard_empty_subscriptions),
            )
        } else {
            subscriptions.forEach { subscription ->
                SubscriptionSummaryCard(subscription = subscription, timezone = timezone)
            }
        }
    }
}

@Composable
private fun SubscriptionSummaryCard(subscription: SubscriptionDto, timezone: String) {
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
                StatusIndicator(
                    active = subscription.enabled,
                    activeText = stringResource(R.string.dashboard_subscription_enabled),
                    inactiveText = stringResource(R.string.dashboard_subscription_disabled),
                )
            }
            val lastChecked = formatTime(subscription.lastCheckedAt, timezone)
            Text(
                text = if (lastChecked != null) {
                    stringResource(R.string.dashboard_last_checked, lastChecked)
                } else {
                    stringResource(R.string.dashboard_time_unknown)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            subscription.progress?.let { progress ->
                val fraction = ProgressCalc.fraction(progress.completed, progress.total)
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
        }
    }
}

private fun formatTime(text: String, timezone: String): String? {
    if (text.isBlank()) return null
    return TimeUtils.formatDisplayFromRfc3339(text, timezone).getOrNull()
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 240)
@Composable
private fun DashboardVideoCardPreview() {
    com.hwatch.app.ui.theme.HWatchTheme {
        DashboardVideoCard(
            video = VideoDto(
                videoId = "source:3144962",
                title = "Original Title",
                translatedTitle = "中文标题",
                creatorName = "Creator",
                width = 1920,
                height = 1080,
                videoCodec = "h264",
                durationSeconds = 201,
                publishedAt = "2026-07-26",
            ),
            baseUrl = "http://192.168.0.249:8787/",
            timezone = "Asia/Shanghai",
            onClick = {},
        )
    }
}
