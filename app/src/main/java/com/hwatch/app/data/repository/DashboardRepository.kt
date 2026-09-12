package com.hwatch.app.data.repository

import com.hwatch.app.data.apiclient.ApiResult
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.safeApiCall
import com.hwatch.app.data.model.DashboardDto

/**
 * Dashboard 数据源。每次加载都从当前配置解析 baseUrl 构建 [HWatchApi]，
 * 并以当前 `translation_language` 作为 `language` 参数调用 `GET /api/dashboard`。
 * Retrofit 异常由 [safeApiCall] 统一收敛为 [ApiResult]，不向上层泄露。
 */
class DashboardRepository(
    private val apiProvider: suspend () -> Result<HWatchApi>,
    private val languageProvider: suspend () -> String,
) {

    suspend fun loadDashboard(): ApiResult<DashboardDto> {
        val api = apiProvider().getOrElse { cause -> return ApiResult.NetworkError(cause) }
        return safeApiCall { api.dashboard(languageProvider()) }
    }
}
