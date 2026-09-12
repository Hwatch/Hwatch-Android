package com.hwatch.app.ui.common

import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.util.MediaFormatters
import com.hwatch.app.util.TimeUtils
import com.hwatch.app.util.videoDisplayTitle

/**
 * 视频卡的轻量 UI model。只做展示所需字段的映射，避免在 Composable 中重复计算，
 * 也便于单测（标题优先级、metadata、时间 fallback、date-only published_at）。
 */
data class VideoCardUiModel(
    val videoId: String,
    val primaryTitle: String,
    val secondaryTitle: String?,
    val creator: String?,
    val metadata: String,
    val time: String?,
)

fun VideoDto.toVideoCardUiModel(timezone: String? = null): VideoCardUiModel {
    val title = videoDisplayTitle(translatedTitle, title)
    return VideoCardUiModel(
        videoId = videoId,
        primaryTitle = title.primary,
        secondaryTitle = title.secondary,
        creator = creatorName.takeIf { it.isNotBlank() },
        metadata = buildMetadata(this),
        time = timezone?.let { formatVideoTime(this, it) },
    )
}

private fun buildMetadata(video: VideoDto): String = buildList {
    MediaFormatters.formatResolution(video.width, video.height)?.let(::add)
    video.videoCodec.takeIf { it.isNotBlank() }?.let(::add)
    MediaFormatters.formatDuration(video.durationSeconds)?.let(::add)
}.joinToString(" · ")

private fun formatVideoTime(video: VideoDto, timezone: String): String? {
    val candidates = listOf(video.downloadedAt, video.publishedAt, video.discoveredAt, video.updatedAt)
    for (candidate in candidates) {
        if (candidate.isBlank()) continue
        TimeUtils.formatDisplayFromRfc3339(candidate, timezone).getOrNull()?.let { return it }
    }
    return null
}
