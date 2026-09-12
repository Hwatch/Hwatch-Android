# UI Phase API Notes

本文件仅记录 Android 客户端在真实响应中发现的、与 `HWatch HTTP API V1.md` 描述存在差异的字段，
以及 UI 的处理方式。**不修改正式 API 文档**（由服务端/Codex 单独维护）。

| 字段 | 真实值/结构 | UI 使用方式 |
|---|---|---|
| `external_links` | 对象数组：`[{"Image":"","Name":"x.com","URL":"https://..."}]`（PascalCase），非 `List<String>` | DTO 用 `ExternalLinkDto(Name/URL/Image)`；当前 UI 不展示，仅保证解析不失败 |
| `published_at` | 可能是日期型 `"2026-07-26"`，而非 RFC3339 Instant | DTO 保留 `String`；`TimeUtils.formatDisplayFromRfc3339` 兼容 `YYYY-MM-DD`（原样显示日期）与 RFC3339（按 `dashboard.timezone` 转换） |
| `downloads_paused` | `false`（Dashboard 顶层，文档未声明） | 不声明该字段，`Json(ignoreUnknownKeys=true)` 忽略；不据此判定服务器非法 |
| `categories` / `tags` | PascalCase `Name`/`URL`/`Image` | 已通过 `@SerialName` 映射，未知附加字段忽略 |

## 约束

- 不新增服务端 API；最近入库/最近播放缩略图使用现有 `GET /thumb/{video_id}`（`ThumbnailUrl` + 共享 Coil ImageLoader）。
- 若某 UI 项当前 API 无对应数据：隐藏该项或显示 unavailable，不发明字段。
- 发现新的 API 差异时，追加到本表：字段 / 实际值 / UI 使用方式。
