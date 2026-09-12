package com.hwatch.app.data.events

/**
 * SSE 传输薄抽象，便于 JVM 单测注入 Fake，不依赖真实网络。
 */
interface DownloadEventsTransport {

    fun connect(url: String, listener: Listener): Cancellable

    fun interface Cancellable {
        fun cancel()
    }

    interface Listener {
        fun onOpen()
        fun onMessage(data: String)
        fun onClosed()
        fun onFailure()
    }
}
