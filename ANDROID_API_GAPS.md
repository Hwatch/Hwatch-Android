# Android API Gaps / 疑点记录

依据根目录 `openapi.yaml`（机器可读唯一契约）+ `API_V1.md`（人工阅读指南）。
**未修改服务端契约文件**，仅记录 Android 端取舍与未接入项。

## 1. 管理员认证（已实现）

服务端 `requireAuth` 包裹整个 mux：除 `/api/auth/status`、`/api/auth/setup`、`/api/auth/login`
外，所有 `/api/`、`/media/`、`/thumb/`（含 `/api/events`）要求 `hwatch_session` Cookie；
非 GET/HEAD/OPTIONS 还要求 `X-CSRF-Token`。

Android 实现（Phase 8.6）：
- `GET /api/auth/status` 判断 `configured` / `authenticated`。
- 未初始化 → Initial Admin Setup（`POST /api/auth/setup`，`{username,password}`，username 3–64 / password 10–256）。
- 已初始化无会话 → Login（`POST /api/auth/login`）。
- 会话/CSRF：`hwatch_session`（HttpOnly、SameSite=Strict、Path=/、7 天）由 `HWatchCookieJar` 按 origin 隔离；
  CSRF 从 login/setup/status/me 响应体 `csrf_token` 获取，`AuthInterceptor` 仅对当前 HWatch origin 的写请求注入。
- 401 → 全局清会话回 Login（login/setup/status 除外）。
- 持久化：`SESSION_PERSISTENCE = ENCRYPTED_PERSISTENT`（服务端 MaxAge=7d），Android Keystore AES/GCM + DataStore。
- 不再有认证 blocker。

## 2. Downloads / SSE

- **SSE `tasks` 语义（resolved）**：契约明确 SSE `tasks` 仅含 `waiting`/`active`/`paused` 活动子集，
  非 per-item delta。Android 判定 `SSE_TASK_SEMANTICS = SNAPSHOT`，REST `/api/tasks` 为基线。
- **收敛修复（Phase 8）**：跟踪 `previousActiveSseIds - currentActiveSseIds`，检测到任务离开活动子集时
  触发一次**合并的** REST `/api/tasks` 校准（默认 1s，多个一起消失只发一次），由 REST 决定真实新状态；
  不本地推断 completed/failed/deleted；校准失败保留旧数据、不高速重试。
- **`GET /api/tasks` 全量（resolved）**：响应 `{items:[Video],total,limit,offset}`，Task == Video；
  Android 直接 `limit=1000`（契约上限 1000）。
- **`POST /api/downloads/toggle`（resolved，已接入）**：`{"paused":bool}` → `202 {"paused":bool}`。
  Android 在 Downloads 顶部提供“暂停全部/继续全部”，通过 `GET /api/dashboard` 的 `downloads_paused`
  读取状态；202 响应即为新状态（同步语义），不做任务级 optimistic。
- **`/api/events` body 日志**：永久禁止（RedactingInterceptor 既有规则）。

## 3. Subscriptions

- **Creator `url` 可能为空**：`GET /api/creators/search` 的 `url` 可能为空。创建订阅需要真实 URL，
  Android 不自行拼接，要求用户手填（搜索结果可辅助填充）。
- **`last_checked_at` 时区**：Subscriptions 无 `dashboard.timezone`，Android 以 UTC 显示，避免设备时区。
- **PATCH enabled**：同步返回 `{"enabled":bool}`，以 response 为准。
- **check**：`202 {"status":"started"}`，仅提示“已提交检查”，不伪造 `last_checked_at`。

## 4. Settings

- **数值边界**：契约未给出 max_concurrent / connections_per_download / download_limit_kbps /
  min_free_bytes / max_duration_seconds 的上下界；Android 只校验“非负整数/数字”，`download_limit_kbps=0`
  合法，不擅自设上限。
- **新增字段（NEW_OPTIONAL，已解码未编辑）**：`downloader_mode`、`backup_dir`、`backup_time`、
  `backup_retention`、`backup_last_success`、`backup_last_error`。Android 1.0 不在 Settings UI 编辑它们。
- **更换服务器（resolved）**：采用候选模式——旧服务器保持有效，测试候选成功后原子替换 DataStore；
  失败/取消旧服务器不变（Phase 8）。
- **敏感字段**：`aria_secret` / `http_proxy` 掩码可编辑；GET/PUT `/api/settings` body logging 永久关闭。

## 5. 未接入的新 endpoint（NEW_OPTIONAL，不阻塞 Phase 9）

- `GET /api/recovery/latest` → `RecoveryStatus{available, summary{...}}`：数据库恢复协调摘要。
  当前 Android 1.0 无恢复管理功能，不接入；不在本地复制服务端恢复状态机。
- `GET/POST /api/backups`、`POST /api/backups/{name}/restore`：备份与恢复，未接入。
- `GET /api/health`：数据库/存储/队列/下载器健康；未接入（Dashboard 已展示部分统计）。
- `downloader_mode` / `downloaders[]`（dashboard）：多下载器（aria2/wget/auto）信息；未在 UI 展示。
- `GET /api/translations/export|import`、`POST /api/plex/refresh-nfo`、`POST /api/import/html`：
  非 1.0 范围。
- `GET /openapi.yaml`、`GET /docs`：公开文档端点，未接入。

## 6. 契约字段对齐（已实现）

- Error：`{"code","error"}`，Android `ErrorBody` 同时读取两者（继续用 `error` 展示）。
- Video：新增 `creators[]`、`source_relation{}`、`downloader_id`、`external_task_id`（已建模，UI 未用）。
- Dashboard：新增 `downloads_paused`、`downloader_mode`、`downloaders[]`（已建模）。
- Settings：新增 `downloader_mode`、`backup_*`（已建模）。
