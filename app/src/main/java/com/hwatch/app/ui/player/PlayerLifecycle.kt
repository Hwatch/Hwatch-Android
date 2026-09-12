package com.hwatch.app.ui.player

/**
 * 播放器生命周期决策（纯逻辑，可单测）。
 *
 * 普通 orientation configuration change 也会触发 `Activity.onStop()`，此时不应暂停；
 * 只有真正进入后台 / 锁屏（`isChangingConfigurations == false`）才暂停。
 */
object PlayerLifecycle {

    fun shouldPauseOnStop(isChangingConfigurations: Boolean): Boolean = !isChangingConfigurations
}
