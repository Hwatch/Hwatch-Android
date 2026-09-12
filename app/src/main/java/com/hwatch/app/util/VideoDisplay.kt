package com.hwatch.app.util

/**
 * 视频标题展示规则（Dashboard / 影视库共用）：
 * - `translated_title` 非空：主标题 = 译文，副标题 = 原文（仅当原文非空且与译文不同）
 * - `translated_title` 为空：主标题 = 原文，无副标题
 */
data class VideoDisplayTitle(val primary: String, val secondary: String?)

fun videoDisplayTitle(translatedTitle: String, title: String): VideoDisplayTitle {
    val translated = translatedTitle.trim()
    val original = title.trim()
    return when {
        translated.isNotEmpty() -> VideoDisplayTitle(
            primary = translated,
            secondary = if (original.isNotEmpty() && original != translated) original else null,
        )
        else -> VideoDisplayTitle(primary = original, secondary = null)
    }
}
