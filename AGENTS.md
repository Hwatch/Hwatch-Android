# AGENTS.md

本文件面向在本仓库工作的自动化/人类协作者。

## 项目

HWatch 服务的原生 Android 客户端（Kotlin + Jetpack Compose + Material 3，MVVM/Repository）。
包名/命名空间：`com.hwatch.app`。版本：`versionName 1.0.0` / `versionCode 1`。
minSdk 24、compileSdk/targetSdk 37。

## 环境

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
```

构建环境：JDK 21、Gradle 9.6.0（Wrapper）、AGP 9.4.0、Kotlin 2.4.20、Compose BOM 2026.08.00。

## 常用命令

```bash
./gradlew :app:testDebugUnitTest    # 单元测试（JVM）
./gradlew :app:assembleDebug        # Debug APK
./gradlew :app:lintDebug            # Lint（必须 0 errors）
./gradlew :app:assembleRelease      # Release（R8 + shrinkResources）
./gradlew :app:lintRelease
./gradlew clean
```

改动后至少运行 `testDebugUnitTest` + `assembleDebug` + `lintDebug`；涉及 release/依赖/Manifest 时还要 `assembleRelease`。

## 架构约定

```
Compose Screen → ViewModel(StateFlow) → Repository → Retrofit/OkHttp / DataStore
```

- 手动 DI：`di/AppContainer`、`di/HWatchViewModelFactory`。**不要引入 Hilt。**
- 网络单一来源：`di/NetworkModule` 的共享 `OkHttpClient`。Retrofit、Coil、Media3 streaming、SSE 都从它派生
  （`newBuilder()`），不要创建独立的 OkHttpClient / CookieJar / 连接池。
- API 缓存：`di/HWatchApiProvider`（按 normalized baseUrl）。
- 认证：`data/auth/*`。`AuthSessionManager` 是 session/CSRF 唯一来源；Cookie 按 origin 隔离；
  CSRF 由 `AuthInterceptor` 自动注入（GET/HEAD/OPTIONS 不加，公开 auth 路径不加）。
- 模型：`data/model/*`（kotlinx.serialization，`ignoreUnknownKeys=true`、`encodeDefaults=false`、`coerceInputValues=false`）。
- UI：`ui/common` 的设计系统与公共组件；`ui/theme` 主题。

## API 契约规则（重要）

- 唯一权威：HWatch 服务端的 `openapi.yaml` + `API_V1.md`。**禁止猜测未文档化的 schema/字段/endpoint。**
- 路径参数（`video_id`、slug）必须经 `util/UrlEncoding.encodePathSegment` + Retrofit `@Path(encoded=true)`，
  query 交给 `@Query`/HttpUrl；禁止裸字符串拼 URL，禁止二次编码。
- `202` 只表示已接受，禁止 optimistic 修改业务状态。
- 未知字段/未知枚举值要容错（`VideoStatus.UNKNOWN`）；不要用 `JsonElement` 做万能兼容。
- 发现契约差异记录到 `ANDROID_API_GAPS.md`，不要改服务端契约文件。

## 安全规则

- 不记录敏感信息：`password`、`aria_secret`、`http_proxy` 凭据、`hwatch_session`、`X-CSRF-Token`、
  Cookie/Set-Cookie、媒体路径。
- 永远禁止 body 日志：`/api/auth/*`、`/api/settings`、`/api/events`、`/media`、`/thumb`。
- Release 无 body 日志（`RedactingInterceptor` 由 `BuildConfig.DEBUG` 驱动）。
- 会话持久化只用 Android Keystore AES/GCM；密码绝不落盘。
- `allowBackup=false`，不要把 session/凭据加入任何备份。

## 禁止事项

- 新增框架：Hilt、Room、Paging 3、Domain/UseCase 大分层、MVI framework。
- 引入 Gson 或服务端生成的 Retrofit client 到 runtime（仅可只读用于交叉验证）。
- 新业务功能（本仓库已 FEATURE FREEZE）：Recovery/Backup/Health/Translations/Import UI、PiP、MediaSession、后台播放、Biometric/Passkey 等。
- 修改已冻结的 DataStore key（`serverBaseUrl`、`translation_language`、auth encrypted store key）。
- 提交密钥：`*.jks`、`*.keystore`、`keystore.properties`、`local.properties`、`*.apk` 均已在 `.gitignore`。

## 测试要求

- 已有 JVM 单测必须全部通过；新增/修改逻辑需补测试。
- 认证相关需覆盖：同 origin 带 Cookie、不同端口/host/scheme 不带、写请求加 CSRF、GET 不加、外域不带、
  401 清会话、login 401 不触发全局过期、切换服务器清旧会话、加密会话恢复/过期/解密失败安全回 Login。
- URL 编码覆盖 `:` `/` `%` `?` `#` 空格 中文，且不 double encode。

## 发布

- `versionName` 语义化递增，`versionCode` 单调递增，绝不回退。
- Release：`minifyEnabled=true`、`shrinkResources=true`、`proguard-android-optimize.txt` + `proguard-rules.pro`。
  只有 R8 实际报错才加最小 keep rule；不要 `-keep class ** { *; }`。
- 签名读取根目录 `keystore.properties`（缺失时安全跳过）。
- 发布产物命名：`HWatch-<version>-release.apk` / `-unsigned.apk` / `-debug.apk`，并记录 SHA-256。
