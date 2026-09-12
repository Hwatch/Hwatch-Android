package com.hwatch.app.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * 本地服务器配置的抽象，便于 ViewModel 单元测试注入内存实现。
 * 生产实现为 [com.hwatch.app.data.local.ServerConfigStore]（DataStore）。
 */
interface ServerConfigRepository {

    val serverBaseUrl: Flow<String>

    val translationLanguage: Flow<String>

    suspend fun setServerBaseUrl(url: String)

    suspend fun setTranslationLanguage(language: String)
}
