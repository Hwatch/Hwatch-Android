package com.hwatch.app.data.apiclient

import com.hwatch.app.di.NetworkModule
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * 将 suspend 网络调用包装为 [ApiResult]。
 *
 * - [CancellationException] 必须原样重抛，不能吞掉协程取消。
 * - [HttpException] 映射为 [ApiResult.HttpError]，并尝试从 `{"error":"..."}` 解析消息。
 * - [java.io.IOException] 映射为 [ApiResult.NetworkError]。
 * - kotlinx 序列化异常（[kotlinx.serialization.SerializationException]）映射为 [ApiResult.ParseError]。
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Unauthorized(extractErrorMessage(e))
            403 -> ApiResult.Forbidden(extractErrorMessage(e))
            else -> ApiResult.HttpError(e.code(), extractErrorMessage(e))
        }
    } catch (e: java.io.IOException) {
        ApiResult.NetworkError(e)
    } catch (e: kotlinx.serialization.SerializationException) {
        ApiResult.ParseError(e)
    }
}

internal fun extractErrorMessage(exception: HttpException): String? {
    val response = exception.response()?.errorBody() ?: return null
    val text = runCatching { response.string() }.getOrNull() ?: return null
    if (text.isBlank()) return null
    return runCatching { NetworkModule.createJson().decodeFromString<ErrorBody>(text).error }
        .getOrNull()
}