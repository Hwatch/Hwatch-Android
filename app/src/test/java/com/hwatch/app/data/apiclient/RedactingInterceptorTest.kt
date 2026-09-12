package com.hwatch.app.data.apiclient

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactingInterceptorTest {

    @Test
    fun apiPathsMayBeBodyLogged() {
        assertTrue(RedactingInterceptor.shouldLogBody("/api/videos"))
        assertTrue(RedactingInterceptor.shouldLogBody("/api/dashboard"))
        assertTrue(RedactingInterceptor.shouldLogBody("/api/subscriptions"))
        assertTrue(RedactingInterceptor.shouldLogBody("/api/creators/search"))
    }

    @Test
    fun settingsNeverBodyLogged() {
        assertFalse(RedactingInterceptor.shouldLogBody("/api/settings"))
    }

    @Test
    fun eventsNeverBodyLogged() {
        assertFalse(RedactingInterceptor.shouldLogBody("/api/events"))
    }

    @Test
    fun authPathsNeverBodyLogged() {
        assertFalse(RedactingInterceptor.shouldLogBody("/api/auth/status"))
        assertFalse(RedactingInterceptor.shouldLogBody("/api/auth/setup"))
        assertFalse(RedactingInterceptor.shouldLogBody("/api/auth/login"))
        assertFalse(RedactingInterceptor.shouldLogBody("/api/auth/me"))
        assertFalse(RedactingInterceptor.shouldLogBody("/api/auth/logout"))
        assertFalse(RedactingInterceptor.shouldLogBody("/api/auth"))
    }

    @Test
    fun mediaAndThumbNeverBodyLogged() {
        assertFalse(RedactingInterceptor.shouldLogBody("/media/abc"))
        assertFalse(RedactingInterceptor.shouldLogBody("/thumb/abc"))
    }

    @Test
    fun encodedMediaPathNeverBodyLogged() {
        assertFalse(RedactingInterceptor.shouldLogBody("/media/source%3A3144962"))
        assertFalse(RedactingInterceptor.shouldLogBody("/thumb/source%3A3144962"))
    }

    @Test
    fun bareApiPrefixNotLogged() {
        assertFalse(RedactingInterceptor.shouldLogBody("/api/"))
        assertFalse(RedactingInterceptor.shouldLogBody("/api"))
    }

    @Test
    fun describeRequestOmitsQuery() {
        val url = "http://192.168.1.10:8787/api/videos?q=secret&scope=all".toHttpUrl()
        assertEquals(
            "GET http://192.168.1.10:8787/api/videos",
            RedactingInterceptor.describeRequest("GET", url),
        )
    }

    @Test
    fun describeRequestSnapshotSettingsPathOnly() {
        // 请求行本身不含 query/body；即使对 /api/settings 也只输出路径，不输出内容。
        val url = "http://192.168.1.10:8787/api/settings".toHttpUrl()
        assertEquals(
            "PUT http://192.168.1.10:8787/api/settings",
            RedactingInterceptor.describeRequest("PUT", url),
        )
    }

    @Test
    fun describeRequestOmitsDefaultHttpsPort() {
        val url = "https://hwatch.example.com/api/videos?x=1".toHttpUrl()
        assertEquals(
            "GET https://hwatch.example.com/api/videos",
            RedactingInterceptor.describeRequest("GET", url),
        )
    }

    @Test
    fun binaryContentTypesExcluded() {
        assertTrue(RedactingInterceptor.isBinaryContentType("image/jpeg"))
        assertTrue(RedactingInterceptor.isBinaryContentType("image/svg+xml"))
        assertTrue(RedactingInterceptor.isBinaryContentType("video/mp4"))
        assertTrue(RedactingInterceptor.isBinaryContentType("audio/mpeg"))
        assertTrue(RedactingInterceptor.isBinaryContentType("application/octet-stream"))
    }

    @Test
    fun textContentTypesAllowed() {
        assertFalse(RedactingInterceptor.isBinaryContentType("application/json"))
        assertFalse(RedactingInterceptor.isBinaryContentType("text/plain"))
        assertFalse(RedactingInterceptor.isBinaryContentType(null))
        assertFalse(RedactingInterceptor.isBinaryContentType(""))
    }
}