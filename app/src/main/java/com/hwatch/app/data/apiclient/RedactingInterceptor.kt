package com.hwatch.app.data.apiclient

import android.util.Log
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 安全日志拦截器。
 *
 * - 所有构建：仅记录请求行 `METHOD scheme://host:port/encodedPath`，**不包含 query**、
 *   不包含 header、不包含 body，避免经 query/header/body 泄露凭据、搜索词等敏感信息。
 * - Debug 构建：对以 `/api/` 开头的路径（排除 `/api/settings` 与 `/api/events`）的
 *   非二进制响应，
 *   允许最多 [MAX_BODY_LOG_CHARS] 字符的 body 日志（响应经 [Response.peekBody] 复制，
 *   不消费原始流）。
 * - `GET/PUT /api/settings` 永远禁止 body 日志（响应可能含 `aria_secret`、`http_proxy`
 *   中的凭据）。
 * - Release 构建：`enableBodyLogging = false`，无任何 body 级日志。
 */
class RedactingInterceptor(
    private val enableBodyLogging: Boolean,
    private val logger: (String) -> Unit = { message -> Log.d(TAG, message) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger(describeRequest(request.method, request.url))
        val response = chain.proceed(request)
        val contentType = response.header("Content-Type")
        if (enableBodyLogging &&
            shouldLogBody(request.url.encodedPath) &&
            !isBinaryContentType(contentType)
        ) {
            runCatching {
                val body = response.peekBody(MAX_BODY_PEEK_BYTES)
                val text = body.string().take(MAX_BODY_LOG_CHARS)
                if (text.isNotBlank()) {
                    logger("-> ${response.code} ${response.message}: $text")
                }
            }
        }
        return response
    }

    companion object {
        const val TAG = "HWatch/HTTP"
        const val MAX_BODY_PEEK_BYTES = 4096L
        const val MAX_BODY_LOG_CHARS = 2048

        /** 是否允许对该路径的 body 做日志。 */
        internal fun shouldLogBody(requestPath: String): Boolean {
            if (!requestPath.startsWith("/api/")) return false
            val apiPath = requestPath.substringAfter("/api/", "")
            if (apiPath.isEmpty()) return false
            return apiPath != "settings" && apiPath != "events"
        }

        /** 请求行：不含 query，避免把搜索词/参数写进日志。 */
        internal fun describeRequest(method: String, url: HttpUrl): String {
            val port = if (url.scheme == "https" && url.port == 443) "" else ":${url.port}"
            return "$method ${url.scheme}://${url.host}$port${url.encodedPath}"
        }

        /** 二进制响应不允许 body 日志。 */
        internal fun isBinaryContentType(contentType: String?): Boolean {
            val lower = contentType?.lowercase(Locale.US) ?: return false
            return lower.startsWith("image/") ||
                lower.startsWith("video/") ||
                lower.startsWith("audio/") ||
                lower.contains("octet-stream")
        }
    }
}