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

                if (serverBaseUrl.isBlank() || editingServer) {
                    val setupViewModel: ServerSetupViewModel = viewModel(factory = viewModelFactory)
                    val setupState by setupViewModel.uiState.collectAsStateWithLifecycle()
                    var handledSuccessId by rememberSaveable { mutableLongStateOf(0L) }
                    LaunchedEffect(setupState.connectionSuccessId) {
                        if (setupState.connectionSuccessId > handledSuccessId) {
                            handledSuccessId = setupState.connectionSuccessId
                            editingServer = false
                        }
                    }
                    ServerSetupScreen(
                        viewModel = setupViewModel,
                        isEditMode = serverBaseUrl.isNotBlank(),
                        initialUrl = serverBaseUrl,
                        onCancel = { editingServer = false },
                    )
                } else {
                    HWatchApp(
                        viewModelFactory = viewModelFactory,
                        serverBaseUrl = serverBaseUrl,
                        onChangeServer = { editingServer = true },
                    )
                }
            }
        }
    }
}
