package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.model.VideoQuery
import com.hwatch.app.di.NetworkModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LibraryRepositoryTest {

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

    private fun repository(): LibraryRepository = LibraryRepository(
        apiProvider = { Result.success(api()) },
        languageProvider = { "zh-CN" },
    )

    private fun enqueuePaged() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items":[],"total":0,"limit":48,"offset":0}"""),
        )
    }

    @Test
    fun defaultQueryUsesCompletedLimit48LanguageAndScopeAll() = runBlocking {
        enqueuePaged()
        repository().loadVideos(VideoQuery())
        val url = server.takeRequest().requestUrl!!
        assertEquals("/api/videos", url.encodedPath)
        assertEquals("completed", url.queryParameter("status"))
        assertEquals("48", url.queryParameter("limit"))
        assertEquals("0", url.queryParameter("offset"))
        assertEquals("zh-CN", url.queryParameter("language"))
        assertEquals("all", url.queryParameter("scope"))
    }

    @Test
    fun fullQueryEncodesAllParameters() = runBlocking {
        enqueuePaged()
        repository().loadVideos(
            VideoQuery(
                limit = 24,
                offset = 48,
                status = "completed",
                q = "银狼 全",
                scope = "translated",
                creator = "source:creator",
                tags = listOf("iwara", "sound"),
                tagMode = "all",
                excludeTags = listOf("compilation"),
                codec = "h264",
                minHeight = 1080,
                sort = "views",
                order = "desc",
            ),
        )
        val url = server.takeRequest().requestUrl!!
        assertEquals("24", url.queryParameter("limit"))
        assertEquals("48", url.queryParameter("offset"))
        assertEquals("银狼 全", url.queryParameter("q"))
        assertEquals("translated", url.queryParameter("scope"))
        assertEquals("source:creator", url.queryParameter("creator"))
        assertEquals("iwara,sound", url.queryParameter("tags"))
        assertEquals("all", url.queryParameter("tag_mode"))
        assertEquals("compilation", url.queryParameter("exclude_tags"))
        assertEquals("h264", url.queryParameter("codec"))
        assertEquals("1080", url.queryParameter("min_height"))
        assertEquals("views", url.queryParameter("sort"))
        assertEquals("desc", url.queryParameter("order"))
        assertTrue(url.encodedQuery!!.contains("creator=source%3Acreator"))
    }

    @Test
    fun blankAndEmptyValuesAreOmitted() = runBlocking {
        enqueuePaged()
        repository().loadVideos(
            VideoQuery(
                q = "   ",
                creator = "",
                codec = "",
                tags = emptyList(),
                tagMode = null,
                excludeTags = emptyList(),
                minHeight = null,
                sort = null,
                order = null,
            ),
        )
        val url = server.takeRequest().requestUrl!!
        assertNull(url.queryParameter("q"))
        assertNull(url.queryParameter("creator"))
        assertNull(url.queryParameter("codec"))
        assertNull(url.queryParameter("tags"))
        assertNull(url.queryParameter("tag_mode"))
        assertNull(url.queryParameter("exclude_tags"))
        assertNull(url.queryParameter("min_height"))
        assertNull(url.queryParameter("sort"))
        assertNull(url.queryParameter("order"))
    }

    @Test
    fun http500MapsToHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        val result = repository().loadVideos(VideoQuery())
        assertTrue(result is ApiResult.HttpError)
    }

    @Test
    fun invalidJsonMapsToParseError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not json"))
        val result = repository().loadVideos(VideoQuery())
        assertTrue(result is ApiResult.ParseError)
    }

    @Test
    fun facetsParse() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"creators":[{"value":"Akt","count":5}],"tags":[{"value":"iwara","count":3}],"codecs":[{"value":"h264","count":2}]}""",
            ),
        )
        val result = repository().loadFacets()
        assertTrue(result is ApiResult.Success)
        val facets = (result as ApiResult.Success).data
        assertEquals("Akt", facets.creators[0].value)
        assertEquals(5L, facets.creators[0].count)
    }
}
