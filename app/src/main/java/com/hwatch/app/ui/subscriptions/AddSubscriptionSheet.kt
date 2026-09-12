package com.hwatch.app.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hwatch.app.R
import com.hwatch.app.data.model.CreatorDto
import com.hwatch.app.data.repository.SubscriptionRepository
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.InlineErrorState

/** 新增订阅底部弹层：平台 + 作者搜索（debounce）+ URL + download_existing。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSubscriptionSheet(
    state: AddSubscriptionUiState,
    onQueryChanged: (String) -> Unit,
    onPlatformChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onDownloadExistingChanged: (Boolean) -> Unit,
    onSelectCreator: (CreatorDto) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = HWatchSpacing.md)
                .padding(bottom = HWatchSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.subscriptions_add),
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.platforms.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.subscriptions_platform),
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs)) {
                    state.platforms.forEach { platform ->
                        FilterChip(
                            selected = state.selectedPlatform == platform.id,
                            onClick = { onPlatformChanged(platform.id) },
                            label = { Text(platform.name) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.subscriptions_search_hint)) },
                supportingText = {
                    if (state.query.isNotBlank() &&
                        state.query.trim().length < SubscriptionRepository.MIN_SEARCH_LENGTH
                    ) {
                        Text(stringResource(R.string.subscriptions_search_min_chars))
                    }
                },
            )
            if (state.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.searchError?.let { error ->
                InlineErrorState(message = stringResource(subscriptionsErrorRes(error)))
            }
            state.results.forEach { creator ->
                CreatorRow(creator = creator, onClick = { onSelectCreator(creator) })
            }
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.subscriptions_url)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.downloadExisting,
                    onCheckedChange = onDownloadExistingChanged,
                )
                Spacer(modifier = Modifier.size(HWatchSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.downloadExisting) {
                            stringResource(R.string.subscriptions_download_existing)
                        } else {
                            stringResource(R.string.subscriptions_new_only)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            state.createError?.let { error ->
                InlineErrorState(message = stringResource(subscriptionsErrorRes(error)))
            }
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.url.isNotBlank() && !state.isCreating,
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.subscriptions_create))
                }
            }
        }
    }
}

@Composable
private fun CreatorRow(creator: CreatorDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HWatchSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = creator.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = creator.platform,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.subscriptions_creator_video_count, creator.videoCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
