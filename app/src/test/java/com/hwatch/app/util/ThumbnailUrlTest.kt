package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailUrlTest {

    private val base = "http://192.168.1.10:8787/"

    @Test
    fun colonIsPercentEncoded() {
        assertEquals(
            "http://192.168.1.10:8787/thumb/source%3A3144962",
            ThumbnailUrl.build(base, "source:3144962"),
        )
    }

    @Test
    fun chineseIsPercentEncoded() {
        val url = ThumbnailUrl.build(base, "中文")!!
        assertEquals("thumb/${UrlEncoding.encodePathSegment("中文")}", url.removePrefix(base))
        assertTrue(url.removePrefix(base).all { it.code < 128 })
    }

    @Test
    fun spaceIsPercentEncoded() {
        assertEquals(
            "http://192.168.1.10:8787/thumb/a%20b",
            ThumbnailUrl.build(base, "a b"),
        )
    }

    @Test
    fun slashIsPercentEncoded() {
        val url = ThumbnailUrl.build(base, "a/b")!!
        assertEquals("thumb/a%2Fb", url.removePrefix(base))
    }

    @Test
    fun percentIsPercentEncoded() {
        val url = ThumbnailUrl.build(base, "50%")!!
        assertEquals("thumb/50%25", url.removePrefix(base))
    }

    @Test
    fun questionMarkIsPercentEncoded() {
        val url = ThumbnailUrl.build(base, "a?b")!!
        assertEquals("thumb/a%3Fb", url.removePrefix(base))
    }

    @Test
    fun baseUrlWithoutTrailingSlashStillBuildsThumbPath() {
        assertEquals(
            "http://192.168.1.10:8787/thumb/source%3A1",
            ThumbnailUrl.build("http://192.168.1.10:8787", "source:1"),
        )
    }

    @Test
    fun invalidBaseUrlReturnsNull() {
        assertNull(ThumbnailUrl.build("not a url", "source:1"))
    }

    @Test
    fun emptyVideoIdReturnsNull() {
        assertNull(ThumbnailUrl.build(base, ""))
    }
}
