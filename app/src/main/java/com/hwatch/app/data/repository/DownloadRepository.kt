package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.pathSegment
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.data.model.ActionResponse
import com.hwatch.app.data.model.DownloadAllResponse
import com.hwatch.app.data.model.PagedResponse
import com.hwatch.app.data.model.ToggleDownloadsRequest
import com.hwatch.app.data.model.ToggleDownloadsResponse
import com.hwatch.app.data.model.VideoDto

/**
 * 下载任务数据源。`GET /api/tasks` 的 item 与公共 Video schema 相同（依据 APIV1.md），
 * 因此复用 [VideoDto]。路径参数严格经 [pathSegment] 编码。
 */
class DownloadRepository(
    private val apiProvider: suspend () -> Result<HWatchApi>,
    private val languageProvider: suspend () -> String,
) {

    suspend fun getTasks(
        limit: Int = DEFAULT_TASKS_LIMIT,
        offset: Int = 0,
        status: String? = null,
    ): ApiResult<PagedResponse<VideoDto>> = withApi { api ->
        api.tasks(
            limit = limit,
            offset = offset,
            status = status,
            language = languageProvider(),
        )
    }

    /** 未下载视频：`GET /api/videos?status=discovered`。 */
    suspend fun getDiscovered(
        limit: Int = DISCOVERED_LIMIT,
        offset: Int = 0,
    ): ApiResult<PagedResponse<VideoDto>> = withApi { api ->
        api.videos(
            limit = limit,
            offset = offset,
            status = "discovered",
            language = languageProvider(),
            scope = "all",
        )
    }

    suspend fun videoAction(videoId: String, action: String): ApiResult<ActionResponse> =
        withApi { api -> api.videoAction(pathSegment(videoId), action) }

    suspend fun downloadAll(subscription: String? = null): ApiResult<DownloadAllResponse> =
        withApi { api -> api.downloadAll(subscription) }

    /** 全局下载开关（`POST /api/downloads/toggle`）。202 返回新状态。 */
    suspend fun toggleDownloads(paused: Boolean): ApiResult<ToggleDownloadsResponse> =
        withApi { api -> api.toggleDownloads(ToggleDownloadsRequest(paused)) }

    /** 通过 dashboard 读取全局暂停状态（`downloads_paused`）。 */
    suspend fun getDownloadsPaused(): ApiResult<Boolean> =
        withApi { api -> api.dashboard(languageProvider()).downloadsPaused }

    private suspend fun <T> withApi(block: suspend (HWatchApi) -> T): ApiResult<T> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall { block(api) }
    }

    companion object {
        const val DEFAULT_TASKS_LIMIT = 1000
        const val DISCOVERED_LIMIT = 48
    }
}
