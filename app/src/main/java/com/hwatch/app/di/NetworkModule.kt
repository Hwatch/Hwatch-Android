package com.hwatch.app.di

import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.apiclient.RedactingInterceptor
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 网络基础设施工厂。手动 DI，无第三方依赖注入框架。
 */
object NetworkModule {

    fun createJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        coerceInputValues = false
    }

    fun createOkHttpClient(
        enableBodyLogging: Boolean,
        cookieJar: okhttp3.CookieJar? = null,
        authInterceptor: okhttp3.Interceptor? = null,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(RedactingInterceptor(enableBodyLogging))
        authInterceptor?.let { builder.addInterceptor(it) }
        cookieJar?.let { builder.cookieJar(it) }
        return builder
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 为长视频流媒体派生客户端：
     * - `callTimeout` 保持 0（无限），避免整个媒体请求被全局超时终止；
     * - `readTimeout = 0`（无限），避免长连接读取过程中被 60s per-read 超时中断；
     * - 通过 `newBuilder()` 派生，**共享原 OkHttpClient 的连接池 / Dispatcher / DNS /
     *   Interceptor**，不重建孤立网络栈。
     */
    fun deriveStreamingClient(base: OkHttpClient): OkHttpClient {
        return base.newBuilder()
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun createRetrofit(json: Json, client: OkHttpClient, baseUrl: String): HWatchApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(HWatchApi::class.java)
    }
}