package com.hwatch.app.ui.settings

import com.hwatch.app.data.model.SettingsDto
import com.hwatch.app.data.model.UpdateSettingsRequest
import com.hwatch.app.data.repository.SettingsRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeHWatchApi): SettingsViewModel = SettingsViewModel(
        repository = SettingsRepository(apiProvider = { Result.success(api) }),
        configRepository = FakeServerConfigRepository(initialBaseUrl = "http://192.168.0.249:8787/"),
    )

    private val settings = SettingsDto(
        timezone = "Asia/Shanghai",
        maxConcurrent = 2,
        minFreeBytes = 5L * SettingsMapper.BYTES_PER_GB,
    )

    @Test
    fun loadPopulatesDraft() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi(onGetSettings = { settings }))
        advanceUntilIdle()
        assertEquals("Asia/Shanghai", vm.uiState.value.draft.timezone)
        assertEquals("2", vm.uiState.value.draft.maxConcurrent)
        assertFalse(vm.uiState.value.hasChanges)
    }

    @Test
    fun saveSuccessUpdatesOriginalAndClearsChanges() = runTest(dispatcher) {
        var putBody: UpdateSettingsRequest? = null
        val api = FakeHWatchApi(
            onGetSettings = { settings },
            onPutSettings = { body ->
                putBody = body
                settings.copy(maxConcurrent = body.maxConcurrent ?: settings.maxConcurrent)
            },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.updateDraft { it.copy(maxConcurrent = "4") }
        assertTrue(vm.uiState.value.hasChanges)
        vm.save()
        advanceUntilIdle()
        assertEquals(4, putBody?.maxConcurrent)
        assertFalse(vm.uiState.value.hasChanges)
        assertTrue(vm.uiState.value.saveSuccessId > 0)
    }

    @Test
    fun saveFailureKeepsDraft() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onGetSettings = { settings },
            onPutSettings = { throw IOException("no route") },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.updateDraft { it.copy(timezone = "UTC") }
        vm.save()
        advanceUntilIdle()
        assertEquals("UTC", vm.uiState.value.draft.timezone)
        assertTrue(vm.uiState.value.hasChanges)
        assertTrue(vm.uiState.value.saveErrorId > 0)
    }

    @Test
    fun invalidFieldBlocksSave() = runTest(dispatcher) {
        var putCalls = 0
        val api = FakeHWatchApi(
            onGetSettings = { settings },
            onPutSettings = {
                putCalls++
                settings
            },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.updateDraft { it.copy(maxConcurrent = "abc") }
        vm.save()
        advanceUntilIdle()
        assertTrue(SettingsField.MAX_CONCURRENT in vm.uiState.value.invalidFields)
        assertEquals(0, putCalls)
    }

    @Test
    fun noChangesDoesNotCallPut() = runTest(dispatcher) {
        var putCalls = 0
        val api = FakeHWatchApi(
            onGetSettings = { settings },
            onPutSettings = {
                putCalls++
                settings
            },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertEquals(0, putCalls)
    }

    @Test
    fun loadFailureExposesError() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi(onGetSettings = { throw IOException("no route") }))
        advanceUntilIdle()
        assertEquals(SettingsError.UNREACHABLE, vm.uiState.value.loadError)
    }
}
