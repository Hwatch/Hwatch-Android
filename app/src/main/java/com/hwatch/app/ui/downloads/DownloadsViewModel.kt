package com.hwatch.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.events.DownloadEventsManager
import com.hwatch.app.data.events.DownloadEventsTransport
import com.hwatch.app.data.model.DownloadEvent
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.repository.DownloadRepository
import com.hwatch.app.util.EventsUrl
import java.net.SocketTimeoutException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * 下载页 ViewModel。
 *
 * - SSE 是活动子集（waiting/active/paused）快照。检测到任务离开活动子集时，
 *   触发一次**合并的** REST `/api/tasks` 校准（默认 1s 延迟等待多个任务一起收敛），
 *   由 REST 决定真实新状态（completed/failed/deleted…），绝不本地猜状态。
 * - 所有 REST 同步（activate / refresh / reconnect / departure calibration）经同一
 *   [syncMutex] 串行化，避免互相覆盖；同一时刻最多一个 calibration job。
 * - 202 只清 pending，不 optimistic 改 status。
 */
class DownloadsViewModel(
    private val repository: DownloadRepository,
    transport: DownloadEventsTransport,
    json: Json,
    private val baseUrlProvider: suspend () -> String,
    private val calibrationDelayMillis: () -> Long = { DEFAULT_CALIBRATION_DELAY_MS },
) : ViewModel() {

    private val manager = DownloadEventsManager(transport, json, viewModelScope)

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val syncMutex = Mutex()

    private var active = false
    private var messageId = 0L
    private var previousActiveIds: Set<String> = emptySet()
    private var calibrationJob: Job? = null

    init {
        viewModelScope.launch {
            manager.events.collect { event -> applyEvent(event) }
        }
        viewModelScope.launch {
            manager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    fun activate() {
        if (active) return
        active = true
        previousActiveIds = emptySet()
        viewModelScope.launch {
            syncTasks(isRefresh = _uiState.value.tasks.isNotEmpty(), silent = false)
            loadPausedState()
            connectEvents()
        }
    }

    fun deactivate() {
        active = false
        calibrationJob?.cancel()
        calibrationJob = null
        manager.deactivate()
    }

    private suspend fun connectEvents() {
        if (!active) return
        val url = EventsUrl.build(baseUrlProvider()) ?: return
        manager.activate(url) { syncTasks(isRefresh = true, silent = true) }
    }

    fun refresh() {
        viewModelScope.launch { syncTasks(isRefresh = _uiState.value.tasks.isNotEmpty(), silent = false) }
    }

    fun retry() {
        viewModelScope.launch { syncTasks(isRefresh = false, silent = false) }
    }

    fun onAction(videoId: String, action: DownloadAction) {
        if (videoId in _uiState.value.pendingActions) return
        _uiState.update { it.copy(pendingActions = it.pendingActions + videoId) }
        viewModelScope.launch {
            when (repository.videoAction(videoId, action.wire)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            pendingActions = it.pendingActions - videoId,
                            message = nextMessage(DownloadsMessage.Kind.ACTION_SUBMITTED),
                        )
                    }
                    if (action == DownloadAction.DELETE) refresh()
                }
                else -> _uiState.update {
                    it.copy(
                        pendingActions = it.pendingActions - videoId,
                        message = nextMessage(DownloadsMessage.Kind.ACTION_FAILED),
                    )
                }
            }
        }
    }

    fun downloadAll() {
        viewModelScope.launch {
            when (val result = repository.downloadAll()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        message = nextMessage(
                            DownloadsMessage.Kind.DOWNLOAD_ALL_ACCEPTED,
                            result.data.accepted,
                        ),
                    )
                }
                else -> _uiState.update {
                    it.copy(message = nextMessage(DownloadsMessage.Kind.DOWNLOAD_ALL_FAILED))
                }
            }
        }
    }

    private suspend fun loadPausedState() {
        when (val result = repository.getDownloadsPaused()) {
            is ApiResult.Success -> _uiState.update { it.copy(isGloballyPaused = result.data) }
            else -> Unit
        }
    }

    fun toggleGlobalPause() {
        if (_uiState.value.isTogglingPause) return
        val target = !_uiState.value.isGloballyPaused
        _uiState.update { it.copy(isTogglingPause = true) }
        viewModelScope.launch {
            when (val result = repository.toggleDownloads(target)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isGloballyPaused = result.data.paused, isTogglingPause = false)
                }
                else -> _uiState.update {
                    it.copy(
                        isTogglingPause = false,
                        message = nextMessage(DownloadsMessage.Kind.ACTION_FAILED),
                    )
                }
            }
        }
    }

    fun loadDiscovered(refresh: Boolean = false) {
        val state = _uiState.value
        if (state.discoveredLoading || state.discoveredLoadingMore) return
        val offset = if (refresh) 0 else state.discovered.size
        _uiState.update {
            it.copy(
                discoveredLoading = refresh || it.discovered.isEmpty(),
                discoveredLoadingMore = !refresh && it.discovered.isNotEmpty(),
            )
        }
        viewModelScope.launch {
            when (val result = repository.getDiscovered(offset = offset)) {
                is ApiResult.Success -> _uiState.update { current ->
                    val items = if (refresh) result.data.items else current.discovered + result.data.items
                    current.copy(
                        discovered = items,
                        discoveredHasMore = result.data.items.size >= DownloadRepository.DISCOVERED_LIMIT &&
                            items.size.toLong() < result.data.total,
                        discoveredLoading = false,
                        discoveredLoadingMore = false,
                        discoveredError = null,
                    )
                }
                is ApiResult.HttpError -> onDiscoveredFailure(DownloadsError.HTTP)
                is ApiResult.ParseError -> onDiscoveredFailure(DownloadsError.PARSE)
                is ApiResult.NetworkError -> onDiscoveredFailure(
                    if (result.cause is SocketTimeoutException) DownloadsError.TIMEOUT else DownloadsError.UNREACHABLE,
                )
            }
        }
    }

    private fun onDiscoveredFailure(error: DownloadsError) {
        _uiState.update {
            it.copy(
                discoveredLoading = false,
                discoveredLoadingMore = false,
                discoveredError = error,
            )
        }
    }

    private suspend fun syncTasks(isRefresh: Boolean, silent: Boolean) {
        syncMutex.withLock {
            if (!silent) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = !isRefresh && it.tasks.isEmpty(),
                        isRefreshing = isRefresh,
                        initialError = if (isRefresh) it.initialError else null,
                    )
                }
            }
            when (val result = repository.getTasks()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        tasks = result.data.items,
                        isInitialLoading = false,
                        isRefreshing = false,
                        initialError = null,
                    )
                }
                is ApiResult.HttpError -> onTasksFailure(isRefresh, silent, DownloadsError.HTTP)
                is ApiResult.ParseError -> onTasksFailure(isRefresh, silent, DownloadsError.PARSE)
                is ApiResult.NetworkError -> onTasksFailure(
                    isRefresh,
                    silent,
                    if (result.cause is SocketTimeoutException) DownloadsError.TIMEOUT else DownloadsError.UNREACHABLE,
                )
            }
        }
    }

    private fun onTasksFailure(isRefresh: Boolean, silent: Boolean, error: DownloadsError) {
        if (silent) return
        _uiState.update { state ->
            if (isRefresh && state.tasks.isNotEmpty()) {
                state.copy(
                    isRefreshing = false,
                    message = nextMessage(DownloadsMessage.Kind.REFRESH_FAILED),
                )
            } else {
                state.copy(isInitialLoading = false, isRefreshing = false, initialError = error)
            }
        }
    }

    private fun applyEvent(event: DownloadEvent) {
        val currentIds = event.tasks.mapTo(HashSet()) { it.videoId }
        val departed = previousActiveIds - currentIds
        previousActiveIds = currentIds
        _uiState.update { state ->
            state.copy(
                tasks = mergeTasks(state.tasks, event.tasks),
                downloadSpeed = event.downloadSpeed,
                connections = event.connections,
            )
        }
        if (departed.isNotEmpty()) scheduleCalibration()
    }

    private fun scheduleCalibration() {
        if (calibrationJob?.isActive == true) return
        calibrationJob = viewModelScope.launch {
            delay(calibrationDelayMillis())
            if (!active) return@launch
            syncTasks(isRefresh = false, silent = true)
        }
    }

    private fun nextMessage(kind: DownloadsMessage.Kind, count: Int = 0): DownloadsMessage {
        messageId += 1
        return DownloadsMessage(id = messageId, kind = kind, count = count)
    }

    companion object {
        const val DEFAULT_CALIBRATION_DELAY_MS = 1_000L

        /**
         * SSE `tasks` 是 {waiting, active, paused} 的活动子集快照。合并规则：
         * 以 baseline 顺序为准，命中 SSE 的项用 SSE 版本替换；baseline 中属于活动子集
         * 但不在 SSE 中的项移除；非活动子集项保留；SSE 新增项追加。
         */
        internal fun mergeTasks(baseline: List<VideoDto>, incoming: List<VideoDto>): List<VideoDto> {
            if (incoming.isEmpty()) return baseline
            val incomingById = incoming.associateBy { it.videoId }
            val result = ArrayList<VideoDto>(baseline.size + incoming.size)
            val added = HashSet<String>(incoming.size)
            for (task in baseline) {
                val updated = incomingById[task.videoId]
                when {
                    updated != null -> {
                        result.add(updated)
                        added.add(task.videoId)
                    }
                    task.status.wire in ACTIVE_STATUSES -> Unit
                    else -> result.add(task)
                }
            }
            for (task in incoming) {
                if (added.add(task.videoId)) result.add(task)
            }
            return result
        }

        private val ACTIVE_STATUSES = setOf("waiting", "active", "paused")
    }
}
