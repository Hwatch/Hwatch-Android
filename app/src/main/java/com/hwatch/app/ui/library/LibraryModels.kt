package com.hwatch.app.ui.library

import androidx.annotation.StringRes
import com.hwatch.app.R

/** 搜索范围，必须作为 `scope` 传给服务端（禁止本地搜索）。 */
enum class SearchScope(val apiValue: String, @StringRes val labelRes: Int) {
    ALL("all", R.string.library_scope_all),
    TRANSLATED("translated", R.string.library_scope_translated),
    ORIGINAL("original", R.string.library_scope_original),
}

/** 排序白名单（服务端支持的值），UI 不允许生成白名单以外的 sort。 */
enum class LibrarySort(val apiValue: String, @StringRes val labelRes: Int) {
    DOWNLOADED_AT("downloaded_at", R.string.library_sort_downloaded_at),
    PUBLISHED_AT("published_at", R.string.library_sort_published_at),
    VIEWS("views", R.string.library_sort_views),
    DURATION("duration", R.string.library_sort_duration),
    SIZE("size", R.string.library_sort_size),
    RESOLUTION("resolution", R.string.library_sort_resolution),
    BIT_RATE("bit_rate", R.string.library_sort_bit_rate),
    TITLE("title", R.string.library_sort_title),
    RECENT_PLAYED("recent_played", R.string.library_sort_recent_played),
    PLAY_COUNT("play_count", R.string.library_sort_play_count),
}

enum class SortOrder(val apiValue: String, @StringRes val labelRes: Int) {
    DESC("desc", R.string.library_order_desc),
    ASC("asc", R.string.library_order_asc),
}

enum class TagMode(val apiValue: String, @StringRes val labelRes: Int) {
    ALL("all", R.string.library_tag_mode_all),
    ANY("any", R.string.library_tag_mode_any),
}

/**
 * 筛选条件（含排序）。标签以 List 保存，Repository 请求前转逗号分隔。
 */
data class LibraryFilters(
    val creator: String? = null,
    val tags: List<String> = emptyList(),
    val excludeTags: List<String> = emptyList(),
    val tagMode: TagMode = TagMode.ALL,
    val codec: String? = null,
    val minHeight: Int? = null,
    val sort: LibrarySort = LibrarySort.DOWNLOADED_AT,
    val order: SortOrder = SortOrder.DESC,
) {
    val activeFilterCount: Int
        get() = (if (creator != null) 1 else 0) +
            tags.size +
            excludeTags.size +
            (if (tagMode != TagMode.ALL) 1 else 0) +
            (if (codec != null) 1 else 0) +
            (if (minHeight != null) 1 else 0)
}

/** 最低分辨率选项；`null` = 不限（不发送 min_height）。 */
val MIN_HEIGHT_OPTIONS: List<Int?> = listOf(null, 720, 1080, 1440, 2160)
