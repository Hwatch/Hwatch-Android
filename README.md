# HWatch Android

HWatch 服务的原生 Android 客户端：Kotlin + Jetpack Compose + Material 3。

服务端接口契约以 HWatch 仓库的 `openapi.yaml`（机器可读）与 `API_V1.md`（人工指南）为唯一权威来源；
本客户端不修改服务端、不臆造未文档化的接口或字段。

## 功能

- **Server Setup**：输入服务器地址，测试候选地址成功后原子保存（可编辑/更换，失败不影响旧服务器）。
- **认证**：首次管理员初始化、登录、退出；`hwatch_session` Cookie + `X-CSRF-Token`；会话按 origin 隔离；加密持久化。
- **Dashboard**：服务器状态、统计、存储/流量、最近入库/播放、订阅概览。
- **影视库**：搜索、scope（全部/译文/原文）、Facets 筛选、排序、分页、缩略图。
- **播放器**：Media3/ExoPlayer 播放 `/media/{id}`，支持进度拖动、双击 ±10s、全屏、横屏、后台暂停。
- **下载**：REST 全量 + SSE 实时状态、进度、暂停/继续/重试/删除、全局暂停/继续、未下载视频下载。
- **订阅**：列表、creator 搜索、新增、启停、检查、删除。
- **设置**：下载/调度/存储/Plex/高级设置（局部更新）、账户与退出登录。
- **UI**：Material 3、深色模式、手机/平板响应式。

## 环境要求

- JDK 17+（开发环境使用 JDK 21）
- Android SDK（compileSdk 37、build-tools 36.0.0）
- Gradle Wrapper（仓库自带，Gradle 9.6.0）
- AGP 9.4.0、Kotlin 2.4.20、Compose BOM 2026.08.00
- minSdk 24 / targetSdk 37

设置环境变量（示例）：

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
```

## 构建与测试

```bash
./gradlew :app:assembleDebug        # Debug APK
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew :app:lintDebug            # Lint
./gradlew :app:assembleRelease      # Release（R8 + resource shrink）
./gradlew :app:lintRelease
```

产物：`app/build/outputs/apk/debug/app-debug.apk`、`app/build/outputs/apk/release/app-release-unsigned.apk`。

## 签名

Release 签名从仓库根目录的 `keystore.properties` 读取（该文件与 `*.jks`/`*.keystore` 已被 `.gitignore` 忽略）。
文件不存在时安全跳过，`assembleDebug`/`testDebug`/`assembleRelease` 均不受影响。

```properties
storeFile=/absolute/path/hwatch-release.jks
storePassword=...
keyAlias=hwatch
keyPassword=...
```

存在该文件后运行 `./gradlew :app:assembleRelease` 即产出已签名 `app-release.apk`。
请妥善保管 keystore：后续版本升级必须使用相同的 applicationId 与签名证书。

## 架构

```
Compose Screen → ViewModel(StateFlow) → Repository → Retrofit/OkHttp / DataStore
```

- 手动依赖注入：`di/AppContainer` + `HWatchViewModelFactory`（无 Hilt）。
- 网络：`di/NetworkModule` 提供共享 `OkHttpClient`（Retrofit、Coil、Media3 streaming、SSE 均派生自它）；
  `di/HWatchApiProvider` 按 normalized baseUrl 缓存 Retrofit/HWatchApi。
- 认证：`data/auth/*`（`AuthSessionManager`、`HWatchCookieJar`、`AuthInterceptor`、`EncryptedAuthSessionStore`）。
- 数据模型：`data/model/*`（kotlinx.serialization）。
- UI：`ui/theme`、`ui/common`（设计系统/公共组件）、`ui/<feature>`。

主要目录：

```
app/src/main/java/com/hwatch/app/
  data/{apiclient,auth,events,local,model,repository}/
  di/
  ui/{auth,common,dashboard,downloads,library,main,navigation,player,server,settings,subscriptions,theme}/
  util/
```

## 安全

- 会话 Cookie 仅发送到当前 HWatch origin（scheme + host + effective port）；同 host 不同端口不共享。
- CSRF token 仅对当前 HWatch origin 的写请求（POST/PUT/PATCH/DELETE）注入。
- 会话使用 Android Keystore AES/GCM 加密持久化；密码不落盘。
- 网络日志只记录脱敏请求行；`/api/auth/*`、`/api/settings`、`/api/events`、`/media`、`/thumb` 永不记录 body。
- `allowBackup=false`。

## 已知限制

- 无 Picture-in-Picture / 后台播放 / MediaSession。
- Recovery / Backups / Health / Translations / Import 等接口未接入。
- 使用 `http://` 时连接未加密（UI 有提示）。
- 订阅列表 `last_checked_at` 以 UTC 显示。

详见 `RELEASE_NOTES_1.0.0.md`、`ANDROID_API_GAPS.md`、`UI_PHASE_API_NOTES.md`。
