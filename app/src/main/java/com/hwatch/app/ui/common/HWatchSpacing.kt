package com.hwatch.app.ui.common

import androidx.compose.ui.unit.dp

/**
 * 统一间距 token。禁止在 Composable 里随机写 7.dp / 13.dp / 19.dp 等。
 * 只有确实有视觉理由时才使用非 token 值。
 */
object HWatchSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** 页面水平内边距。 */
    val screenHorizontal = md

    /** 卡片内边距。 */
    val cardPadding = md

    /** 相邻 section 之间的间距。 */
    val sectionGap = lg

    /** 列表网格间距。 */
    val gridGap = sm
}
