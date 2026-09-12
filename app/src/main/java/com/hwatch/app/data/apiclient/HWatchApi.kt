package com.hwatch.app.data.apiclient

import com.hwatch.app.data.auth.AuthLogoutResponse
import com.hwatch.app.data.auth.AuthSessionDto
import com.hwatch.app.data.auth.AuthStatusDto
import com.hwatch.app.data.auth.Credentials
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
import com.hwatch.app.util.UrlEncoding
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** HWatch HTTP API V1 接口定义。 */
interface HWatchApi {

    @GET("api/auth/status")
    suspend fun authStatus(): AuthStatusDto

    @POST("api/auth/setup")
    suspend fun authSetup(@Body body: Credentials): AuthSessionDto

    @POST("api/auth/login")
    suspend fun authLogin(@Body body: Credentials): AuthSessionDto

    @GET("api/auth/me")
    suspend fun authMe(): AuthSessionDto

    @POST("api/auth/logout")
    suspend fun authLogout(): AuthLogoutResponse

    @GET("api/dashboard")
    suspend fun dashboard(@Query("language") language: String? = null): DashboardDto

    @GET("api/videos")
    suspend fun videos(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("status") status: String? = null,
        @Query("language") language: String? = null,
        @Query("q") q: String? = null,
        @Query("scope") scope: String? = null,
        @Query("creator") creator: String? = null,
        @Query("tags") tags: String? = null,
        @Query("tag_mode") tagMode: String? = null,
        @Query("exclude_tags") excludeTags: String? = null,
        @Query("codec") codec: String? = null,
        @Query("min_height") minHeight: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
    ): PagedResponse<VideoDto>

    @GET("api/videos/facets")
    suspend fun facets(): FacetsDto

    @GET("api/tasks")
    suspend fun tasks(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("status") status: String? = null,
        @Query("language") language: String? = null,
    ): PagedResponse<VideoDto>

    @GET("media/{video_id}")
    @Streaming
    suspend fun media(@Path("video_id", encoded = true) videoId: String): ResponseBody

    @GET("thumb/{video_id}")
    @Streaming
    suspend fun thumb(@Path("video_id", encoded = true) videoId: String): ResponseBody

    @POST("api/videos/download-all")
    suspend fun downloadAll(@Query("subscription") subscription: String? = null): DownloadAllResponse

    @POST("api/downloads/toggle")
    suspend fun toggleDownloads(@Body body: ToggleDownloadsRequest): ToggleDownloadsResponse

    @POST("api/videos/{video_id}/{action}")
    suspend fun videoAction(
        @Path("video_id", encoded = true) videoId: String,
        @Path("action") action: String,
    ): ActionResponse

    @GET("api/subscriptions")
    suspend fun subscriptions(): ItemsResponse<SubscriptionDto>

    @GET("api/platforms")
    suspend fun platforms(): ItemsResponse<PlatformDto>

    @GET("api/creators/search")
    suspend fun searchCreators(
        @Query("q") q: String,
        @Query("platform") platform: String? = null,
    ): ItemsResponse<CreatorDto>

    @POST("api/subscriptions")
    suspend fun createSubscription(@Body body: CreateSubscriptionRequest): CreateSubscriptionResponse

    @PATCH("api/subscriptions/{slug}")
    suspend fun patchSubscription(
        @Path("slug", encoded = true) slug: String,
        @Body body: UpdateSubscriptionRequest,
    ): UpdateSubscriptionResponse

    @DELETE("api/subscriptions/{slug}")
    suspend fun deleteSubscription(@Path("slug", encoded = true) slug: String): DeleteResponse

    @POST("api/check")
    suspend fun checkAll(): ActionResponse

    @POST("api/check/{slug}")
    suspend fun checkSubscription(@Path("slug", encoded = true) slug: String): ActionResponse

    @GET("api/settings")
    suspend fun getSettings(): SettingsDto

    @PUT("api/settings")
    suspend fun putSettings(@Body body: UpdateSettingsRequest): SettingsDto
}

/**
 * 路径参数帮助函数：video_id / slug 一律先经 pathSegment 编码再交给
 * `@Path(encoded = true)`，保证 `source:3144962` 以 `source%3A3144962` 出现在
 * 最终请求 URL 中。
 */
fun pathSegment(value: String): String = UrlEncoding.encodePathSegment(value)