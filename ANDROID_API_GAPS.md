# Android API Gaps / 疑点记录

依据最新版 `APIV1.md`（唯一正式契约）。以下为 Android 端实现时发现的契约未明确点或取舍，
**未修改 APIV1.md**，仅记录，避免猜测。

## Downloads / SSE

- **SSE `tasks` 语义**：APIV1.md 明确 SSE `tasks` 只包含 `waiting`、`active`、`paused`（活动子集），
  且未使用 per-item delta 描述。Android 判定为 **SNAPSHOT（活动子集快照）**：
  合并时以 REST `/api/tasks` 为基线，命中 SSE 的项用 SSE 版本替换；基线中属于活动子集但不在 SSE
  中的项移除；非活动子集项（queued/failed/excluded/completed）保留。
  - 影响：`active → completed` 的任务会离开 SSE，UI 保留其最后状态直到下一次 REST 校准（PullToRefresh / 重连）。
  - `delete` 后立即触发一次 REST 校准，任务在服务端不再返回时消失（不做乐观删除）。
  - **Phase 8 收敛修复**：客户端跟踪 `previousActiveSseIds - currentActiveSseIds`，检测到任务离开活动子集时，
    触发一次**合并的** REST `/api/tasks` 校准（默认 1s 延迟，多个任务一起消失只发一次），由 REST 决定真实新状态；
    绝不本地推断 completed/failed/deleted。校准失败保留旧数据、不启动高速重试循环。
- **`GET /api/tasks` 全量**：文档 limit 最大 1000，Android 初版直接 `limit=1000` 全量加载，不做分页。
- **`POST /api/downloads/toggle`**：APIV1.md 已定义，但本次 Downloads V1 未提供全局暂停 UI（`downloads_paused`
  仅出现在 dashboard，SSE 不含该字段）。Android 暂未调用该 endpoint。
- **`/api/events` 响应 body 日志**：沿用 RedactingInterceptor 既有规则，永久禁止 body logging。

## Subscriptions

- **Creator `url` 可能为空**：`GET /api/creators/search` 的 `url` 可能为空字符串。创建订阅需要真实
  subscription URL，Android 不自行拼接，要求用户在 Add Subscription 中手动填写 URL（搜索结果可辅助填充）。
- **`last_checked_at` 时区**：Subscriptions 列表没有 `dashboard.timezone`，Android 以 UTC 显示该时间，
  避免使用设备时区。若需本地化，需要额外获取 dashboard timezone。
- **PATCH enabled**：APIV1.md 为同步返回 `{"enabled":bool}`，Android 以 response 为准更新。
- **check**：`POST /api/check[/{slug}]` 返回 `202 {"status":"started"}`，Android 仅提示“已提交检查”，
  不伪造 `last_checked_at`，由用户 refresh 获取真实结果。

## Settings

- **数值边界**：APIV1.md 未给出 max_concurrent / connections_per_download / download_limit_kbps /
  min_free_bytes / max_duration_seconds 的上下界。Android 只校验“非负整数/数字”与 `download_limit_kbps=0`
  合法，不擅自设定上限。
- **min_free_bytes**：UI 以 GB 输入并转换为 bytes（Long，防溢出）；请求仍发送 bytes。
- **更换服务器**：HWatch serverBaseUrl 是 Android 本地配置，不是 `/api/settings` 字段。Settings 的
  “更换服务器”会（确认后）清除本地 DataStore 地址并回到 Server Setup，重新测试成功后才保存；不会 PUT 到
  `/api/settings`。
- **敏感字段**：`aria_secret` / `http_proxy` 允许编辑，`aria_secret` 默认掩码 + 显示/隐藏；GET/PUT
  `/api/settings` body logging 永久关闭，不写日志/崩溃报告/analytics。
