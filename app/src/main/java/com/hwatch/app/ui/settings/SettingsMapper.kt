package com.hwatch.app.ui.settings

import com.hwatch.app.data.model.SettingsDto
import com.hwatch.app.data.model.UpdateSettingsRequest
import java.time.ZoneId
import java.util.Locale

/**
 * Settings GET/PUT 的纯映射与校验逻辑（可单测）：
 * - [toDraft] 从 GET 响应生成草稿
 * - [buildUpdateRequest] 只包含实际修改的字段（partial merge）
 * - [invalidFields] 数值/时区/时间格式校验，非法时禁止保存
 */
object SettingsMapper {

    const val BYTES_PER_GB = 1024L * 1024L * 1024L
    private val CHECK_TIME_REGEX = Regex("""([01]\d|2[0-3]):[0-5]\d""")

    fun toDraft(settings: SettingsDto): SettingsDraft = SettingsDraft(
        httpProxy = settings.httpProxy.orEmpty(),
        downloadDir = settings.downloadDir.orEmpty(),
        ariaRpcUrl = settings.ariaRpcUrl.orEmpty(),
        ariaSecret = settings.ariaSecret.orEmpty(),
        checkTime = settings.checkTime.orEmpty(),
        timezone = settings.timezone.orEmpty(),
        plexLanguage = settings.plexLanguage.orEmpty(),
        maxConcurrent = settings.maxConcurrent?.toString().orEmpty(),
        connectionsPerDownload = settings.connectionsPerDownload?.toString().orEmpty(),
        downloadLimitKbps = settings.downloadLimitKbps?.toString().orEmpty(),
        minFreeGb = settings.minFreeBytes?.let(::bytesToGbString).orEmpty(),
        requireCreatorMatch = settings.requireCreatorMatch ?: false,
        excludeTitleKeywords = settings.excludeTitleKeywords.orEmpty(),
        excludeUploaders = settings.excludeUploaders.orEmpty(),
        excludeCategories = settings.excludeCategories.orEmpty(),
        excludeTags = settings.excludeTags.orEmpty(),
        maxDurationSeconds = settings.maxDurationSeconds?.toString().orEmpty(),
    )

    fun invalidFields(draft: SettingsDraft): Set<SettingsField> {
        val invalid = mutableSetOf<SettingsField>()
        if (invalidNonNegativeInt(draft.maxConcurrent)) invalid += SettingsField.MAX_CONCURRENT
        if (invalidNonNegativeInt(draft.connectionsPerDownload)) invalid += SettingsField.CONNECTIONS_PER_DOWNLOAD
        if (invalidNonNegativeInt(draft.downloadLimitKbps)) invalid += SettingsField.DOWNLOAD_LIMIT_KBPS
        if (invalidNonNegativeLong(draft.maxDurationSeconds)) invalid += SettingsField.MAX_DURATION_SECONDS
        if (draft.minFreeGb.trim().isNotEmpty() && gbToBytes(draft.minFreeGb) == null) {
            invalid += SettingsField.MIN_FREE_GB
        }
        if (draft.timezone.trim().isNotEmpty() && !isValidTimezone(draft.timezone)) {
            invalid += SettingsField.TIMEZONE
        }
        if (draft.checkTime.trim().isNotEmpty() && !CHECK_TIME_REGEX.matches(draft.checkTime.trim())) {
            invalid += SettingsField.CHECK_TIME
        }
        return invalid
    }

    fun hasChanges(original: SettingsDto, draft: SettingsDraft): Boolean =
        buildUpdateRequest(original, draft) != UpdateSettingsRequest()

    fun buildUpdateRequest(original: SettingsDto, draft: SettingsDraft): UpdateSettingsRequest =
        UpdateSettingsRequest(
            httpProxy = changedString(original.httpProxy, draft.httpProxy),
            downloadDir = changedString(original.downloadDir, draft.downloadDir),
            ariaRpcUrl = changedString(original.ariaRpcUrl, draft.ariaRpcUrl),
            ariaSecret = changedString(original.ariaSecret, draft.ariaSecret),
            checkTime = changedString(original.checkTime, draft.checkTime),
            timezone = changedString(original.timezone, draft.timezone),
            plexLanguage = changedString(original.plexLanguage, draft.plexLanguage),
            maxConcurrent = changedInt(original.maxConcurrent, draft.maxConcurrent),
            connectionsPerDownload = changedInt(original.connectionsPerDownload, draft.connectionsPerDownload),
            downloadLimitKbps = changedInt(original.downloadLimitKbps, draft.downloadLimitKbps),
            minFreeBytes = changedLong(original.minFreeBytes, gbToBytes(draft.minFreeGb)),
            requireCreatorMatch = draft.requireCreatorMatch.takeIf { it != (original.requireCreatorMatch ?: false) },
            excludeTitleKeywords = changedString(original.excludeTitleKeywords, draft.excludeTitleKeywords),
            excludeUploaders = changedString(original.excludeUploaders, draft.excludeUploaders),
            excludeCategories = changedString(original.excludeCategories, draft.excludeCategories),
            excludeTags = changedString(original.excludeTags, draft.excludeTags),
            maxDurationSeconds = changedLong(original.maxDurationSeconds, draft.maxDurationSeconds.trim().toLongOrNull()),
        )

    fun isValidTimezone(value: String): Boolean =
        runCatching { ZoneId.of(value.trim()) }.isSuccess

    fun gbToBytes(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val gb = trimmed.toDoubleOrNull() ?: return null
        if (gb < 0 || !gb.isFinite()) return null
        val bytes = gb * BYTES_PER_GB.toDouble()
        if (bytes > Long.MAX_VALUE.toDouble()) return null
        return bytes.toLong()
    }

    fun bytesToGbString(bytes: Long): String =
        if (bytes % BYTES_PER_GB == 0L) {
            (bytes / BYTES_PER_GB).toString()
        } else {
            String.format(Locale.US, "%.2f", bytes.toDouble() / BYTES_PER_GB.toDouble())
        }

    private fun invalidNonNegativeInt(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.toIntOrNull()?.let { it >= 0 } != true
    }

    private fun invalidNonNegativeLong(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.toLongOrNull()?.let { it >= 0 } != true
    }

    private fun changedString(original: String?, value: String): String? {
        val trimmed = value.trim()
        return if (trimmed != original.orEmpty()) trimmed else null
    }

    private fun changedInt(original: Int?, value: String): Int? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val parsed = trimmed.toIntOrNull() ?: return null
        return if (parsed != original) parsed else null
    }

    private fun changedLong(original: Long?, value: Long?): Long? {
        if (value == null) return null
        return if (value != original) value else null
    }
}
