package com.hwatch.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hwatch.app.R
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.ui.common.EmptyState
import com.hwatch.app.ui.common.FullScreenErrorState
import com.hwatch.app.ui.common.HWatchShapes
import com.hwatch.app.ui.common.HWatchSpacing
import com.hwatch.app.ui.common.InlineErrorState
import com.hwatch.app.ui.common.LocalSnackbarHostState

/** 影视库：搜索 + scope + Facets 筛选 + 排序 + 分页网格。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    baseUrl: String,
    onVideoClick: (VideoDto) -> Unit,
    onChangeServer: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = state
    val snackbarHostState = LocalSnackbarHostState.current
    var showFilters by remember { mutableStateOf(false) }

    val refreshFailedText = stringResource(R.string.snackbar_refresh_failed)
    LaunchedEffect(snapshot.refreshErrorId) {
        if (snapshot.refreshErrorId > 0) {
            snackbarHostState.showSnackbar(refreshFailedText)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchRow(
                searchText = state.searchText,
                activeFilterCount = state.filters.activeFilterCount,
                onSearchTextChanged = viewModel::onSearchTextChanged,
                onOpenFilters = { showFilters = true },
            )
            ScopeRow(
                selected = state.scope,
                onScopeChanged = viewModel::onScopeChanged,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    snapshot.isInitialLoading -> LibrarySkeletonGrid()
                    snapshot.error != null && snapshot.items.isEmpty() -> {
                        val networkError = snapshot.error == LibraryError.UNREACHABLE ||
                            snapshot.error == LibraryError.TIMEOUT
                        FullScreenErrorState(
                            icon = Icons.Filled.CloudOff,
                            title = stringResource(R.string.library_load_error),
                            message = stringResource(libraryErrorRes(snapshot.error)),
                            onRetry = viewModel::retry,
                            secondaryActionLabel = if (networkError) {
                                stringResource(R.string.settings_change_server)
                            } else {
                                null
                            },
                            onSecondaryAction = if (networkError) onChangeServer else null,
                        )
                    }
                    snapshot.items.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.Filled.VideoLibrary,
                            title = stringResource(R.string.library_empty),
                        )
                    }
                    else -> LibraryGrid(
                        state = snapshot,
                        baseUrl = baseUrl,
                        onRefresh = viewModel::refresh,
                        onLoadMore = viewModel::loadMore,
                        onVideoClick = onVideoClick,
                    )
                }
            }
        }
    }

    if (showFilters) {
        LibraryFilterSheet(
            facets = state.facets,
            facetsError = state.facetsError,
            initialFilters = state.filters,
            onRetryFacets = viewModel::retryFacets,
            onApply = { filters ->
                viewModel.applyFilters(filters)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchRow(
    searchText: String,
    activeFilterCount: Int,
    onSearchTextChanged: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HWatchSpacing.screenHorizontal, vertical = HWatchSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs),
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.library_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChanged("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.library_clear_search))
                    }
                }
            },
        )
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(activeFilterCount.toString()) }
                }
            },
        ) {
            FilledTonalIconButton(onClick = onOpenFilters) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.library_filters_title),
                )
            }
        }
    }
}

@Composable
private fun ScopeRow(
    selected: SearchScope,
    onScopeChanged: (SearchScope) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HWatchSpacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.xs),
    ) {
        SearchScope.entries.forEach { scope ->
            FilterChip(
                selected = selected == scope,
                onClick = { onScopeChanged(scope) },
                label = { Text(stringResource(scope.labelRes)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryGrid(
    state: LibraryUiState,
    baseUrl: String,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onVideoClick: (VideoDto) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalCount = gridState.layoutInfo.totalItemsCount
            totalCount > 0 && lastVisible >= totalCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.items.size) {
        if (shouldLoadMore) onLoadMore()
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(HWatchSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.gridGap),
            verticalArrangement = Arrangement.spacedBy(HWatchSpacing.gridGap),
        ) {
            items(items = state.items, key = { it.videoId }) { video ->
                VideoGridCard(
                    video = video,
                    baseUrl = baseUrl,
                    onClick = { onVideoClick(video) },
                )
            }
            if (state.isLoadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(HWatchSpacing.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(HWatchSpacing.xl))
                    }
                }
            } else if (state.loadMoreError != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InlineErrorState(
                        message = stringResource(R.string.library_load_more_failed) +
                            "：" + stringResource(libraryErrorRes(state.loadMoreError)),
                        onRetry = onLoadMore,
                    )
                }
            } else if (!state.hasMore && state.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.library_end_of_list),
                        modifier = Modifier.fillMaxWidth().padding(HWatchSpacing.sm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(HWatchSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(HWatchSpacing.gridGap),
        verticalArrangement = Arrangement.spacedBy(HWatchSpacing.gridGap),
        userScrollEnabled = false,
    ) {
        repeat(6) {
            item { SkeletonCard() }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(HWatchShapes.thumbnail)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(modifier = Modifier.size(HWatchSpacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(HWatchShapes.chip)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(modifier = Modifier.size(HWatchSpacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .clip(HWatchShapes.chip)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

private fun libraryErrorRes(error: LibraryError): Int = when (error) {
    LibraryError.UNREACHABLE -> R.string.error_unreachable
    LibraryError.TIMEOUT -> R.string.error_timeout
    LibraryError.HTTP -> R.string.error_http
    LibraryError.PARSE -> R.string.error_parse
}
