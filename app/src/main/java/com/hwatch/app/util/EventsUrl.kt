package com.hwatch.app.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** SSE 事件地址：`GET /api/events`。 */
object EventsUrl {
    fun build(baseUrl: String): String? {
        val base = baseUrl.toHttpUrlOrNull() ?: return null
        return base.newBuilder().addPathSegments("api/events").build().toString()
    }
}
