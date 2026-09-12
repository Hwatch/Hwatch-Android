package com.hwatch.app.ui.player

import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Player 路由参数。只携带必要字段，不塞完整 VideoDto。 */
@Serializable
data class PlayerArgs(
    val videoId: String,
    val displayTitle: String,
)

/**
 * Navigation 参数编码（与 HTTP API path 编码是两套问题）。
 *
 * 说明：Navigation 的 type-safe route 编码器在 Kotlin 中为 `internal`，无法在 JVM
 * 单测中验证。这里使用纯、可测的可靠方案：把参数序列化为 JSON 后做 URL-safe
 * Base64（无 padding）。产物只含 `[A-Za-z0-9_-]`，不包含 `/ ? # % :` 或空格，
 * 因此无论 Navigation 是否对参数做一次 URI 解码都不会损坏。
 */
object PlayerArgsCodec {

    private val json = Json { encodeDefaults = true }

    fun encode(args: PlayerArgs): String {
        val bytes = json.encodeToString(PlayerArgs.serializer(), args).toByteArray(Charsets.UTF_8)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun decode(token: String): PlayerArgs? = runCatching {
        val bytes = Base64.getUrlDecoder().decode(token)
        json.decodeFromString(PlayerArgs.serializer(), bytes.toString(Charsets.UTF_8))
    }.getOrNull()
}

/** Player destination 的 route 常量与构造。 */
object PlayerDestination {

    const val ARG = "playerArgs"
    const val ROUTE = "player/{$ARG}"

    fun route(videoId: String, displayTitle: String): String =
        "player/${PlayerArgsCodec.encode(PlayerArgs(videoId = videoId, displayTitle = displayTitle))}"
}
