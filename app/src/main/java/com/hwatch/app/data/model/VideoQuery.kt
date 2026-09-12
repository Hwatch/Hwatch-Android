package com.hwatch.app.data.model

/**
 * `GET /api/videos` 的请求参数模型（简单参数对象，不做 domain 抽象）。
 * `sort` / `order` / `scope` / `tagMode` 保持 String，兼容服务端后续扩展；
 * UI 层只允许生成白名单内的值。
 */
data class VideoQuery(
    val limit: Int = DEFAULT_LIMIT,
    val offset: Int = 0,
    val status: String? = DEFAULT_STATUS,
    val language: String? = null,
    val q: String? = null,
    val scope: String = DEFAULT_SCOPE,
    val creator: String? = null,
    val tags: List<String> = emptyList(),
    val tagMode: String? = null,
    val excludeTags: List<String> = emptyList(),
    val codec: String? = null,
    val minHeight: Int? = null,
    val sort: String? = null,
    val order: String? = null,
) {
    companion object {
        const val DEFAULT_LIMIT = 48
        const val DEFAULT_STATUS = "completed"
        const val DEFAULT_SCOPE = "all"
    }
}
