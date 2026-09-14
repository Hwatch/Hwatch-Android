# HWatch Android 1.0.1

## What's new
- **筛选体验**：影视库筛选底部弹层新增「已选」区、搜索框、按数量降序的 Top-N + 「展开全部/收起」，
  解决作者/标签过多时列表过长的问题（零后端改动，纯客户端本地过滤）。
- **连不上服务器时可切换服务器**：Dashboard / 影视库 / 下载 / 订阅 / 设置 的首屏网络错误
  （不可达 / 超时）以及播放器网络错误，除「重试」外新增「更换服务器」入口，可直接进入服务器设置。

## Security
- 与 1.0.0 一致：会话按 origin 隔离、CSRF 仅写请求注入、Keystore 加密持久会话、`allowBackup=false`、
  `/api/auth/*` 等永不记录 body。

## Known Limitations
- 无 PiP / 无后台播放 / 无 MediaSession
- Recovery / Backup UI 未提供
- `http://` 连接未加密（UI 有提示）
- MANUAL_RELEASE_SMOKE 未执行（无 adb/emulator）

## Build
- versionName 1.0.1 / versionCode 2
- minifyEnabled=true, shrinkResources=true
- Release 未签名（缺少正式 release keystore）：`HWatch-1.0.1-unsigned.apk`
