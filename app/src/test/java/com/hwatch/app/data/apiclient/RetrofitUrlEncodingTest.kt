package com.hwatch.app.data.apiclient

import com.hwatch.app.di.NetworkModule
import com.hwatch.app.util.UrlEncoding
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * URL 编码端到端测试：通过真实 Retrofit 接口调用生成最终 HTTP Request，
 * 断言最终 URL 满足 HWatch API Contract（冒号必须为 %3A，query 交给 HttpUrl 编码）。
 */
class RetrofitUrlEncodingTest {

    private lateinit var recording: RecordingInterceptor
    private lateinit var api: HWatchApi

    @Before
    fun setUp() {
        recording = RecordingInterceptor()
        val client = OkHttpClient.Builder().addInterceptor(recording).build()
        api = NetworkModule.createRetrofit(
            json = NetworkModule.createJson(),
            client = client,
            baseUrl = "http://192.168.1.10:8787/",
        )
    }

    @Test
    fun mediaColonPercentEncoded() = runBlocking {
        api.media(UrlEncoding.encodePathSegment("source:3144962"))
        assertEquals("/media/source%3A3144962", recording.lastRequest().url.encodedPath)
    }

    @Test
    fun thumbColonPercentEncoded() = runBlocking {
        api.thumb(UrlEncoding.encodePathSegment("source:3144962"))
        assertEquals("/thumb/source%3A3144962", recording.lastRequest().url.encodedPath)
    }

    @Test
    fun videoActionColonPercentEncoded() = runBlocking {
        api.videoAction(UrlEncoding.encodePathSegment("source:3144962"), "delete")
        assertEquals("/api/videos/source%3A3144962/delete", recording.lastRequest().url.encodedPath)
    }

    @Test
    fun checkSubscriptionSlugColonPercentEncoded() = runBlocking {
        api.checkSubscription(UrlEncoding.encodePathSegment("source:creator"))
        assertEquals("/api/check/source%3Acreator", recording.lastRequest().url.encodedPath)
    }

    @Test
    fun deleteSubscriptionSlugColonPercentEncoded() = runBlocking {
        api.deleteSubscription(UrlEncoding.encodePathSegment("source:creator"))
        assertEquals("/api/subscriptions/source%3Acreator", recording.lastRequest().url.encodedPath)
    }

    @Test
    fun queryParametersEncodedByHttpUrl() = runBlocking {
        api.videos(
            status = "completed",
            q = "银狼 全",
            scope = "all",
            tags = "iwara,sound",
            tagMode = "all",
            excludeTags = "compilation",
            sort = "views",
            order = "desc",
            limit = 48,
            language = "zh-CN",
        )
        val url = recording.lastRequest().url
        assertEquals("/api/videos", url.encodedPath)
        assertEquals("completed", url.queryParameter("status"))
        assertEquals("zh-CN", url.queryParameter("language"))
        assertEquals("银狼 全", url.queryParameter("q"))
        assertEquals("all", url.queryParameter("scope"))
        assertEquals("iwara,sound", url.queryParameter("tags"))
        assertEquals("all", url.queryParameter("tag_mode"))
        assertEquals("compilation", url.queryParameter("exclude_tags"))
        assertEquals("views", url.queryParameter("sort"))
        assertEquals("desc", url.queryParameter("order"))
        assertEquals("48", url.queryParameter("limit"))
    }

    @Test
    fun querySpecialCharactersRoundTrip() = runBlocking {
        val raw = "a/b c%d?e"
        api.videos(q = raw)
        val url = recording.lastRequest().url
        assertEquals(raw, url.queryParameter("q"))
    }

    @Test
    fun downloadAllSubscriptionQueryEncoded() = runBlocking {
        api.downloadAll(subscription = "source:creator")
        val url = recording.lastRequest().url
        assertEquals("source:creator", url.queryParameter("subscription"))
    }

    @Test
    fun dashboardHasNoUnexpectedQueryWhenDefault() = runBlocking {
        api.dashboard()
        val url = recording.lastRequest().url
        assertEquals("/api/dashboard", url.encodedPath)
        assertEquals(null, url.queryParameter("language"))
    }

    private class RecordingInterceptor : Interceptor {
        private val requests = mutableListOf<Request>()

        override fun intercept(chain: Interceptor.Chain): Response {
            requests.add(chain.request())
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        fun lastRequest(): Request = requests.last()
    }
}