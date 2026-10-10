package com.hwatch.app.ui.server

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hwatch.app.BuildConfig
import com.hwatch.app.R
import com.hwatch.app.data.repository.ConnectionFailure
import com.hwatch.app.ui.common.HWatchShapes
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.connectionFailureRes
import com.hwatch.app.ui.theme.HWatchTheme

/** 首次连接 / 编辑服务器流程：候选地址测试成功后才原子保存。 */
@Composable
fun ServerSetupScreen(
    viewModel: ServerSetupViewModel,
    isEditMode: Boolean,
    initialUrl: String,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var prepared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isEditMode, initialUrl) {
        if (!prepared) {
            viewModel.prepare(initialUrl)
            prepared = true
        }
    }
    ServerSetupContent(
        state = state,
        isEditMode = isEditMode,
        onCancel = onCancel,
        onServerUrlChanged = viewModel::onServerUrlChanged,
        onTestAndConnect = viewModel::testAndConnect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSetupContent(
    state: ServerSetupUiState,
    isEditMode: Boolean,
    onCancel: () -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onTestAndConnect: () -> Unit,
) {
    val hasError = state.validationError != null || state.connectionError != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditMode) R.string.server_setup_edit_title else R.string.server_setup_title,
                        ),
                    )
                },
                navigationIcon = {
                    if (isEditMode) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.common_cancel),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(HWatchSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HWatchSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(HWatchShapes.logo)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.server_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(HWatchSpacing.xs))
                Text(
                    text = stringResource(R.string.server_setup_server_label),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = onServerUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isTesting,
                    singleLine = true,
                    isError = hasError,
                    placeholder = { Text(stringResource(R.string.server_setup_url_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                )
                if (hasError) {
                    val failure = state.validationError ?: state.connectionError
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(HWatchSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xxs),
                        ) {
                            if (failure != null) {
                                Text(
                                    text = stringResource(connectionFailureRes(failure)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            val detail = state.connectionErrorDetail
                            if (BuildConfig.DEBUG && !detail.isNullOrBlank()) {
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.server_setup_no_auth_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showsHttpWarning(state.serverUrl)) {
                    Text(
                        text = stringResource(R.string.server_setup_http_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(
                    onClick = onTestAndConnect,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isTesting && state.serverUrl.isNotBlank(),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(HWatchSpacing.xs))
                        Text(stringResource(R.string.server_setup_testing))
                    } else {
                        Text(stringResource(R.string.server_setup_test_connect))
                    }
                }
            }
        }
    }
}

/**
 * 非 HTTPS 时提示未加密。这里只做提示，不限制 RFC1918 / IPv6 / .local / VPN / 公网反代，
 * 合法性完全交给 `ServerUrl.normalize`。
 */
private fun showsHttpWarning(input: String): Boolean {
    val trimmed = input.trim()
    return !trimmed.startsWith("https://", ignoreCase = true)
}

@Preview(showBackground = true)
@Composable
private fun ServerSetupContentPreview() {
    HWatchTheme {
        ServerSetupContent(
            state = ServerSetupUiState(
                serverUrl = "http://192.168.1.100:8787",
                connectionError = ConnectionFailure.PARSE,
                connectionErrorDetail = "JsonDecodingException: external_links[0]",
            ),
            isEditMode = true,
            onCancel = {},
            onServerUrlChanged = {},
            onTestAndConnect = {},
        )
    }
}
