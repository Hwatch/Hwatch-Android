package com.hwatch.app.ui.subscriptions

import com.hwatch.app.data.model.ItemsResponse
import com.hwatch.app.data.model.SubscriptionDto
import com.hwatch.app.data.model.UpdateSubscriptionRequest
import com.hwatch.app.data.model.UpdateSubscriptionResponse
import com.hwatch.app.data.repository.SubscriptionRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeHWatchApi): SubscriptionsViewModel =
        SubscriptionsViewModel(SubscriptionRepository(apiProvider = { Result.success(api) }))

    private fun sub(slug: String, enabled: Boolean = true) =
        SubscriptionDto(slug = slug, name = slug, platform = "source", enabled = enabled)

    @Test
    fun loadPopulatesItems() = runTest(dispatcher) {
        val api = FakeHWatchApi(onSubscriptions = { ItemsResponse(listOf(sub("a"))) })
        val vm = viewModel(api)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun setEnabledUsesServerResponse() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onSubscriptions = { ItemsResponse(listOf(sub("a", enabled = true))) },
            onPatchSubscription = { _, body -> UpdateSubscriptionResponse(enabled = body.enabled) },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.setEnabled(vm.uiState.value.items.single(), false)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.items.single().enabled)
    }

    @Test
    fun setEnabledFailureKeepsStateAndEmitsMessage() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onSubscriptions = { ItemsResponse(listOf(sub("a", enabled = true))) },
            onPatchSubscription = { _, _ -> throw IOException("no route") },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.setEnabled(vm.uiState.value.items.single(), false)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.items.single().enabled)
        assertEquals(SubscriptionsMessage.Kind.ACTION_FAILED, vm.uiState.value.message?.kind)
    }

    @Test
    fun deleteRemovesItem() = runTest(dispatcher) {
        val api = FakeHWatchApi(onSubscriptions = { ItemsResponse(listOf(sub("a"))) })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.delete(vm.uiState.value.items.single())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun checkOneEmitsSubmittedWithoutFakingState() = runTest(dispatcher) {
        val api = FakeHWatchApi(onSubscriptions = { ItemsResponse(listOf(sub("a"))) })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.checkOne(vm.uiState.value.items.single())
        advanceUntilIdle()
        assertEquals(SubscriptionsMessage.Kind.CHECK_SUBMITTED, vm.uiState.value.message?.kind)
        assertEquals("", vm.uiState.value.items.single().lastCheckedAt)
    }

    @Test
    fun refreshFailureKeepsItems() = runTest(dispatcher) {
        var first = true
        val api = FakeHWatchApi(onSubscriptions = {
            if (first) {
                first = false
                ItemsResponse(listOf(sub("a")))
            } else {
                throw IOException("no route")
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(SubscriptionsMessage.Kind.REFRESH_FAILED, vm.uiState.value.message?.kind)
    }
}
