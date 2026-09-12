package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.data.auth.AuthLogoutResponse
import com.hwatch.app.data.auth.AuthSessionDto
import com.hwatch.app.data.auth.AuthSessionManager
import com.hwatch.app.data.auth.AuthStatusDto
import com.hwatch.app.data.auth.Credentials
import com.hwatch.app.data.auth.ServerOrigin

/**
 * 认证数据源。严格按 API_V1.md：
 * - 公开：`GET /api/auth/status`、`POST /api/auth/setup`、`POST /api/auth/login`
 * - 受保护：`GET /api/auth/me`、`POST /api/auth/logout`（CSRF 由 AuthInterceptor 注入）
 * session cookie 由 [com.hwatch.app.data.auth.HWatchCookieJar] 捕获；CSRF/username 从响应体写入 session。
 */
class AuthRepository(
    private val apiProvider: suspend () -> Result<HWatchApi>,
    private val sessionManager: AuthSessionManager,
    private val originProvider: suspend () -> ServerOrigin?,
) {

    suspend fun status(): ApiResult<AuthStatusDto> = withApi { it.authStatus() }

    suspend fun setup(username: String, password: String): ApiResult<AuthSessionDto> {
        val result = withApi { it.authSetup(Credentials(username, password)) }
        applySession(result)
        return result
    }

    suspend fun login(username: String, password: String): ApiResult<AuthSessionDto> {
        val result = withApi { it.authLogin(Credentials(username, password)) }
        applySession(result)
        return result
    }

    suspend fun me(): ApiResult<AuthSessionDto> {
        val result = withApi { it.authMe() }
        applySession(result)
        return result
    }

    suspend fun logout(): ApiResult<AuthLogoutResponse> {
        val result = withApi { it.authLogout() }
        sessionManager.clear()
        return result
    }

    private suspend fun applySession(result: ApiResult<AuthSessionDto>) {
        if (result is ApiResult.Success && result.data.authenticated) {
            val origin = originProvider() ?: return
            sessionManager.setCredentials(
                origin = origin,
                username = result.data.username.orEmpty(),
                csrfToken = result.data.csrfToken.orEmpty(),
            )
        }
    }

    private suspend fun <T> withApi(block: suspend (HWatchApi) -> T): ApiResult<T> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall { block(api) }
    }
}
