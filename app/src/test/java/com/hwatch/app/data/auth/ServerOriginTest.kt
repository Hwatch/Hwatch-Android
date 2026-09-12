package com.hwatch.app.data.auth

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerOriginTest {

    @Test
    fun effectiveDefaultPorts() {
        assertEquals(ServerOrigin("http", "nas.local", 80), ServerOrigin.from("http://nas.local/api".toHttpUrl()))
        assertEquals(ServerOrigin("https", "nas.local", 443), ServerOrigin.from("https://nas.local/api".toHttpUrl()))
    }

    @Test
    fun explicitPorts() {
        assertEquals(ServerOrigin("http", "nas.local", 8787), ServerOrigin.from("http://nas.local:8787/api".toHttpUrl()))
    }

    @Test
    fun sameHostDifferentPortAreNotEqual() {
        val a = ServerOrigin.from("http://nas.local:8787/".toHttpUrl())
        val b = ServerOrigin.from("http://nas.local:9000/".toHttpUrl())
        assertNotEquals(a, b)
    }

    @Test
    fun schemeDifferenceIsNotEqual() {
        val http = ServerOrigin.from("http://nas.local:8787/".toHttpUrl())
        val https = ServerOrigin.from("https://nas.local:8787/".toHttpUrl())
        assertNotEquals(http, https)
    }

    @Test
    fun keyRoundTrip() {
        val origin = ServerOrigin("http", "nas.local", 8787)
        assertEquals(origin, ServerOrigin.fromKey(origin.key()))
    }

    @Test
    fun invalidKeyReturnsNull() {
        assertNull(ServerOrigin.fromKey("not a url"))
    }
}
