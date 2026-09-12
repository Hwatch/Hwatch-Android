package com.hwatch.app.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.model.CreatorDto
import com.hwatch.app.data.repository.SubscriptionRepository
import java.net.SocketTimeoutException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 新增订阅 ViewModel：platform 列表 + creator 搜索（300ms debounce、q>=2、collectLatest
 * 取消旧请求避免 stale）+ 创建请求。
 */
@OptIn(FlowPreview::class)
class AddSubscriptionViewModel(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSubscriptionUiState())
    val uiState: StateFlow<AddSubscriptionUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            when (val result = repository.getPlatforms()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        platforms = result.data.items,
                        selectedPlatform = result.data.items.firstOrNull()?.id,
                    )
                }
                else -> Unit
            }
        }
        viewModelScope.launch {
            query
                .debounce(SEARCH_DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { text ->
                    if (text.length < SubscriptionRepository.MIN_SEARCH_LENGTH) {
                        _uiState.update {
                            it.copy(results = emptyList(), isSearching = false, searchError = null)
                        }
                    } else {
                        search(text)
                    }
                }
        }
    }

    fun onQueryChanged(value: String) {
        query.value = value
        _uiState.update { it.copy(query = value) }
    }

    fun onPlatformChanged(platform: String) {
        _uiState.update { it.copy(selectedPlatform = platform, results = emptyList()) }
    }

    fun onUrlChanged(value: String) {
        _uiState.update { it.copy(url = value) }
    }

    fun onDownloadExistingChanged(value: Boolean) {
        _uiState.update { it.copy(downloadExisting = value) }
    }

    fun selectCreator(creator: CreatorDto) {
        _uiState.update {
            it.copy(
                query = creator.name,
                url = creator.url.ifBlank { it.url },
                results = emptyList(),
            )
        }
    }

    fun create() {
        val state = _uiState.value
        val url = state.url.trim()
        if (url.isBlank() || state.isCreating) return
        _uiState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            when (val result = repository.createSubscription(url, state.downloadExisting)) {
                is ApiResult.Success -> _uiState.update { it.copy(isCreating = false, created = true) }
                is ApiResult.HttpError -> onCreateFailure(SubscriptionsError.HTTP)
                is ApiResult.Unauthorized, is ApiResult.Forbidden -> onCreateFailure(SubscriptionsError.HTTP)
                is ApiResult.ParseError -> onCreateFailure(SubscriptionsError.PARSE)
                is ApiResult.NetworkError -> onCreateFailure(
                    if (result.cause is SocketTimeoutException) SubscriptionsError.TIMEOUT else SubscriptionsError.UNREACHABLE,
                )
            }
        }
    }

    private fun onCreateFailure(error: SubscriptionsError) {
        _uiState.update { it.copy(isCreating = false, createError = error) }
    }

    private suspend fun search(text: String) {
        _uiState.update { it.copy(isSearching = true, searchError = null) }
        when (val result = repository.searchCreators(text, _uiState.value.selectedPlatform)) {
            is ApiResult.Success -> _uiState.update {
                it.copy(results = result.data.items, isSearching = false, searchError = null)
            }
            is ApiResult.HttpError -> onSearchFailure(SubscriptionsError.HTTP)
            is ApiResult.Unauthorized, is ApiResult.Forbidden -> onSearchFailure(SubscriptionsError.HTTP)
            is ApiResult.ParseError -> onSearchFailure(SubscriptionsError.PARSE)
            is ApiResult.NetworkError -> onSearchFailure(
                if (result.cause is SocketTimeoutException) SubscriptionsError.TIMEOUT else SubscriptionsError.UNREACHABLE,
            )
        }
    }

    private fun onSearchFailure(error: SubscriptionsError) {
        _uiState.update { it.copy(isSearching = false, searchError = error, results = emptyList()) }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
