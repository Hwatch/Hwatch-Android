package com.hwatch.app.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLifecycleTest {

    @Test
    fun pauseWhenNotChangingConfigurations() {
        assertTrue(PlayerLifecycle.shouldPauseOnStop(isChangingConfigurations = false))
    }

    @Test
    fun doNotPauseDuringConfigurationChange() {
        assertFalse(PlayerLifecycle.shouldPauseOnStop(isChangingConfigurations = true))
    }
}
