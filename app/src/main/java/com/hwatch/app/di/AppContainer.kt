package com.hwatch.app.di

import android.content.Context
import coil3.ImageLoader
import coil3.request.crossfade
import com.hwatch.app.BuildConfig
import com.hwatch.app.data.apiclient.HWatchApi
import com.hwatch.app.data.events.DownloadEventsTransport
import com.hwatch.app.data.events.OkHttpDownloadEventsTransport
import com.hwatch.app.data.local.ServerConfigStore
import com.hwatch.app.data.repository.DashboardRepository
import com.hwatch.app.data.repository.DownloadRepository
import com.hwatch.app.data.repository.LibraryRepository
import com.hwatch.app.data.repository.ServerConnectionRepository
import com.hwatch.app.data.repository.SettingsRepository
import com.hwatch.app.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * 应用级手动依赖容器。仅在 application 生命周期内存在。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val serverConfigStore: ServerConfigStore by lazy { ServerConfigStore(appContext) }

    val json: Json by lazy { NetworkModule.createJson() }

    /** App 级唯一 OkHttpClient：Retrofit 与 Coil 共享。 */
    private val okHttpClient: OkHttpClient by lazy {
        NetworkModule.createOkHttpClient(enableBodyLogging = BuildConfig.DEBUG)
    }

    /** 按 normalized baseUrl 缓存 Retrofit/HWatchApi。 */
    private val apiProvider: HWatchApiProvider by lazy {
        HWatchApiProvider(json = json, okHttpClient = okHttpClient)
    }

    /** 长视频流媒体专用客户端：共享连接池，仅放宽 timeout。 */
    private val streamingOkHttpClient: OkHttpClient by lazy {
        NetworkModule.deriveStreamingClient(okHttpClient)
    }

    /** Media3 ExoPlayer 工厂。 */
    val playerFactory: PlayerFactory by lazy {
        PlayerFactory(appContext, streamingOkHttpClient)
    }

    /** Server Setup 的“测试并连接”服务（复用共享 OkHttpClient）。 */
    val serverConnectionRepository: ServerConnectionRepository by lazy {
        ServerConnectionRepository(buildApi = apiProvider::getApiForRawUrl)
    }

    /** Dashboard 数据源；每次请求读取当前 baseUrl 与 translation_language。 */
    val dashboardRepository: DashboardRepository by lazy {
        DashboardRepository(
            apiProvider = ::currentApi,
            languageProvider = { serverConfigStore.translationLanguage.first() },
        )
    }

    /** 影视库数据源。 */
    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            apiProvider = ::currentApi,
            languageProvider = { serverConfigStore.translationLanguage.first() },
        )
    }

    /** 下载任务数据源。 */
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            apiProvider = ::currentApi,
            languageProvider = { serverConfigStore.translationLanguage.first() },
        )
    }

    /** SSE 传输（复用共享 streaming OkHttpClient）。 */
    val downloadEventsTransport: DownloadEventsTransport by lazy {
        OkHttpDownloadEventsTransport(streamingOkHttpClient)
    }

    /** 订阅数据源。 */
    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(apiProvider = ::currentApi)
    }

    /** 设置数据源。 */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(apiProvider = ::currentApi)
    }

    /** Coil ImageLoader：共享 OkHttpClient + SVG Decoder。 */
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(appContext)
            .components { CoilImageLoaderFactory.register(this, okHttpClient) }
            .crossfade(true)
            .build()
    }

    private suspend fun currentApi(): Result<HWatchApi> =
        apiProvider.getApiForRawUrl(serverConfigStore.serverBaseUrl.first())

    suspend fun currentBaseUrl(): String = serverConfigStore.serverBaseUrl.first()
}
