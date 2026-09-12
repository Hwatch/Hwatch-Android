package com.hwatch.app.di

import okhttp3.OkHttpClient
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HWatchApiProviderTest {

    private fun provider(): HWatchApiProvider =
        HWatchApiProvider(json = NetworkModule.createJson(), okHttpClient = OkHttpClient())

    @Test
    fun sameBaseUrlReturnsSameApiInstance() {
        val provider = provider()
        val first = provider.getApi("http://192.168.1.10:8787/")
        val second = provider.getApi("http://192.168.1.10:8787/")
        assertSame(first, second)
    }

    @Test
    fun changedBaseUrlReturnsNewApiInstance() {
        val provider = provider()
        val first = provider.getApi("http://192.168.1.10:8787/")
        val second = provider.getApi("http://192.168.1.11:8787/")
        assertNotSame(first, second)
    }

    @Test
    fun okHttpClientIsSharedAcrossRebuilds() {
        val provider = provider()
        val client = provider.okHttpClient
        provider.getApi("http://192.168.1.10:8787/")
        provider.getApi("http://192.168.1.11:8787/")
        assertSame(client, provider.okHttpClient)
    }

    @Test
    fun temporaryUrlIsNotCached() {
        val provider = provider()
        val first = provider.getApiForRawUrl("http://192.168.1.99:8787/").getOrThrow()
        val second = provider.getApiForRawUrl("http://192.168.1.99:8787/").getOrThrow()
        assertNotSame(first, second)
    }

    @Test
    fun rawUrlInvalidReturnsFailure() {
        assertTrue(provider().getApiForRawUrl("ftp://nas.local:8787").isFailure)
    }

    @Test
    fun temporaryUrlDoesNotPolluteOfficialCache() {
        val provider = provider()
        val official = provider.getApi("http://192.168.1.10:8787/")
        val temp = provider.getApiForRawUrl("http://192.168.1.99:8787/").getOrThrow()
        assertNotSame(official, temp)
        // 正式缓存未被临时地址覆盖
        assertSame(official, provider.getApi("http://192.168.1.10:8787/"))
    }

    @Test
    fun temporaryUrlSameAsOfficialReadsCacheWithoutWriting() {
        val provider = provider()
        val official = provider.getApi("http://192.168.1.10:8787/")
        val same = provider.getApiForRawUrl("http://192.168.1.10:8787/").getOrThrow()
        assertSame(official, same)
    }
}
