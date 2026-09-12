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

class SubscriptionRepositoryTest {

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

    private fun repository(): SubscriptionRepository =
        SubscriptionRepository(apiProvider = { Result.success(api()) })

    @Test
    fun subscriptionsDecodeWithExternalLinksObjects() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"id":1,"slug":"rule34video:akt","platform":"rule34video","name":"Akt","enabled":true,"external_links":[{"Image":"","Name":"x","URL":"https://x"}],"progress":{"total":10,"completed":4,"active":1,"waiting":5,"failed":0,"bytes_downloaded":1,"total_bytes":2}}]}""",
            ),
        )
        val result = repository().getSubscriptions()
        assertTrue(result is ApiResult.Success)
        val item = (result as ApiResult.Success).data.items.single()
        assertEquals("rule34video:akt", item.slug)
        assertEquals("x", item.externalLinks.single().name)
        assertEquals(10L, item.progress?.total)
    }

    @Test
    fun setEnabledEncodesSlugAndSendsPatchBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"enabled":false}"""))
        repository().setEnabled("rule34video:akt", false)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/subscriptions/rule34video%3Aakt", request.path)
        assertTrue(request.body.readUtf8().contains("\"enabled\":false"))
    }

    @Test
    fun deleteEncodesSlug() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"deleted":true}"""))
        repository().deleteSubscription("source:creator")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/subscriptions/source%3Acreator", request.path)
    }

    @Test
    fun checkOneEncodesSlug() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"status":"started"}"""))
        repository().checkOne("source:creator")
        assertEquals("/api/check/source%3Acreator", server.takeRequest().path)
    }

    @Test
    fun checkAllUsesApiCheck() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"status":"started"}"""))
        repository().checkAll()
        assertEquals("/api/check", server.takeRequest().path)
    }

    @Test
    fun creatorSearchSendsQueryAndPlatform() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}"""))
        repository().searchCreators("akt", "source")
        val url = server.takeRequest().requestUrl!!
        assertEquals("/api/creators/search", url.encodedPath)
        assertEquals("akt", url.queryParameter("q"))
        assertEquals("source", url.queryParameter("platform"))
    }

    @Test
    fun createSendsContractBody() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"slug":"source:creator","platform":"source","baselined":1,"queued":0}""",
            ),
        )
        repository().createSubscription(url = "https://example.com/a", downloadExisting = true)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/subscriptions", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"url\":\"https://example.com/a\""))
        assertTrue(body.contains("\"type\":\"creator\""))
        assertTrue(body.contains("\"download_existing\":true"))
    }

    @Test
    fun platformsDecode() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items":[{"id":"source","name":"Source"}]}"""),
        )
        val result = repository().getPlatforms()
        assertTrue(result is ApiResult.Success)
        assertEquals("source", (result as ApiResult.Success).data.items.single().id)
    }

    @Test
    fun http500MapsToHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        assertTrue(repository().getSubscriptions() is ApiResult.HttpError)
    }
}
