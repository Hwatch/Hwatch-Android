package com.hwatch.app.ui.dashboard

import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.data.repository.DashboardRepository
import com.hwatch.app.testutil.FakeHWatchApi
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeHWatchApi): DashboardViewModel {
        val repository = DashboardRepository(
            apiProvider = { Result.success(api) },
            languageProvider = { "zh-CN" },
        )
        return DashboardViewModel(repository)
    }

    @Test
    fun initialLoadSuccessProducesContent() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi { DashboardDto(timezone = "Asia/Shanghai") })
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Content)
        assertEquals("Asia/Shanghai", (state as DashboardUiState.Content).dashboard.timezone)
    }

    @Test
    fun initialLoadFailureProducesError() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi { throw IOException("no route") })
        advanceUntilIdle()
        assertEquals(DashboardUiState.Error(DashboardError.UNREACHABLE), vm.uiState.value)
    }

    @Test
    fun retryAfterFailureLoadsContent() = runTest(dispatcher) {
        val api = FakeHWatchApi { throw IOException("no route") }
        val vm = viewModel(api)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is DashboardUiState.Error)
        api.onDashboard = { DashboardDto(timezone = "UTC") }
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is DashboardUiState.Content)
    }

    @Test
    fun refreshSuccessReplacesContent() = runTest(dispatcher) {
        val api = FakeHWatchApi { DashboardDto(timezone = "UTC", currentConnections = 1) }
        val vm = viewModel(api)
        advanceUntilIdle()
        api.onDashboard = { DashboardDto(timezone = "UTC", currentConnections = 2) }
        vm.refresh()
        advanceUntilIdle()
        val state = vm.uiState.value as DashboardUiState.Content
        assertEquals(2, state.dashboard.currentConnections)
        assertFalse(state.isRefreshing)
        assertNull(state.refreshError)
    }

    @Test
    fun refreshFailureKeepsOldContent() = runTest(dispatcher) {
        val api = FakeHWatchApi { DashboardDto(timezone = "UTC", currentConnections = 7) }
        val vm = viewModel(api)
        advanceUntilIdle()
        api.onDashboard = { throw IOException("no route") }
        vm.refresh()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Content)
        state as DashboardUiState.Content
        assertEquals(7, state.dashboard.currentConnections)
        assertEquals(DashboardError.UNREACHABLE, state.refreshError)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun onlyOneRequestOnInitialLoad() = runTest(dispatcher) {
        var calls = 0
        val api = FakeHWatchApi {
            calls++
            DashboardDto()
        }
        val vm = viewModel(api)
        advanceUntilIdle()
        repeat(100) { vm.uiState.value }
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun refreshFailureIncrementsEventId() = runTest(dispatcher) {
        val api = FakeHWatchApi { DashboardDto(timezone = "UTC") }
        val vm = viewModel(api)
        advanceUntilIdle()
        api.onDashboard = { throw IOException("no route") }
        vm.refresh()
        advanceUntilIdle()
        val first = (vm.uiState.value as DashboardUiState.Content).refreshErrorId
        vm.refresh()
        advanceUntilIdle()
        val second = (vm.uiState.value as DashboardUiState.Content).refreshErrorId
        assertTrue(second > first)
    }
}
