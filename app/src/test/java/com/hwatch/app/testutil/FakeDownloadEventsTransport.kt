package com.hwatch.app.testutil

import com.hwatch.app.data.events.DownloadEventsTransport

/** JVM Fake SSE 传输：记录连接、支持触发 listener 回调。 */
class FakeDownloadEventsTransport : DownloadEventsTransport {

    class Connection(
        val url: String,
        val listener: DownloadEventsTransport.Listener,
    ) {
        var cancelled = false
            private set

        fun cancel() {
            cancelled = true
        }
    }

    val connections = mutableListOf<Connection>()

    override fun connect(
        url: String,
        listener: DownloadEventsTransport.Listener,
    ): DownloadEventsTransport.Cancellable {
        val connection = Connection(url, listener)
        connections.add(connection)
        return DownloadEventsTransport.Cancellable { connection.cancel() }
    }

    fun latest(): Connection = connections.last()

    val connectionCount: Int get() = connections.size

    val activeCount: Int get() = connections.count { !it.cancelled }
}
