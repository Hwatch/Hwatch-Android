package com.hwatch.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.model.FacetsDto
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.model.VideoQuery
import com.hwatch.app.data.repository.LibraryRepository
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryError {
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
}

data class LibraryUiState(
    val items: List<VideoDto> = emptyList(),
    val total: Long = 0,
    val hasMore: Boolean = false,
    val facets: FacetsDto? = null,
    val facetsError: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: LibraryError? = null,
    val refreshError: LibraryError? = null,
    val refreshErrorId: Long = 0,
    val loadMoreError: LibraryError? = null,
    val searchText: String = "",
    val scope: SearchScope = SearchScope.ALL,
    val filters: LibraryFilters = LibraryFilters(),
    val appliedQuery: VideoQuery? = null,
)

/**
 * 影视库 ViewModel。
 *
 * - 搜索 300ms debounce；清空立即恢复；[combine] + [collectLatest] 保证新请求取消旧请求。
 * - 每次首页请求分配单调 generation，晚到的旧响应会被丢弃，绝不覆盖新结果。
 * - 分页显式管理 offset / hasMore，`isLoadingMore` 期间禁止重复 loadMore。
 * - Facets 与 Videos 为独立错误域。
 */
@OptIn(FlowPreview::class)
class LibraryViewModel(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isInitialLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val searchText = MutableStateFlow("")
    private val scope = MutableStateFlow(SearchScope.ALL)
    private val filters = MutableStateFlow(LibraryFilters())
    private val reloadTrigger = MutableStateFlow(0L)

    private val generation = AtomicLong(0)
    private var previousQuery: VideoQuery? = null

    private data class Request(val query: VideoQuery, val id: Long)

    private val debouncedSearch: Flow<String> = searchText
        .debounce { text -> if (text.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()

    private val requestFlow: Flow<Request> = combine(
        debouncedSearch,
        scope,
        filters,
        reloadTrigger,
    ) { search, scopeValue, filterValue, reloadId ->
        Request(query = buildQuery(search, scopeValue, filterValue), id = reloadId)
    }

    init {
        viewModelScope.launch { loadFacets() }
        viewModelScope.launch {
            requestFlow.collectLatest { request -> loadFirstPage(request) }
        }
    }

    fun onSearchTextChanged(text: String) {
        searchText.value = text
        _uiState.update { it.copy(searchText = text) }
    }

    fun onScopeChanged(newScope: SearchScope) {
        scope.value = newScope
        _uiState.update { it.copy(scope = newScope) }
    }

    fun applyFilters(newFilters: LibraryFilters) {
        filters.value = newFilters
        _uiState.update { it.copy(filters = newFilters) }
    }

    fun resetFilters() {
        applyFilters(LibraryFilters())
    }

    fun onSortChanged(sort: LibrarySort, order: SortOrder) {
        applyFilters(filters.value.copy(sort = sort, order = order))
    }

    fun refresh() {
        reloadTrigger.value = reloadTrigger.value + 1
    }

    fun retry() {
        reloadTrigger.value = reloadTrigger.value + 1
    }

    fun retryFacets() {
        viewModelScope.launch { loadFacets() }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isInitialLoading || state.isRefreshing) {
            return
        }
        val query = state.appliedQuery ?: return
        val requestGeneration = generation.get()
        val offset = state.items.size
        _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        viewModelScope.launch {
            when (val result = repository.loadVideos(query.copy(offset = offset))) {
                is ApiResult.Success -> {
                    if (requestGeneration != generation.get()) return@launch
                    _uiState.update { current ->
                        if (current.items.size != offset) {
                            current.copy(isLoadingMore = false)
                        } else {
                            val merged = current.items + result.data.items
                            current.copy(
                                items = merged,
                                total = result.data.total,
                                hasMore = computeHasMore(merged.size, result.data.total, result.data.items.size),
                                isLoadingMore = false,
                                loadMoreError = null,
                            )
                        }
                    }
                }
                is ApiResult.HttpError -> failLoadMore(requestGeneration, LibraryError.HTTP)
                is ApiResult.Unauthorized, is ApiResult.Forbidden -> failLoadMore(requestGeneration, LibraryError.HTTP)
                is ApiResult.ParseError -> failLoadMore(requestGeneration, LibraryError.PARSE)
                is ApiResult.NetworkError -> failLoadMore(
                    requestGeneration,
                    if (result.cause is SocketTimeoutException) LibraryError.TIMEOUT else LibraryError.UNREACHABLE,
                )
            }
        }
    }

    private fun failLoadMore(requestGeneration: Long, error: LibraryError) {
        if (requestGeneration != generation.get()) return
        _uiState.update { it.copy(isLoadingMore = false, loadMoreError = error) }
    }

    private suspend fun loadFirstPage(request: Request) {
        val requestGeneration = generation.incrementAndGet()
        val query = request.query.copy(offset = 0)
        val hadItems = _uiState.value.items.isNotEmpty()
        val isRefresh = hadItems && query == previousQuery
        previousQuery = query
        _uiState.update {
            it.copy(
                isInitialLoading = !hadItems,
                isRefreshing = hadItems,
                error = null,
                refreshError = null,
                loadMoreError = null,
                appliedQuery = query,
            )
        }
        when (val result = repository.loadVideos(query)) {
            is ApiResult.Success -> {
                if (requestGeneration != generation.get()) return
                _uiState.update { current ->
                    current.copy(
                        items = result.data.items,
                        total = result.data.total,
                        hasMore = computeHasMore(result.data.items.size, result.data.total, result.data.items.size),
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = null,
                        refreshError = null,
                    )
                }
            }
            is ApiResult.HttpError -> failFirstPage(requestGeneration, LibraryError.HTTP, isRefresh)
            is ApiResult.Unauthorized, is ApiResult.Forbidden -> failFirstPage(requestGeneration, LibraryError.HTTP, isRefresh)
            is ApiResult.ParseError -> failFirstPage(requestGeneration, LibraryError.PARSE, isRefresh)
            is ApiResult.NetworkError -> failFirstPage(
                requestGeneration,
                if (result.cause is SocketTimeoutException) LibraryError.TIMEOUT else LibraryError.UNREACHABLE,
                isRefresh,
            )
        }
    }

    private fun failFirstPage(requestGeneration: Long, error: LibraryError, isRefresh: Boolean) {
        if (requestGeneration != generation.get()) return
        _uiState.update { current ->
            if (isRefresh && current.items.isNotEmpty()) {
                current.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    refreshError = error,
                    refreshErrorId = current.refreshErrorId + 1,
                )
            } else {
                current.copy(
                    items = emptyList(),
                    total = 0,
                    hasMore = false,
                    isInitialLoading = false,
                    isRefreshing = false,
                    error = error,
                )
            }
        }
    }

    private suspend fun loadFacets() {
        when (val result = repository.loadFacets()) {
            is ApiResult.Success -> _uiState.update {
                it.copy(facets = result.data, facetsError = false)
            }
            else -> _uiState.update { it.copy(facetsError = true) }
        }
    }

    private fun buildQuery(
        search: String,
        scope: SearchScope,
        filters: LibraryFilters,
    ): VideoQuery = VideoQuery(
        limit = VideoQuery.DEFAULT_LIMIT,
        offset = 0,
        status = VideoQuery.DEFAULT_STATUS,
        q = search.takeIf { it.isNotBlank() },
        scope = scope.apiValue,
        creator = filters.creator,
        tags = filters.tags,
        tagMode = if (filters.tags.isNotEmpty()) filters.tagMode.apiValue else null,
        excludeTags = filters.excludeTags,
        codec = filters.codec,
        minHeight = filters.minHeight,
        sort = filters.sort.apiValue,
        order = filters.order.apiValue,
    )

    private fun computeHasMore(loadedCount: Int, total: Long, pageSize: Int): Boolean {
        if (pageSize <= 0) return false
        if (total <= 0) return false
        return loadedCount.toLong() < total
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
