# AERIAL-20261118-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review every upstream commit after `b7c3624` through `3d4571b` (15 commits, v2.1 included). Keep the desktop boundaries: `com.dangbei.tvlauncher`, namespace `app.orchard.tv`, Java 17, minSdk 21, root Gradle `:app`, local update channel, free 4–8 column layout, glass/control cards, system tools, aerial preferences and the independent-player contract.

Do not import Lite/player source, vendor takeover behavior, HOME/accessibility changes, release numbers, site/README/device claims, or operate shared devices.

## Plan

1. Confirm the desktop worktree is clean, owned by its branch, and not building.
2. Fetch upstream and inspect each commit's behavior and dependencies.
3. Select only capability-gated desktop fixes that preserve the local screensaver, input, settings, HOME and Android 5/6 boundaries.
4. Record every selection, deferral, skip, and verification boundary.
5. Run affected unit tests, the Java 17 rules check, both Release builds, and APK contract checks.

## Changes

Three bounded desktop adaptations are included:

- `44b2e1c`: choosing the firmware `System` screensaver now re-enables the firmware screensaver after `Never` had disabled it, gives the firmware dream back even when ownership was not recorded, and releases Home's `FLAG_KEEP_SCREEN_ON` when it is no longer needed. The local timer and Xiaomi/Android 5/6 screensaver paths remain intact.
- `69b3dc0`: adds capability-gated Baofeng AML_T962 settings/network and FUI source-bar fallbacks. Only exported, resolvable components are started; no source is selected automatically.
- `21e4a71`: treats the known TCL `com.tcl.sourcemananger` package as a firmware input panel so the local Inputs page is not drawn underneath it.

## Per-commit decisions

| Upstream commit | Decision | Reason and boundary |
| --- | --- | --- |
| `707851a` | Skipped | Lite-only TCL MStar source switcher; the desktop root does not load `lite/`. |
| `a8745bc` | Skipped | Lite-only TCL MStar port list. |
| `e34da46` | Skipped | Lite-only firmware source application. |
| `781c59b` | Skipped | Lite-only Home restore after a source switch. |
| `80f818c` | Skipped | Lite-only TCL storage compatibility. |
| `bb15cb6` | Skipped | Lite-only cursor lifecycle fix. |
| `21e4a71` | Selected, bounded | Capability-detects the known TCL source-manager package and leaves the remote key to the firmware panel. No generic package or HOME rule is added. |
| `69b3dc0` | Selected, bounded | Adds exported-component fallbacks for the reported Baofeng AML_T962 settings and source bar while preserving local settings/source pages and failure fallback. |
| `b69e4b8` | Deferred | Tencent/Skyworth Digital settings and wired page require vendor capability and Android 5/6 device evidence; no target device is available. |
| `44b2e1c` | Selected, bounded | Fixes the local `System`/`Never` screensaver state transition and stale keep-screen-on flag without changing the user's selected mode. |
| `76e1ef0` | Skipped | README/device claim only; not a desktop behavior or local verification fact. |
| `86bfc4f` | Deferred | App-side video poster extraction and AV1 software decoders can add synchronous work and memory pressure to the customized Lite/Full wallpaper path; requires a dedicated performance and F3 package. |
| `77ff37e` | Skipped | Handoff and device notes; local work packages and evidence remain authoritative. |
| `50056e3` | Skipped | Upstream product/Lite version metadata; local product remains 0.3.1 / versionCode 106. |
| `3d4571b` | Skipped | Upstream release handoff; no local source behavior to import. |

## Status

Active until Release and APK checks finish. Product source changes are limited to `MainActivity.kt`, `GuardSetup.kt`, `SystemState.kt`, and `HomeScreen.kt`; no player worktree, shared device, default-home setting, accessibility service, user data, signature, or `local/` file was touched.

## Evidence

- Worktree: clean before this review; desktop branch and ownership confirmed; no build process was active.
- Upstream fetch: exit 0, `b7c3624` → `3d4571b`, 15 commits reviewed; tag `v2.1` observed.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest`: pass (58 actionable tasks; both suites passed). One existing Android deprecation warning for `allNetworks` remains.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./tools/check-project-rules.sh`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: pass (100 actionable tasks; both lint gates and release packaging passed).
- APK checks: Standard and Guard are `com.dangbei.tvlauncher`, versionName `0.3.1` / `0.3.1-guard`, versionCode `106`, minSdk `21`, targetSdk `34`, with `app.orchard.tv.MainActivity`; Standard retains HOME and Guard has no HOME category; both APKs verify with the same certificate; no embedded player classes.
- Device: no Dangbei F3 or shared device operation; screensaver, TCL/Baofeng vendor panels, HOME, and remote behavior remain **未验证**.

## Exact next action

Review the final diff, commit the desktop-only changes, and push the desktop branch. Device behavior remains unverified until a separately coordinated F3 Release run.

## Deferred

The video poster/AV1 path, Tencent settings, all Lite changes, and previously open Guard/HOME, vendor, wallpaper, player, and release-metadata candidates remain open for dedicated work packages.
