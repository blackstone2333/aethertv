# AetherLive

Independent foreground IPTV companion for AetherTV. Package `app.aethertv.live`, namespace `app.orchard.tv`, version `0.1.0` / `1`, minSdk 21. No bundled channels, boot receiver, HOME category, accessibility service, background playback or launcher dependency.

## Build

Use Java 17 and an Android SDK. This directory is a standalone Gradle root with its own wrapper, settings, project cache and outputs. From the repository root:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk players/live/gradlew -p players/live --no-daemon testReleaseUnitTest assembleRelease
```

Alternatively, run `./gradlew --no-daemon testReleaseUnitTest assembleRelease` inside `players/live`. This loads only the `AetherLive` root project, never the desktop `:app` or NAS project. The inherited repository-root build belongs to the desktop Owner; do not invoke it for player work.

Output: `players/live/build/outputs/apk/release/AetherLive-release.apk`. Release signing retains the existing strategy: use repository `local/signing.properties` and its referenced key when privately provisioned, otherwise the same machine's Android debug key for test builds. The isolation checkpoint copies no keys, signing properties, local configuration, test data or existing build outputs. Only matching signatures can import desktop configuration.

`contracts/` is a frozen MediaContract v1 source snapshot, compiled directly without loading another Gradle project. Do not edit it in this workspace. Propose protocol changes to the desktop Owner in the player work package before updating either implementation.

The independent worktree is `/Users/black/.codex/worktrees/aetherlive-independent/AetherTV`, branch `codex/aerial/aerial-20261102-live-independent`, based on `5edcc5bd2b57a9061a4478a682f4903d1369790b`. Only the player's source and independent-media work/handoff are owned here. Original phone evidence remains in the original checkout's ignored `local/`; it is not copied or published.

## Setup and controls

Open Playlist settings, choose Add playlist, optionally give it a name, then enter an HTTP(S) M3U or TXT URL and optionally an XMLTV / gzip EPG URL, then Save. Select an existing source to edit, enable/disable or remove it; removal requires a second confirmation. Existing request headers, priority and timeout survive editing. Name, URL and enabled changes are saved together; Back or Cancel discards the draft and restores the originating focus. Back returns to playback; returning from settings reloads the configuration.

Import from AetherTV explicitly requests its read-only signature-protected export. The player validates the whole document before adding missing IDs. Repeating import leaves existing player entries unchanged. The desktop's original preferences and caches are preserved. Different-signature installations cannot migrate; re-enter their source URLs in the player instead.

OK opens/selects the channel list, Up/Down switches while video has focus, Menu toggles the list, and Back closes the list before exiting. Touch supports the same actions through the list/HUD. The last channel persists; one foreground Media3 decoder is released synchronously on pause, including when opening settings or returning to the desktop. Failed routes advance through a finite list; Retry explicitly restarts the list.

Only HTTP(S) playback is enabled in this companion. Parsing retains common IPTV URL schemes for compatibility, but multicast/RTSP playback is outside the verified transport scope.

## Upstream attribution

Selective adaptation of [lizongying/my-tv-0](https://github.com/lizongying/my-tv-0) at commit `19ab99c4c2f819fdfd08b09f64219bb7cd93eaa1` (MIT, Copyright 2025 李宗英). `MainViewModel.kt` TXT import semantics (`#genre#`, comma-separated routes and duplicate merging) are adapted into `M3uParser.kt` with bounded inputs, URL validation and regression tests. UI, configuration and playback remain this project's Compose/Media3 implementation. This is not a complete fork of upstream features. The MIT license is included under `third-party/` and in APK assets. FFmpeg AARs, upstream update channels, proxy settings and global codec overrides are not included. Inter uses the repository's OFL font; see `FONT-LICENSE-OFL.txt` at the root.

## Validation

See `docs/work/AERIAL-20261027-independent-media.md` for current evidence. A successful build or phone test does not validate F3 installation whitelist, remote behavior or MStar decoding.


## Materials and navigation

The UI follows the project Apple-inspired design system: distinct playing/selected/focused states, large type, grouped configuration and interruptible focus springs. Settings uses one immutable 768px scene with bounded cached lenses; video controls refract the current OES video texture in the same fullscreen pass, with directional edge highlights. No screenshots or extra decoder are used. A failed GL initialization falls back to SurfaceView with a readable translucent plate. Reduced motion follows Android animator settings. Static scenes have no idle animation. The desktop's material code and preferences remain separately owned.
