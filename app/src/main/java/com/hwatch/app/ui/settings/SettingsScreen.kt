package com.hwatch.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hwatch.app.R
import com.hwatch.app.ui.common.FullScreenErrorState
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.LoadingState
import com.hwatch.app.ui.common.LocalSnackbarHostState

/** 设置页：分区编辑，保存只 PUT 修改字段。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onChangeServer: () -> Unit,
    onLogout: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = state
    val snackbarHostState = LocalSnackbarHostState.current
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showChangeServerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }

    val savedText = stringResource(R.string.settings_saved)
    val saveFailedText = stringResource(R.string.settings_save_failed)
    LaunchedEffect(state.saveSuccessId) {
        if (state.saveSuccessId > 0) snackbarHostState.showSnackbar(savedText)
    }
    LaunchedEffect(state.saveErrorId) {
        if (state.saveErrorId > 0) snackbarHostState.showSnackbar(saveFailedText)
    }

    BackHandler(enabled = state.hasChanges) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = HWatchSpacing.md).size(20.dp),
                        )
                    } else {
                        TextButton(
                            onClick = viewModel::save,
                            enabled = state.hasChanges && state.invalidFields.isEmpty(),
                        ) {
                            Text(stringResource(R.string.settings_save))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> LoadingState()
                snapshot.loadError != null -> {
                    val networkError = snapshot.loadError == SettingsError.UNREACHABLE ||
                        snapshot.loadError == SettingsError.TIMEOUT
                    FullScreenErrorState(
                        icon = Icons.Filled.CloudOff,
                        title = stringResource(R.string.settings_load_error),
                        message = stringResource(settingsErrorRes(snapshot.loadError)),
                        onRetry = viewModel::retry,
                        secondaryActionLabel = if (networkError) {
                            stringResource(R.string.settings_change_server)
                        } else {
                            null
                        },
                        onSecondaryAction = if (networkError) onChangeServer else null,
                    )
                }
                else -> SettingsForm(
                    state = state,
                    secretVisible = secretVisible,
                    onSecretVisibleChange = { secretVisible = it },
                    onDraftChange = viewModel::updateDraft,
                    onChangeServer = { showChangeServerDialog = true },
                    onLogout = { showLogoutDialog = true },
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.settings_discard_title)) },
            text = { Text(stringResource(R.string.settings_discard_desc)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.settings_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showChangeServerDialog) {
        AlertDialog(
            onDismissRequest = { showChangeServerDialog = false },
            title = { Text(stringResource(R.string.settings_change_server_title)) },
            text = { Text(stringResource(R.string.settings_change_server_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showChangeServerDialog = false
                    onChangeServer()
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeServerDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title)) },
            text = { Text(stringResource(R.string.settings_logout_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text(stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsForm(
    state: SettingsUiState,
    secretVisible: Boolean,
    onSecretVisibleChange: (Boolean) -> Unit,
    onDraftChange: ((SettingsDraft) -> SettingsDraft) -> Unit,
    onChangeServer: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxHeight().imePadding(),
            contentPadding = PaddingValues(HWatchSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.md),
        ) {
        item {
            SettingsSection(R.string.settings_section_server) {
                Text(
                    text = state.serverBaseUrl.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onChangeServer) {
                    Text(stringResource(R.string.settings_change_server))
                }
            }
        }
        item {
            SettingsSection(R.string.settings_account) {
                TextButton(onClick = onLogout) {
                    Text(stringResource(R.string.settings_logout))
                }
            }
        }
        item {
            SettingsSection(R.string.settings_section_download) {
                SettingsTextField(
                    value = state.draft.httpProxy,
                    onValueChange = { value -> onDraftChange { it.copy(httpProxy = value) } },
                    labelRes = R.string.settings_http_proxy,
                )
                SettingsTextField(
                    value = state.draft.downloadDir,
                    onValueChange = { value -> onDraftChange { it.copy(downloadDir = value) } },
                    labelRes = R.string.settings_download_dir,
                )
                SettingsTextField(
                    value = state.draft.ariaRpcUrl,
                    onValueChange = { value -> onDraftChange { it.copy(ariaRpcUrl = value) } },
                    labelRes = R.string.settings_aria_rpc_url,
                )
                SecretField(
                    value = state.draft.ariaSecret,
                    onValueChange = { value -> onDraftChange { it.copy(ariaSecret = value) } },
                    visible = secretVisible,
                    onVisibleChange = onSecretVisibleChange,
                )
                SettingsTextField(
                    value = state.draft.downloadLimitKbps,
                    onValueChange = { value -> onDraftChange { it.copy(downloadLimitKbps = value) } },
                    labelRes = R.string.settings_download_limit,
                    supportingRes = R.string.settings_download_limit_hint,
                    keyboardType = KeyboardType.Number,
                    errorRes = if (SettingsField.DOWNLOAD_LIMIT_KBPS in state.invalidFields) {
                        R.string.settings_error_number
                    } else {
                        null
                    },
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_schedule) {
                SettingsTextField(
                    value = state.draft.checkTime,
                    onValueChange = { value -> onDraftChange { it.copy(checkTime = value) } },
                    labelRes = R.string.settings_check_time,
                    errorRes = if (SettingsField.CHECK_TIME in state.invalidFields) {
                        R.string.settings_error_time
                    } else {
                        null
                    },
                )
                SettingsTextField(
                    value = state.draft.timezone,
                    onValueChange = { value -> onDraftChange { it.copy(timezone = value) } },
                    labelRes = R.string.settings_timezone,
                    errorRes = if (SettingsField.TIMEZONE in state.invalidFields) {
                        R.string.settings_error_timezone
                    } else {
                        null
                    },
                )
                SettingsTextField(
                    value = state.draft.maxConcurrent,
                    onValueChange = { value -> onDraftChange { it.copy(maxConcurrent = value) } },
                    labelRes = R.string.settings_max_concurrent,
                    keyboardType = KeyboardType.Number,
                    errorRes = if (SettingsField.MAX_CONCURRENT in state.invalidFields) {
                        R.string.settings_error_number
                    } else {
                        null
                    },
                )
                SettingsTextField(
                    value = state.draft.connectionsPerDownload,
                    onValueChange = { value -> onDraftChange { it.copy(connectionsPerDownload = value) } },
                    labelRes = R.string.settings_connections_per_download,
                    keyboardType = KeyboardType.Number,
                    errorRes = if (SettingsField.CONNECTIONS_PER_DOWNLOAD in state.invalidFields) {
                        R.string.settings_error_number
                    } else {
                        null
                    },
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_storage) {
                SettingsTextField(
                    value = state.draft.minFreeGb,
                    onValueChange = { value -> onDraftChange { it.copy(minFreeGb = value) } },
                    labelRes = R.string.settings_min_free_gb,
                    keyboardType = KeyboardType.Number,
                    errorRes = if (SettingsField.MIN_FREE_GB in state.invalidFields) {
                        R.string.settings_error_gb
                    } else {
                        null
                    },
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_plex) {
                SettingsTextField(
                    value = state.draft.plexLanguage,
                    onValueChange = { value -> onDraftChange { it.copy(plexLanguage = value) } },
                    labelRes = R.string.settings_plex_language,
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_advanced) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = state.draft.requireCreatorMatch,
                        onCheckedChange = { value -> onDraftChange { it.copy(requireCreatorMatch = value) } },
                    )
                    Text(
                        text = stringResource(R.string.settings_require_creator_match),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = HWatchSpacing.sm),
                    )
                }
                SettingsTextField(
                    value = state.draft.excludeTitleKeywords,
                    onValueChange = { value -> onDraftChange { it.copy(excludeTitleKeywords = value) } },
                    labelRes = R.string.settings_exclude_title_keywords,
                )
                SettingsTextField(
                    value = state.draft.excludeUploaders,
                    onValueChange = { value -> onDraftChange { it.copy(excludeUploaders = value) } },
                    labelRes = R.string.settings_exclude_uploaders,
                )
                SettingsTextField(
                    value = state.draft.excludeCategories,
                    onValueChange = { value -> onDraftChange { it.copy(excludeCategories = value) } },
                    labelRes = R.string.settings_exclude_categories,
                )
                SettingsTextField(
                    value = state.draft.excludeTags,
                    onValueChange = { value -> onDraftChange { it.copy(excludeTags = value) } },
                    labelRes = R.string.settings_exclude_tags,
                )
                SettingsTextField(
                    value = state.draft.maxDurationSeconds,
                    onValueChange = { value -> onDraftChange { it.copy(maxDurationSeconds = value) } },
                    labelRes = R.string.settings_max_duration,
                    supportingRes = R.string.settings_max_duration_hint,
                    keyboardType = KeyboardType.Number,
                    errorRes = if (SettingsField.MAX_DURATION_SECONDS in state.invalidFields) {
                        R.string.settings_error_number
                    } else {
                        null
                    },
                )
            }
        }
    }
    }
}

@Composable
private fun SettingsSection(
    @StringRes titleRes: Int,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HWatchSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.xs),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    @StringRes supportingRes: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    @StringRes errorRes: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(labelRes)) },
        isError = errorRes != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = when {
            errorRes != null -> {
                { Text(stringResource(errorRes)) }
            }
            supportingRes != null -> {
                { Text(stringResource(supportingRes)) }
            }
            else -> null
        },
    )
}

@Composable
private fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.settings_aria_secret)) },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { onVisibleChange(!visible) }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.settings_hide_secret else R.string.settings_show_secret,
                    ),
                )
            }
        },
    )
}

private fun settingsErrorRes(error: SettingsError): Int = when (error) {
    SettingsError.UNREACHABLE -> R.string.error_unreachable
    SettingsError.TIMEOUT -> R.string.error_timeout
    SettingsError.HTTP -> R.string.error_http
    SettingsError.PARSE -> R.string.error_parse
}
