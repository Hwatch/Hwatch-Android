package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaFormattersTest {

    @Test
    fun durationUnderOneHourUsesMmSs() {
        assertEquals("00:00", MediaFormatters.formatDuration(0))
        assertEquals("00:59", MediaFormatters.formatDuration(59))
        assertEquals("01:00", MediaFormatters.formatDuration(60))
        assertEquals("02:51", MediaFormatters.formatDuration(171))
        assertEquals("59:59", MediaFormatters.formatDuration(3599))
    }

    @Test
    fun durationOneHourOrMoreUsesHmmSs() {
        assertEquals("1:00:00", MediaFormatters.formatDuration(3600))
        assertEquals("1:01:01", MediaFormatters.formatDuration(3661))
        assertEquals("10:00:00", MediaFormatters.formatDuration(36000))
    }

    @Test
    fun negativeDurationReturnsNull() {
        assertNull(MediaFormatters.formatDuration(-1))
    }

    @Test
    fun playbackTimeFormatsMilliseconds() {
        assertEquals("00:00", MediaFormatters.formatPlaybackTime(0L))
        assertEquals("03:21", MediaFormatters.formatPlaybackTime(201_000L))
        assertEquals("12:45", MediaFormatters.formatPlaybackTime(765_000L))
        assertEquals("1:03:21", MediaFormatters.formatPlaybackTime(3_801_000L))
    }

    @Test
    fun playbackTimeUnsetOrNegativeShowsPlaceholder() {
        assertEquals("--:--", MediaFormatters.formatPlaybackTime(-1L))
        assertEquals("--:--", MediaFormatters.formatPlaybackTime(androidx.media3.common.C.TIME_UNSET))
    }

    @Test
    fun viewsAreHumanReadable() {
        assertEquals("0", MediaFormatters.formatViews(0))
        assertEquals("0", MediaFormatters.formatViews(-5))
        assertEquals("999", MediaFormatters.formatViews(999))
        assertEquals("1K", MediaFormatters.formatViews(1000))
        assertEquals("1.2K", MediaFormatters.formatViews(1200))
        assertEquals("12.4K", MediaFormatters.formatViews(12400))
        assertEquals("1M", MediaFormatters.formatViews(1_000_000))
        assertEquals("1.3M", MediaFormatters.formatViews(1_300_000))
        assertEquals("2.5B", MediaFormatters.formatViews(2_500_000_000))
    }

    @Test
    fun resolutionRequiresPositiveDimensions() {
        assertEquals("1920×1080", MediaFormatters.formatResolution(1920, 1080))
        assertEquals("3840×2160", MediaFormatters.formatResolution(3840, 2160))
        assertNull(MediaFormatters.formatResolution(0, 1080))
        assertNull(MediaFormatters.formatResolution(1920, 0))
        assertNull(MediaFormatters.formatResolution(-1, -1))
    }
}
