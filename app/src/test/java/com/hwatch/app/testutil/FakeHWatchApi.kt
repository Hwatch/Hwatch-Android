package com.hwatch.app.testutil

import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.model.ActionResponse
import com.hwatch.app.data.model.CreateSubscriptionRequest
import com.hwatch.app.data.model.CreateSubscriptionResponse
import com.hwatch.app.data.model.CreatorDto
import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.data.model.DeleteResponse
import com.hwatch.app.data.model.DownloadAllResponse
import com.hwatch.app.data.model.FacetsDto
import com.hwatch.app.data.model.ItemsResponse
import com.hwatch.app.data.model.PagedResponse
import com.hwatch.app.data.model.PlatformDto
import com.hwatch.app.data.model.SettingsDto
import com.hwatch.app.data.model.SubscriptionDto
import com.hwatch.app.data.model.ToggleDownloadsRequest
import com.hwatch.app.data.model.ToggleDownloadsResponse
import com.hwatch.app.data.model.UpdateSettingsRequest
import com.hwatch.app.data.model.UpdateSubscriptionRequest
import com.hwatch.app.data.model.UpdateSubscriptionResponse
import com.hwatch.app.data.model.VideoDto
import okhttp3.ResponseBody

/** 记录 `videos()` 实际收到的全部 query 参数，便于断言映射。 */
data class VideoRequest(
    val limit: Int?,
    val offset: Int?,
    val status: String?,
    val language: String?,
    val q: String?,
    val scope: String?,
    val creator: String?,
    val tags: String?,
    val tagMode: String?,
    val excludeTags: String?,
    val codec: String?,
    val minHeight: Int?,
    val sort: String?,
    val order: String?,
)

/** 记录 `tasks()` 参数。 */
data class TaskRequest(
    val limit: Int?,
    val offset: Int?,
    val status: String?,
    val language: String?,
)

/**
 * 测试用 [HWatchApi]。仅被测试使用的方法可配置（可返回数据或抛异常），其余方法不应被调用。
 */
class FakeHWatchApi(
    var onVideos: suspend (VideoRequest) -> PagedResponse<VideoDto> = { PagedResponse() },
    var onFacets: suspend () -> FacetsDto = { FacetsDto() },
    var onTasks: suspend (TaskRequest) -> PagedResponse<VideoDto> = { PagedResponse() },
    var onVideoAction: suspend (String, String) -> ActionResponse = { _, _ -> ActionResponse("accepted") },
    var onDownloadAll: suspend (String?) -> DownloadAllResponse = { DownloadAllResponse(0) },
    var onToggleDownloads: suspend (ToggleDownloadsRequest) -> ToggleDownloadsResponse = { ToggleDownloadsResponse(it.paused) },
    var onSubscriptions: suspend () -> ItemsResponse<SubscriptionDto> = { ItemsResponse() },
    var onPlatforms: suspend () -> ItemsResponse<PlatformDto> = { ItemsResponse() },
    var onSearchCreators: suspend (String, String?) -> ItemsResponse<CreatorDto> = { _, _ -> ItemsResponse() },
    var onCreateSubscription: suspend (CreateSubscriptionRequest) -> CreateSubscriptionResponse = { CreateSubscriptionResponse() },
    var onPatchSubscription: suspend (String, UpdateSubscriptionRequest) -> UpdateSubscriptionResponse = { _, body -> UpdateSubscriptionResponse(body.enabled) },
    var onDeleteSubscription: suspend (String) -> DeleteResponse = { DeleteResponse(true) },
    var onCheckAll: suspend () -> ActionResponse = { ActionResponse("started") },
    var onCheckSubscription: suspend (String) -> ActionResponse = { ActionResponse("started") },
    var onGetSettings: suspend () -> SettingsDto = { SettingsDto() },
    var onPutSettings: suspend (UpdateSettingsRequest) -> SettingsDto = { SettingsDto() },
    var onDashboard: suspend (String?) -> DashboardDto = { DashboardDto() },
) : HWatchApi {

    override suspend fun dashboard(language: String?): DashboardDto = onDashboard(language)

    override suspend fun videos(
        limit: Int?,
        offset: Int?,
        status: String?,
        language: String?,
        q: String?,
        scope: String?,
        creator: String?,
        tags: String?,
        tagMode: String?,
        excludeTags: String?,
        codec: String?,
        minHeight: Int?,
        sort: String?,
        order: String?,
    ): PagedResponse<VideoDto> = onVideos(
        VideoRequest(
            limit = limit,
            offset = offset,
            status = status,
            language = language,
            q = q,
            scope = scope,
            creator = creator,
            tags = tags,
            tagMode = tagMode,
            excludeTags = excludeTags,
            codec = codec,
            minHeight = minHeight,
            sort = sort,
            order = order,
        ),
    )

    override suspend fun facets(): FacetsDto = onFacets()

    override suspend fun tasks(
        limit: Int?,
        offset: Int?,
        status: String?,
        language: String?,
    ): PagedResponse<VideoDto> = onTasks(
        TaskRequest(limit = limit, offset = offset, status = status, language = language),
    )

    override suspend fun media(videoId: String): ResponseBody = notUsed()

    override suspend fun thumb(videoId: String): ResponseBody = notUsed()

    override suspend fun downloadAll(subscription: String?): DownloadAllResponse = onDownloadAll(subscription)

    override suspend fun toggleDownloads(body: ToggleDownloadsRequest): ToggleDownloadsResponse =
        onToggleDownloads(body)

    override suspend fun videoAction(videoId: String, action: String): ActionResponse =
        onVideoAction(videoId, action)

    override suspend fun subscriptions(): ItemsResponse<SubscriptionDto> = onSubscriptions()

    override suspend fun platforms(): ItemsResponse<PlatformDto> = onPlatforms()

    override suspend fun searchCreators(q: String, platform: String?): ItemsResponse<CreatorDto> =
        onSearchCreators(q, platform)

    override suspend fun createSubscription(
        body: CreateSubscriptionRequest,
    ): CreateSubscriptionResponse = onCreateSubscription(body)

    override suspend fun patchSubscription(
        slug: String,
        body: UpdateSubscriptionRequest,
    ): UpdateSubscriptionResponse = onPatchSubscription(slug, body)

    override suspend fun deleteSubscription(slug: String): DeleteResponse = onDeleteSubscription(slug)

    override suspend fun checkAll(): ActionResponse = onCheckAll()

    override suspend fun checkSubscription(slug: String): ActionResponse = onCheckSubscription(slug)

    override suspend fun getSettings(): SettingsDto = onGetSettings()

    override suspend fun putSettings(body: UpdateSettingsRequest): SettingsDto = onPutSettings(body)

    private fun notUsed(): Nothing = throw NotImplementedError("FakeHWatchApi: method not used in this test")
}
