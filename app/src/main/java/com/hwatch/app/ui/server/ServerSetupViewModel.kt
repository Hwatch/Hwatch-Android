package com.hwatch.app.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.repository.ConnectionFailure
import com.hwatch.app.data.repository.ServerConfigRepository
import com.hwatch.app.data.repository.ServerConnectionRepository
import com.hwatch.app.data.repository.ServerConnectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Server Setup 状态。错误用 [ConnectionFailure] 枚举承载，具体文案由 UI 映射到
 * `strings.xml`，避免 ViewModel 依赖 Android Context。
 */
data class ServerSetupUiState(
    val serverUrl: String = "",
    val isTesting: Boolean = false,
    val validationError: ConnectionFailure? = null,
    val connectionError: ConnectionFailure? = null,
    val connectionErrorDetail: String? = null,
    val connectionSuccessId: Long = 0,
)

class ServerSetupViewModel(
    private val connectionRepository: ServerConnectionRepository,
    private val configRepository: ServerConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerSetupUiState())
    val uiState: StateFlow<ServerSetupUiState> = _uiState.asStateFlow()

    private var successId = 0L

    /** 进入编辑/初次设置时重置状态并预填地址（candidate 仅存在于 UI/ViewModel）。 */
    fun prepare(initialUrl: String) {
        _uiState.value = ServerSetupUiState(serverUrl = initialUrl)
    }

    fun onServerUrlChanged(value: String) {
        _uiState.update {
            it.copy(
                serverUrl = value,
                validationError = null,
                connectionError = null,
                connectionErrorDetail = null,
            )
        }
    }

    /**
     * 测试并连接：只有 `GET /api/dashboard` 成功后才把规范化地址写入 DataStore，
     * 失败绝不保存地址，也不离开 Server Setup 页面。
     */
    fun testAndConnect() {
        if (_uiState.value.isTesting) return
        val rawUrl = _uiState.value.serverUrl
        _uiState.update {
            it.copy(isTesting = true, validationError = null, connectionError = null)
        }
        viewModelScope.launch {
            when (val result = connectionRepository.testConnection(rawUrl)) {
                is ServerConnectionResult.Success -> {
                    configRepository.setServerBaseUrl(result.normalizedUrl)
                    successId += 1
                    _uiState.update { it.copy(isTesting = false, connectionSuccessId = successId) }
                }
                is ServerConnectionResult.Failure -> {
                    val isValidation = result.kind == ConnectionFailure.INVALID_URL
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            validationError = if (isValidation) result.kind else null,
                            connectionError = if (isValidation) null else result.kind,
                            connectionErrorDetail = if (isValidation) null else result.detail,
                        )
                    }
                }
            }
        }
    }
}
