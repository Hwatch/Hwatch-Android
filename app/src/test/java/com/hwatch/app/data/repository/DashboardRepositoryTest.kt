package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.di.NetworkModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(): HWatchApi = NetworkModule.createRetrofit(
        NetworkModule.createJson(),
        OkHttpClient(),
        server.url("/").toString(),
    )

    private fun repository(api: HWatchApi): DashboardRepository = DashboardRepository(
        apiProvider = { Result.success(api) },
        languageProvider = { "zh-CN" },
    )

    @Test
    fun successMapsToSuccessAndSendsLanguage() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"counts":{"videos":5},"timezone":"Asia/Shanghai"}"""),
        )
        val result = repository(api()).loadDashboard()
        assertTrue(result is ApiResult.Success)
        val dashboard = (result as ApiResult.Success).data
        assertEquals(5L, dashboard.counts?.videos)
        assertEquals("Asia/Shanghai", dashboard.timezone)
        assertEquals("/api/dashboard?language=zh-CN", server.takeRequest().path)
    }

    @Test
    fun http500MapsToHttpError() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""),
        )
        val result = repository(api()).loadDashboard()
        assertTrue(result is ApiResult.HttpError)
        assertEquals(500, (result as ApiResult.HttpError).code)
    }

    @Test
    fun invalidJsonMapsToParseError() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("{not valid json"),
        )
        val result = repository(api()).loadDashboard()
        assertTrue(result is ApiResult.ParseError)
    }

    @Test
    fun networkFailureMapsToNetworkError() = runBlocking {
        val api = api()
        server.shutdown()
        val result = repository(api).loadDashboard()
        assertTrue(result is ApiResult.NetworkError)
    }
}
