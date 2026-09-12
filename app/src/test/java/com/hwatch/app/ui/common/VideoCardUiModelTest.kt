package com.hwatch.app.ui.common

import com.hwatch.app.data.model.VideoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoCardUiModelTest {

    @Test
    fun translatedTitlePreferredWithOriginalSubtitle() {
        val ui = VideoDto(title = "Original", translatedTitle = "中文", creatorName = "C").toVideoCardUiModel()
        assertEquals("中文", ui.primaryTitle)
        assertEquals("Original", ui.secondaryTitle)
    }

    @Test
    fun blankTranslatedFallsBackToOriginal() {
        val ui = VideoDto(title = "Original").toVideoCardUiModel()
        assertEquals("Original", ui.primaryTitle)
        assertNull(ui.secondaryTitle)
    }

    @Test
    fun metadataIncludesResolutionCodecDuration() {
        val ui = VideoDto(
            width = 1920,
            height = 1080,
            videoCodec = "h264",
            durationSeconds = 201,
        ).toVideoCardUiModel()
        assertEquals("1920×1080 · h264 · 03:21", ui.metadata)
    }

    @Test
    fun metadataOmitsInvalidResolutionAndCodec() {
        val ui = VideoDto(width = 0, height = 0, videoCodec = "", durationSeconds = 0).toVideoCardUiModel()
        assertEquals("00:00", ui.metadata)
    }

    @Test
    fun dateOnlyPublishedAtFormatsAsDate() {
        val ui = VideoDto(publishedAt = "2026-07-26").toVideoCardUiModel("Asia/Shanghai")
        assertEquals("2026-07-26", ui.time)
    }

    @Test
    fun rfc3339PublishedAtFormatsInTimezone() {
        val ui = VideoDto(publishedAt = "2026-06-10T00:00:00Z").toVideoCardUiModel("Asia/Shanghai")
        assertEquals("2026-06-10 08:00", ui.time)
    }

    @Test
    fun timeIsNullWhenTimezoneNotProvided() {
        val ui = VideoDto(publishedAt = "2026-07-26").toVideoCardUiModel()
        assertNull(ui.time)
    }

    @Test
    fun timeFallsBackThroughFields() {
        val ui = VideoDto(discoveredAt = "2026-09-11T16:39:37Z").toVideoCardUiModel("UTC")
        assertEquals("2026-09-11 16:39", ui.time)
    }

    @Test
    fun blankCreatorBecomesNull() {
        assertNull(VideoDto(creatorName = "   ").toVideoCardUiModel().creator)
    }
}
