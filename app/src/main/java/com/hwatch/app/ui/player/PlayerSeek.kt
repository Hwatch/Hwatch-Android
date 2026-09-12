package com.hwatch.app.ui.player

import androidx.media3.common.C

/** 快进/后退目标计算，结果始终 clamp 在 `[0, duration]`（duration 已知时）。 */
object PlayerSeek {

    const val STEP_MS = 10_000L

    fun seekTarget(positionMs: Long, durationMs: Long, deltaMs: Long): Long {
        val base = if (positionMs < 0) 0L else positionMs
        var target = base + deltaMs
        if (target < 0) target = 0L
        if (durationMs != C.TIME_UNSET && durationMs > 0 && target > durationMs) {
            target = durationMs
        }
        return target
    }
}
