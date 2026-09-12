package com.hwatch.app.ui.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

/** 用户可理解的播放错误类别。 */
enum class PlaybackErrorKind {
    NETWORK,
    NOT_FOUND,
    SERVER,
    UNSUPPORTED,
    DECODER,
    UNKNOWN,
}

/**
 * 把 [PlaybackException] 映射为 [PlaybackErrorKind]，不向 UI 泄露 stack trace / URL。
 * HTTP 404 与其它 HTTP 状态通过 `HttpDataSource.InvalidResponseCodeException` 区分。
 */
object PlaybackErrorMapper {

    fun map(error: PlaybackException): PlaybackErrorKind =
        map(
            errorCode = error.errorCode,
            httpStatusCode = httpStatusFrom(error.cause),
        )

    /** 从 cause 提取 HTTP 状态码（非 HTTP 响应异常返回 null）。 */
    internal fun httpStatusFrom(cause: Throwable?): Int? =
        (cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode

    internal fun map(errorCode: Int, httpStatusCode: Int?): PlaybackErrorKind {
        if (httpStatusCode != null) {
            return if (httpStatusCode == 404) PlaybackErrorKind.NOT_FOUND else PlaybackErrorKind.SERVER
        }
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            -> PlaybackErrorKind.NETWORK

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackErrorKind.NOT_FOUND

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> PlaybackErrorKind.SERVER

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            -> PlaybackErrorKind.UNSUPPORTED

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            -> PlaybackErrorKind.DECODER

            else -> PlaybackErrorKind.UNKNOWN
        }
    }
}
