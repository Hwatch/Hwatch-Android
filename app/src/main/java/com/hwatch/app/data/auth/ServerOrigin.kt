package com.hwatch.app.data.auth

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * HWatch 服务器 origin：scheme + host + effective port。
 * Cookie 语义不按端口隔离，因此必须自行按 origin 隔离 session，避免把
 * 8787 的 hwatch_session 发给同 host 的 9000。
 */
data class ServerOrigin(
    val scheme: String,
    val host: String,
    val port: Int,
) {
    fun key(): String = "$scheme://$host:$port"

    companion object {
        fun from(url: HttpUrl): ServerOrigin =
            ServerOrigin(scheme = url.scheme, host = url.host, port = url.port)

        fun fromKey(key: String): ServerOrigin? =
            key.toHttpUrlOrNull()?.let(::from)
    }
}
