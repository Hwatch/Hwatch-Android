package com.hwatch.app.data.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 自动为「当前 HWatch origin」的写请求注入 `X-CSRF-Token`（GET/HEAD/OPTIONS 不加），
 * 公开 auth 路径（status/setup/login）不加。绝不向其他 origin（含外部链接）注入。
 * 受保护路径返回 401 时清除全局会话（login/setup/status 除外），驱动 Root 回登录。
 */
class AuthInterceptor(
    private val manager: AuthSessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val origin = ServerOrigin.from(request.url)
        val session = manager.sessionFor(origin)
        val path = request.url.encodedPath
        val builder = request.newBuilder()
        if (session != null &&
            session.csrfToken.isNotEmpty() &&
            request.method !in SAFE_METHODS &&
            path !in PUBLIC_AUTH_PATHS
        ) {
            builder.header(CSRF_HEADER, session.csrfToken)
        }
        val response = chain.proceed(builder.build())
        if (response.code == 401 && path !in PUBLIC_AUTH_PATHS) {
            manager.clear()
        }
        return response
    }

    companion object {
        const val CSRF_HEADER = "X-CSRF-Token"
        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val PUBLIC_AUTH_PATHS = setOf(
            "/api/auth/status",
            "/api/auth/setup",
            "/api/auth/login",
        )
    }
}
