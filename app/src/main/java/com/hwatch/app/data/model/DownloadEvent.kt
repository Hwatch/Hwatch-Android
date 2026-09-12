package com.hwatch.app.data.model

/**
 * `GET /api/events` SSE 单条 data。
 * `tasks` 只包含 `waiting`、`active`、`paused` 项（活动子集快照），不含 `queued`。
 */
@kotlinx.serialization.Serializable
data class DownloadEvent(
    val tasks: List<VideoDto> = emptyList(),
    @kotlinx.serialization.SerialName("download_speed") val downloadSpeed: Long = 0,
    val connections: Int = 0,
)
