package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlTest {

    @Test
    fun normalize_ipWithoutTrailingSlash() {
        val result = ServerUrl.normalize("http://192.168.1.10:8787")
        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.10:8787/", result.getOrNull())
    }

    @Test
    fun normalize_ipWithTrailingSlash() {
        val result = ServerUrl.normalize("http://192.168.1.10:8787/")
        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.10:8787/", result.getOrNull())
    }

    @Test
    fun normalize_httpsHost() {
        val result = ServerUrl.normalize("https://hwatch.example.com")
        assertTrue(result.isSuccess)
        assertEquals("https://hwatch.example.com/", result.getOrNull())
    }

    @Test
    fun normalize_mdnsLocalHost() {
        val result = ServerUrl.normalize("http://nas.local:8787")
        assertTrue(result.isSuccess)
        assertEquals("http://nas.local:8787/", result.getOrNull())
    }

    @Test
    fun normalize_missingSchemeIsPrepended() {
        val result = ServerUrl.normalize("192.168.1.100:8787")
        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.100:8787/", result.getOrNull())
    }

    @Test
    fun normalize_surroundingWhitespaceTrimmed() {
        val result = ServerUrl.normalize("  http://nas.local:8787  ")
        assertTrue(result.isSuccess)
        assertEquals("http://nas.local:8787/", result.getOrNull())
    }

    @Test
    fun normalize_endsWithSlashForRetrofit() {
        val result = ServerUrl.normalize("http://192.168.1.10:8787/")
        assertTrue(result.getOrNull()!!.endsWith("/"))
    }

    @Test
    fun normalize_emptyInputFailsWithMessage() {
        val result = ServerUrl.normalize("")
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    @Test
    fun normalize_blankInputFails() {
        val result = ServerUrl.normalize("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun normalize_stringWithSpacesIsInvalid() {
        val result = ServerUrl.normalize("not a url")
        assertTrue(result.isFailure)
    }

    @Test
    fun normalize_brokenSchemeIsInvalid() {
        val result = ServerUrl.normalize("://broken")
        assertTrue(result.isFailure)
    }

    @Test
    fun normalize_unsupportedSchemeFails() {
        val result = ServerUrl.normalize("ftp://nas.local:8787")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("http") == true)
    }
}