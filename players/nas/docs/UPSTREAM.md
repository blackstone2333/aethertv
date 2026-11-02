# Source baseline and modifications

AetherCinema is a focused derivative of [Jellyfin Android TV](https://github.com/jellyfin/jellyfin-androidtv), released here as a test build under GPL v2. The independent application root is `players/nas`; no Jellyfin SDK or decoder is added to AetherTV Desktop.

- Adopted stable tag: **v0.19.10**.
- Fixed upstream commit: **984181a3d6ab14e9a6d2dcc850c582e1c138bd95**.
- Reproducible source archive: `https://codeload.github.com/jellyfin/jellyfin-androidtv/tar.gz/984181a3d6ab14e9a6d2dcc850c582e1c138bd95`.
- Upstream declares JDK21, minSdk21, AGP8.11.1, Kotlin2.2.21, Media3 1.8.0 and SDK1.7.1. AetherCinema requires Android6/minSdk23, builds on JDK21, emits JVM17 bytecode and uses AGP8.12.0/Kotlin2.2.10/Media3 1.8.1. Its complete dependency graph is checked by Android manifest merging; the app does not silently increase minSdk.
- Historical comparison: v0.16.11 / `a257651f8ef28f85d85fa0f12e589ec144294f7f` explicitly uses JDK17. v0.17.9 already requires JDK21. The user explicitly authorized JDK21 for this independent app on 2026-10-28; Desktop remains JDK17.

## Adopted paths

Unmodified reference copies and contributor attribution are retained in `upstream/reference/`. The full upstream GPL is retained as `LICENSE` and bundled with the APK.

| Upstream source | AetherCinema implementation | Changes |
| --- | --- | --- |
| `playback/jellyfin/src/main/kotlin/mediastream/JellyfinMediaStreamResolver.kt` | `cinema/Protocol.kt` `BaseProvider.negotiate` | Port direct play → remux → transcode choice and server streaming endpoint selection. Replace SDK DTOs with bounded JSON transport; reject foreign negotiated origins, retain reverse proxy prefixes and opaque item IDs. |
| `playback/jellyfin/src/main/kotlin/playsession/PlaySessionService.kt` | `cinema/Protocol.kt` `report`, `cinema/PlayerScreen.kt` | Port start/progress/stop and tick semantics into one foreground decoder. Ordered bounded reporting queue, local resume checkpoint, final stop and own encoding cleanup. |
| `app/src/main/java/org/jellyfin/androidtv/util/profile/deviceProfile.kt` | `cinema/Protocol.kt` `deviceProfile` | Replace broad upstream codec preferences with conservative AVC/AAC, 1080p and HLS conversion, external text subtitles and server burn-in choices. F3 capability claims remain unverified. |
| `app/src/main/java/org/jellyfin/androidtv/util/TimeUtils.java` | `cinema/CinemaActivity.kt` `formatMillis` | Port duration formatter only, locale-stable. |

AetherTV Pressable and shared theme constants retain the original MIT copyright notice in bundled `licenses/AetherTV-MIT.txt` (Copyright2026 pengchujin).

The upstream Leanback UI, analytics/crash reporting, automatic discovery, account database, WebSocket remote control, background services, FFmpeg native decoder and broad plugin framework are not bundled. TV UI is rebuilt with Compose, project palette/motion and the AetherTV Pressable behavior copied into this independent root. The source is a real port of upstream playback logic, not an upstream APK with a new label, and not a claim that the entire upstream application is compiled unchanged.

Emby has its own provider for `/emby` paths, `X-Emby-Authorization` / `X-Emby-Token` and legacy password hashing fields, checked against the official Java API client `MediaBrowser/Emby.ApiClient.Java` (`BaseApiClient.GetApiUrl`, `ApiClient.AuthenticateUserAsync`). JSON DTOs shared by both protocols are parsed without UUID coercion. Runtime compatibility still requires real-server verification.

MoonTV uses a separately implemented Cookie/API adapter, based on the deployed user-authorized web client. No MoonTV web code is copied into the APK; repository availability and observed protocol fingerprints are documented separately.

## Rebuilding

Run `tools/build.sh`. It requires JDK21 and Android SDK36. Release is a **test APK** signed with the machine's Android debug/test key; it is not a production release. Build output and runtime downloads are ignored. Retain this entire source root, upstream references, GPL, Gradle wrapper/configuration and third-party notices when distributing the corresponding APK.
