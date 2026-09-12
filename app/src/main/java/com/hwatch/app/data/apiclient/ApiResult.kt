package com.hwatch.app.data.apiclient

import kotlinx.serialization.Serializable

/**
 * 统一的 API 结果类型。UI 层只消费该结果，不让 Retrofit 异常直接进入 Compose。
 *
 * - [Success]：请求成功并解析成功
 * - [HttpError]：服务端返回非 2xx，message 尽量从 `{"error":"..."}` 中提取
 * - [NetworkError]：网络不可达 / NAS 离线 / 超时等 IO 异常
 * - [ParseError]：HTTP 成功但 JSON 解析失败
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>

    data class HttpError(val code: Int, val message: String?) : ApiResult<Nothing>

    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>

    data class ParseError(val cause: Throwable) : ApiResult<Nothing>
}

@Serializable
internal data class ErrorBody(
    val error: String? = null,
    val code: String? = null,
)