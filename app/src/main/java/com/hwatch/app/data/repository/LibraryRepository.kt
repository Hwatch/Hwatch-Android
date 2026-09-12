package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.data.model.FacetsDto
import com.hwatch.app.data.model.PagedResponse
import com.hwatch.app.data.model.VideoDto
import com.hwatch.app.data.model.VideoQuery

/**
 * 影视库数据源。负责把 [VideoQuery] 映射为 `GET /api/videos` 的 query 参数：
 * 空 tags / exclude_tags / q / creator / codec 一律省略参数，tags 多值用逗号连接。
 * `language` 未显式指定时使用当前 `translation_language`。
 */
class LibraryRepository(
    private val apiProvider: suspend () -> Result<HWatchApi>,
    private val languageProvider: suspend () -> String,
) {

    suspend fun loadVideos(query: VideoQuery): ApiResult<PagedResponse<VideoDto>> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall {
            api.videos(
                limit = query.limit,
                offset = query.offset,
                status = query.status,
                language = query.language ?: languageProvider(),
                q = query.q?.takeIf { it.isNotBlank() },
                scope = query.scope,
                creator = query.creator?.takeIf { it.isNotBlank() },
                tags = query.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
                tagMode = query.tagMode,
                excludeTags = query.excludeTags.takeIf { it.isNotEmpty() }?.joinToString(","),
                codec = query.codec?.takeIf { it.isNotBlank() },
                minHeight = query.minHeight,
                sort = query.sort?.takeIf { it.isNotBlank() },
                order = query.order?.takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun loadFacets(): ApiResult<FacetsDto> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall { api.facets() }
    }
}
