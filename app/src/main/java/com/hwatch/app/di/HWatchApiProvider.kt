package com.hwatch.app.di

import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.util.ServerUrl
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * 按 normalized baseUrl 缓存 [HWatchApi]/Retrofit 的轻量 provider。
 *
 * - [okHttpClient] 为 App 级长生命周期实例，任何情况下都复用（连接池/DNS/timeout 共享）。
 * - 同一 normalized baseUrl 连续调用返回同一个 [HWatchApi]。
 * - baseUrl 真正变化时才重建 Retrofit/HWatchApi。
 * - 普通 refresh / search / pagination 不会重建 OkHttpClient。
 */
class HWatchApiProvider(
    private val json: Json,
    val okHttpClient: OkHttpClient,
) {

    private var cachedBaseUrl: String? = null
    private var cachedApi: HWatchApi? = null

    /** 正式 baseUrl 的缓存 API。 */
    @Synchronized
    fun getApi(normalizedBaseUrl: String): HWatchApi {
        val current = cachedApi
        if (current != null && cachedBaseUrl == normalizedBaseUrl) {
            return current
        }
        val api = NetworkModule.createRetrofit(json, okHttpClient, normalizedBaseUrl)
        cachedBaseUrl = normalizedBaseUrl
        cachedApi = api
        return api
    }

    /**
     * 临时地址（Server Setup 测试尚未保存的服务器）：
     * 复用共享 OkHttpClient，但**不写入正式缓存**。仅当规范化地址恰好等于当前正式
     * baseUrl 时读取缓存，否则创建临时 Retrofit/HWatchApi。
     */
    fun getApiForRawUrl(rawUrl: String): Result<HWatchApi> =
        ServerUrl.normalize(rawUrl).map { normalized ->
            val current = cachedApi
            if (current != null && cachedBaseUrl == normalized) {
                current
            } else {
                NetworkModule.createRetrofit(json, okHttpClient, normalized)
            }
        }
}
