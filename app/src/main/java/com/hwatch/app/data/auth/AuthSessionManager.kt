package com.hwatch.app.data.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.HttpUrl

/**
 * HWatch 会话/CSRF 的单一来源。session 按 [ServerOrigin]（scheme+host+port）隔离，
 * 不同 origin（含同 host 不同端口）绝不共享。只保存 hwatch_session，不保存第三方 Cookie。
 */
class AuthSessionManager(
    private val store: AuthSessionStore,
    private val scope: CoroutineScope,
) {

    data class Session(
        val origin: ServerOrigin,
        val token: String,
        val csrfToken: String,
        val username: String,
        val expiresAtMillis: Long,
        val cookie: Cookie,
    )

    private val _state = MutableStateFlow<AuthState>(AuthState.Unknown)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    @Volatile
    private var current: Session? = null

    /** 从加密存储恢复（若未过期）。由 AuthViewModel 启动时调用。 */
    suspend fun restore() {
        val stored = store.load()
        if (stored == null) {
            current = null
            _state.value = AuthState.Unauthenticated
            return
        }
        val origin = ServerOrigin.fromKey(stored.origin)
        if (origin == null || stored.expiresAtMillis <= System.currentTimeMillis()) {
            store.clear()
            current = null
            _state.value = AuthState.Unauthenticated
            return
        }
        val cookie = buildCookie(origin, stored.token, stored.expiresAtMillis)
        current = Session(
            origin = origin,
            token = stored.token,
            csrfToken = stored.csrfToken,
            username = stored.username,
            expiresAtMillis = stored.expiresAtMillis,
            cookie = cookie,
        )
        _state.value = AuthState.Authenticated(stored.username)
    }

    /** CookieJar 收到 Set-Cookie 时调用；空值表示服务端清除会话。 */
    fun onCookie(url: HttpUrl, cookie: Cookie) {
        if (cookie.name != SESSION_COOKIE) return
        if (cookie.value.isEmpty()) {
            clear()
            return
        }
        val origin = ServerOrigin.from(url)
        val existing = current
        val session = Session(
            origin = origin,
            token = cookie.value,
            csrfToken = if (existing?.origin == origin) existing.csrfToken else "",
            username = if (existing?.origin == origin) existing.username else "",
            expiresAtMillis = cookie.expiresAt,
            cookie = cookie,
        )
        current = session
        persist(session)
    }

    /** login/setup/me 成功后写入 username + CSRF（cookie 已由 CookieJar 写入）。 */
    fun setCredentials(origin: ServerOrigin, username: String, csrfToken: String) {
        val existing = current ?: return
        if (existing.origin != origin) return
        val updated = existing.copy(username = username, csrfToken = csrfToken)
        current = updated
        persist(updated)
        _state.value = AuthState.Authenticated(username)
    }

    fun sessionFor(origin: ServerOrigin): Session? {
        val session = current ?: return null
        if (session.origin != origin) return null
        if (session.expiresAtMillis <= System.currentTimeMillis()) return null
        return session
    }

    fun cookieFor(url: HttpUrl): Cookie? {
        val session = sessionFor(ServerOrigin.from(url)) ?: return null
        if (session.cookie.secure && !url.isHttps) return null
        return session.cookie
    }

    fun clear() {
        current = null
        _state.value = AuthState.Unauthenticated
        scope.launch { store.clear() }
    }

    fun clearIfOriginMismatch(origin: ServerOrigin?) {
        val session = current ?: return
        if (origin == null || session.origin != origin) clear()
    }

    private fun persist(session: Session) {
        val stored = StoredAuthSession(
            origin = session.origin.key(),
            token = session.token,
            csrfToken = session.csrfToken,
            username = session.username,
            expiresAtMillis = session.expiresAtMillis,
        )
        scope.launch { store.save(stored) }
    }

    private fun buildCookie(origin: ServerOrigin, token: String, expiresAtMillis: Long): Cookie {
        val builder = Cookie.Builder()
            .name(SESSION_COOKIE)
            .value(token)
            .hostOnlyDomain(origin.host)
            .path("/")
            .expiresAt(expiresAtMillis)
        if (origin.scheme == "https") builder.secure()
        return builder.build()
    }

    companion object {
        const val SESSION_COOKIE = "hwatch_session"
    }
}
