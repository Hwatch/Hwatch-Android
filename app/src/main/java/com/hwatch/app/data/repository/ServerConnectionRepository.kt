package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.util.ServerUrl
import java.net.SocketTimeoutException

/** Server Setup 失败原因分类，UI 据此映射为用户可理解文案。 */
enum class ConnectionFailure {
    INVALID_URL,
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
}

sealed interface ServerConnectionResult {
    data class Success(val normalizedUrl: String) : ServerConnectionResult

    /**
     * @param kind 用户可理解的原因分类
     * @param detail 脱敏技术细节（仅 Debug 展示），如 `JsonDecodingException: ... external_links[0]`
     */
    data class Failure(
        val kind: ConnectionFailure,
        val detail: String? = null,
    ) : ServerConnectionResult
}

/**
 * 仅用于 Server Setup 的“测试并连接”：规范化地址 → 构建临时 [HWatchApi] →
 * `GET /api/dashboard?language=...`。不保存任何配置，保存由 ViewModel 在成功后执行。
 */
class ServerConnectionRepository(
    private val buildApi: (String) -> Result<HWatchApi>,
) {

    suspend fun testConnection(rawUrl: String, language: String): ServerConnectionResult {
        val normalized = ServerUrl.normalize(rawUrl).getOrElse {
            return ServerConnectionResult.Failure(ConnectionFailure.INVALID_URL)
        }
        val api = buildApi(normalized).getOrElse {
            return ServerConnectionResult.Failure(ConnectionFailure.INVALID_URL)
        }
        return when (val result = safeApiCall { api.dashboard(language) }) {
            is ApiResult.Success -> ServerConnectionResult.Success(normalized)
            is ApiResult.HttpError -> ServerConnectionResult.Failure(ConnectionFailure.HTTP)
            is ApiResult.ParseError -> ServerConnectionResult.Failure(
                kind = ConnectionFailure.PARSE,
                detail = sanitizeParseError(result.cause),
            )
            is ApiResult.NetworkError -> ServerConnectionResult.Failure(
                if (result.cause is SocketTimeoutException) {
                    ConnectionFailure.TIMEOUT
                } else {
                    ConnectionFailure.UNREACHABLE
                },
            )
        }
    }

    /** 仅保留异常类型与消息，截断，避免泄露 stack trace / 敏感内容。 */
    private fun sanitizeParseError(cause: Throwable): String {
        val type = cause::class.simpleName ?: "ParseError"
        val message = cause.message?.replace(Regex("\\s+"), " ")?.take(240)
        return if (message.isNullOrBlank()) type else "$type: $message"
    }
}
