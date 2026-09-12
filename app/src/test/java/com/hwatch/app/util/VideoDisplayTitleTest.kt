package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoDisplayTitleTest {

    @Test
    fun translatedTitlePreferredWithOriginalSubtitle() {
        val title = videoDisplayTitle("中文标题", "Original Title")
        assertEquals("中文标题", title.primary)
        assertEquals("Original Title", title.secondary)
    }

    @Test
    fun identicalTranslatedAndOriginalHasNoSubtitle() {
        val title = videoDisplayTitle("Same", "Same")
        assertEquals("Same", title.primary)
        assertNull(title.secondary)
    }

    @Test
    fun blankTranslatedTitleFallsBackToOriginal() {
        val title = videoDisplayTitle("", "Original Title")
        assertEquals("Original Title", title.primary)
        assertNull(title.secondary)
    }

    @Test
    fun blankOriginalWithTranslationHasNoSubtitle() {
        val title = videoDisplayTitle("中文标题", "")
        assertEquals("中文标题", title.primary)
        assertNull(title.secondary)
    }

    @Test
    fun whitespaceIsTrimmed() {
        val title = videoDisplayTitle("  ", "  Original  ")
        assertEquals("Original", title.primary)
        assertNull(title.secondary)
    }

    @Test
    fun bothBlankProducesEmptyPrimary() {
        val title = videoDisplayTitle("", "")
        assertEquals("", title.primary)
        assertNull(title.secondary)
    }
}
