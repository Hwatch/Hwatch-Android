package com.hwatch.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerArgsCodecTest {

    private val specialValues = listOf(
        "source:3144962",
        "source:a/b",
        "中文",
        "50%",
        "a?b",
        "a#b",
        "a b",
        "a&b=c",
        "a+b",
    )

    @Test
    fun roundTripPreservesVideoIdAndTitle() {
        specialValues.forEach { value ->
            val args = PlayerArgs(videoId = value, displayTitle = value)
            val decoded = PlayerArgsCodec.decode(PlayerArgsCodec.encode(args))
            assertEquals(value, decoded?.videoId)
            assertEquals(value, decoded?.displayTitle)
        }
    }

    @Test
    fun encodedTokenContainsOnlyUrlSafeCharacters() {
        val token = PlayerArgsCodec.encode(PlayerArgs("source:a/b?c#d 中", "标题?#%"))
        assertTrue(token.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun destinationRouteRoundTrip() {
        val route = PlayerDestination.route("source:a/b", "标题")
        assertTrue(route.startsWith("player/"))
        val token = route.removePrefix("player/")
        assertEquals("source:a/b", PlayerArgsCodec.decode(token)?.videoId)
        assertEquals("标题", PlayerArgsCodec.decode(token)?.displayTitle)
    }

    @Test
    fun invalidTokenReturnsNull() {
        assertNull(PlayerArgsCodec.decode("!!!not-base64!!!"))
    }
}
