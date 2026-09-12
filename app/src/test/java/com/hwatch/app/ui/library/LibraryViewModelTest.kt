package com.hwatch.app.ui.library

import com.hwatch.app.data.model.FacetsDto
import com.hwatch.app.data.model.PagedResponse
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.repository.LibraryRepository
import com.hwatch.app.testutil.FakeHWatchApi
import com.hwatch.app.testutil.VideoRequest
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeHWatchApi): LibraryViewModel {
        val repository = LibraryRepository(
            apiProvider = { Result.success(api) },
            languageProvider = { "zh-CN" },
        )
        return LibraryViewModel(repository)
    }

    private fun video(id: String): VideoDto = VideoDto(videoId = id, title = "title $id")

    private fun page(ids: List<String>, total: Long, offset: Int): PagedResponse<VideoDto> =
        PagedResponse(items = ids.map(::video), total = total, limit = 48, offset = offset)

    @Test
    fun initialLoadProducesItems() = runTest(dispatcher) {
        val api = FakeHWatchApi(onVideos = { page(listOf("1"), total = 1, offset = 0) })
        val vm = viewModel(api)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(1, state.items.size)
        assertFalse(state.isInitialLoading)
        assertFalse(state.hasMore)
        assertNull(state.error)
    }

    @Test
    fun loadMoreAppendsSecondPageAndStopsAtTotal() = runTest(dispatcher) {
        val api = FakeHWatchApi(onVideos = { request ->
            if (request.offset == 0) {
                page(List(48) { "p1_$it" }, total = 60, offset = 0)
            } else {
                page(List(12) { "p2_$it" }, total = 60, offset = 48)
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasMore)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(60, vm.uiState.value.items.size)
        assertFalse(vm.uiState.value.hasMore)
        assertNull(vm.uiState.value.loadMoreError)
    }

    @Test
    fun duplicateLoadMoreWhileLoadingIsIgnored() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var secondPageCalls = 0
        val api = FakeHWatchApi(onVideos = { request ->
            if (request.offset == 0) {
                page(List(48) { "p1_$it" }, total = 60, offset = 0)
            } else {
                secondPageCalls++
                gate.await()
                page(List(12) { "p2_$it" }, total = 60, offset = 48)
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()
        vm.loadMore()
        vm.loadMore()
        assertEquals(1, secondPageCalls)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(60, vm.uiState.value.items.size)
    }

    @Test
    fun loadMoreErrorKeepsItemsAndRetrySucceeds() = runTest(dispatcher) {
        var failNext = true
        val api = FakeHWatchApi(onVideos = { request ->
            if (request.offset == 0) {
                page(List(48) { "p1_$it" }, total = 60, offset = 0)
            } else if (failNext) {
                failNext = false
                throw IOException("no route")
            } else {
                page(List(12) { "p2_$it" }, total = 60, offset = 48)
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(48, vm.uiState.value.items.size)
        assertEquals(LibraryError.UNREACHABLE, vm.uiState.value.loadMoreError)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(60, vm.uiState.value.items.size)
        assertNull(vm.uiState.value.loadMoreError)
    }

    @Test
    fun rapidTypingDebouncesToSingleRequest() = runTest(dispatcher) {
        var calls = 0
        val api = FakeHWatchApi(onVideos = {
            calls++
            PagedResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        calls = 0

        vm.onSearchTextChanged("a")
        advanceTimeBy(100)
        vm.onSearchTextChanged("ab")
        advanceTimeBy(100)
        vm.onSearchTextChanged("abc")
        advanceTimeBy(100)
        assertEquals(0, calls)

        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun clearSearchReloadsImmediately() = runTest(dispatcher) {
        var calls = 0
        val api = FakeHWatchApi(onVideos = {
            calls++
            PagedResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.onSearchTextChanged("abc")
        advanceTimeBy(400)
        advanceUntilIdle()
        calls = 0

        vm.onSearchTextChanged("")
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun staleSearchResponseDoesNotOverwriteNewerResult() = runTest(dispatcher) {
        val abGate = CompletableDeferred<Unit>()
        val api = FakeHWatchApi(onVideos = { request ->
            when (request.q) {
                "ab" -> {
                    abGate.await()
                    page(listOf("AB"), total = 1, offset = 0)
                }
                "abc" -> page(listOf("ABC"), total = 1, offset = 0)
                else -> PagedResponse()
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.onSearchTextChanged("ab")
        advanceTimeBy(400)
        advanceUntilIdle()
        vm.onSearchTextChanged("abc")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals("ABC", vm.uiState.value.items.single().videoId)

        abGate.complete(Unit)
        advanceUntilIdle()
        assertEquals("ABC", vm.uiState.value.items.single().videoId)
    }

    @Test
    fun applyFiltersTriggersSingleRequest() = runTest(dispatcher) {
        var calls = 0
        val api = FakeHWatchApi(onVideos = {
            calls++
            PagedResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        calls = 0

        vm.applyFilters(LibraryFilters(tags = listOf("x")))
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun resetFiltersRestoresDefaults() = runTest(dispatcher) {
        var lastRequest: VideoRequest? = null
        val api = FakeHWatchApi(onVideos = { request ->
            lastRequest = request
            PagedResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.applyFilters(LibraryFilters(creator = "A", tags = listOf("t"), minHeight = 1080))
        advanceUntilIdle()
        assertEquals("A", lastRequest?.creator)
        assertEquals("t", lastRequest?.tags)
        assertEquals(1080, lastRequest?.minHeight)

        vm.resetFilters()
        advanceUntilIdle()
        assertNull(lastRequest?.creator)
        assertNull(lastRequest?.tags)
        assertNull(lastRequest?.minHeight)
    }

    @Test
    fun filterFailureClearsOldResultsAndShowsError() = runTest(dispatcher) {
        var first = true
        val api = FakeHWatchApi(onVideos = {
            if (first) {
                first = false
                page(listOf("1"), total = 1, offset = 0)
            } else {
                throw IOException("no route")
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size)

        vm.applyFilters(LibraryFilters(creator = "A"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals(LibraryError.UNREACHABLE, vm.uiState.value.error)
    }

    @Test
    fun refreshFailureKeepsOldItems() = runTest(dispatcher) {
        var first = true
        val api = FakeHWatchApi(onVideos = {
            if (first) {
                first = false
                page(listOf("1"), total = 1, offset = 0)
            } else {
                throw IOException("no route")
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(LibraryError.UNREACHABLE, vm.uiState.value.refreshError)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun facetsFailureDoesNotFailList() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onVideos = { page(listOf("1"), total = 1, offset = 0) },
            onFacets = { throw IOException("no route") },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.facetsError)
        assertEquals(1, vm.uiState.value.items.size)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun totalZeroMeansNoMore() = runTest(dispatcher) {
        val api = FakeHWatchApi(onVideos = { PagedResponse(items = emptyList(), total = 0) })
        val vm = viewModel(api)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasMore)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun scopeChangeIsSentToServer() = runTest(dispatcher) {
        var lastScope: String? = null
        val api = FakeHWatchApi(onVideos = { request ->
            lastScope = request.scope
            PagedResponse()
        })
        val vm = viewModel(api)
        advanceUntilIdle()

        vm.onScopeChanged(SearchScope.TRANSLATED)
        advanceUntilIdle()
        assertEquals("translated", lastScope)
    }

    @Test
    fun facetsLoadedOnInit() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onVideos = { PagedResponse() },
            onFacets = { FacetsDto(creators = emptyList(), tags = emptyList(), codecs = emptyList()) },
        )
        val vm = viewModel(api)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.facetsError)
    }

    @Test
    fun refreshFailureIncrementsEventIdAndKeepsItems() = runTest(dispatcher) {
        var first = true
        val api = FakeHWatchApi(onVideos = {
            if (first) {
                first = false
                page(listOf("1"), total = 1, offset = 0)
            } else {
                throw IOException("no route")
            }
        })
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(1, state.items.size)
        assertTrue(state.refreshErrorId > 0)
        assertEquals(LibraryError.UNREACHABLE, state.refreshError)
    }
}
