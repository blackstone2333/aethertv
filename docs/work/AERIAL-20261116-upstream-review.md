# AERIAL-20261116-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

- Review every upstream commit after `50ba53c` through `d5a3473` (v1.99, 18 commits).
- Keep the fixed desktop boundaries: `com.dangbei.tvlauncher`, namespace `app.orchard.tv`, Java 17, minSdk 21, root Gradle `:app`, local update channel, free 4–8 column layout, glass/control cards, system tools, aerial preferences and independent-player contract.
- Do not import Lite/player source, vendor takeover behavior, HOME/accessibility changes, release numbers, site/README/device claims, or operate shared devices.

## Changes

Two upstream behaviors were adapted manually:

- `a66b3cb`: weather retries after three minutes following a failed foreground refresh, and HTTPS requests fall back to HTTP after either a TLS failure or a socket timeout. The existing diagnostics and Android 5/6 network boundary remain unchanged.
- `d5a3473`: the TV Personalize screen can hide the aerial/photo screensaver place or photo title. The preference is persisted, exposed through the ADB ConfigReceiver, included in diagnostics, and suppresses the label shade when no screensaver label remains. The local phone page keeps its existing compact settings surface; no web key table was changed.

`187daff` was reviewed but not copied: its `ScrollState` bring-into-view modifier belongs to the upstream settings sheet, while AetherTV uses a custom `LazyColumn` with independent glass groups and soft edges. Adding it would replace or bypass the local focus/layout behavior. The current local scroll implementation remains intact.

## Per-commit decisions

| Commit | Decision |
|---|---|
| `1ad4269` | Deferred: older Xiaomi MAIN-only app discovery needs a local catalog capability check and Android 5/6 fixture; does not affect the desktop surface this pass. |
| `5d52b73` | Deferred: MStar `tvsetting` source list is vendor-specific and requires capability detection plus target-device evidence. |
| `a66b3cb` | Selected: safe weather fallback and shorter retry window for old/slow TLS stacks. |
| `de1c002` | Deferred: Dangbei shutdown/Guard lifecycle changes HOME timing and needs DBF3 release regression. |
| `b30ed50` | Deferred: video container track fallback touches the independent screensaver decoder path and Lite memory budget. |
| `1ef2444` | Deferred: external-audio volume semantics change Control Center state and require remote/audio-device evidence. |
| `9f09982` | Deferred: LeTV picture settings are outside the detected local vendor capability set. |
| `b0edb57` | Deferred: locked-boot Guard diagnostics are coupled to the unmodified takeover chain. |
| `829cb68` | Deferred: LeTV source/network/picture routes require capability checks and a separate vendor work package. |
| `bbf3ba1` | Skipped: standalone Lite/TCL Android 4.x implementation is outside the desktop root and Java 17 boundary. |
| `5b6b502` | Skipped: Lite-only Android 4.1 track condition; no desktop source exists to adapt. |
| `187daff` | Deferred: upstream ScrollState modifier does not match the local LazyColumn/card architecture; no safe mechanical port. |
| `5ded240` | Skipped: documentation/device notes only; no product behavior or local evidence. |
| `425f542` | Skipped: upstream handoff record; local work package is authoritative. |
| `f5ea92e` | Skipped: upstream recheck record; no desktop source change. |
| `25f1251` | Skipped: upstream release/version metadata; product remains 0.3.1 / versionCode 106. |
| `b3abf8f` | Skipped: upstream release/site record; no formal release or channel change. |
| `d5a3473` | Selected in bounded form: TV-side place/title visibility setting, without importing the upstream phone-page table changes. |

## Verification

- `git diff --check`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk tools/check-project-rules.sh`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: pass; both release lint gates ran.
- APK checks: Standard and Guard are `com.dangbei.tvlauncher`, versionName `0.3.1`, versionCode `106`, minSdk `21`, targetSdk `34`, with `app.orchard.tv.MainActivity`; Standard retains HOME and Guard has no HOME category; both APKs verify with the same signing certificate; no embedded player classes.
- No F3 or shared device was connected. HOME, accessibility, vendor settings, user data, and player worktrees were not touched. Device behavior remains 未验证.

