package com.hwatch.app.ui.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSeekTest {

    @Test
    fun backFromZeroClampsToZero() {
        assertEquals(0L, PlayerSeek.seekTarget(0L, 100_000L, -PlayerSeek.STEP_MS))
    }

    @Test
    fun backFromFiveSecondsClampsToZero() {
        assertEquals(0L, PlayerSeek.seekTarget(5_000L, 100_000L, -PlayerSeek.STEP_MS))
    }

    @Test
    fun forwardNearEndClampsToDuration() {
        assertEquals(100_000L, PlayerSeek.seekTarget(95_000L, 100_000L, PlayerSeek.STEP_MS))
    }

    @Test
    fun timeUnsetDoesNotClampUpperBound() {
        assertEquals(105_000L, PlayerSeek.seekTarget(95_000L, C.TIME_UNSET, PlayerSeek.STEP_MS))
    }

    @Test
    fun normalBackAndForward() {
        assertEquals(10_000L, PlayerSeek.seekTarget(20_000L, 100_000L, -PlayerSeek.STEP_MS))
        assertEquals(30_000L, PlayerSeek.seekTarget(20_000L, 100_000L, PlayerSeek.STEP_MS))
    }

    @Test
    fun negativePositionTreatedAsZero() {
        assertEquals(0L, PlayerSeek.seekTarget(-500L, 100_000L, -PlayerSeek.STEP_MS))
    }

    @Test
    fun neverReturnsNegative() {
        assertEquals(0L, PlayerSeek.seekTarget(-100_000L, C.TIME_UNSET, -PlayerSeek.STEP_MS))
    }
}
