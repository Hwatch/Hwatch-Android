package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalcTest {

    @Test
    fun normalProgress() {
        assertEquals(0.5f, ProgressCalc.fraction(5, 10)!!, 0.0001f)
    }

    @Test
    fun zeroTotalReturnsNull() {
        assertNull(ProgressCalc.fraction(0, 0))
        assertNull(ProgressCalc.fraction(5, 0))
    }

    @Test
    fun negativeTotalReturnsNull() {
        assertNull(ProgressCalc.fraction(5, -10))
    }

    @Test
    fun overCompletedIsClampedToOne() {
        val fraction = ProgressCalc.fraction(20, 10)!!
        assertEquals(1f, fraction, 0.0f)
        assertTrue(fraction in 0f..1f)
    }

    @Test
    fun negativePartIsClampedToZero() {
        val fraction = ProgressCalc.fraction(-5, 10)!!
        assertEquals(0f, fraction, 0.0f)
    }

    @Test
    fun resultNeverNaNOrInfinite() {
        val samples = listOf(
            0L to 0L,
            Long.MAX_VALUE to 1L,
            1L to Long.MAX_VALUE,
            Long.MIN_VALUE to Long.MAX_VALUE,
        )
        samples.forEach { (part, total) ->
            ProgressCalc.fraction(part, total)?.let { fraction ->
                assertFalse(fraction.isNaN())
                assertFalse(fraction.isInfinite())
                assertTrue(fraction in 0f..1f)
            }
        }
    }
}
