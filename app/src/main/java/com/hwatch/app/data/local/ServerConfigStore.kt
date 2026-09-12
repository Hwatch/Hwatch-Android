package com.hwatch.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hwatch.app.data.repository.ServerConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.serverConfigDataStore by preferencesDataStore(name = "server_config")

/**
 * 客户端本地配置（DataStore Preferences）。
 *
 * - `serverBaseUrl`：用户填写的服务器地址，如 `http://192.168.1.100:8787`
 * - `translationLanguage`：译文语言，默认 `zh-CN`
 */
class ServerConfigStore(context: Context) : ServerConfigRepository {

    private val dataStore = context.applicationContext.serverConfigDataStore

    override val serverBaseUrl: Flow<String> = dataStore.data
        .map { prefs -> prefs[KEY_SERVER_BASE_URL] ?: "" }

    override val translationLanguage: Flow<String> = dataStore.data
        .map { prefs -> prefs[KEY_TRANSLATION_LANGUAGE] ?: DEFAULT_LANGUAGE }

    override suspend fun setServerBaseUrl(url: String) {
        dataStore.edit { prefs -> prefs[KEY_SERVER_BASE_URL] = url }
    }

    override suspend fun setTranslationLanguage(language: String) {
        dataStore.edit { prefs -> prefs[KEY_TRANSLATION_LANGUAGE] = language }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "zh-CN"

        private val KEY_SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        private val KEY_TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
    }
}