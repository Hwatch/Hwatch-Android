package com.hwatch.app.util

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 时间工具。HWatch 服务端时间一律为 RFC3339 UTC（如 `2026-09-11T16:39:37Z`），
 * 显示时按 `dashboard.timezone` 转换为本地时区时间。
 *
 * 真实服务器部分字段（如 `published_at`）可能是日期型 `YYYY-MM-DD`，展示层需兼容，
 * 日期型按原样显示日期，不做时区换算。
 */
object TimeUtils {

    private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val DATE_ONLY_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

    fun parseInstant(text: String): Instant {
        val normalized = text.trim()
        if (normalized.endsWith("Z", ignoreCase = true)) {
            return Instant.parse(normalized)
        }
        return runCatching {
            OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        }.getOrElse {
            Instant.parse(normalized + "Z")
        }
    }

    /** 解析时区名；无效时回退 UTC，避免发生崩溃。 */
    fun resolveZone(timezone: String): ZoneId {
        return try {
            ZoneId.of(timezone)
        } catch (e: DateTimeException) {
            ZoneOffset.UTC
        }
    }

    fun toZoned(instant: Instant, zone: ZoneId): ZonedDateTime = instant.atZone(zone)

    fun formatDisplay(instant: Instant, zone: ZoneId): String {
        return instant.atZone(zone).format(DISPLAY_FORMAT)
    }

    /**
     * 一步完成 解析 -> 时区转换 -> 显示格式化。
     * - RFC3339：按 `timezone` 转换后显示 `yyyy-MM-dd HH:mm`
     * - `YYYY-MM-DD`：原样显示日期（不做时区换算）
     * - 解析失败：返回失败信息
     */
    fun formatDisplayFromRfc3339(text: String, timezone: String): Result<String> {
        return try {
            val normalized = text.trim()
            if (DATE_ONLY_REGEX.matches(normalized)) {
                Result.success(LocalDate.parse(normalized).toString())
            } else {
                val instant = parseInstant(normalized)
                Result.success(formatDisplay(instant, resolveZone(timezone)))
            }
        } catch (e: DateTimeParseException) {
            Result.failure(e)
        } catch (e: DateTimeException) {
            Result.failure(e)
        }
    }
}