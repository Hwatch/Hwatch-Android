package com.hwatch.app.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.model.SubscriptionDto
import com.hwatch.app.data.repository.SubscriptionRepository
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 订阅列表 ViewModel。PATCH enabled 为同步返回（依据 APIV1.md），以 response 为准更新；
 * check 为 202，只提示“已提交检查”，不伪造 last_checked_at。
 */
class SubscriptionsViewModel(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState(isLoading = true))
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private var messageId = 0L

    init {
        load(isRefresh = false)
    }

    fun refresh() {
        load(isRefresh = _uiState.value.items.isNotEmpty())
    }

    fun retry() {
        load(isRefresh = false)
    }

    private fun load(isRefresh: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = !isRefresh && it.items.isEmpty(),
                isRefreshing = isRefresh,
            )
        }
        viewModelScope.launch {
            when (val result = repository.getSubscriptions()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(items = result.data.items, isLoading = false, isRefreshing = false, error = null)
                }
                is ApiResult.HttpError -> onFailure(isRefresh, SubscriptionsError.HTTP)
                is ApiResult.ParseError -> onFailure(isRefresh, SubscriptionsError.PARSE)
                is ApiResult.NetworkError -> onFailure(
                    isRefresh,
                    if (result.cause is SocketTimeoutException) SubscriptionsError.TIMEOUT else SubscriptionsError.UNREACHABLE,
                )
            }
        }
    }

    private fun onFailure(isRefresh: Boolean, error: SubscriptionsError) {
        _uiState.update { state ->
            if (isRefresh && state.items.isNotEmpty()) {
                state.copy(isRefreshing = false, message = nextMessage(SubscriptionsMessage.Kind.REFRESH_FAILED))
            } else {
                state.copy(isLoading = false, isRefreshing = false, error = error)
            }
        }
    }

    fun setEnabled(subscription: SubscriptionDto, enabled: Boolean) {
        val slug = subscription.slug
        if (slug in _uiState.value.pendingActions) return
        _uiState.update { it.copy(pendingActions = it.pendingActions + slug) }
        viewModelScope.launch {
            when (val result = repository.setEnabled(slug, enabled)) {
                is ApiResult.Success -> _uiState.update { state ->
                    state.copy(
                        items = state.items.map { item ->
                            if (item.slug == slug) item.copy(enabled = result.data.enabled) else item
                        },
                        pendingActions = state.pendingActions - slug,
                    )
                }
                else -> _uiState.update {
                    it.copy(
                        pendingActions = it.pendingActions - slug,
                        message = nextMessage(SubscriptionsMessage.Kind.ACTION_FAILED),
                    )
                }
            }
        }
    }

    fun delete(subscription: SubscriptionDto) {
        val slug = subscription.slug
        if (slug in _uiState.value.pendingActions) return
        _uiState.update { it.copy(pendingActions = it.pendingActions + slug) }
        viewModelScope.launch {
            when (repository.deleteSubscription(slug)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            items = state.items.filterNot { it.slug == slug },
                            pendingActions = state.pendingActions - slug,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        pendingActions = it.pendingActions - slug,
                        message = nextMessage(SubscriptionsMessage.Kind.ACTION_FAILED),
                    )
                }
            }
        }
    }

    fun checkAll() {
        if (_uiState.value.isCheckingAll) return
        _uiState.update { it.copy(isCheckingAll = true) }
        viewModelScope.launch {
            val result = repository.checkAll()
            _uiState.update {
                it.copy(
                    isCheckingAll = false,
                    message = nextMessage(
                        if (result is ApiResult.Success) {
                            SubscriptionsMessage.Kind.CHECK_SUBMITTED
                        } else {
                            SubscriptionsMessage.Kind.ACTION_FAILED
                        },
                    ),
                )
            }
        }
    }

    fun checkOne(subscription: SubscriptionDto) {
        val slug = subscription.slug
        if (slug in _uiState.value.pendingActions) return
        _uiState.update { it.copy(pendingActions = it.pendingActions + slug) }
        viewModelScope.launch {
            val result = repository.checkOne(slug)
            _uiState.update {
                it.copy(
                    pendingActions = it.pendingActions - slug,
                    message = nextMessage(
                        if (result is ApiResult.Success) {
                            SubscriptionsMessage.Kind.CHECK_SUBMITTED
                        } else {
                            SubscriptionsMessage.Kind.ACTION_FAILED
                        },
                    ),
                )
            }
        }
    }

    private fun nextMessage(kind: SubscriptionsMessage.Kind): SubscriptionsMessage {
        messageId += 1
        return SubscriptionsMessage(messageId, kind)
    }
}
