package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlTest {

    private val base = "http://192.168.1.10:8787/"

    @Test
    fun colonIsPercentEncoded() {
        assertEquals(
            "http://192.168.1.10:8787/media/source%3A3144962",
            MediaUrl.build(base, "source:3144962"),
        )
    }

    @Test
    fun chineseIsPercentEncoded() {
        val url = MediaUrl.build(base, "中文")!!
        assertEquals("media/${UrlEncoding.encodePathSegment("中文")}", url.removePrefix(base))
        assertTrue(url.removePrefix(base).all { it.code < 128 })
    }

    @Test
    fun spaceIsPercentEncoded() {
        assertEquals("http://192.168.1.10:8787/media/a%20b", MediaUrl.build(base, "a b"))
    }

    @Test
    fun slashIsPercentEncoded() {
        assertEquals("media/a%2Fb", MediaUrl.build(base, "a/b")!!.removePrefix(base))
    }

    @Test
    fun percentIsPercentEncoded() {
        assertEquals("media/50%25", MediaUrl.build(base, "50%")!!.removePrefix(base))
    }

    @Test
    fun questionMarkIsPercentEncoded() {
        assertEquals("media/a%3Fb", MediaUrl.build(base, "a?b")!!.removePrefix(base))
    }

    @Test
    fun noDoubleEncodingOfColon() {
        val url = MediaUrl.build(base, "source:3144962")!!
        assertTrue(url.contains("%3A"))
        assertTrue(!url.contains("%253A"))
    }

    @Test
    fun baseUrlWithoutTrailingSlashStillBuildsMediaPath() {
        assertEquals(
            "http://192.168.1.10:8787/media/source%3A1",
            MediaUrl.build("http://192.168.1.10:8787", "source:1"),
        )
    }

    @Test
    fun invalidBaseUrlReturnsNull() {
        assertNull(MediaUrl.build("not a url", "source:1"))
    }

    @Test
    fun emptyVideoIdReturnsNull() {
        assertNull(MediaUrl.build(base, ""))
    }
}
