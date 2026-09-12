package com.hwatch.app.data.auth

import com.hwatch.app.testutil.FakeAuthSessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSessionManagerTest {

    private fun cookie(
        host: String,
        value: String = "tok",
        expiresAt: Long = System.currentTimeMillis() + 86_400_000,
        secure: Boolean = false,
    ): Cookie {
        val builder = Cookie.Builder()
            .name(AuthSessionManager.SESSION_COOKIE)
            .value(value)
            .hostOnlyDomain(host)
            .path("/")
            .expiresAt(expiresAt)
        if (secure) builder.secure()
        return builder.build()
    }

    @Test
    fun cookieIsOriginScoped() = runTest {
        val manager = AuthSessionManager(FakeAuthSessionStore(), this)
        val a = "http://nas.local:8787/".toHttpUrl()
        manager.onCookie(a, cookie("nas.local"))

        assertNotNull(manager.cookieFor(a))
        assertNull(manager.cookieFor("http://nas.local:9000/".toHttpUrl()))
        assertNull(manager.cookieFor("http://other.local:8787/".toHttpUrl()))
    }

    @Test
    fun secureCookieNotSentOverHttp() = runTest {
        val manager = AuthSessionManager(FakeAuthSessionStore(), this)
        manager.onCookie("https://nas.local:8787/".toHttpUrl(), cookie("nas.local", secure = true))

        assertNull(manager.cookieFor("http://nas.local:8787/".toHttpUrl()))
        assertNotNull(manager.cookieFor("https://nas.local:8787/".toHttpUrl()))
    }

    @Test
    fun expiredCookieNotReturned() = runTest {
        val manager = AuthSessionManager(FakeAuthSessionStore(), this)
        val url = "http://nas.local:8787/".toHttpUrl()
        manager.onCookie(url, cookie("nas.local", expiresAt = System.currentTimeMillis() - 1000))
        assertNull(manager.cookieFor(url))
    }

    @Test
    fun clearRemovesSessionAndState() = runTest {
        val store = FakeAuthSessionStore()
        val manager = AuthSessionManager(store, this)
        val url = "http://nas.local:8787/".toHttpUrl()
        manager.onCookie(url, cookie("nas.local"))
        manager.clear()
        advanceUntilIdle()

        assertNull(manager.cookieFor(url))
        assertTrue(manager.state.value is AuthState.Unauthenticated)
        assertNull(store.stored)
    }

    @Test
    fun clearIfOriginMismatchOnlyClearsDifferentOrigin() = runTest {
        val manager = AuthSessionManager(FakeAuthSessionStore(), this)
        val a = ServerOrigin.from("http://nas.local:8787/".toHttpUrl())
        val b = ServerOrigin.from("http://nas.local:9000/".toHttpUrl())
        manager.onCookie("http://nas.local:8787/".toHttpUrl(), cookie("nas.local"))

        manager.clearIfOriginMismatch(a)
        assertNotNull(manager.cookieFor("http://nas.local:8787/".toHttpUrl()))

        manager.clearIfOriginMismatch(b)
        assertNull(manager.cookieFor("http://nas.local:8787/".toHttpUrl()))
    }

    @Test
    fun restoreValidPersistedSession() = runTest {
        val store = FakeAuthSessionStore()
        store.save(
            StoredAuthSession(
                origin = "http://nas.local:8787",
                token = "tok",
                csrfToken = "csrf",
                username = "admin",
                expiresAtMillis = System.currentTimeMillis() + 100_000,
            ),
        )
        val manager = AuthSessionManager(store, this)
        manager.restore()

        assertTrue(manager.state.value is AuthState.Authenticated)
        assertNotNull(manager.cookieFor("http://nas.local:8787/".toHttpUrl()))
        assertNull(manager.cookieFor("http://nas.local:9000/".toHttpUrl()))
    }

    @Test
    fun restoreExpiredSessionClearsStore() = runTest {
        val store = FakeAuthSessionStore()
        store.save(
            StoredAuthSession(
                origin = "http://nas.local:8787",
                token = "tok",
                csrfToken = "csrf",
                username = "admin",
                expiresAtMillis = System.currentTimeMillis() - 1000,
            ),
        )
        val manager = AuthSessionManager(store, this)
        manager.restore()
        advanceUntilIdle()

        assertTrue(manager.state.value is AuthState.Unauthenticated)
        assertNull(store.stored)
    }

    @Test
    fun emptyCookieValueClearsSession() = runTest {
        val manager = AuthSessionManager(FakeAuthSessionStore(), this)
        val url = "http://nas.local:8787/".toHttpUrl()
        manager.onCookie(url, cookie("nas.local", value = ""))
        assertNull(manager.cookieFor(url))
    }
}
