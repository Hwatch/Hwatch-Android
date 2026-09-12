package com.hwatch.app.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

/**
 * 全局 SnackbarHostState。由主 Scaffold 提供，页面用它显示短暂通知（如刷新失败）。
 * 原则：短暂通知 → Snackbar；持续错误 → Inline；页面无法加载 → FullScreenError。
 */
val LocalSnackbarHostState = compositionLocalOf { SnackbarHostState() }
