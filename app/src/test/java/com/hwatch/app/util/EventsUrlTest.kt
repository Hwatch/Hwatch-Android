package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventsUrlTest {

    @Test
    fun buildsApiEventsPath() {
        assertEquals("http://192.168.1.100:8787/api/events", EventsUrl.build("http://192.168.1.100:8787/"))
    }

    @Test
    fun worksWithoutTrailingSlash() {
        assertEquals("http://192.168.1.100:8787/api/events", EventsUrl.build("http://192.168.1.100:8787"))
    }

    @Test
    fun invalidBaseUrlReturnsNull() {
        assertNull(EventsUrl.build("not a url"))
    }
}
