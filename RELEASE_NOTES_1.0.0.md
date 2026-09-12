# HWatch Android 1.0.0

## Features
- Server setup（候选地址测试成功后原子保存）
- 管理员初始化 / 登录 / 会话（Cookie + CSRF）
- Dashboard（服务器状态、统计、存储、最近入库/播放、订阅概览）
- 影视库：搜索、scope、Facets 筛选、排序、分页
- 需认证的缩略图（Coil + SVG decoder，`/thumb/{id}`）
- Media3/ExoPlayer 原生播放（`/media/{id}`，Range）
- Downloads：REST 全量 + SSE 实时、进度、暂停/继续/重试/删除、全局暂停/继续
- Subscriptions：列表、creator 搜索、新增、启停、检查、删除
- Settings：下载/调度/存储/Plex/高级设置（partial PUT）、账户与退出登录
- Dark Mode、手机/平板响应式、统一 Material 3 设计系统

## Security
- 会话按 origin（scheme+host+effective port）隔离；同 host 不同端口不共享
- CSRF：仅对当前 HWatch origin 的写请求注入 `X-CSRF-Token`
- 加密持久会话（Android Keystore AES/GCM + DataStore）；密码不落盘
- Release 关闭 body 日志；`/api/auth/*`、`/api/settings`、`/api/events`、`/media`、`/thumb` 永不记录 body
- `allowBackup=false`（不备份会话/配置）

## Known Limitations
- 无 PiP / 无后台播放 / 无 MediaSession
- Recovery / Backup UI 未提供（服务端能力存在）
- 使用 `http://` 时连接未加密（UI 有提示）
- creator `url` 为空时需手动填写订阅地址
- 订阅列表 `last_checked_at` 以 UTC 显示
- MANUAL_RELEASE_SMOKE 未执行（无 adb/emulator）

## Build
- versionName 1.0.0 / versionCode 1
- minifyEnabled=true, shrinkResources=true
- Release 未签名（缺少用户正式 release keystore）：`HWatch-1.0.0-unsigned.apk`
