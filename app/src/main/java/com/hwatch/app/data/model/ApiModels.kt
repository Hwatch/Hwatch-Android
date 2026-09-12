package com.hwatch.app.data.model

/**
 * HWatch HTTP API V1 数据模型。字段严格对应 `API_V1.md`，未知字段统一忽略。
 * 时间字段保留原始 RFC3339 字符串，解析交给 [com.hwatch.app.util.TimeUtils]。
 *
 * 注意：文档未给出完整 schema 的结构（如 `GET /api/tasks`、`dashboard.recently_played`）
 * 在 Phase 1 一律不建模，等待真实响应确认。
 */

enum class VideoStatus(val wire: String) {
    DISCOVERED("discovered"),
    QUEUED("queued"),
    WAITING("waiting"),
    ACTIVE("active"),
    PAUSED("paused"),
    FAILED("failed"),
    EXCLUDED("excluded"),
    COMPLETED("completed"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): VideoStatus =
            entries.firstOrNull { it.wire == value } ?: UNKNOWN
    }
}

/**
 * [VideoStatus] 的自定义序列化器：未知 wire 值（服务端未来新增状态）解码为
 * [VideoStatus.UNKNOWN]，而不是抛异常。不使用全局 `coerceInputValues=true`。
 */
object VideoStatusSerializer : kotlinx.serialization.KSerializer<VideoStatus> {

    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            "com.hwatch.app.data.model.VideoStatus",
            kotlinx.serialization.descriptors.PrimitiveKind.STRING,
        )

    override fun serialize(
        encoder: kotlinx.serialization.encoding.Encoder,
        value: VideoStatus,
    ) {
        encoder.encodeString(value.wire)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): VideoStatus =
        VideoStatus.fromWire(decoder.decodeString())
}

@kotlinx.serialization.Serializable
data class VideoDto(
    @kotlinx.serialization.SerialName("video_id") val videoId: String = "",
    @kotlinx.serialization.SerialName("subscription_slug") val subscriptionSlug: String = "",
    val title: String = "",
    @kotlinx.serialization.SerialName("translated_title") val translatedTitle: String = "",
    @kotlinx.serialization.SerialName("translated_tags") val translatedTags: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("translation_language") val translationLanguage: String = "",
    @kotlinx.serialization.SerialName("page_url") val pageUrl: String = "",
    @kotlinx.serialization.SerialName("published_at") val publishedAt: String = "",
    @kotlinx.serialization.SerialName("discovered_at") val discoveredAt: String = "",
    @kotlinx.serialization.SerialName("downloaded_at") val downloadedAt: String = "",
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String = "",
    @kotlinx.serialization.SerialName("thumbnail_url") val thumbnailUrl: String = "",
    val views: Long = 0,
    @kotlinx.serialization.SerialName("duration_seconds") val durationSeconds: Long = 0,
    @kotlinx.serialization.SerialName("creator_name") val creatorName: String = "",
    @kotlinx.serialization.SerialName("creator_url") val creatorUrl: String = "",
    @kotlinx.serialization.SerialName("creator_avatar") val creatorAvatar: String = "",
    val creators: List<CreatorRefDto> = emptyList(),
    @kotlinx.serialization.SerialName("source_relation") val sourceRelation: SourceRelationDto? = null,
    @kotlinx.serialization.SerialName("uploader_name") val uploaderName: String = "",
    @kotlinx.serialization.SerialName("uploader_url") val uploaderUrl: String = "",
    @kotlinx.serialization.SerialName("uploader_avatar") val uploaderAvatar: String = "",
    val categories: List<CategoryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    @kotlinx.serialization.SerialName("external_links") val externalLinks: List<ExternalLinkDto> = emptyList(),
    @kotlinx.serialization.SerialName("status")
    @kotlinx.serialization.Serializable(with = VideoStatusSerializer::class)
    val status: VideoStatus = VideoStatus.UNKNOWN,
    @kotlinx.serialization.SerialName("aria_gid") val ariaGid: String = "",
    @kotlinx.serialization.SerialName("downloader_id") val downloaderId: String = "",
    @kotlinx.serialization.SerialName("external_task_id") val externalTaskId: String = "",
    @kotlinx.serialization.SerialName("file_path") val filePath: String = "",
    @kotlinx.serialization.SerialName("plex_path") val plexPath: String = "",
    @kotlinx.serialization.SerialName("bytes_downloaded") val bytesDownloaded: Long = 0,
    @kotlinx.serialization.SerialName("total_bytes") val totalBytes: Long = 0,
    @kotlinx.serialization.SerialName("download_speed") val downloadSpeed: Long = 0,
    val connections: Int = 0,
    @kotlinx.serialization.SerialName("video_codec") val videoCodec: String = "",
    @kotlinx.serialization.SerialName("audio_codec") val audioCodec: String = "",
    val container: String = "",
    val width: Int = 0,
    val height: Int = 0,
    @kotlinx.serialization.SerialName("frame_rate") val frameRate: Double = 0.0,
    @kotlinx.serialization.SerialName("bit_rate") val bitRate: Long = 0,
    @kotlinx.serialization.SerialName("bit_depth") val bitDepth: Int = 0,
    @kotlinx.serialization.SerialName("media_probed_at") val mediaProbedAt: String = "",
    @kotlinx.serialization.SerialName("transcode_status") val transcodeStatus: String = "",
    @kotlinx.serialization.SerialName("plex_season") val plexSeason: Int = 0,
    @kotlinx.serialization.SerialName("plex_episode") val plexEpisode: Int = 0,
    val error: String = "",
)

/** `categories` 元素：key 为 PascalCase（Name/URL/Image）。 */
@kotlinx.serialization.Serializable
data class CategoryDto(
    @kotlinx.serialization.SerialName("Name") val name: String = "",
    @kotlinx.serialization.SerialName("URL") val url: String = "",
    @kotlinx.serialization.SerialName("Image") val image: String = "",
)

/** `tags` 元素：key 为 PascalCase（Name/URL）。 */
@kotlinx.serialization.Serializable
data class TagDto(
    @kotlinx.serialization.SerialName("Name") val name: String = "",
    @kotlinx.serialization.SerialName("URL") val url: String = "",
)

/**
 * `external_links` 元素：真实服务器返回对象数组，key 为 PascalCase（Name/URL/Image）。
 * 未知附加字段忽略。
 */
@kotlinx.serialization.Serializable
data class ExternalLinkDto(
    @kotlinx.serialization.SerialName("Name") val name: String = "",
    @kotlinx.serialization.SerialName("URL") val url: String = "",
    @kotlinx.serialization.SerialName("Image") val image: String = "",
)

/** `Video.creators` 元素：PascalCase（Name/URL/Image）。 */
@kotlinx.serialization.Serializable
data class CreatorRefDto(
    @kotlinx.serialization.SerialName("Name") val name: String = "",
    @kotlinx.serialization.SerialName("URL") val url: String = "",
    @kotlinx.serialization.SerialName("Image") val image: String = "",
)

/** `Video.source_relation`：记录视频由哪个订阅列表发现。 */
@kotlinx.serialization.Serializable
data class SourceRelationDto(
    val type: String = "",
    val platform: String = "",
    @kotlinx.serialization.SerialName("subscription_slug") val subscriptionSlug: String = "",
    @kotlinx.serialization.SerialName("subscription_url") val subscriptionUrl: String = "",
    @kotlinx.serialization.SerialName("creator_key") val creatorKey: String = "",
)

@kotlinx.serialization.Serializable
data class SubscriptionDto(
    val id: Long = 0,
    val slug: String = "",
    val platform: String = "",
    @kotlinx.serialization.SerialName("subscription_type") val subscriptionType: String = "",
    @kotlinx.serialization.SerialName("source_id") val sourceId: String = "",
    val name: String = "",
    val url: String = "",
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String = "",
    @kotlinx.serialization.SerialName("external_links") val externalLinks: List<ExternalLinkDto> = emptyList(),
    val enabled: Boolean = true,
    @kotlinx.serialization.SerialName("baseline_complete") val baselineComplete: Boolean = false,
    @kotlinx.serialization.SerialName("created_at") val createdAt: String = "",
    @kotlinx.serialization.SerialName("last_checked_at") val lastCheckedAt: String = "",
    @kotlinx.serialization.SerialName("last_error") val lastError: String = "",
    val progress: SubscriptionProgressDto? = null,
)

@kotlinx.serialization.Serializable
data class SubscriptionProgressDto(
    val total: Long = 0,
    val completed: Long = 0,
    val active: Long = 0,
    val waiting: Long = 0,
    val failed: Long = 0,
    @kotlinx.serialization.SerialName("bytes_downloaded") val bytesDownloaded: Long = 0,
    @kotlinx.serialization.SerialName("total_bytes") val totalBytes: Long = 0,
)

/**
 * `GET /api/dashboard` 响应。文档已明确声明全部字段；`videos` 与 `recently_played`
 * 复用公共 Video Contract，`subscriptions` 复用公共 Subscription 结构。
 * 字段缺失时使用 `emptyList()` 默认值，但元素内部 schema 错误仍会正常抛解析异常。
 */
@kotlinx.serialization.Serializable
data class DashboardDto(
    val subscriptions: List<SubscriptionDto> = emptyList(),
    val videos: List<VideoDto> = emptyList(),
    @kotlinx.serialization.SerialName("recently_played") val recentlyPlayed: List<VideoDto> = emptyList(),
    val counts: DashboardCountsDto? = null,
    @kotlinx.serialization.SerialName("traffic_bytes") val trafficBytes: Long = 0,
    @kotlinx.serialization.SerialName("library_bytes") val libraryBytes: Long = 0,
    @kotlinx.serialization.SerialName("current_download_speed") val currentDownloadSpeed: Long = 0,
    @kotlinx.serialization.SerialName("current_connections") val currentConnections: Int = 0,
    @kotlinx.serialization.SerialName("disk_total") val diskTotal: Long = 0,
    @kotlinx.serialization.SerialName("disk_free") val diskFree: Long = 0,
    @kotlinx.serialization.SerialName("aria2_online") val aria2Online: Boolean = false,
    @kotlinx.serialization.SerialName("downloads_paused") val downloadsPaused: Boolean = false,
    @kotlinx.serialization.SerialName("downloader_mode") val downloaderMode: String? = null,
    val downloaders: List<DownloaderDto> = emptyList(),
    val timezone: String = "UTC",
)

/** `dashboard.downloaders[]` / `health.downloaders[]`：Go 字段名（首字母大写）。 */
@kotlinx.serialization.Serializable
data class DownloaderDto(
    @kotlinx.serialization.SerialName("ID") val id: String = "",
    @kotlinx.serialization.SerialName("Name") val name: String = "",
    @kotlinx.serialization.SerialName("Version") val version: String = "",
    @kotlinx.serialization.SerialName("Error") val error: String = "",
    @kotlinx.serialization.SerialName("Online") val online: Boolean = false,
    @kotlinx.serialization.SerialName("SupportsMulti") val supportsMulti: Boolean = false,
    @kotlinx.serialization.SerialName("Active") val active: Long = 0,
    @kotlinx.serialization.SerialName("Connections") val connections: Long = 0,
    @kotlinx.serialization.SerialName("DownloadSpeed") val downloadSpeed: Long = 0,
    @kotlinx.serialization.SerialName("UptimeSeconds") val uptimeSeconds: Long = 0,
)

@kotlinx.serialization.Serializable
data class DashboardCountsDto(
    val videos: Long = 0,
    val active: Long = 0,
    val waiting: Long = 0,
    val completed: Long = 0,
    val discovered: Long = 0,
)

/** `GET /api/videos` 分页响应。 */
@kotlinx.serialization.Serializable
data class PagedResponse<T>(
    val items: List<T> = emptyList(),
    val total: Long = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

/** 仅含 `items` 的响应（订阅、平台、Creator 搜索）。 */
@kotlinx.serialization.Serializable
data class ItemsResponse<T>(
    val items: List<T> = emptyList(),
)

/** `GET /api/videos/facets` 响应。 */
@kotlinx.serialization.Serializable
data class FacetsDto(
    val creators: List<FacetDto> = emptyList(),
    val tags: List<FacetDto> = emptyList(),
    val codecs: List<FacetDto> = emptyList(),
)

@kotlinx.serialization.Serializable
data class FacetDto(
    val value: String = "",
    val count: Long = 0,
)

/** `POST /api/videos/download-all` 响应。 */
@kotlinx.serialization.Serializable
data class DownloadAllResponse(
    val accepted: Int = 0,
)

/** `POST /api/downloads/toggle` 请求：`{"paused":true|false}`。 */
@kotlinx.serialization.Serializable
data class ToggleDownloadsRequest(
    val paused: Boolean,
)

/** `POST /api/downloads/toggle` 响应（202）：`{"paused":true|false}`。 */
@kotlinx.serialization.Serializable
data class ToggleDownloadsResponse(
    val paused: Boolean = false,
)

/** 通用操作响应（`{"status":"accepted"}` / `{"status":"started"}`）。 */
@kotlinx.serialization.Serializable
data class ActionResponse(
    val status: String = "",
)

/** `GET /api/platforms` 元素。 */
@kotlinx.serialization.Serializable
data class PlatformDto(
    val id: String = "",
    val name: String = "",
)

/** `GET /api/creators/search` 元素。 */
@kotlinx.serialization.Serializable
data class CreatorDto(
    val platform: String = "",
    @kotlinx.serialization.SerialName("source_id") val sourceId: String = "",
    val name: String = "",
    val url: String = "",
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String = "",
    @kotlinx.serialization.SerialName("video_count") val videoCount: Long = 0,
)

@kotlinx.serialization.Serializable
data class CreateSubscriptionRequest(
    val url: String = "",
    @kotlinx.serialization.EncodeDefault val type: String = "creator",
    @kotlinx.serialization.SerialName("download_existing") val downloadExisting: Boolean = false,
)

/** `POST /api/subscriptions` 响应。 */
@kotlinx.serialization.Serializable
data class CreateSubscriptionResponse(
    val slug: String = "",
    val platform: String = "",
    val baselined: Long = 0,
    val queued: Long = 0,
)

@kotlinx.serialization.Serializable
data class UpdateSubscriptionRequest(
    val enabled: Boolean,
)

@kotlinx.serialization.Serializable
data class UpdateSubscriptionResponse(
    val enabled: Boolean = false,
)

/** `DELETE /api/subscriptions/{slug}` 响应。 */
@kotlinx.serialization.Serializable
data class DeleteResponse(
    val deleted: Boolean = false,
)

/**
 * `GET /api/settings` 响应。字段全为可空以兼容缺失；PUT 请使用 [UpdateSettingsRequest]。
 */
@kotlinx.serialization.Serializable
data class SettingsDto(
    @kotlinx.serialization.SerialName("http_proxy") val httpProxy: String? = null,
    @kotlinx.serialization.SerialName("download_dir") val downloadDir: String? = null,
    @kotlinx.serialization.SerialName("aria_rpc_url") val ariaRpcUrl: String? = null,
    @kotlinx.serialization.SerialName("aria_secret") val ariaSecret: String? = null,
    @kotlinx.serialization.SerialName("downloader_mode") val downloaderMode: String? = null,
    @kotlinx.serialization.SerialName("check_time") val checkTime: String? = null,
    val timezone: String? = null,
    @kotlinx.serialization.SerialName("plex_language") val plexLanguage: String? = null,
    @kotlinx.serialization.SerialName("max_concurrent") val maxConcurrent: Int? = null,
    @kotlinx.serialization.SerialName("connections_per_download") val connectionsPerDownload: Int? = null,
    @kotlinx.serialization.SerialName("download_limit_kbps") val downloadLimitKbps: Int? = null,
    @kotlinx.serialization.SerialName("min_free_bytes") val minFreeBytes: Long? = null,
    @kotlinx.serialization.SerialName("require_creator_match") val requireCreatorMatch: Boolean? = null,
    @kotlinx.serialization.SerialName("exclude_title_keywords") val excludeTitleKeywords: String? = null,
    @kotlinx.serialization.SerialName("exclude_uploaders") val excludeUploaders: String? = null,
    @kotlinx.serialization.SerialName("exclude_categories") val excludeCategories: String? = null,
    @kotlinx.serialization.SerialName("exclude_tags") val excludeTags: String? = null,
    @kotlinx.serialization.SerialName("max_duration_seconds") val maxDurationSeconds: Long? = null,
    @kotlinx.serialization.SerialName("backup_dir") val backupDir: String? = null,
    @kotlinx.serialization.SerialName("backup_time") val backupTime: String? = null,
    @kotlinx.serialization.SerialName("backup_retention") val backupRetention: Int? = null,
    @kotlinx.serialization.SerialName("backup_last_success") val backupLastSuccess: String? = null,
    @kotlinx.serialization.SerialName("backup_last_error") val backupLastError: String? = null,
)

/**
 * `PUT /api/settings` 请求。所有字段可空；配合 `encodeDefaults=false` 只发送用户实际修改的字段，
 * 实现文档定义的 partial merge。不要用 [SettingsDto] 全量回传。
 */
@kotlinx.serialization.Serializable
data class UpdateSettingsRequest(
    @kotlinx.serialization.SerialName("http_proxy") val httpProxy: String? = null,
    @kotlinx.serialization.SerialName("download_dir") val downloadDir: String? = null,
    @kotlinx.serialization.SerialName("aria_rpc_url") val ariaRpcUrl: String? = null,
    @kotlinx.serialization.SerialName("aria_secret") val ariaSecret: String? = null,
    @kotlinx.serialization.SerialName("downloader_mode") val downloaderMode: String? = null,
    @kotlinx.serialization.SerialName("check_time") val checkTime: String? = null,
    val timezone: String? = null,
    @kotlinx.serialization.SerialName("plex_language") val plexLanguage: String? = null,
    @kotlinx.serialization.SerialName("max_concurrent") val maxConcurrent: Int? = null,
    @kotlinx.serialization.SerialName("connections_per_download") val connectionsPerDownload: Int? = null,
    @kotlinx.serialization.SerialName("download_limit_kbps") val downloadLimitKbps: Int? = null,
    @kotlinx.serialization.SerialName("min_free_bytes") val minFreeBytes: Long? = null,
    @kotlinx.serialization.SerialName("require_creator_match") val requireCreatorMatch: Boolean? = null,
    @kotlinx.serialization.SerialName("exclude_title_keywords") val excludeTitleKeywords: String? = null,
    @kotlinx.serialization.SerialName("exclude_uploaders") val excludeUploaders: String? = null,
    @kotlinx.serialization.SerialName("exclude_categories") val excludeCategories: String? = null,
    @kotlinx.serialization.SerialName("exclude_tags") val excludeTags: String? = null,
    @kotlinx.serialization.SerialName("max_duration_seconds") val maxDurationSeconds: Long? = null,
    @kotlinx.serialization.SerialName("backup_dir") val backupDir: String? = null,
    @kotlinx.serialization.SerialName("backup_time") val backupTime: String? = null,
    @kotlinx.serialization.SerialName("backup_retention") val backupRetention: Int? = null,
    @kotlinx.serialization.SerialName("backup_last_success") val backupLastSuccess: String? = null,
    @kotlinx.serialization.SerialName("backup_last_error") val backupLastError: String? = null,
)