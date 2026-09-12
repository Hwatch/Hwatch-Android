package com.hwatch.app.data.events

import com.hwatch.app.data.model.DownloadEvent
import com.hwatch.app.di.NetworkModule
import com.hwatch.app.testutil.FakeDownloadEventsTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadEventsManagerTest {

    private val json = NetworkModule.createJson()
    private val url = "http://192.168.0.249:8787/api/events"

    @Test
    fun activateCreatesExactlyOneConnection() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        assertEquals(1, transport.connectionCount)
        assertEquals(ConnectionState.CONNECTING, manager.connectionState.value)
    }

    @Test
    fun duplicateActivateSameUrlDoesNotCreateSecondConnection() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        manager.activate(url) {}
        manager.activate(url) {}
        assertEquals(1, transport.connectionCount)
    }

    @Test
    fun baseUrlChangeCancelsOldConnectionAndConnectsNew() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        val first = transport.latest()
        manager.activate("http://other:8787/api/events") {}
        assertTrue(first.cancelled)
        assertEquals(2, transport.connectionCount)
    }

    @Test
    fun deactivateCancelsConnection() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        val connection = transport.latest()
        manager.deactivate()
        assertTrue(connection.cancelled)
        assertEquals(ConnectionState.DISCONNECTED, manager.connectionState.value)
    }

    @Test
    fun repeatedFailureSchedulesSingleReconnectAndFullSyncs() = runTest {
        val transport = FakeDownloadEventsTransport()
        var syncCount = 0
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) { syncCount++ }
        val connection = transport.latest()
        connection.listener.onFailure()
        connection.listener.onFailure()
        connection.listener.onFailure()
        assertEquals(1, transport.connectionCount)
        assertEquals(0, syncCount)

        advanceTimeBy(2000)
        runCurrent()
        assertEquals(1, syncCount)
        assertEquals(2, transport.connectionCount)
    }

    @Test
    fun deactivateDuringBackoffPreventsReconnect() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        transport.latest().listener.onFailure()
        manager.deactivate()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, transport.connectionCount)
    }

    @Test
    fun malformedEventIsDroppedWithoutCrash() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        val events = mutableListOf<DownloadEvent>()
        backgroundScope.launch { manager.events.collect { events.add(it) } }
        manager.activate(url) {}
        runCurrent()
        transport.latest().listener.onMessage("{ not json")
        runCurrent()
        assertTrue(events.isEmpty())
    }

    @Test
    fun validEventIsParsedAndEmitted() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        val events = mutableListOf<DownloadEvent>()
        backgroundScope.launch { manager.events.collect { events.add(it) } }
        manager.activate(url) {}
        runCurrent()
        transport.latest().listener.onMessage("""{"tasks":[],"download_speed":123,"connections":8}""")
        runCurrent()
        assertEquals(1, events.size)
        assertEquals(123L, events.first().downloadSpeed)
        assertEquals(8, events.first().connections)
    }

    @Test
    fun onOpenTransitionsToConnected() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        transport.latest().listener.onOpen()
        assertEquals(ConnectionState.CONNECTED, manager.connectionState.value)
    }

    @Test
    fun closedWhileInactiveDoesNotReconnect() = runTest {
        val transport = FakeDownloadEventsTransport()
        val manager = DownloadEventsManager(transport, json, this) { 2000L }
        manager.activate(url) {}
        manager.deactivate()
        transport.latest().listener.onClosed()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, transport.connectionCount)
        assertFalse(manager.connectionState.value == ConnectionState.RECONNECTING)
    }
}
