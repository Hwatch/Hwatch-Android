package com.hwatch.app.ui.subscriptions

import com.hwatch.app.data.model.CreateSubscriptionRequest
import com.hwatch.app.data.model.CreateSubscriptionResponse
import com.hwatch.app.data.model.CreatorDto
import com.hwatch.app.data.model.ItemsResponse
import com.hwatch.app.data.model.PlatformDto
import com.hwatch.app.data.repository.SubscriptionRepository
import com.hwatch.app.testutil.FakeHWatchApi
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddSubscriptionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeHWatchApi): AddSubscriptionViewModel =
        AddSubscriptionViewModel(SubscriptionRepository(apiProvider = { Result.success(api) }))

    @Test
    fun platformsLoadedOnInit() = runTest(dispatcher) {
        val api = FakeHWatchApi(onPlatforms = { ItemsResponse(listOf(PlatformDto("source", "Source"))) })
        val vm = viewModel(api)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.platforms.size)
        assertEquals("source", vm.uiState.value.selectedPlatform)
    }

    @Test
    fun shortQueryDoesNotSearch() = runTest(dispatcher) {
        var calls = 0
        val api = FakeHWatchApi(onSearchCreators = { _, _ ->
            calls++
            ItemsResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.onQueryChanged("a")
        advanceTimeBy(1000)
        advanceUntilIdle()
        assertEquals(0, calls)
    }

    @Test
    fun rapidTypingDebouncesToSingleSearch() = runTest(dispatcher) {
        var calls = 0
        val api = FakeHWatchApi(onSearchCreators = { _, _ ->
            calls++
            ItemsResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.onQueryChanged("ak")
        advanceTimeBy(100)
        vm.onQueryChanged("akt")
        advanceTimeBy(100)
        vm.onQueryChanged("akta")
        advanceTimeBy(500)
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun staleSearchResponseDoesNotOverwriteNewer() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val api = FakeHWatchApi(onSearchCreators = { q, _ ->
            if (q == "ab") {
                gate.await()
                ItemsResponse(listOf(CreatorDto(name = "OLD")))
            } else {
                ItemsResponse(listOf(CreatorDto(name = "NEW")))
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.onQueryChanged("ab")
        advanceTimeBy(400)
        advanceUntilIdle()
        vm.onQueryChanged("abc")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals("NEW", vm.uiState.value.results.single().name)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("NEW", vm.uiState.value.results.single().name)
    }

    @Test
    fun createSuccessSetsCreated() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onCreateSubscription = { CreateSubscriptionResponse(slug = "source:creator") },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.onUrlChanged("https://example.com/a")
        vm.create()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.created)
    }

    @Test
    fun createSendsUrlAndDownloadExisting() = runTest(dispatcher) {
        var sent: CreateSubscriptionRequest? = null
        val api = FakeHWatchApi(
            onCreateSubscription = { body ->
                sent = body
                CreateSubscriptionResponse()
            },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.onUrlChanged("https://example.com/a")
        vm.onDownloadExistingChanged(true)
        vm.create()
        advanceUntilIdle()
        assertEquals("https://example.com/a", sent?.url)
        assertEquals("creator", sent?.type)
        assertEquals(true, sent?.downloadExisting)
    }

    @Test
    fun createFailureKeepsInputAndSetsError() = runTest(dispatcher) {
        val api = FakeHWatchApi(onCreateSubscription = { throw IOException("no route") })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.onUrlChanged("https://example.com/a")
        vm.create()
        advanceUntilIdle()
        assertEquals("https://example.com/a", vm.uiState.value.url)
        assertEquals(SubscriptionsError.UNREACHABLE, vm.uiState.value.createError)
    }
}
