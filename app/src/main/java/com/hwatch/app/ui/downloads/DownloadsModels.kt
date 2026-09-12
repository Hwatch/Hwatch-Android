package com.hwatch.app.ui.downloads

import androidx.annotation.StringRes
import com.hwatch.app.R
import com.hwatch.app.data.events.ConnectionState
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.model.VideoStatus

enum class DownloadsError {
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
}

enum class DownloadAction(val wire: String) {
    DOWNLOAD("download"),
    RETRY("retry"),
    PAUSE("pause"),
    RESUME("resume"),
    DELETE("delete"),
}

/**
 * 依据 APIV1.md：状态不做本地不可逆状态机；操作仅表示“可对服务端发起的动作”。
 */
fun availableActions(status: VideoStatus): List<DownloadAction> = when (status) {
    VideoStatus.ACTIVE, VideoStatus.WAITING -> listOf(DownloadAction.PAUSE, DownloadAction.DELETE)
    VideoStatus.PAUSED -> listOf(DownloadAction.RESUME, DownloadAction.DELETE)
    VideoStatus.FAILED -> listOf(DownloadAction.RETRY, DownloadAction.DELETE)
    VideoStatus.QUEUED -> listOf(DownloadAction.DELETE)
    VideoStatus.COMPLETED -> listOf(DownloadAction.DELETE)
    VideoStatus.DISCOVERED, VideoStatus.EXCLUDED -> listOf(DownloadAction.DOWNLOAD)
    VideoStatus.UNKNOWN -> listOf(DownloadAction.DELETE)
}

@StringRes
fun downloadStatusRes(status: VideoStatus): Int = when (status) {
    VideoStatus.DISCOVERED -> R.string.download_status_discovered
    VideoStatus.QUEUED -> R.string.download_status_queued
    VideoStatus.WAITING -> R.string.download_status_waiting
    VideoStatus.ACTIVE -> R.string.download_status_active
    VideoStatus.PAUSED -> R.string.download_status_paused
    VideoStatus.FAILED -> R.string.download_status_failed
    VideoStatus.EXCLUDED -> R.string.download_status_excluded
    VideoStatus.COMPLETED -> R.string.download_status_completed
    VideoStatus.UNKNOWN -> R.string.download_status_unknown
}

@StringRes
fun downloadActionRes(action: DownloadAction): Int = when (action) {
    DownloadAction.DOWNLOAD -> R.string.download_action_download
    DownloadAction.RETRY -> R.string.download_action_retry
    DownloadAction.PAUSE -> R.string.download_action_pause
    DownloadAction.RESUME -> R.string.download_action_resume
    DownloadAction.DELETE -> R.string.download_action_delete
}

@StringRes
fun connectionStateRes(state: ConnectionState): Int = when (state) {
    ConnectionState.CONNECTED -> R.string.downloads_connection_connected
    ConnectionState.CONNECTING -> R.string.downloads_connection_connecting
    ConnectionState.RECONNECTING -> R.string.downloads_connection_reconnecting
    ConnectionState.DISCONNECTED -> R.string.downloads_connection_disconnected
}

/** 一次性 Snackbar 消息。 */
data class DownloadsMessage(
    val id: Long,
    val kind: Kind,
    val count: Int = 0,
) {
    enum class Kind {
        ACTION_SUBMITTED,
        ACTION_FAILED,
        REFRESH_FAILED,
        DOWNLOAD_ALL_ACCEPTED,
        DOWNLOAD_ALL_FAILED,
    }
}

data class DownloadsUiState(
    val tasks: List<VideoDto> = emptyList(),
    val downloadSpeed: Long = 0,
    val connections: Int = 0,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val initialError: DownloadsError? = null,
    val pendingActions: Set<String> = emptySet(),
    val message: DownloadsMessage? = null,
    val isGloballyPaused: Boolean = false,
    val isTogglingPause: Boolean = false,
    val discovered: List<VideoDto> = emptyList(),
    val discoveredLoading: Boolean = false,
    val discoveredLoadingMore: Boolean = false,
    val discoveredHasMore: Boolean = false,
    val discoveredError: DownloadsError? = null,
)
