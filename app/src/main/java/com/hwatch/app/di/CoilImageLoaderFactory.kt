package com.hwatch.app.di

import coil3.ComponentRegistry
import coil3.Uri
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import okhttp3.OkHttpClient

/** Coil 3 组件集合，抽成可测结构。 */
class CoilComponents(
    val decoders: List<Decoder.Factory>,
    val fetchers: List<Fetcher.Factory<Uri>>,
)

/**
 * Coil 3 组件配置：
 * - 注册 SVG Decoder（服务端缩略图不可用时返回 `image/svg+xml`，按 Content-Type 解码）。
 * - 网络层复用共享 [OkHttpClient]（连接池/DNS/timeout/cleartext 行为与 Retrofit 一致）。
 */
object CoilImageLoaderFactory {

    fun create(okHttpClient: OkHttpClient): CoilComponents = CoilComponents(
        decoders = listOf(SvgDecoder.Factory()),
        fetchers = listOf(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })),
    )

    fun register(builder: ComponentRegistry.Builder, okHttpClient: OkHttpClient) {
        val components = create(okHttpClient)
        components.decoders.forEach { decoder -> builder.add(decoder) }
        components.fetchers.forEach { fetcher -> builder.add(fetcher) }
    }
}
