package com.hwatch.app.ui.downloads

import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.data.model.PagedResponse
import com.hwatch.app.data.model.ToggleDownloadsResponse
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.model.VideoStatus
import com.hwatch.app.data.repository.DownloadRepository
import com.hwatch.app.di.NetworkModule
import com.hwatch.app.testutil.FakeDownloadEventsTransport
import com.hwatch.app.testutil.FakeHWatchApi
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val json = NetworkModule.createJson()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        api: FakeHWatchApi,
        transport: FakeDownloadEventsTransport,
    ): DownloadsViewModel {
        val repository = DownloadRepository(
            apiProvider = { Result.success(api) },
            languageProvider = { "zh-CN" },
        )
        return DownloadsViewModel(
            repository = repository,
            transport = transport,
            json = json,
            baseUrlProvider = { "http://192.168.1.100:8787/" },
            calibrationDelayMillis = { 1000L },
        )
    }

    private fun task(id: String, status: VideoStatus) = VideoDto(videoId = id, title = "t $id", status = status)

    @Test
    fun activateLoadsTasksAndConnectsOnce() = runTest(dispatcher) {
        val api = FakeHWatchApi(onTasks = { PagedResponse(items = listOf(task("1", VideoStatus.ACTIVE)), total = 1) })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.tasks.size)
        assertEquals(1, transport.connectionCount)
    }

    @Test
    fun repeatedActivateDoesNotCreateSecondSse() = runTest(dispatcher) {
        val api = FakeHWatchApi(onTasks = { PagedResponse() })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        vm.activate()
        vm.activate()
        advanceUntilIdle()
        assertEquals(1, transport.connectionCount)
    }

    @Test
    fun deactivateCancelsSse() = runTest(dispatcher) {
        val api = FakeHWatchApi(onTasks = { PagedResponse() })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        vm.deactivate()
        assertEquals(0, transport.activeCount)
    }

    @Test
    fun pauseAcceptedDoesNotOptimisticallyChangeStatus() = runTest(dispatcher) {
        val api = FakeHWatchApi(onTasks = { PagedResponse(items = listOf(task("1", VideoStatus.ACTIVE)), total = 1) })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()

        vm.onAction("1", DownloadAction.PAUSE)
        advanceUntilIdle()
        assertEquals(VideoStatus.ACTIVE, vm.uiState.value.tasks.single().status)
        assertEquals(DownloadsMessage.Kind.ACTION_SUBMITTED, vm.uiState.value.message?.kind)

        transport.latest().listener.onMessage(
            """{"tasks":[{"video_id":"1","status":"paused"}],"download_speed":0,"connections":0}""",
        )
        advanceUntilIdle()
        assertEquals(VideoStatus.PAUSED, vm.uiState.value.tasks.single().status)
    }

    @Test
    fun deleteAcceptedDoesNotRemoveImmediately() = runTest(dispatcher) {
        var tasks = listOf(task("1", VideoStatus.ACTIVE))
        val api = FakeHWatchApi(onTasks = { PagedResponse(items = tasks, total = tasks.size.toLong()) })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()

        vm.onAction("1", DownloadAction.DELETE)
        assertEquals(1, vm.uiState.value.tasks.size)

        tasks = emptyList()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.tasks.isEmpty())
    }

    @Test
    fun malformedSseEventIsIgnored() = runTest(dispatcher) {
        val api = FakeHWatchApi(onTasks = { PagedResponse(items = listOf(task("1", VideoStatus.ACTIVE)), total = 1) })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        transport.latest().listener.onMessage("{ not json")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.tasks.size)
    }

    @Test
    fun refreshFailureKeepsTasksAndEmitsMessage() = runTest(dispatcher) {
        var first = true
        val api = FakeHWatchApi(onTasks = {
            if (first) {
                first = false
                PagedResponse(items = listOf(task("1", VideoStatus.ACTIVE)), total = 1)
            } else {
                throw IOException("no route")
            }
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.tasks.size)
        assertEquals(DownloadsMessage.Kind.REFRESH_FAILED, vm.uiState.value.message?.kind)
    }

    @Test
    fun downloadAllAcceptedEmitsCount() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onTasks = { PagedResponse() },
            onDownloadAll = { com.hwatch.app.data.model.DownloadAllResponse(accepted = 5) },
        )
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.downloadAll()
        advanceUntilIdle()
        val message = vm.uiState.value.message
        assertEquals(DownloadsMessage.Kind.DOWNLOAD_ALL_ACCEPTED, message?.kind)
        assertEquals(5, message?.count)
    }

    @Test
    fun mergeTasksReplacesActiveSubsetAndKeepsOthers() {
        val baseline = listOf(
            task("active", VideoStatus.ACTIVE),
            task("queued", VideoStatus.QUEUED),
            task("failed", VideoStatus.FAILED),
            task("completed", VideoStatus.COMPLETED),
        )
        val incoming = listOf(task("active", VideoStatus.PAUSED), task("new", VideoStatus.ACTIVE))
        val merged = DownloadsViewModel.mergeTasks(baseline, incoming)
        assertEquals(listOf("active", "queued", "failed", "completed", "new"), merged.map { it.videoId })
        assertEquals(VideoStatus.PAUSED, merged.first { it.videoId == "active" }.status)
    }

    @Test
    fun mergeTasksDropsActiveTaskMissingFromSseSnapshot() {
        val baseline = listOf(task("gone", VideoStatus.ACTIVE), task("queued", VideoStatus.QUEUED))
        val merged = DownloadsViewModel.mergeTasks(
            baseline,
            listOf(task("queued", VideoStatus.QUEUED)),
        )
        assertEquals(listOf("queued"), merged.map { it.videoId })
    }

    @Test
    fun departureTriggersExactlyOneCalibration() = runTest(dispatcher) {
        var restCalls = 0
        val api = FakeHWatchApi(onTasks = {
            restCalls++
            if (restCalls == 1) {
                PagedResponse(items = listOf(task("A", VideoStatus.ACTIVE)), total = 1)
            } else {
                PagedResponse(items = listOf(task("A", VideoStatus.COMPLETED)), total = 1)
            }
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        assertEquals(1, restCalls)

        transport.latest().listener.onMessage("""{"tasks":[{"video_id":"A","status":"active"}],"download_speed":0,"connections":0}""")
        runCurrent()
        assertEquals(1, restCalls)

        transport.latest().listener.onMessage("""{"tasks":[],"download_speed":0,"connections":0}""")
        runCurrent()
        assertEquals(1, restCalls)

        advanceTimeBy(1000)
        runCurrent()
        assertEquals(2, restCalls)
        assertEquals(VideoStatus.COMPLETED, vm.uiState.value.tasks.single().status)
    }

    @Test
    fun twoDeparturesCoalesceIntoOneCalibration() = runTest(dispatcher) {
        var restCalls = 0
        val api = FakeHWatchApi(onTasks = {
            restCalls++
            PagedResponse(
                items = listOf(task("A", VideoStatus.ACTIVE), task("B", VideoStatus.ACTIVE)),
                total = 2,
            )
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        transport.latest().listener.onMessage(
            """{"tasks":[{"video_id":"A","status":"active"},{"video_id":"B","status":"active"}],"download_speed":0,"connections":0}""",
        )
        runCurrent()
        transport.latest().listener.onMessage("""{"tasks":[],"download_speed":0,"connections":0}""")
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(2, restCalls)
    }

    @Test
    fun repeatedSnapshotsWithoutDepartureDoNotCalibrate() = runTest(dispatcher) {
        var restCalls = 0
        val api = FakeHWatchApi(onTasks = {
            restCalls++
            PagedResponse(items = listOf(task("A", VideoStatus.ACTIVE)), total = 1)
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        repeat(5) {
            transport.latest().listener.onMessage(
                """{"tasks":[{"video_id":"A","status":"active"}],"download_speed":0,"connections":0}""",
            )
            runCurrent()
        }
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1, restCalls)
    }

    @Test
    fun calibrationFailureKeepsTasksAndDoesNotLoop() = runTest(dispatcher) {
        var restCalls = 0
        val api = FakeHWatchApi(onTasks = {
            restCalls++
            if (restCalls == 1) {
                PagedResponse(items = listOf(task("A", VideoStatus.ACTIVE)), total = 1)
            } else {
                throw IOException("no route")
            }
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        transport.latest().listener.onMessage("""{"tasks":[{"video_id":"A","status":"active"}],"download_speed":0,"connections":0}""")
        runCurrent()
        transport.latest().listener.onMessage("""{"tasks":[],"download_speed":0,"connections":0}""")
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(2, restCalls)
        assertEquals(1, vm.uiState.value.tasks.size)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(2, restCalls)
    }

    @Test
    fun deleteConvergesViaRestCalibration() = runTest(dispatcher) {
        var restCalls = 0
        val api = FakeHWatchApi(onTasks = {
            restCalls++
            if (restCalls == 1) {
                PagedResponse(items = listOf(task("A", VideoStatus.ACTIVE)), total = 1)
            } else {
                PagedResponse(items = emptyList(), total = 0)
            }
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        transport.latest().listener.onMessage("""{"tasks":[{"video_id":"A","status":"active"}],"download_speed":0,"connections":0}""")
        runCurrent()
        transport.latest().listener.onMessage("""{"tasks":[],"download_speed":0,"connections":0}""")
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()
        assertTrue(vm.uiState.value.tasks.isEmpty())
    }

    @Test
    fun calibrationIsSingleFlightAcrossRapidDepartures() = runTest(dispatcher) {
        var restCalls = 0
        val api = FakeHWatchApi(onTasks = {
            restCalls++
            PagedResponse(items = emptyList(), total = 0)
        })
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        transport.latest().listener.onMessage(
            """{"tasks":[{"video_id":"A","status":"active"},{"video_id":"B","status":"active"}],"download_speed":0,"connections":0}""",
        )
        runCurrent()
        transport.latest().listener.onMessage("""{"tasks":[{"video_id":"A","status":"active"}],"download_speed":0,"connections":0}""")
        runCurrent()
        transport.latest().listener.onMessage("""{"tasks":[],"download_speed":0,"connections":0}""")
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(2, restCalls)
    }

    @Test
    fun toggleGlobalPauseUpdatesFromResponse() = runTest(dispatcher) {
        var toggled: Boolean? = null
        val api = FakeHWatchApi(
            onTasks = { PagedResponse() },
            onDashboard = { DashboardDto(downloadsPaused = false) },
            onToggleDownloads = { request ->
                toggled = request.paused
                ToggleDownloadsResponse(paused = request.paused)
            },
        )
        val transport = FakeDownloadEventsTransport()
        val vm = viewModel(api, transport)
        vm.activate()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isGloballyPaused)

        vm.toggleGlobalPause()
        advanceUntilIdle()
        assertEquals(true, toggled)
        assertTrue(vm.uiState.value.isGloballyPaused)
        assertFalse(vm.uiState.value.isTogglingPause)
    }
}
