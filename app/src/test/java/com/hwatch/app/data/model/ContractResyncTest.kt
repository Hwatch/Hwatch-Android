package com.hwatch.app.data.model

import com.hwatch.app.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 8.5 契约对齐：新增可选字段解码回归。 */
class ContractResyncTest {

    private val json: Json = NetworkModule.createJson()

    @Test
    fun videoNewFieldsDecode() {
        val doc = """
            {
              "video_id":"source:1",
              "creators":[{"Name":"C","URL":"https://c","Image":"i"}],
              "source_relation":{"type":"creator_listing","platform":"source","subscription_slug":"source:creator","subscription_url":"","creator_key":"c"},
              "downloader_id":"aria2",
              "external_task_id":"0123456789abcdef"
            }
        """.trimIndent()
        val video = json.decodeFromString<VideoDto>(doc)
        assertEquals("C", video.creators.single().name)
        assertEquals("https://c", video.creators.single().url)
        assertEquals("source:creator", video.sourceRelation?.subscriptionSlug)
        assertEquals("creator_listing", video.sourceRelation?.type)
        assertEquals("aria2", video.downloaderId)
        assertEquals("0123456789abcdef", video.externalTaskId)
    }

    @Test
    fun dashboardNewFieldsDecode() {
        val doc = """
            {
              "downloads_paused":true,
              "downloader_mode":"auto",
              "downloaders":[
                {"ID":"aria2","Name":"aria2","Version":"","Error":"","Online":true,"SupportsMulti":true,"Active":2,"Connections":8,"DownloadSpeed":789,"UptimeSeconds":3600},
                {"ID":"wget","Name":"wget","Version":"GNU Wget 1.21.3","Online":true,"SupportsMulti":false,"Active":0,"Connections":0,"DownloadSpeed":0,"UptimeSeconds":3600}
              ],
              "timezone":"Asia/Shanghai"
            }
        """.trimIndent()
        val dashboard = json.decodeFromString<DashboardDto>(doc)
        assertTrue(dashboard.downloadsPaused)
        assertEquals("auto", dashboard.downloaderMode)
        assertEquals(2, dashboard.downloaders.size)
        assertEquals("aria2", dashboard.downloaders[0].id)
        assertEquals(789L, dashboard.downloaders[0].downloadSpeed)
        assertEquals(8L, dashboard.downloaders[0].connections)
        assertEquals("GNU Wget 1.21.3", dashboard.downloaders[1].version)
    }

    @Test
    fun settingsNewFieldsDecode() {
        val doc = """
            {
              "downloader_mode":"wget",
              "backup_dir":"/media/.hwatch-backups",
              "backup_time":"03:30",
              "backup_retention":7,
              "backup_last_success":"2026-09-12T03:30:00Z",
              "backup_last_error":""
            }
        """.trimIndent()
        val settings = json.decodeFromString<SettingsDto>(doc)
        assertEquals("wget", settings.downloaderMode)
        assertEquals("/media/.hwatch-backups", settings.backupDir)
        assertEquals("03:30", settings.backupTime)
        assertEquals(7, settings.backupRetention)
        assertEquals("2026-09-12T03:30:00Z", settings.backupLastSuccess)
        assertEquals("", settings.backupLastError)
    }

    @Test
    fun toggleDownloadsResponseDecodes() {
        val response = json.decodeFromString<ToggleDownloadsResponse>("""{"paused":true}""")
        assertTrue(response.paused)
    }
}
