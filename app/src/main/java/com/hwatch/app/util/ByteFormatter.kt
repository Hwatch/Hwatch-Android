package com.hwatch.app.util

import java.util.Locale

/**
 * 字节/下载速度的人类可读格式化。使用 1024 进制：KB、MB、GB、TB…（匹配
 * dashboard 的 disk_total/disk_free 等 size 字段与 download_speed 的显示需求）。
 */
object ByteFormatter {

    private const val UNIT_BYTES = 1024.0
    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    fun formatFileSize(bytes: Long): String = format(bytes, "")

    fun formatDownloadSpeed(bytesPerSecond: Long): String = format(bytesPerSecond, "/s")

    private fun format(bytes: Long, suffix: String): String {
        if (bytes < 1024) {
            return "$bytes B$suffix"
        }
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < UNITS.size - 1) {
            value /= UNIT_BYTES
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s%s", value, UNITS[unitIndex], suffix)
    }
}