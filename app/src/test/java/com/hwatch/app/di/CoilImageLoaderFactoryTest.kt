package com.hwatch.app.di

import coil3.network.NetworkFetcher
import coil3.svg.SvgDecoder
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoilImageLoaderFactoryTest {

    @Test
    fun svgDecoderIsRegistered() {
        val components = CoilImageLoaderFactory.create(OkHttpClient())
        assertTrue(components.decoders.any { it is SvgDecoder.Factory })
    }

    @Test
    fun okHttpNetworkFetcherIsRegistered() {
        val components = CoilImageLoaderFactory.create(OkHttpClient())
        assertEquals(1, components.fetchers.size)
        assertTrue(components.fetchers.single() is NetworkFetcher.Factory)
    }
}
