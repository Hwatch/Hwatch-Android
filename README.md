<div align="center">
  <img width="100" alt="HWatch" src="docs/icon.png" />
  <p><em>📱 Native Android client for HWatch — self-hosted NAS media subscription and Plex library manager (Kotlin + Jetpack Compose).</em></p>
  <p>English · <a href="README.zh-CN.md">简体中文</a></p>
  <div>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/Hwatch/Hwatch-Android" alt="License" /></a>
    <a href="https://github.com/Hwatch/Hwatch-Android/actions/workflows/build.yml"><img src="https://github.com/Hwatch/Hwatch-Android/actions/workflows/build.yml/badge.svg" alt="Build" /></a>
    <a href="https://github.com/Hwatch/Hwatch-Android/tags"><img src="https://img.shields.io/github/v/tag/Hwatch/Hwatch-Android?label=release" alt="Release" /></a>
    <img src="https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white" alt="Android 24+" />
    <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose Material 3" />
  </div>
</div>

Native Android client for the HWatch service: Kotlin + Jetpack Compose + Material 3.

The server API contract is defined solely by the `openapi.yaml` (machine-readable) and `API_V1.md` (human guide) in the HWatch server repository; this client never modifies the server and never invents undocumented endpoints or fields.

## Screenshots

> App screenshots are not checked in yet. Contributions of real-device screenshots are welcome.

## Features

- **Server setup**: enter a server address, test candidate URLs and save atomically (editable/replaceable, a failure never affects the current server).
- **Auth**: first-run admin setup, login, logout; `hwatch_session` cookie + `X-CSRF-Token`; sessions isolated per origin; encrypted persistence.
- **Dashboard**: server status, stats, storage/traffic, recent library/plays, subscription overview.
- **Library**: search, scope (all/translated/original), facets filtering, sorting, pagination, thumbnails.
- **Player**: Media3/ExoPlayer plays `/media/{id}`, with seek, double-tap ±10s, fullscreen, landscape and background pause.
- **Downloads**: full REST + SSE live status, progress, pause/resume/retry/delete, global pause/resume, download for undiscovered videos.
- **Subscriptions**: list, creator search, add, enable/disable, check, delete.
- **Settings**: download/schedule/storage/Plex/advanced (partial updates), account and logout.
- **UI**: Material 3, dark mode, phone/tablet responsive.

## Requirements

- JDK 17+ (development uses JDK 21)
- Android SDK (compileSdk 37, build-tools 36.0.0)
- Gradle wrapper (shipped in the repo, Gradle 9.6.0)
- AGP 9.4.0, Kotlin 2.4.20, Compose BOM 2026.08.00
- minSdk 24 / targetSdk 37

Set environment variables (example):

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
```

## Build & test

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # lint
./gradlew :app:assembleRelease      # release (R8 + resource shrink)
./gradlew :app:lintRelease
```

Artifacts: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Signing

Release signing reads `keystore.properties` from the repo root (that file and `*.jks`/`*.keystore` are ignored by `.gitignore`). When the file is absent it is skipped safely; `assembleDebug`/`testDebug`/`assembleRelease` are unaffected.

```properties
storeFile=/absolute/path/hwatch-release.jks
storePassword=...
keyAlias=hwatch
keyPassword=...
```

Once the file exists, `./gradlew :app:assembleRelease` produces a signed `app-release.apk`. Keep the keystore safe: future upgrades must use the same applicationId and signing certificate.

## Architecture

```
Compose Screen → ViewModel(StateFlow) → Repository → Retrofit/OkHttp / DataStore
```

- Manual dependency injection: `di/AppContainer` + `HWatchViewModelFactory` (no Hilt).
- Network: `di/NetworkModule` provides a shared `OkHttpClient` (Retrofit, Coil, Media3 streaming and SSE all derive from it); `di/HWatchApiProvider` caches Retrofit/HWatchApi by normalized baseUrl.
- Auth: `data/auth/*` (`AuthSessionManager`, `HWatchCookieJar`, `AuthInterceptor`, `EncryptedAuthSessionStore`).
- Models: `data/model/*` (kotlinx.serialization).
- UI: `ui/theme`, `ui/common` (design system / shared components), `ui/<feature>`.

Main directories:

```
app/src/main/java/com/hwatch/app/
  data/{apiclient,auth,events,local,model,repository}/
  di/
  ui/{auth,common,dashboard,downloads,library,main,navigation,player,server,settings,subscriptions,theme}/
  util/
```

## Security

- Session cookies are only sent to the current HWatch origin (scheme + host + effective port); the same host on a different port does not share them.
- The CSRF token is only injected into write requests (POST/PUT/PATCH/DELETE) for the current HWatch origin.
- Sessions are persisted encrypted with Android Keystore AES/GCM; passwords are never written to disk.
- Network logs only record redacted request lines; `/api/auth/*`, `/api/settings`, `/api/events`, `/media` and `/thumb` never have their bodies logged.
- `allowBackup=false`.

## Known limitations

- No Picture-in-Picture / background playback / MediaSession.
- Recovery / Backups / Health / Translations / Import endpoints are not integrated.
- Connections are unencrypted over `http://` (the UI warns about this).
- The subscription list shows `last_checked_at` in UTC.

See `RELEASE_NOTES_1.0.0.md` and `RELEASE_NOTES_1.0.1.md`.

## Disclaimer

- This is an open-source, **self-hosted** personal media management client. It does **not** host, bundle, mirror or distribute any media or third-party content.
- It is only meant to connect to a HWatch server that **you** deploy, and to access content you are authorized to access and choose to manage. You are solely responsible for complying with applicable laws and the terms of service of any site you access.
- This project is **not affiliated with, endorsed by, or sponsored by** any content source or platform.
- Intended for **adults only**. Do not use it to access, store or share content that is illegal in your jurisdiction or that infringes copyright.
- The software is provided **"as is"**, without warranty of any kind. The authors and contributors are not liable for any misuse, data loss or damages.
- Do not expose your HWatch server to the public internet without authentication and encryption.

## License

MIT License. See [LICENSE](LICENSE).
