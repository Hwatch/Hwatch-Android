package com.hwatch.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hwatch.app.R
import com.hwatch.app.data.model.FacetDto
import com.hwatch.app.data.model.FacetsDto

/**
 * 筛选底部弹层。所有修改只写入本地 draft，点击「应用」才提交给 ViewModel，
 * 避免每点一个 Chip 就发一次请求。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterSheet(
    facets: FacetsDto?,
    facetsError: Boolean,
    initialFilters: LibraryFilters,
    onRetryFacets: () -> Unit,
    onApply: (LibraryFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(initialFilters) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.library_filters_title),
                style = MaterialTheme.typography.titleLarge,
            )
            if (facetsError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.library_facets_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetryFacets) {
                        Text(stringResource(R.string.library_facets_retry))
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_creator)) {
                val creators = facets?.creators.orEmpty()
                if (creators.isEmpty()) {
                    NoOptionsHint()
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        creators.forEach { facet ->
                            FilterChip(
                                selected = draft.creator == facet.value,
                                onClick = {
                                    draft = draft.copy(
                                        creator = if (draft.creator == facet.value) null else facet.value,
                                    )
                                },
                                label = { Text(facetLabel(facet)) },
                            )
                        }
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_tags)) {
                val tags = facets?.tags.orEmpty()
                if (tags.isEmpty()) {
                    NoOptionsHint()
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { facet ->
                            FilterChip(
                                selected = facet.value in draft.tags,
                                onClick = { draft = draft.copy(tags = draft.tags.toggle(facet.value)) },
                                label = { Text(facetLabel(facet)) },
                            )
                        }
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_exclude_tags)) {
                val tags = facets?.tags.orEmpty()
                if (tags.isEmpty()) {
                    NoOptionsHint()
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { facet ->
                            FilterChip(
                                selected = facet.value in draft.excludeTags,
                                onClick = {
                                    draft = draft.copy(excludeTags = draft.excludeTags.toggle(facet.value))
                                },
                                label = { Text(facetLabel(facet)) },
                            )
                        }
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_tag_mode)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.tagMode == mode,
                            onClick = { draft = draft.copy(tagMode = mode) },
                            label = { Text(stringResource(mode.labelRes)) },
                        )
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_codec)) {
                val codecs = facets?.codecs.orEmpty()
                if (codecs.isEmpty()) {
                    NoOptionsHint()
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        codecs.forEach { facet ->
                            FilterChip(
                                selected = draft.codec == facet.value,
                                onClick = {
                                    draft = draft.copy(
                                        codec = if (draft.codec == facet.value) null else facet.value,
                                    )
                                },
                                label = { Text(facetLabel(facet)) },
                            )
                        }
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_min_height)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MIN_HEIGHT_OPTIONS.forEach { height ->
                        FilterChip(
                            selected = draft.minHeight == height,
                            onClick = { draft = draft.copy(minHeight = height) },
                            label = { Text(minHeightLabel(height)) },
                        )
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_sort)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibrarySort.entries.forEach { sort ->
                        FilterChip(
                            selected = draft.sort == sort,
                            onClick = { draft = draft.copy(sort = sort) },
                            label = { Text(stringResource(sort.labelRes)) },
                        )
                    }
                }
            }

            FilterSection(stringResource(R.string.library_filter_order)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = draft.order == order,
                            onClick = { draft = draft.copy(order = order) },
                            label = { Text(stringResource(order.labelRes)) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { draft = LibraryFilters() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.library_filter_reset))
                }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.library_filter_apply))
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun NoOptionsHint() {
    Text(
        text = stringResource(R.string.library_filter_no_options),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun facetLabel(facet: FacetDto): String = "${facet.value} (${facet.count})"

@Composable
private fun minHeightLabel(height: Int?): String =
    if (height == null) stringResource(R.string.library_min_height_any) else "${height}p"

private fun List<String>.toggle(value: String): List<String> =
    if (value in this) this - value else this + value
