package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiskStatsCalculatorTest {

    @Test
    fun normalCapacityComputesUsedAndFraction() {
        assertTrue(DiskStatsCalculator.isValid(1000, 400))
        assertEquals(600L, DiskStatsCalculator.usedBytes(1000, 400))
        assertEquals(0.6f, DiskStatsCalculator.usedFraction(1000, 400)!!, 0.0001f)
    }

    @Test
    fun zeroTotalIsInvalid() {
        assertFalse(DiskStatsCalculator.isValid(0, 0))
        assertNull(DiskStatsCalculator.usedBytes(0, 0))
        assertNull(DiskStatsCalculator.usedFraction(0, 0))
    }

    @Test
    fun negativeTotalIsInvalid() {
        assertNull(DiskStatsCalculator.usedBytes(-1, 0))
        assertNull(DiskStatsCalculator.usedFraction(-1, 0))
    }

    @Test
    fun freeGreaterThanTotalIsInvalid() {
        assertFalse(DiskStatsCalculator.isValid(1000, 2000))
        assertNull(DiskStatsCalculator.usedBytes(1000, 2000))
        assertNull(DiskStatsCalculator.usedFraction(1000, 2000))
    }

    @Test
    fun negativeFreeIsInvalid() {
        assertFalse(DiskStatsCalculator.isValid(1000, -1))
        assertNull(DiskStatsCalculator.usedBytes(1000, -1))
        assertNull(DiskStatsCalculator.usedFraction(1000, -1))
    }

    @Test
    fun fullDiskFractionIsOneAndClamped() {
        val fraction = DiskStatsCalculator.usedFraction(1000, 0)
        assertEquals(1f, fraction!!, 0.0f)
        assertTrue(fraction in 0f..1f)
    }

    @Test
    fun emptyDiskFractionIsZero() {
        assertEquals(0f, DiskStatsCalculator.usedFraction(1000, 1000)!!, 0.0f)
    }

    @Test
    fun fractionNeverNaNOrInfiniteOrOutOfRange() {
        val samples = listOf(
            0L to 0L,
            -5L to -5L,
            1L to Long.MAX_VALUE,
            Long.MAX_VALUE to 0L,
            Long.MAX_VALUE to Long.MAX_VALUE,
        )
        samples.forEach { (total, free) ->
            val fraction = DiskStatsCalculator.usedFraction(total, free)
            if (fraction != null) {
                assertFalse("fraction must be finite for total=$total free=$free", fraction.isNaN())
                assertFalse(fraction.isInfinite())
                assertTrue(fraction in 0f..1f)
            }
        }
    }
}
