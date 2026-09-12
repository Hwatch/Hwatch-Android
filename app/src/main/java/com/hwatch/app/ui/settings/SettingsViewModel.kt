package com.hwatch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.model.SettingsDto
import com.hwatch.app.data.repository.ServerConfigRepository
import com.hwatch.app.data.repository.SettingsRepository
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings ViewModel：GET → original + draft；保存时只 PUT 修改字段。
 * 保存成功用服务端响应更新 original/draft；失败保留 draft。
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val configRepository: ServerConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var original: SettingsDto? = null
    private var successId = 0L
    private var errorId = 0L

    init {
        load()
        viewModelScope.launch {
            configRepository.serverBaseUrl.collect { url ->
                _uiState.update { it.copy(serverBaseUrl = url) }
            }
        }
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.getSettings()) {
                is ApiResult.Success -> {
                    original = result.data
                    _uiState.update {
                        it.copy(
                            draft = SettingsMapper.toDraft(result.data),
                            isLoading = false,
                            loadError = null,
                            invalidFields = emptySet(),
                            hasChanges = false,
                        )
                    }
                }
                is ApiResult.HttpError -> onLoadFailure(SettingsError.HTTP)
                is ApiResult.ParseError -> onLoadFailure(SettingsError.PARSE)
                is ApiResult.NetworkError -> onLoadFailure(
                    if (result.cause is SocketTimeoutException) SettingsError.TIMEOUT else SettingsError.UNREACHABLE,
                )
            }
        }
    }

    private fun onLoadFailure(error: SettingsError) {
        _uiState.update { it.copy(isLoading = false, loadError = error) }
    }

    fun updateDraft(transform: (SettingsDraft) -> SettingsDraft) {
        _uiState.update { state ->
            val draft = transform(state.draft)
            val currentOriginal = original
            state.copy(
                draft = draft,
                invalidFields = SettingsMapper.invalidFields(draft),
                hasChanges = currentOriginal != null && SettingsMapper.hasChanges(currentOriginal, draft),
            )
        }
    }

    fun save() {
        val currentOriginal = original ?: return
        val state = _uiState.value
        if (state.isSaving || state.invalidFields.isNotEmpty() || !state.hasChanges) return
        val request = SettingsMapper.buildUpdateRequest(currentOriginal, state.draft)
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = repository.updateSettings(request)) {
                is ApiResult.Success -> {
                    original = result.data
                    successId += 1
                    _uiState.update {
                        it.copy(
                            draft = SettingsMapper.toDraft(result.data),
                            isSaving = false,
                            invalidFields = emptySet(),
                            hasChanges = false,
                            saveSuccessId = successId,
                        )
                    }
                }
                else -> {
                    errorId += 1
                    _uiState.update { it.copy(isSaving = false, saveErrorId = errorId) }
                }
            }
        }
    }
}
