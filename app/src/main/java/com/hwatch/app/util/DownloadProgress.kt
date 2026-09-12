package com.hwatch.app.util

/**
 * 下载进度纯函数。`total <= 0` 或 `downloaded < 0` 视为未知并返回 null（不显示 0/0）。
 * 有效结果 clamp 到 `0f..1f`，不产生 NaN / Infinity。
 */
object DownloadProgress {

    fun fraction(downloaded: Long, total: Long): Float? {
        if (total <= 0 || downloaded < 0) return null
        val value = downloaded.toDouble() / total.toDouble()
        if (!value.isFinite()) return null
        return value.toFloat().coerceIn(0f, 1f)
    }
}
