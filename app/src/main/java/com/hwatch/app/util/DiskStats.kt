package com.hwatch.app.util

/**
 * 磁盘容量计算，全部带防御：
 * `total <= 0`、`free < 0`、`free > total` 均视为无效容量并返回 `null`（UI 显示未知）。
 * 有效时 used = total - free，占用比例 clamp 到 `0f..1f`，绝不产生 NaN / Infinity / 负值。
 */
object DiskStatsCalculator {

    fun isValid(total: Long, free: Long): Boolean = total > 0 && free >= 0 && free <= total

    fun usedBytes(total: Long, free: Long): Long? =
        if (isValid(total, free)) total - free else null

    fun usedFraction(total: Long, free: Long): Float? {
        if (!isValid(total, free)) return null
        val fraction = (total - free).toDouble() / total.toDouble()
        if (!fraction.isFinite()) return null
        return fraction.toFloat().coerceIn(0f, 1f)
    }
}
