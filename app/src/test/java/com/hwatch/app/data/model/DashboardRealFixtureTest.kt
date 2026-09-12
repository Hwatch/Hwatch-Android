package com.hwatch.app.data.model

import com.hwatch.app.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实 GET /api/dashboard 响应回归。fixture 来自真实服务器（含 published_at 日期型、
 * external_links 对象数组、downloads_paused 等文档未覆盖的差异）。
 */
class DashboardRealFixtureTest {

    private val json: Json = NetworkModule.createJson()

    private val realJson: String = checkNotNull(
        javaClass.classLoader?.getResourceAsStream("dashboard-real.json"),
    ) { "dashboard-real.json fixture missing" }.bufferedReader().use { it.readText() }

    @Test
    fun realDashboardFixtureDecodes() {
        val dashboard = json.decodeFromString<DashboardDto>(realJson)
        assertEquals(1, dashboard.subscriptions.size)
        assertEquals(1, dashboard.videos.size)
        assertEquals(1, dashboard.recentlyPlayed.size)
        assertEquals("source:3144962", dashboard.videos[0].videoId)
        assertEquals("2026-07-26", dashboard.videos[0].publishedAt)
        assertTrue(dashboard.aria2Online)
        assertEquals("Asia/Shanghai", dashboard.timezone)
    }

    @Test
    fun externalLinksAreObjectsOnBothVideoAndSubscription() {
        val dashboard = json.decodeFromString<DashboardDto>(realJson)
        assertEquals(1, dashboard.videos[0].externalLinks.size)
        assertEquals("x.com", dashboard.videos[0].externalLinks[0].name)
        assertEquals("https://x.com/p/3144962", dashboard.videos[0].externalLinks[0].url)
        assertEquals(1, dashboard.subscriptions[0].externalLinks.size)
        assertEquals("https://x.com/creator", dashboard.subscriptions[0].externalLinks[0].url)
    }

    @Test
    fun dateOnlyAndRfc3339PublishedAtBothDecode() {
        val dateOnly = json.decodeFromString<VideoDto>("""{"video_id":"a","published_at":"2026-07-26"}""")
        assertEquals("2026-07-26", dateOnly.publishedAt)
        val rfc3339 = json.decodeFromString<VideoDto>(
            """{"video_id":"b","published_at":"2026-06-10T00:00:00Z"}""",
        )
        assertEquals("2026-06-10T00:00:00Z", rfc3339.publishedAt)
    }
}
