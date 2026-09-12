package com.hwatch.app.ui.auth

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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hwatch.app.R
import com.hwatch.app.ui.common.FullScreenErrorState
import com.hwatch.app.ui.common.HWatchSpacing

@Composable
fun AuthCheckingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.size(HWatchSpacing.md))
            Text(
                text = stringResource(R.string.auth_checking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AuthUnreachableScreen(
    error: AuthError,
    onRetry: () -> Unit,
    onChangeServer: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        FullScreenErrorState(
            icon = Icons.Filled.CloudOff,
            title = stringResource(R.string.auth_unreachable_title),
            message = stringResource(authErrorRes(error)),
            onRetry = onRetry,
        )
        TextButton(
            onClick = onChangeServer,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.auth_change_server))
        }
    }
}

@Composable
fun InitialAdminScreen(
    isSubmitting: Boolean,
    error: AuthError?,
    onSubmit: (String, String) -> Unit,
    onChangeServer: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<AuthError?>(null) }

    AuthFormScaffold(
        titleRes = R.string.auth_setup_title,
        subtitleRes = R.string.auth_setup_subtitle,
        onChangeServer = onChangeServer,
    ) {
        AuthTextField(
            value = username,
            onValueChange = { username = it },
            labelRes = R.string.auth_username,
            enabled = !isSubmitting,
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            labelRes = R.string.auth_password,
            enabled = !isSubmitting,
        )
        PasswordField(
            value = confirm,
            onValueChange = { confirm = it },
            labelRes = R.string.auth_confirm_password,
            enabled = !isSubmitting,
        )
        val shownError = localError ?: error
        if (shownError != null) {
            Text(
                text = stringResource(authErrorRes(shownError)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        SubmitButton(
            textRes = R.string.auth_setup,
            isSubmitting = isSubmitting,
            enabled = username.isNotBlank() && password.isNotBlank(),
            onClick = {
                localError = when {
                    username.trim().length !in 3..64 -> AuthError.HTTP
                    password.length !in 10..256 -> AuthError.HTTP
                    password != confirm -> AuthError.HTTP
                    else -> null
                }
                if (localError == null) onSubmit(username.trim(), password)
            },
        )
    }
}

@Composable
fun LoginScreen(
    serverBaseUrl: String,
    isSubmitting: Boolean,
    error: AuthError?,
    onSubmit: (String, String) -> Unit,
    onChangeServer: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthFormScaffold(
        titleRes = R.string.auth_login_title,
        subtitleRes = R.string.auth_login_subtitle,
        serverBaseUrl = serverBaseUrl,
        onChangeServer = onChangeServer,
    ) {
        AuthTextField(
            value = username,
            onValueChange = { username = it },
            labelRes = R.string.auth_username,
            enabled = !isSubmitting,
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            labelRes = R.string.auth_password,
            enabled = !isSubmitting,
        )
        if (error != null) {
            Text(
                text = stringResource(authErrorRes(error)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        SubmitButton(
            textRes = R.string.auth_login,
            isSubmitting = isSubmitting,
            enabled = username.isNotBlank() && password.isNotBlank(),
            onClick = { onSubmit(username.trim(), password) },
        )
    }
}

@Composable
private fun AuthFormScaffold(
    titleRes: Int,
    subtitleRes: Int,
    onChangeServer: () -> Unit,
    serverBaseUrl: String? = null,
    content: @Composable () -> Unit,
) {
    Scaffold { innerPadding ->
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
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (serverBaseUrl != null) {
                    Text(
                        text = serverBaseUrl,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                content()
                TextButton(onClick = onChangeServer) {
                    Text(stringResource(R.string.auth_change_server))
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    enabled: Boolean,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        label = { Text(stringResource(labelRes)) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
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

@Composable
private fun SubmitButton(
    textRes: Int,
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !isSubmitting,
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(textRes))
        }
    }
}

internal fun authErrorRes(error: AuthError): Int = when (error) {
    AuthError.UNREACHABLE -> R.string.auth_error_unreachable
    AuthError.TIMEOUT -> R.string.auth_error_timeout
    AuthError.HTTP -> R.string.auth_error_http
    AuthError.PARSE -> R.string.auth_error_parse
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.RATE_LIMITED -> R.string.auth_error_rate_limited
    AuthError.SETUP_CONFLICT -> R.string.auth_error_setup_conflict
}
