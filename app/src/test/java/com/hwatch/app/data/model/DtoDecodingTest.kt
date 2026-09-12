package com.hwatch.app.data.model

import com.hwatch.app.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoDecodingTest {

    private val json: Json = NetworkModule.createJson()

    @Test
    fun videoFullSampleParses() {
        val doc = """
            {
              "video_id": "source:3144962",
              "subscription_slug": "source:creator",
              "title": "Original title",
              "translated_title": "中文标题",
              "translated_tags": ["标签一", "标签二"],
              "translation_language": "zh-CN",
              "page_url": "",
              "published_at": "2026-06-10T00:00:00Z",
              "discovered_at": "2026-09-11T16:39:37Z",
              "downloaded_at": "2026-09-11T18:00:00Z",
              "updated_at": "2026-09-11T18:00:00Z",
              "thumbnail_url": "",
              "views": 63132,
              "duration_seconds": 171,
              "creator_name": "Creator",
              "creator_url": "",
              "creator_avatar": "",
              "uploader_name": "uploader",
              "uploader_url": "",
              "uploader_avatar": "",
              "categories": [{"Name":"Animation","URL":"","Image":""}],
              "tags": [{"Name":"sound","URL":""}],
              "external_links": [],
              "status": "completed",
              "aria_gid": "0123456789abcdef",
              "file_path": "/media/...mp4",
              "plex_path": "/media/...mp4",
              "bytes_downloaded": 123456,
              "total_bytes": 123456,
              "download_speed": 0,
              "connections": 0,
              "video_codec": "h264",
              "audio_codec": "aac",
              "container": "mp4",
              "width": 1920,
              "height": 1080,
              "frame_rate": 30,
              "bit_rate": 4000000,
              "bit_depth": 8,
              "media_probed_at": "2026-09-11T18:00:10Z",
              "transcode_status": "not_requested",
              "plex_season": 1,
              "plex_episode": 398,
              "error": ""
            }
        """.trimIndent()
        val video = json.decodeFromString<VideoDto>(doc)
        assertEquals("source:3144962", video.videoId)
        assertEquals("中文标题", video.translatedTitle)
        assertEquals("Creator", video.creatorName)
        assertEquals(63132L, video.views)
        assertEquals(171L, video.durationSeconds)
        assertEquals(1920, video.width)
        assertEquals(1080, video.height)
        assertEquals(listOf("标签一", "标签二"), video.translatedTags)
        assertEquals(1, video.categories.size)
        assertEquals("Animation", video.categories[0].name)
        assertEquals(1, video.tags.size)
        assertEquals("sound", video.tags[0].name)
        assertEquals(VideoStatus.COMPLETED, video.status)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val doc = """
            {
              "video_id": "source:1",
              "title": "t",
              "totally_new_field_from_future": 123,
              "nested_unknown": {"a": 1}
            }
        """.trimIndent()
        val video = json.decodeFromString<VideoDto>(doc)
        assertEquals("source:1", video.videoId)
        assertEquals("t", video.title)
    }

    @Test
    fun videoStatusMapsKnownAndUnknown() {
        assertEquals(VideoStatus.DISCOVERED, VideoStatus.fromWire("discovered"))
        assertEquals(VideoStatus.QUEUED, VideoStatus.fromWire("queued"))
        assertEquals(VideoStatus.WAITING, VideoStatus.fromWire("waiting"))
        assertEquals(VideoStatus.ACTIVE, VideoStatus.fromWire("active"))
        assertEquals(VideoStatus.PAUSED, VideoStatus.fromWire("paused"))
        assertEquals(VideoStatus.FAILED, VideoStatus.fromWire("failed"))
        assertEquals(VideoStatus.COMPLETED, VideoStatus.fromWire("completed"))
        assertEquals(VideoStatus.UNKNOWN, VideoStatus.fromWire("future_state"))
        assertEquals(VideoStatus.UNKNOWN, VideoStatus.fromWire(null))
    }

    @Test
    fun videoStatusDecodesViaCustomSerializer() {
        val expected = mapOf(
            "discovered" to VideoStatus.DISCOVERED,
            "queued" to VideoStatus.QUEUED,
            "waiting" to VideoStatus.WAITING,
            "active" to VideoStatus.ACTIVE,
            "paused" to VideoStatus.PAUSED,
            "failed" to VideoStatus.FAILED,
            "completed" to VideoStatus.COMPLETED,
        )
        expected.forEach { (wire, status) ->
            val doc = """{"video_id":"source:1","status":"$wire"}"""
            assertEquals(wire, status, json.decodeFromString<VideoDto>(doc).status)
        }
        val unknownDoc = """{"video_id":"source:1","status":"future_new_status"}"""
        assertEquals(VideoStatus.UNKNOWN, json.decodeFromString<VideoDto>(unknownDoc).status)
    }

    @Test
    fun videoWithoutStatusDefaultsToUnknown() {
        val doc = """{"video_id":"source:1"}"""
        assertEquals(VideoStatus.UNKNOWN, json.decodeFromString<VideoDto>(doc).status)
    }

    @Test
    fun dashboardParsesDocumentedFields() {
        val doc = """
            {
              "subscriptions": [{"id":1,"slug":"x","enabled":true,"name":"Creator","platform":"source","last_checked_at":"2026-09-11T16:39:37Z","progress":{"total":737,"completed":583,"active":2,"waiting":152,"failed":0}}],
              "videos": [{"video_id":"source:1","title":"t","status":"completed"}],
              "recently_played": [{"video_id":"source:2","title":"played","status":"active"}],
              "counts": {"videos":874,"active":2,"waiting":100,"completed":600,"discovered":172},
              "traffic_bytes": 123,
              "library_bytes": 456,
              "current_download_speed": 789,
              "current_connections": 8,
              "disk_total": 965554995200,
              "disk_free": 273582915584,
              "aria2_online": true,
              "timezone": "Asia/Shanghai"
            }
        """.trimIndent()
        val dashboard = json.decodeFromString<DashboardDto>(doc)
        val counts = dashboard.counts
        assertEquals(874L, counts?.videos)
        assertEquals(2L, counts?.active)
        assertEquals(100L, counts?.waiting)
        assertEquals(600L, counts?.completed)
        assertEquals(172L, counts?.discovered)
        assertEquals(965_554_995_200L, dashboard.diskTotal)
        assertEquals(273_582_915_584L, dashboard.diskFree)
        assertTrue(dashboard.aria2Online)
        assertEquals("Asia/Shanghai", dashboard.timezone)
        assertEquals(1, dashboard.videos.size)
        assertEquals("source:1", dashboard.videos[0].videoId)
        assertEquals(1, dashboard.recentlyPlayed.size)
        assertEquals("source:2", dashboard.recentlyPlayed[0].videoId)
        assertEquals(1, dashboard.subscriptions.size)
        assertEquals("x", dashboard.subscriptions[0].slug)
        assertEquals(737L, dashboard.subscriptions[0].progress?.total)
    }

    @Test
    fun dashboardMissingCollectionsDefaultsToEmpty() {
        val doc = """{"timezone":"UTC"}"""
        val dashboard = json.decodeFromString<DashboardDto>(doc)
        assertTrue(dashboard.videos.isEmpty())
        assertTrue(dashboard.recentlyPlayed.isEmpty())
        assertTrue(dashboard.subscriptions.isEmpty())
    }

    @Test
    fun pagedResponseParses() {
        val doc = """{"items":[],"total":0,"limit":48,"offset":0}""".trimIndent()
        val paged = json.decodeFromString<PagedResponse<VideoDto>>(doc)
        assertEquals(0L, paged.total)
        assertEquals(48, paged.limit)
        assertEquals(0, paged.offset)
        assertTrue(paged.items.isEmpty())
    }

    @Test
    fun facetsParses() {
        val doc = """
            {
              "creators": [{"value":"Akt","count":595}],
              "tags": [{"value":"iwara","count":516}],
              "codecs": [{"value":"h264","count":500}]
            }
        """.trimIndent()
        val facets = json.decodeFromString<FacetsDto>(doc)
        assertEquals("Akt", facets.creators[0].value)
        assertEquals(595L, facets.creators[0].count)
        assertEquals("iwara", facets.tags[0].value)
        assertEquals("h264", facets.codecs[0].value)
    }

    @Test
    fun settingsParses() {
        val doc = """
            {
              "http_proxy": "",
              "download_dir": "/media",
              "aria_rpc_url": "http://127.0.0.1:6800/jsonrpc",
              "aria_secret": "",
              "check_time": "04:00",
              "timezone": "Asia/Shanghai",
              "plex_language": "zh-CN",
              "max_concurrent": 2,
              "connections_per_download": 4,
              "download_limit_kbps": 0,
              "min_free_bytes": 5368709120
            }
        """.trimIndent()
        val settings = json.decodeFromString<SettingsDto>(doc)
        assertEquals("/media", settings.downloadDir)
        assertEquals("http://127.0.0.1:6800/jsonrpc", settings.ariaRpcUrl)
        assertEquals("Asia/Shanghai", settings.timezone)
        assertEquals(2, settings.maxConcurrent)
        assertEquals(5_368_709_120L, settings.minFreeBytes)
    }

    @Test
    fun settingsPutOnlyEncodesNonNullFields() {
        val partial = SettingsDto(maxConcurrent = 2)
        val encoded = json.encodeToString(SettingsDto.serializer(), partial)
        assertEquals("{\"max_concurrent\":2}", encoded)
    }

    @Test
    fun settingsUpdateRequestEncodesEnabled() {
        val encoded = json.encodeToString(
            UpdateSubscriptionRequest.serializer(),
            UpdateSubscriptionRequest(enabled = false),
        )
        assertEquals("{\"enabled\":false}", encoded)
    }

    @Test
    fun createSubscriptionRequestEncodes() {
        val request = CreateSubscriptionRequest(url = "https://example.com/a", downloadExisting = true)
        val encoded = json.encodeToString(CreateSubscriptionRequest.serializer(), request)
        assertEquals(
            "{\"url\":\"https://example.com/a\",\"type\":\"creator\",\"download_existing\":true}",
            encoded,
        )
    }

    @Test
    fun subscriptionWithProgressParses() {
        val doc = """
            {"id":1,"slug":"source:creator","platform":"source","name":"Creator","enabled":true,
             "baseline_complete":true,"progress":{"total":737,"completed":583,"active":2,
             "waiting":152,"failed":0,"bytes_downloaded":1000,"total_bytes":2000}}
        """.trimIndent()
        val subscription = json.decodeFromString<SubscriptionDto>(doc)
        assertEquals("source:creator", subscription.slug)
        assertEquals(737L, subscription.progress?.total)
        assertEquals(583L, subscription.progress?.completed)
        assertEquals(2L, subscription.progress?.active)
    }

    @Test
    fun dashboardWithoutCountsParses() {
        val doc = """{"timezone":"UTC","disk_total":1}""".trimIndent()
        val dashboard = json.decodeFromString<DashboardDto>(doc)
        assertNull(dashboard.counts)
        assertEquals("UTC", dashboard.timezone)
    }
}