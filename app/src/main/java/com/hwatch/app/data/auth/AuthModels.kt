package com.hwatch.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `POST /api/auth/setup|login` 请求体（严格 `{username,password}`）。 */
@Serializable
data class Credentials(
    val username: String,
    val password: String,
)

/** `GET /api/auth/status`（公开）。 */
@Serializable
data class AuthStatusDto(
    val configured: Boolean = false,
    val authenticated: Boolean = false,
    val username: String? = null,
    @SerialName("csrf_token") val csrfToken: String? = null,
)

/** `POST /api/auth/setup|login` 成功响应；`GET /api/auth/me` 同结构。 */
@Serializable
data class AuthSessionDto(
    val authenticated: Boolean = false,
    val username: String? = null,
    @SerialName("csrf_token") val csrfToken: String? = null,
)

/** `POST /api/auth/logout` 响应。 */
@Serializable
data class AuthLogoutResponse(
    val authenticated: Boolean = false,
)

/** 认证状态（供 Root flow 使用）。 */
sealed interface AuthState {
    data object Unknown : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val username: String) : AuthState
}

/** 可持久化的会话（token 为 hwatch_session 值，仅存于加密存储）。 */
@Serializable
data class StoredAuthSession(
    val origin: String,
    val token: String,
    val csrfToken: String,
    val username: String,
    val expiresAtMillis: Long,
)
