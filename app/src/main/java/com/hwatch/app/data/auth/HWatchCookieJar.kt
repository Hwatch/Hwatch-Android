package com.hwatch.app.data.auth

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 仅处理 HWatch 的 hwatch_session；按 origin 隔离（[AuthSessionManager]）。
 * 不保存/发送任何第三方 Cookie。
 */
class HWatchCookieJar(
    private val manager: AuthSessionManager,
) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies
            .firstOrNull { it.name == AuthSessionManager.SESSION_COOKIE }
            ?.let { manager.onCookie(url, it) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookie = manager.cookieFor(url) ?: return emptyList()
        return listOf(cookie)
    }
}
