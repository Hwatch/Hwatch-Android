package com.hwatch.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.data.repository.DashboardRepository
import java.net.SocketTimeoutException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Dashboard 错误分类，UI 映射为用户可理解文案。 */
enum class DashboardError {
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
}

sealed interface DashboardUiState {

    data object Loading : DashboardUiState

    data class Content(
        val dashboard: DashboardDto,
        val isRefreshing: Boolean = false,
        val refreshError: DashboardError? = null,
        val refreshErrorId: Long = 0,
    ) : DashboardUiState

    data class Error(val error: DashboardError) : DashboardUiState
}

/**
 * Dashboard 数据请求所有权在 ViewModel：
 * - `init` 只加载一次，Compose 重组不会重复请求；
 * - [refresh] 保留旧内容并置 `isRefreshing`，失败只写 `refreshError`，不清空内容；
 * - 初次加载失败进入完整 [DashboardUiState.Error]，[retry] 重新加载。
 */
class DashboardViewModel(
    private val repository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun refresh() {
        val current = _uiState.value
        if (current is DashboardUiState.Content) {
            if (current.isRefreshing) return
            _uiState.value = current.copy(isRefreshing = true, refreshError = null)
            launchLoad(isRefresh = true)
        } else {
            load()
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        _uiState.value = DashboardUiState.Loading
        launchLoad(isRefresh = false)
    }

    private fun launchLoad(isRefresh: Boolean) {
        loadJob = viewModelScope.launch {
            when (val result = repository.loadDashboard()) {
                is ApiResult.Success -> {
                    _uiState.value = DashboardUiState.Content(result.data)
                }
                is ApiResult.HttpError -> onFailure(isRefresh, DashboardError.HTTP)
                is ApiResult.ParseError -> onFailure(isRefresh, DashboardError.PARSE)
                is ApiResult.NetworkError -> onFailure(
                    isRefresh,
                    if (result.cause is SocketTimeoutException) {
                        DashboardError.TIMEOUT
                    } else {
                        DashboardError.UNREACHABLE
                    },
                )
            }
        }
    }

    private fun onFailure(isRefresh: Boolean, error: DashboardError) {
        val current = _uiState.value
        if (isRefresh && current is DashboardUiState.Content) {
            _uiState.value = current.copy(
                isRefreshing = false,
                refreshError = error,
                refreshErrorId = current.refreshErrorId + 1,
            )
        } else {
            _uiState.value = DashboardUiState.Error(error)
        }
    }
}
