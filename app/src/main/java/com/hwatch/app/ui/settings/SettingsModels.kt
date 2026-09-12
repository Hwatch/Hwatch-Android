package com.hwatch.app.ui.settings

enum class SettingsError {
    UNREACHABLE,
    TIMEOUT,
    HTTP,
    PARSE,
}

/** 需要做输入校验的字段。 */
enum class SettingsField {
    MAX_CONCURRENT,
    CONNECTIONS_PER_DOWNLOAD,
    DOWNLOAD_LIMIT_KBPS,
    MIN_FREE_GB,
    TIMEZONE,
    CHECK_TIME,
    MAX_DURATION_SECONDS,
}

/** 编辑草稿：数值字段用字符串承载，避免直接 toInt() 崩溃。 */
data class SettingsDraft(
    val httpProxy: String = "",
    val downloadDir: String = "",
    val ariaRpcUrl: String = "",
    val ariaSecret: String = "",
    val checkTime: String = "",
    val timezone: String = "",
    val plexLanguage: String = "",
    val maxConcurrent: String = "",
    val connectionsPerDownload: String = "",
    val downloadLimitKbps: String = "",
    val minFreeGb: String = "",
    val requireCreatorMatch: Boolean = false,
    val excludeTitleKeywords: String = "",
    val excludeUploaders: String = "",
    val excludeCategories: String = "",
    val excludeTags: String = "",
    val maxDurationSeconds: String = "",
)

data class SettingsUiState(
    val draft: SettingsDraft = SettingsDraft(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadError: SettingsError? = null,
    val invalidFields: Set<SettingsField> = emptySet(),
    val hasChanges: Boolean = false,
    val saveSuccessId: Long = 0,
    val saveErrorId: Long = 0,
    val serverBaseUrl: String = "",
)
