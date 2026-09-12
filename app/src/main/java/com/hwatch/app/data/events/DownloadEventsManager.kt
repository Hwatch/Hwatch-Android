package com.hwatch.app.data.events

import com.hwatch.app.data.model.DownloadEvent
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * 下载 SSE 连接管理。
 *
 * - [activate] 幂等：相同 url 重复调用不会新建第二条连接。
 * - 失败/关闭时，仅当仍 active 才调度**唯一** reconnect job（2–5s jitter）。
 * - reconnect 前先执行 [onReconnectSync] 做 REST 全量校准，再重连。
 * - [deactivate] 取消 EventSource 与 reconnect job。
 * - 单条 malformed JSON 丢弃该 event，不影响连接、不 crash。
 * - EventSource 不在 Composable 中创建。
 */
class DownloadEventsManager(
    private val transport: DownloadEventsTransport,
    private val json: Json,
    private val scope: CoroutineScope,
    private val reconnectDelayMillis: () -> Long = { Random.nextLong(MIN_RECONNECT_MS, MAX_RECONNECT_MS + 1) },
) {

    private val _events = MutableSharedFlow<DownloadEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentUrl: String? = null
    private var active = false
    private var source: DownloadEventsTransport.Cancellable? = null
    private var reconnectJob: Job? = null
    private var onReconnectSync: (suspend () -> Unit)? = null

    fun activate(url: String, onReconnectSync: suspend () -> Unit) {
        if (active && currentUrl == url) return
        cancelSourceAndReconnect()
        active = true
        currentUrl = url
        this.onReconnectSync = onReconnectSync
        open(ConnectionState.CONNECTING)
    }

    fun deactivate() {
        active = false
        cancelSourceAndReconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun cancelSourceAndReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        source?.cancel()
        source = null
    }

    private fun open(state: ConnectionState) {
        val url = currentUrl ?: return
        _connectionState.value = state
        source?.cancel()
        source = transport.connect(url, listener)
    }

    private val listener = object : DownloadEventsTransport.Listener {
        override fun onOpen() {
            _connectionState.value = ConnectionState.CONNECTED
        }

        override fun onMessage(data: String) {
            val event = runCatching {
                json.decodeFromString(DownloadEvent.serializer(), data)
            }.getOrNull() ?: return
            _events.tryEmit(event)
        }

        override fun onClosed() {
            scheduleReconnect()
        }

        override fun onFailure() {
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (!active) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            delay(reconnectDelayMillis())
            if (!active) return@launch
            onReconnectSync?.invoke()
            if (!active) return@launch
            open(ConnectionState.CONNECTING)
        }
    }

    companion object {
        const val MIN_RECONNECT_MS = 2_000L
        const val MAX_RECONNECT_MS = 5_000L
    }
}
