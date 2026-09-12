package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.pathSegment
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.data.model.ActionResponse
import com.hwatch.app.data.model.CreateSubscriptionRequest
import com.hwatch.app.data.model.CreateSubscriptionResponse
import com.hwatch.app.data.model.CreatorDto
import com.hwatch.app.data.model.DeleteResponse
import com.hwatch.app.data.model.ItemsResponse
import com.hwatch.app.data.model.PlatformDto
import com.hwatch.app.data.model.SubscriptionDto
import com.hwatch.app.data.model.UpdateSubscriptionRequest
import com.hwatch.app.data.model.UpdateSubscriptionResponse

/** 订阅数据源。slug 一律经 [pathSegment] 编码。 */
class SubscriptionRepository(
    private val apiProvider: suspend () -> Result<HWatchApi>,
) {

    suspend fun getSubscriptions(): ApiResult<ItemsResponse<SubscriptionDto>> =
        withApi { it.subscriptions() }

    suspend fun getPlatforms(): ApiResult<ItemsResponse<PlatformDto>> =
        withApi { it.platforms() }

    suspend fun searchCreators(q: String, platform: String?): ApiResult<ItemsResponse<CreatorDto>> =
        withApi { it.searchCreators(q, platform?.takeIf { value -> value.isNotBlank() }) }

    suspend fun createSubscription(
        url: String,
        downloadExisting: Boolean,
        type: String = DEFAULT_TYPE,
    ): ApiResult<CreateSubscriptionResponse> = withApi {
        it.createSubscription(
            CreateSubscriptionRequest(url = url, type = type, downloadExisting = downloadExisting),
        )
    }

    suspend fun setEnabled(slug: String, enabled: Boolean): ApiResult<UpdateSubscriptionResponse> =
        withApi { it.patchSubscription(pathSegment(slug), UpdateSubscriptionRequest(enabled = enabled)) }

    suspend fun deleteSubscription(slug: String): ApiResult<DeleteResponse> =
        withApi { it.deleteSubscription(pathSegment(slug)) }

    suspend fun checkAll(): ApiResult<ActionResponse> = withApi { it.checkAll() }

    suspend fun checkOne(slug: String): ApiResult<ActionResponse> =
        withApi { it.checkSubscription(pathSegment(slug)) }

    private suspend fun <T> withApi(block: suspend (HWatchApi) -> T): ApiResult<T> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall { block(api) }
    }

    companion object {
        const val DEFAULT_TYPE = "creator"
        const val MIN_SEARCH_LENGTH = 2
    }
}
