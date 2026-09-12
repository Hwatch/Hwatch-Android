package com.hwatch.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.auth.AuthSessionManager
import com.hwatch.app.data.auth.AuthState
import com.hwatch.app.data.auth.ServerOrigin
import com.hwatch.app.data.repository.AuthRepository
import com.hwatch.app.data.repository.ServerConfigRepository
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AuthError {
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
    INVALID_CREDENTIALS,
    RATE_LIMITED,
    SETUP_CONFLICT,
}

sealed interface RootUiState {
    data object NoServer : RootUiState
    data object Checking : RootUiState
    data object SetupRequired : RootUiState
    data class LoginRequired(val error: AuthError? = null) : RootUiState
    data class Authenticated(val serverBaseUrl: String) : RootUiState
    data class Unreachable(val error: AuthError) : RootUiState
}

/**
 * Root 认证状态机：NO_SERVER → Server Setup；SERVER_CONFIGURED → Auth bootstrap
 * （未初始化 → Initial Admin；已初始化无会话 → Login；会话有效 → Main）。
 * session 失效（401）时保留 serverBaseUrl，仅回 Login。
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: AuthSessionManager,
    private val configRepository: ServerConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Checking)
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _formError = MutableStateFlow<AuthError?>(null)
    val formError: StateFlow<AuthError?> = _formError.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.restore()
            configRepository.serverBaseUrl.collectLatest { url -> bootstrap(url) }
        }
        viewModelScope.launch {
            sessionManager.state.collect { state ->
                if (state is AuthState.Unauthenticated && _uiState.value is RootUiState.Authenticated) {
                    _uiState.value = RootUiState.LoginRequired()
                }
            }
        }
    }

    fun retry() {
        viewModelScope.launch { bootstrap(configRepository.serverBaseUrl.first()) }
    }

    fun submitSetup(username: String, password: String) {
        submit { authRepository.setup(username, password) }
    }

    fun submitLogin(username: String, password: String) {
        submit { authRepository.login(username, password) }
    }

    private fun submit(block: suspend () -> ApiResult<*>) {
        if (_isSubmitting.value) return
        _formError.value = null
        _isSubmitting.value = true
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _uiState.value = RootUiState.Authenticated(configRepository.serverBaseUrl.first())
                }
                else -> {
                    _isSubmitting.value = false
                    _formError.value = mapError(result)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _formError.value = null
            _uiState.value = RootUiState.LoginRequired()
        }
    }

    private suspend fun bootstrap(url: String) {
        if (url.isBlank()) {
            _uiState.value = RootUiState.NoServer
            return
        }
        sessionManager.clearIfOriginMismatch(ServerOrigin.fromKey(url))
        _uiState.value = RootUiState.Checking
        when (val result = authRepository.status()) {
            is ApiResult.Success -> {
                val status = result.data
                _uiState.value = when {
                    !status.configured -> RootUiState.SetupRequired
                    status.authenticated -> RootUiState.Authenticated(url)
                    else -> RootUiState.LoginRequired()
                }
            }
            is ApiResult.Unauthorized -> _uiState.value = RootUiState.LoginRequired()
            else -> _uiState.value = RootUiState.Unreachable(mapError(result))
        }
    }

    private fun mapError(result: ApiResult<*>): AuthError = when (result) {
        is ApiResult.Unauthorized -> AuthError.INVALID_CREDENTIALS
        is ApiResult.Forbidden -> AuthError.HTTP
        is ApiResult.HttpError -> when (result.code) {
            409 -> AuthError.SETUP_CONFLICT
            429 -> AuthError.RATE_LIMITED
            else -> AuthError.HTTP
        }
        is ApiResult.ParseError -> AuthError.PARSE
        is ApiResult.NetworkError ->
            if (result.cause is SocketTimeoutException) AuthError.TIMEOUT else AuthError.UNREACHABLE
        is ApiResult.Success -> AuthError.HTTP
    }
}
