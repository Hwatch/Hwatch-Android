package com.hwatch.app.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hwatch.app.R
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.model.VideoStatus
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.VideoThumbnail
import com.hwatch.app.ui.common.toVideoCardUiModel
import com.hwatch.app.util.ByteFormatter
import com.hwatch.app.util.DownloadProgress

/** 下载任务卡：缩略图 + 标题 + 状态 + 进度 + 按状态提供的操作。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadTaskCard(
    video: VideoDto,
    baseUrl: String,
    pending: Boolean,
    onPlay: () -> Unit,
    onAction: (DownloadAction) -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = remember(video) { video.toVideoCardUiModel() }
    val status = video.status
    val clickable = if (status == VideoStatus.COMPLETED) {
        modifier.fillMaxWidth().clickable(onClick = onPlay)
    } else {
        modifier.fillMaxWidth()
    }
    Card(
        modifier = clickable,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(HWatchSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
        ) {
            VideoThumbnail(
                baseUrl = baseUrl,
                videoId = ui.videoId,
                contentDescription = ui.primaryTitle,
                modifier = Modifier.width(120.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xxs),
            ) {
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
                StatusChip(status)
                if (status in PROGRESS_STATUSES) {
                    val fraction = DownloadProgress.fraction(video.bytesDownloaded, video.totalBytes)
                    if (fraction != null) {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(
                                R.string.downloads_bytes_progress,
                                ByteFormatter.formatFileSize(video.bytesDownloaded),
                                ByteFormatter.formatFileSize(video.totalBytes),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                if (status == VideoStatus.ACTIVE) {
                    Text(
                        text = ByteFormatter.formatDownloadSpeed(video.downloadSpeed) +
                            " · " + stringResource(R.string.downloads_connections_short, video.connections),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
                    availableActions(status).forEach { action ->
                        if (action == DownloadAction.DELETE) {
                            TextButton(onClick = onDeleteRequest, enabled = !pending) {
                                Text(stringResource(downloadActionRes(action)))
                            }
                        } else {
                            TextButton(onClick = { onAction(action) }, enabled = !pending) {
                                Text(stringResource(downloadActionRes(action)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: VideoStatus) {
    val (container, content) = statusColors(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = HWatchSpacing.xs, vertical = HWatchSpacing.xxs),
    ) {
        Text(
            text = stringResource(downloadStatusRes(status)),
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
private fun statusColors(status: VideoStatus): Pair<Color, Color> = when (status) {
    VideoStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    VideoStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    VideoStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
}

private val PROGRESS_STATUSES = setOf(
    VideoStatus.WAITING,
    VideoStatus.ACTIVE,
    VideoStatus.PAUSED,
)
