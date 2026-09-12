package com.hwatch.app.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 播放地址构造，严格复用 Phase 1 的 URL 编码规则：
 * `GET /media/{video_id}`，`video_id` 先经 [UrlEncoding.encodePathSegment] 再作为
 * 已编码 path segment 拼入，确保 `source:3144962 → /media/source%3A3144962`，
 * 且不会二次编码（`%3A` 不会变成 `%253A`）。
 */
object MediaUrl {

    fun build(baseUrl: String, videoId: String): String? {
        val base = baseUrl.toHttpUrlOrNull() ?: return null
        if (videoId.isEmpty()) return null
        return base.newBuilder()
            .addPathSegment("media")
            .addEncodedPathSegment(UrlEncoding.encodePathSegment(videoId))
            .build()
            .toString()
    }
}
