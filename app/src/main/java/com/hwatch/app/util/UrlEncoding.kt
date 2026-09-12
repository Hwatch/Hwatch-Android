package com.hwatch.app.util

/**
 * 按 HWatch HTTP API V1 契约对路径段（video_id / subscription slug）进行编码。
 *
 * 契约要求 `source:3144962` 编码为 `source%3A3144962`、`source:creator` 编码为
 * `source%3Acreator`（冒号不允许保留在路径中）。这里只保留 RFC 3986 unreserved
 * 字符（A-Za-z0-9 - _ . ~），其余字符按 UTF-8 字节编码为 `%XX`。
 *
 * 该结果配合 Retrofit `@Path(encoded = true)` 使用，避免 Retrofit 再次编码造成
 * 双编码（`%` 被编码成 `%25`）。
 */
object UrlEncoding {

    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private val HEX = "0123456789ABCDEF".toCharArray()

    fun encodePathSegment(input: String): String {
        val builder = StringBuilder(input.length * 3)
        for (ch in input) {
            if (ch in UNRESERVED) {
                builder.append(ch)
            } else {
                for (byte in ch.toString().toByteArray(Charsets.UTF_8)) {
                    builder.append('%')
                    builder.append(HEX[(byte.toInt() ushr 4) and 0xF])
                    builder.append(HEX[byte.toInt() and 0xF])
                }
            }
        }
        return builder.toString()
    }
}