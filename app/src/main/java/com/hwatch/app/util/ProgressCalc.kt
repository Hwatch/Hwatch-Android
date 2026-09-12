package com.hwatch.app.util

/**
 * 通用进度计算。`total <= 0` 视为未知并返回 `null`（避免除零）。
 * 有效结果 clamp 到 `0f..1f`，不产生 NaN / Infinity。
 */
object ProgressCalc {

    fun fraction(part: Long, total: Long): Float? {
        if (total <= 0) return null
        val fraction = part.toDouble() / total.toDouble()
        if (!fraction.isFinite()) return null
        return fraction.toFloat().coerceIn(0f, 1f)
    }
}
