package com.hwatch.app.util

import java.util.Locale

/**
 * 媒体元数据格式化。所有函数对非法输入返回 `null` 或安全 fallback，不崩溃、不输出 `0×0`。
 */
object MediaFormatters {

    /** `duration_seconds` → `mm:ss`（<1h）或 `h:mm:ss`（>=1h）；负数/非法返回 null。 */
    fun formatDuration(seconds: Long): String? {
        if (seconds < 0) return null
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    /**
     * 播放位置/时长（毫秒）→ `mm:ss` / `h:mm:ss`。
     * `C.TIME_UNSET` 等负值显示 `--:--`，绝不显示负时间。
     */
    fun formatPlaybackTime(milliseconds: Long): String {
        if (milliseconds < 0) return "--:--"
        return formatDuration(milliseconds / 1000) ?: "--:--"
    }

    /** `views` → 可读数字：`999` / `1.2K` / `12.4K` / `1.3M`。 */
    fun formatViews(views: Long): String {
        if (views <= 0) return "0"
        return when {
            views < 1_000 -> views.toString()
            views < 1_000_000 -> trimDecimal(views / 1_000.0) + "K"
            views < 1_000_000_000 -> trimDecimal(views / 1_000_000.0) + "M"
            else -> trimDecimal(views / 1_000_000_000.0) + "B"
        }
    }

    /** `width`/`height` → `1920×1080`；任一 <= 0 返回 null（隐藏该元数据）。 */
    fun formatResolution(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        return "$width×$height"
    }

    private fun trimDecimal(value: Double): String {
        val formatted = String.format(Locale.US, "%.1f", value)
        return formatted.removeSuffix(".0")
    }
}
