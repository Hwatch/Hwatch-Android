package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlEncodingTest {

    @Test
    fun colonEncodedForVideoId() {
        assertEquals("source%3A3144962", UrlEncoding.encodePathSegment("source:3144962"))
    }

    @Test
    fun colonEncodedForSlug() {
        assertEquals("source%3Acreator", UrlEncoding.encodePathSegment("source:creator"))
    }

    @Test
    fun spaceEncoded() {
        assertEquals("a%20b", UrlEncoding.encodePathSegment("a b"))
    }

    @Test
    fun chineseEncodedUtf8() {
        assertEquals("%E4%B8%AD%E6%96%87", UrlEncoding.encodePathSegment("中文"))
    }

    @Test
    fun slashEncoded() {
        assertEquals("a%2Fb", UrlEncoding.encodePathSegment("a/b"))
    }

    @Test
    fun percentEncoded() {
        assertEquals("100%25", UrlEncoding.encodePathSegment("100%"))
    }

    @Test
    fun questionMarkAndEqualsEncoded() {
        assertEquals("a%3Fb%3Dc", UrlEncoding.encodePathSegment("a?b=c"))
    }

    @Test
    fun unreservedKeptRaw() {
        assertEquals("a-b_c.d~e", UrlEncoding.encodePathSegment("a-b_c.d~e"))
    }

    @Test
    fun onlyValidCharactersEncoded() {
        val encoded = UrlEncoding.encodePathSegment("source:my file 中文/100%?x=1")
        val allowed = Regex("[A-Za-z0-9%.-_~]*")
        assertEquals(encoded, allowed.matchEntire(encoded)!!.value)
    }
}