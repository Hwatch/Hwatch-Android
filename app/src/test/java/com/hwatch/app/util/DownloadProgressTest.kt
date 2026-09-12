package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadProgressTest {

    @Test
    fun normalProgress() {
        assertEquals(0.5f, DownloadProgress.fraction(500, 1000)!!, 0.0001f)
    }

    @Test
    fun zeroTotalIsUnknown() {
        assertNull(DownloadProgress.fraction(0, 0))
        assertNull(DownloadProgress.fraction(100, 0))
    }

    @Test
    fun negativeTotalIsUnknown() {
        assertNull(DownloadProgress.fraction(100, -1))
    }

    @Test
    fun negativeDownloadedIsUnknown() {
        assertNull(DownloadProgress.fraction(-1, 100))
    }

    @Test
    fun downloadedGreaterThanTotalClampsToOne() {
        assertEquals(1f, DownloadProgress.fraction(2000, 1000)!!, 0f)
    }

    @Test
    fun neverNaNOrInfiniteOrOutOfRange() {
        val samples = listOf(
            0L to 0L,
            Long.MAX_VALUE to 1L,
            1L to Long.MAX_VALUE,
            Long.MIN_VALUE to Long.MAX_VALUE,
        )
        samples.forEach { (downloaded, total) ->
            DownloadProgress.fraction(downloaded, total)?.let { fraction ->
                assert(!fraction.isNaN())
                assert(!fraction.isInfinite())
                assert(fraction in 0f..1f)
            }
        }
    }
}
