package com.hwatch.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.VideoThumbnail
import com.hwatch.app.ui.common.toVideoCardUiModel

/** 影视库网格卡片：16:9 缩略图 + 标题层级 + 轻量媒体元数据。 */
@Composable
fun VideoGridCard(
    video: VideoDto,
    baseUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = remember(video) { video.toVideoCardUiModel() }
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            VideoThumbnail(
                baseUrl = baseUrl,
                videoId = ui.videoId,
                contentDescription = ui.primaryTitle,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier.padding(HWatchSpacing.sm),
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
                if (ui.metadata.isNotBlank()) {
                    Text(
                        text = ui.metadata,
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 220)
@Composable
private fun VideoGridCardPreview() {
    com.hwatch.app.ui.theme.HWatchTheme {
        VideoGridCard(
            video = com.hwatch.app.data.model.VideoDto(
                videoId = "source:3144962",
                title = "Original Title",
                translatedTitle = "中文标题",
                creatorName = "Creator",
                width = 1920,
                height = 1080,
                videoCodec = "h264",
                durationSeconds = 201,
                views = 12400,
            ),
            baseUrl = "http://192.168.1.100:8787/",
            onClick = {},
            modifier = Modifier.width(200.dp),
        )
    }
}
