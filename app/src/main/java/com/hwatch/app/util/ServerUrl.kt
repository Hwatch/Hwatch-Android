package com.hwatch.app.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 服务器地址规范化。
 *
 * - 自动补全缺失的 `http://` scheme（用户常直接输入 `192.168.1.100:8787`）。
 * - 规范化后以 `/` 结尾（Retrofit baseUrl 必需）。
 * - 仅接受 http/https。无效输入返回明确的失败信息，绝不 crash。
 * - 允许 IPv6、mDNS/.local、自定义 DNS、Tailscale/VPN 及 HTTPS 公网反代地址。
 */
object ServerUrl {

    fun normalize(raw: String): Result<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("服务器地址不能为空"))
        }
        val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
        val scheme = withScheme.substringBefore("://").lowercase()
        if (scheme != "http" && scheme != "https") {
            return Result.failure(IllegalArgumentException("仅支持 http / https 协议：$raw"))
        }
        val parsed = withScheme.toHttpUrlOrNull()
            ?: return Result.failure(IllegalArgumentException("无效的服务器地址：$raw"))
        return Result.success(parsed.toString())
    }
}