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

class DownloadRepositoryTest {

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

    private fun repository(): DownloadRepository = DownloadRepository(
        apiProvider = { Result.success(api()) },
        languageProvider = { "zh-CN" },
    )

    @Test
    fun tasksRequestUsesLimitOffsetLanguage() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"items":[],"total":0,"limit":1000,"offset":0}"""),
        )
        repository().getTasks()
        val url = server.takeRequest().requestUrl!!
        assertEquals("/api/tasks", url.encodedPath)
        assertEquals("1000", url.queryParameter("limit"))
        assertEquals("0", url.queryParameter("offset"))
        assertEquals("zh-CN", url.queryParameter("language"))
    }

    @Test
    fun tasksDecodeVideoItems() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"video_id":"source:1","title":"t","status":"active","bytes_downloaded":5,"total_bytes":10}],"total":1,"limit":1000,"offset":0}""",
            ),
        )
        val result = repository().getTasks()
        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).data
        assertEquals(1, page.items.size)
        assertEquals("source:1", page.items[0].videoId)
    }

    @Test
    fun videoActionEncodesVideoId() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"status":"accepted"}"""))
        repository().videoAction("source:3144962", "pause")
        val url = server.takeRequest().requestUrl!!
        assertEquals("/api/videos/source%3A3144962/pause", url.encodedPath)
    }

    @Test
    fun downloadAllEncodesSubscriptionQuery() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":3}"""))
        repository().downloadAll("source:creator")
        val url = server.takeRequest().requestUrl!!
        assertEquals("/api/videos/download-all", url.encodedPath)
        assertEquals("source:creator", url.queryParameter("subscription"))
        assertTrue(url.encodedQuery!!.contains("subscription=source%3Acreator"))
    }

    @Test
    fun http500MapsToHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        assertTrue(repository().getTasks() is ApiResult.HttpError)
    }

    @Test
    fun invalidJsonMapsToParseError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not json"))
        assertTrue(repository().getTasks() is ApiResult.ParseError)
    }

    @Test
    fun toggleDownloadsSendsPausedAndReadsResponse() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"paused":true}"""))
        val result = repository().toggleDownloads(true)
        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data.paused)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/downloads/toggle", request.path)
        assertEquals("{\"paused\":true}", request.body.readUtf8())
    }

    @Test
    fun getDownloadsPausedReadsDashboard() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"downloads_paused":true,"timezone":"UTC"}"""),
        )
        val result = repository().getDownloadsPaused()
        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data)
    }
}
