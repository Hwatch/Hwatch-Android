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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
 * 筛选底部弹层。所有修改只写入本地 draft，点击「应用」才提交给 ViewModel。
 *
 * 长列表处理（零后端改动）：
 * - 顶部「已选」区，快速移除已选项；
 * - 顶部搜索框，对已加载 facets 做本地过滤（不产生网络请求）；
 * - 每个 facet 分区按 count 降序，默认只显示前 [FACET_PREVIEW_COUNT] 个，可「展开全部/收起」。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var facetQuery by remember { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

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

            SelectedFilterSection(
                filters = draft,
                onRemoveCreator = { draft = draft.copy(creator = null) },
                onRemoveTag = { value -> draft = draft.copy(tags = draft.tags - value) },
                onRemoveExcludeTag = { value -> draft = draft.copy(excludeTags = draft.excludeTags - value) },
                onRemoveCodec = { draft = draft.copy(codec = null) },
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

            OutlinedTextField(
                value = facetQuery,
                onValueChange = { facetQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.library_filter_search_hint)) },
            )

            FilterSection(stringResource(R.string.library_filter_creator)) {
                FacetChipGroup(
                    facets = facets?.creators.orEmpty(),
                    query = facetQuery,
                    isSelected = { it == draft.creator },
                    onToggle = { value ->
                        draft = draft.copy(creator = if (draft.creator == value) null else value)
                    },
                    expanded = expanded["creator"] == true,
                    onToggleExpanded = { expanded["creator"] = !(expanded["creator"] ?: false) },
                )
            }

            FilterSection(stringResource(R.string.library_filter_tags)) {
                FacetChipGroup(
                    facets = facets?.tags.orEmpty(),
                    query = facetQuery,
                    isSelected = { it in draft.tags },
                    onToggle = { value -> draft = draft.copy(tags = draft.tags.toggle(value)) },
                    expanded = expanded["tags"] == true,
                    onToggleExpanded = { expanded["tags"] = !(expanded["tags"] ?: false) },
                )
            }

            FilterSection(stringResource(R.string.library_filter_exclude_tags)) {
                FacetChipGroup(
                    facets = facets?.tags.orEmpty(),
                    query = facetQuery,
                    isSelected = { it in draft.excludeTags },
                    onToggle = { value ->
                        draft = draft.copy(excludeTags = draft.excludeTags.toggle(value))
                    },
                    expanded = expanded["excludeTags"] == true,
                    onToggleExpanded = { expanded["excludeTags"] = !(expanded["excludeTags"] ?: false) },
                )
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
                FacetChipGroup(
                    facets = facets?.codecs.orEmpty(),
                    query = facetQuery,
                    isSelected = { it == draft.codec },
                    onToggle = { value ->
                        draft = draft.copy(codec = if (draft.codec == value) null else value)
                    },
                    expanded = expanded["codec"] == true,
                    onToggleExpanded = { expanded["codec"] = !(expanded["codec"] ?: false) },
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedFilterSection(
    filters: LibraryFilters,
    onRemoveCreator: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onRemoveExcludeTag: (String) -> Unit,
    onRemoveCodec: () -> Unit,
) {
    val hasSelection = filters.creator != null ||
        filters.tags.isNotEmpty() ||
        filters.excludeTags.isNotEmpty() ||
        filters.codec != null
    if (!hasSelection) return

    FilterSection(stringResource(R.string.library_filter_selected)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.creator?.let { creator ->
                FilterChip(
                    selected = true,
                    onClick = onRemoveCreator,
                    label = { Text(creator) },
                )
            }
            filters.tags.forEach { tag ->
                FilterChip(
                    selected = true,
                    onClick = { onRemoveTag(tag) },
                    label = { Text(tag) },
                )
            }
            filters.excludeTags.forEach { tag ->
                FilterChip(
                    selected = true,
                    onClick = { onRemoveExcludeTag(tag) },
                    label = { Text("-${tag}") },
                )
            }
            filters.codec?.let { codec ->
                FilterChip(
                    selected = true,
                    onClick = onRemoveCodec,
                    label = { Text(codec) },
                )
            }
        }
    }
}

/**
 * 单个 facet 分区：按 count 降序、按 query 本地过滤、默认只显示前 [FACET_PREVIEW_COUNT] 个。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacetChipGroup(
    facets: List<FacetDto>,
    query: String,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    if (facets.isEmpty()) {
        NoOptionsHint()
        return
    }
    val trimmedQuery = query.trim()
    val filtered = remember(facets, trimmedQuery) {
        facets
            .asSequence()
            .filter { trimmedQuery.isEmpty() || it.value.contains(trimmedQuery, ignoreCase = true) }
            .sortedByDescending { it.count }
            .toList()
    }
    if (filtered.isEmpty()) {
        Text(
            text = stringResource(R.string.library_filter_no_match),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val visible = if (expanded) filtered else filtered.take(FACET_PREVIEW_COUNT)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { facet ->
                FilterChip(
                    selected = isSelected(facet.value),
                    onClick = { onToggle(facet.value) },
                    label = { Text(facetLabel(facet)) },
                )
            }
        }
        if (filtered.size > FACET_PREVIEW_COUNT) {
            TextButton(onClick = onToggleExpanded) {
                Text(
                    if (expanded) {
                        stringResource(R.string.library_filter_show_less)
                    } else {
                        stringResource(R.string.library_filter_show_more, filtered.size)
                    },
                )
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

private const val FACET_PREVIEW_COUNT = 24
