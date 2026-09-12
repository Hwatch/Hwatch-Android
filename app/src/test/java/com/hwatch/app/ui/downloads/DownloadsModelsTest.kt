package com.hwatch.app.ui.downloads

import com.hwatch.app.data.model.VideoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsModelsTest {

    @Test
    fun activeSupportsPauseAndDelete() {
        val actions = availableActions(VideoStatus.ACTIVE)
        assertTrue(actions.contains(DownloadAction.PAUSE))
        assertTrue(actions.contains(DownloadAction.DELETE))
    }

    @Test
    fun pausedSupportsResume() {
        assertEquals(listOf(DownloadAction.RESUME, DownloadAction.DELETE), availableActions(VideoStatus.PAUSED))
    }

    @Test
    fun failedSupportsRetry() {
        assertEquals(listOf(DownloadAction.RETRY, DownloadAction.DELETE), availableActions(VideoStatus.FAILED))
    }

    @Test
    fun queuedOnlyDelete() {
        assertEquals(listOf(DownloadAction.DELETE), availableActions(VideoStatus.QUEUED))
    }

    @Test
    fun completedOnlyDelete() {
        assertEquals(listOf(DownloadAction.DELETE), availableActions(VideoStatus.COMPLETED))
    }

    @Test
    fun discoveredAndExcludedSupportDownload() {
        assertEquals(listOf(DownloadAction.DOWNLOAD), availableActions(VideoStatus.DISCOVERED))
        assertEquals(listOf(DownloadAction.DOWNLOAD), availableActions(VideoStatus.EXCLUDED))
    }

    @Test
    fun unknownOnlyDelete() {
        assertEquals(listOf(DownloadAction.DELETE), availableActions(VideoStatus.UNKNOWN))
    }

    @Test
    fun everyStatusHasAtLeastOneAction() {
        VideoStatus.entries.forEach { status ->
            assertTrue("status $status has no action", availableActions(status).isNotEmpty())
        }
    }
}
