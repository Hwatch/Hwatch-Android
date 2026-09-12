package com.hwatch.app.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 缩略图 URL 构造，严格复用 Phase 1 的 URL 编码规则：
 * `GET /thumb/{video_id}`，`video_id` 先经 [UrlEncoding.encodePathSegment] 再作为
 * 已编码 path segment 拼入，确保 `source:3144962 → /thumb/source%3A3144962`。
 * 不使用字符串手拼，不复制新的 percent encoder。
 */
object ThumbnailUrl {

    fun build(baseUrl: String, videoId: String): String? {
        val base = baseUrl.toHttpUrlOrNull() ?: return null
        if (videoId.isEmpty()) return null
        return base.newBuilder()
            .addPathSegment("thumb")
            .addEncodedPathSegment(UrlEncoding.encodePathSegment(videoId))
            .build()
            .toString()
    }
}
