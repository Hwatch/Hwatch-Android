package com.hwatch.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun parseInstant_utc() {
        assertEquals(
            Instant.parse("2026-09-11T16:39:37Z"),
            TimeUtils.parseInstant("2026-09-11T16:39:37Z"),
        )
    }

    @Test
    fun parseInstant_withOffset() {
        assertEquals(
            Instant.parse("2026-09-11T16:39:37Z"),
            TimeUtils.parseInstant("2026-09-12T00:39:37+08:00"),
        )
    }

    @Test
    fun parseInstant_invalid_throws() {
        assertThrows(DateTimeParseException::class.java) {
            TimeUtils.parseInstant("not-a-time")
        }
    }

    @Test
    fun resolveZone_valid() {
        assertEquals(ZoneId.of("Asia/Shanghai"), TimeUtils.resolveZone("Asia/Shanghai"))
    }

    @Test
    fun resolveZone_invalidFallsBackToUtc() {
        assertEquals(ZoneOffset.UTC, TimeUtils.resolveZone("Bad/Zone"))
    }

    @Test
    fun toZoned_utcToShanghai() {
        val instant = Instant.parse("2026-09-11T16:39:37Z")
        val zoned = TimeUtils.toZoned(instant, ZoneId.of("Asia/Shanghai"))
        assertEquals(0, zoned.hour)
        assertEquals(39, zoned.minute)
        assertEquals(28_800, zoned.offset.totalSeconds)
        assertEquals("Asia/Shanghai", zoned.zone.id)
    }

    @Test
    fun formatDisplay_shanghai() {
        val instant = Instant.parse("2026-09-11T16:39:37Z")
        assertEquals("2026-09-12 00:39", TimeUtils.formatDisplay(instant, ZoneId.of("Asia/Shanghai")))
    }

    @Test
    fun formatDisplayFromRfc3339_success() {
        val result = TimeUtils.formatDisplayFromRfc3339("2026-09-11T16:39:37Z", "Asia/Shanghai")
        assertTrue(result.isSuccess)
        assertEquals("2026-09-12 00:39", result.getOrNull())
    }

    @Test
    fun formatDisplayFromRfc3339_invalidZoneFallsBackToUtc() {
        val result = TimeUtils.formatDisplayFromRfc3339("2026-09-11T16:39:37Z", "Bad/Zone")
        assertTrue(result.isSuccess)
        assertEquals("2026-09-11 16:39", result.getOrNull())
    }

    @Test
    fun formatDisplayFromRfc3339_invalidDateFails() {
        val result = TimeUtils.formatDisplayFromRfc3339("not-a-time", "Asia/Shanghai")
        assertTrue(result.isFailure)
    }

    @Test
    fun formatDisplayFromRfc3339_acceptsDateOnly() {
        val result = TimeUtils.formatDisplayFromRfc3339("2026-07-26", "Asia/Shanghai")
        assertTrue(result.isSuccess)
        assertEquals("2026-07-26", result.getOrNull())
    }

    @Test
    fun formatDisplayFromRfc3339_rfc3339StillConverts() {
        assertEquals(
            "2026-06-10 08:00",
            TimeUtils.formatDisplayFromRfc3339("2026-06-10T00:00:00Z", "Asia/Shanghai").getOrThrow(),
        )
    }

    @Test
    fun toZoned_timezoneAppliedNotConcatenated() {
        // 东京是 UTC+9：16:00Z -> 次日 01:00（+9 小时），验证真的使用时区转换。
        val tokyo = TimeUtils.toZoned(Instant.parse("2026-09-11T16:00:00Z"), ZoneId.of("Asia/Tokyo"))
        assertEquals(1, tokyo.hour)
        assertEquals("Asia/Tokyo", tokyo.zone.id)
        assertEquals(32_400, tokyo.offset.totalSeconds)
    }
}