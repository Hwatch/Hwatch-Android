package com.hwatch.app.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleTest {

    private fun base(): okhttp3.OkHttpClient =
        NetworkModule.createOkHttpClient(enableBodyLogging = false)

    @Test
    fun baseClientHasNoGlobalCallTimeout() {
        assertEquals(0, base().callTimeoutMillis)
    }

    @Test
    fun streamingClientHasNoCallOrReadTimeout() {
        val streaming = NetworkModule.deriveStreamingClient(base())
        assertEquals(0, streaming.callTimeoutMillis)
        assertEquals(0, streaming.readTimeoutMillis)
        assertTrue(streaming.connectTimeoutMillis > 0)
    }

    @Test
    fun streamingClientSharesConnectionInfrastructure() {
        val baseClient = base()
        val streaming = NetworkModule.deriveStreamingClient(baseClient)
        assertSame(baseClient.connectionPool, streaming.connectionPool)
        assertSame(baseClient.dispatcher, streaming.dispatcher)
    }

    @Test
    fun deriveDoesNotMutateBaseClient() {
        val baseClient = base()
        NetworkModule.deriveStreamingClient(baseClient)
        assertEquals(60_000, baseClient.readTimeoutMillis)
    }
}
