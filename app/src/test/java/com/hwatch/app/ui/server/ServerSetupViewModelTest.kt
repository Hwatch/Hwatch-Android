package com.hwatch.app.ui.server

import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.data.repository.ConnectionFailure
import com.hwatch.app.data.repository.ServerConnectionRepository
import com.hwatch.app.testutil.FakeHWatchApi
import com.hwatch.app.testutil.FakeServerConfigRepository
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
class ServerSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()

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
        config: FakeServerConfigRepository,
    ): ServerSetupViewModel = ServerSetupViewModel(
        connectionRepository = ServerConnectionRepository(buildApi = { Result.success(api) }),
        configRepository = config,
    )

    @Test
    fun successfulConnectionSavesNormalizedUrl() = runTest(dispatcher) {
        val config = FakeServerConfigRepository()
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true) }), config)
        vm.onServerUrlChanged("192.168.1.10:8787")
        vm.testAndConnect()
        advanceUntilIdle()
        assertEquals("http://192.168.1.10:8787/", config.savedBaseUrl)
        assertFalse(vm.uiState.value.isTesting)
        assertNull(vm.uiState.value.connectionError)
        assertNull(vm.uiState.value.validationError)
    }

    @Test
    fun failedConnectionDoesNotSaveUrl() = runTest(dispatcher) {
        val config = FakeServerConfigRepository()
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { throw IOException("no route") }), config)
        vm.onServerUrlChanged("http://192.168.1.10:8787")
        vm.testAndConnect()
        advanceUntilIdle()
        assertNull(config.savedBaseUrl)
        assertEquals(ConnectionFailure.UNREACHABLE, vm.uiState.value.connectionError)
        assertNull(vm.uiState.value.validationError)
        assertFalse(vm.uiState.value.isTesting)
    }

    @Test
    fun invalidUrlSetsValidationErrorAndDoesNotSave() = runTest(dispatcher) {
        val config = FakeServerConfigRepository()
        val vm = viewModel(FakeHWatchApi(), config)
        vm.onServerUrlChanged("ftp://nas.local:8787")
        vm.testAndConnect()
        advanceUntilIdle()
        assertNull(config.savedBaseUrl)
        assertEquals(ConnectionFailure.INVALID_URL, vm.uiState.value.validationError)
        assertNull(vm.uiState.value.connectionError)
    }

    @Test
    fun editModeFailureKeepsOldServerAndDoesNotSave() = runTest(dispatcher) {
        val config = FakeServerConfigRepository(initialBaseUrl = "http://old:8787/")
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { throw IOException("no route") }), config)
        vm.prepare("http://old:8787/")
        vm.onServerUrlChanged("http://new:8787/")
        vm.testAndConnect()
        advanceUntilIdle()
        assertNull(config.savedBaseUrl)
        assertEquals("http://old:8787/", config.currentBaseUrl)
        assertEquals(ConnectionFailure.UNREACHABLE, vm.uiState.value.connectionError)
    }

    @Test
    fun editModeCancelKeepsOldServer() = runTest(dispatcher) {
        val config = FakeServerConfigRepository(initialBaseUrl = "http://old:8787/")
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true) }), config)
        vm.prepare("http://old:8787/")
        vm.onServerUrlChanged("http://new:8787/")
        // 用户取消，不发请求
        advanceUntilIdle()
        assertNull(config.savedBaseUrl)
        assertEquals("http://old:8787/", config.currentBaseUrl)
    }

    @Test
    fun editModeSuccessAtomicallySavesNewServer() = runTest(dispatcher) {
        val config = FakeServerConfigRepository(initialBaseUrl = "http://old:8787/")
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true) }), config)
        vm.prepare("http://old:8787/")
        vm.onServerUrlChanged("http://new:8787/")
        vm.testAndConnect()
        advanceUntilIdle()
        assertEquals("http://new:8787/", config.savedBaseUrl)
        assertTrue(vm.uiState.value.connectionSuccessId > 0)
    }

    @Test
    fun prepareResetsStateAndPrefillsUrl() = runTest(dispatcher) {
        val config = FakeServerConfigRepository()
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { throw IOException("no route") }), config)
        vm.onServerUrlChanged("http://x:1/")
        vm.testAndConnect()
        advanceUntilIdle()
        assertEquals(ConnectionFailure.UNREACHABLE, vm.uiState.value.connectionError)

        vm.prepare("http://old:8787/")
        assertEquals("http://old:8787/", vm.uiState.value.serverUrl)
        assertNull(vm.uiState.value.connectionError)
        assertFalse(vm.uiState.value.isTesting)
    }
}
