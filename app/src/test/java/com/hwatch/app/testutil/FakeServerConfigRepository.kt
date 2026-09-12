package com.hwatch.app.testutil

import com.hwatch.app.data.repository.ServerConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 内存版 [ServerConfigRepository]，记录最近一次保存的地址。 */
class FakeServerConfigRepository(
    initialBaseUrl: String = "",
    initialLanguage: String = "zh-CN",
) : ServerConfigRepository {

    private val baseUrl = MutableStateFlow(initialBaseUrl)
    private val language = MutableStateFlow(initialLanguage)

    var savedBaseUrl: String? = null
        private set

    val currentBaseUrl: String get() = baseUrl.value

    override val serverBaseUrl: Flow<String> = baseUrl

    override val translationLanguage: Flow<String> = language

    override suspend fun setServerBaseUrl(url: String) {
        savedBaseUrl = url
        baseUrl.value = url
    }

    override suspend fun setTranslationLanguage(language: String) {
        this.language.value = language
    }
}
