package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.model.UpdateSettingsRequest
import com.hwatch.app.di.NetworkModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

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

    private fun repository(): SettingsRepository =
        SettingsRepository(apiProvider = { Result.success(api()) })

    @Test
    fun getSettingsDecodesAllFields() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"http_proxy":"","download_dir":"/media","aria_rpc_url":"http://127.0.0.1:6800/jsonrpc","aria_secret":"s","check_time":"04:00","timezone":"Asia/Shanghai","plex_language":"zh-CN","max_concurrent":2,"connections_per_download":4,"download_limit_kbps":0,"min_free_bytes":5368709120,"require_creator_match":true,"exclude_title_keywords":"compilation","exclude_uploaders":"","exclude_categories":"","exclude_tags":"","max_duration_seconds":0}""",
            ),
        )
        val result = repository().getSettings()
        assertTrue(result is ApiResult.Success)
        val settings = (result as ApiResult.Success).data
        assertEquals("Asia/Shanghai", settings.timezone)
        assertEquals(true, settings.requireCreatorMatch)
        assertEquals("compilation", settings.excludeTitleKeywords)
        assertEquals(0L, settings.maxDurationSeconds)
    }

    @Test
    fun putSettingsSendsOnlyProvidedFields() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"max_concurrent":4}"""))
        repository().updateSettings(UpdateSettingsRequest(maxConcurrent = 4))
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/settings", request.path)
        val body = request.body.readUtf8()
        assertEquals("{\"max_concurrent\":4}", body)
        assertFalse(body.contains("aria_secret"))
        assertFalse(body.contains("http_proxy"))
    }

    @Test
    fun http500MapsToHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        assertTrue(repository().getSettings() is ApiResult.HttpError)
    }
}
