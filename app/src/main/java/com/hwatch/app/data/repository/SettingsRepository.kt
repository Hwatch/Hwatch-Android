package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.data.model.SettingsDto
import com.hwatch.app.data.model.UpdateSettingsRequest

/** 设置数据源。GET 用 [SettingsDto]，PUT 用 [UpdateSettingsRequest]（partial merge）。 */
class SettingsRepository(
    private val apiProvider: suspend () -> Result<HWatchApi>,
) {

    suspend fun getSettings(): ApiResult<SettingsDto> = withApi { it.getSettings() }

    suspend fun updateSettings(request: UpdateSettingsRequest): ApiResult<SettingsDto> =
        withApi { it.putSettings(request) }

    private suspend fun <T> withApi(block: suspend (HWatchApi) -> T): ApiResult<T> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall { block(api) }
    }
}
