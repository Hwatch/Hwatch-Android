package com.hwatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwatch.app.di.HWatchViewModelFactory
import com.hwatch.app.ui.auth.AuthCheckingScreen
import com.hwatch.app.ui.auth.AuthUnreachableScreen
import com.hwatch.app.ui.auth.AuthViewModel
import com.hwatch.app.ui.auth.InitialAdminScreen
import com.hwatch.app.ui.auth.LoginScreen
import com.hwatch.app.ui.auth.RootUiState
import com.hwatch.app.ui.main.HWatchApp
import com.hwatch.app.ui.server.ServerSetupScreen
import com.hwatch.app.ui.server.ServerSetupViewModel
import com.hwatch.app.ui.theme.HWatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as HWatchApplication).container
        enableEdgeToEdge()
        setContent {
            HWatchTheme {
                val viewModelFactory = remember(container) { HWatchViewModelFactory(container) }
                var editingServer by rememberSaveable { mutableStateOf(false) }
                val serverBaseUrl by container.serverConfigStore.serverBaseUrl
                    .collectAsStateWithLifecycle(initialValue = "")
                val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
                val rootState by authViewModel.uiState.collectAsStateWithLifecycle()
                val isSubmitting by authViewModel.isSubmitting.collectAsStateWithLifecycle()
                val formError by authViewModel.formError.collectAsStateWithLifecycle()

                if (editingServer) {
                    ServerSetupRoute(
                        viewModelFactory = viewModelFactory,
                        serverBaseUrl = serverBaseUrl,
                        onDone = { editingServer = false },
                    )
                } else {
                    when (val state = rootState) {
                        RootUiState.NoServer -> ServerSetupRoute(
                            viewModelFactory = viewModelFactory,
                            serverBaseUrl = serverBaseUrl,
                            onDone = {},
                        )
                        RootUiState.Checking -> AuthCheckingScreen()
                        RootUiState.SetupRequired -> InitialAdminScreen(
                            isSubmitting = isSubmitting,
                            error = formError,
                            onSubmit = authViewModel::submitSetup,
                            onChangeServer = { editingServer = true },
                        )
                        is RootUiState.LoginRequired -> LoginScreen(
                            serverBaseUrl = serverBaseUrl,
                            isSubmitting = isSubmitting,
                            error = formError ?: state.error,
                            onSubmit = authViewModel::submitLogin,
                            onChangeServer = { editingServer = true },
                        )
                        is RootUiState.Authenticated -> HWatchApp(
                            viewModelFactory = viewModelFactory,
                            serverBaseUrl = state.serverBaseUrl,
                            onChangeServer = { editingServer = true },
                            onLogout = authViewModel::logout,
                        )
                        is RootUiState.Unreachable -> AuthUnreachableScreen(
                            error = state.error,
                            onRetry = authViewModel::retry,
                            onChangeServer = { editingServer = true },
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ServerSetupRoute(
    viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory,
    serverBaseUrl: String,
    onDone: () -> Unit,
) {
    val setupViewModel: ServerSetupViewModel = viewModel(factory = viewModelFactory)
    val setupState by setupViewModel.uiState.collectAsStateWithLifecycle()
    var handledSuccessId by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(setupState.connectionSuccessId) {
        if (setupState.connectionSuccessId > handledSuccessId) {
            handledSuccessId = setupState.connectionSuccessId
            onDone()
        }
    }
    ServerSetupScreen(
        viewModel = setupViewModel,
        isEditMode = serverBaseUrl.isNotBlank(),
        initialUrl = serverBaseUrl,
        onCancel = onDone,
    )
}
