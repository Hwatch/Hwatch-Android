package com.hwatch.app.data.auth

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.repository.AuthRepository
import com.hwatch.app.di.NetworkModule
import com.hwatch.app.testutil.FakeAuthSessionStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthNetworkTest {

    private lateinit var serverA: MockWebServer
    private lateinit var serverB: MockWebServer
    private lateinit var store: FakeAuthSessionStore
    private lateinit var manager: AuthSessionManager
    private lateinit var client: OkHttpClient
    private lateinit var api: HWatchApi
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        serverA = MockWebServer().also { it.start() }
        serverB = MockWebServer().also { it.start() }
        store = FakeAuthSessionStore()
        manager = AuthSessionManager(store, CoroutineScope(Dispatchers.Unconfined))
        client = OkHttpClient.Builder()
            .cookieJar(HWatchCookieJar(manager))
            .addInterceptor(AuthInterceptor(manager))
            .build()
        api = NetworkModule.createRetrofit(NetworkModule.createJson(), client, serverA.url("/").toString())
        repository = AuthRepository(
            apiProvider = { Result.success(api) },
            sessionManager = manager,
            originProvider = { ServerOrigin.from(serverA.url("/")) },
        )
    }

    @After
    fun tearDown() {
        serverA.shutdown()
        serverB.shutdown()
    }

    private fun loginOnA() = runBlocking {
        serverA.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Set-Cookie", "hwatch_session=tok; Path=/")
                .setBody("""{"authenticated":true,"username":"admin","csrf_token":"csrf1"}"""),
        )
        val result = repository.login("admin", "password1234")
        assertTrue("login failed: $result", result is ApiResult.Success)
        requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS))
    }

    @Test
    fun loginSetsSessionAndCsrf() = runBlocking {
        serverA.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Set-Cookie", "hwatch_session=tok; Path=/")
                .setBody("""{"authenticated":true,"username":"admin","csrf_token":"csrf1"}"""),
        )
        val result = repository.login("admin", "password1234")
        assertTrue(result is ApiResult.Success)
        assertTrue(manager.state.value is AuthState.Authenticated)
        assertEquals("csrf1", manager.sessionFor(ServerOrigin.from(serverA.url("/")))?.csrfToken)
    }

    @Test
    fun cookieAttachedToSameOriginRequests() = runBlocking {
        loginOnA()

        serverA.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        api.dashboard()
        assertEquals("hwatch_session=tok", requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS)).getHeader("Cookie"))

        serverA.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        api.thumb("source%3A1")
        assertEquals("hwatch_session=tok", requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS)).getHeader("Cookie"))

        serverA.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        api.media("source%3A1")
        assertEquals("hwatch_session=tok", requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS)).getHeader("Cookie"))

        serverA.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[],"total":0,"limit":48,"offset":0}"""))
        api.tasks()
        assertEquals("hwatch_session=tok", requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS)).getHeader("Cookie"))
    }

    @Test
    fun csrfAddedToWritesOnly() = runBlocking {
        loginOnA()

        serverA.enqueue(MockResponse().setResponseCode(202).setBody("""{"status":"accepted"}"""))
        api.videoAction("source%3A1", "pause")
        val write = requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("csrf1", write.getHeader("X-CSRF-Token"))

        serverA.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        api.dashboard()
        assertNull(requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS)).getHeader("X-CSRF-Token"))
    }

    @Test
    fun csrfNotAddedToPublicAuthPaths() = runBlocking {
        loginOnA()
        serverA.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Set-Cookie", "hwatch_session=tok2; Path=/")
                .setBody("""{"authenticated":true,"username":"admin","csrf_token":"csrf2"}"""),
        )
        api.authLogin(Credentials("admin", "password1234"))
        assertNull(requireNotNull(serverA.takeRequest(5, TimeUnit.SECONDS)).getHeader("X-CSRF-Token"))
    }

    @Test
    fun otherOriginDoesNotReceiveCookieOrCsrf() = runBlocking {
        loginOnA()
        serverB.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val request = Request.Builder().url(serverB.url("/api/dashboard")).build()
        client.newCall(request).execute().close()
        val recorded = requireNotNull(serverB.takeRequest(5, TimeUnit.SECONDS))
        assertNull(recorded.getHeader("Cookie"))
        assertNull(recorded.getHeader("X-CSRF-Token"))
    }

    @Test
    fun sameHostDifferentPortDoesNotLeakSession() = runBlocking {
        loginOnA()
        // serverA 与 serverB 均为 localhost，端口不同
        assertNull(manager.cookieFor(serverB.url("/")))
    }

    @Test
    fun unauthorizedClearsSession() = runBlocking {
        loginOnA()
        serverA.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"请先登录"}"""))
        runCatching { api.dashboard() }
        assertTrue(manager.state.value is AuthState.Unauthenticated)
    }

    @Test
    fun wrongPasswordMapsUnauthorizedAndKeepsUnauthenticated() = runBlocking {
        serverA.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"用户名或密码错误"}"""))
        val result = repository.login("admin", "wrong")
        assertTrue(result is ApiResult.Unauthorized)
        assertTrue(manager.state.value !is AuthState.Authenticated)
    }

    @Test
    fun logoutClearsSession() = runBlocking {
        loginOnA()
        serverA.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Set-Cookie", "hwatch_session=; Path=/; Max-Age=0")
                .setBody("""{"authenticated":false}"""),
        )
        repository.logout()
        assertTrue(manager.state.value is AuthState.Unauthenticated)
        assertNull(store.stored)
    }

    @Test
    fun statusDecodesConfigured() = runBlocking {
        serverA.enqueue(MockResponse().setResponseCode(200).setBody("""{"configured":true,"authenticated":false}"""))
        val result = repository.status()
        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data.configured)
    }
}
