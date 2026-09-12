package com.hwatch.app.data.events

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/** 基于 okhttp-sse 的生产传输实现（复用共享/派生的 streaming OkHttpClient）。 */
class OkHttpDownloadEventsTransport(
    client: OkHttpClient,
) : DownloadEventsTransport {

    private val factory = EventSources.createFactory(client)

    override fun connect(
        url: String,
        listener: DownloadEventsTransport.Listener,
    ): DownloadEventsTransport.Cancellable {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()
        val source = factory.newEventSource(
            request,
            object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    listener.onOpen()
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    listener.onMessage(data)
                }

                override fun onClosed(eventSource: EventSource) {
                    listener.onClosed()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?,
                ) {
                    listener.onFailure()
                }
            },
        )
        return DownloadEventsTransport.Cancellable { source.cancel() }
    }
}
