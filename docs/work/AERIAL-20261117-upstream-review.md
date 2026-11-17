# AERIAL-20261117-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review every upstream commit after `d5a3473` through `b7c3624` (11 commits, v2.0 metadata included). Keep the desktop boundaries: `com.dangbei.tvlauncher`, namespace `app.orchard.tv`, Java 17, minSdk 21, root Gradle `:app`, local update channel, free 4–8 column layout, glass/control cards, system tools, aerial preferences and the independent-player contract.

Do not import Lite/player source, vendor takeover behavior, HOME/accessibility changes, release numbers, site/README/device claims, or operate shared devices.

## Plan

1. Confirm the worktree is clean, owned by the desktop branch, and not building.
2. Fetch upstream and inspect each commit's behavior and dependencies.
3. Adapt only the input-key fallback that fits the existing source-panel capability chain.
4. Record every selection, deferral, skip, and verification boundary.
5. Run the affected unit test, Java 17 rules check, both Release builds, and APK contract checks.

## Changes

`c736096` was selected in a bounded form. When Android reports no TV inputs, the Home `KEYCODE_TV_INPUT` path now calls the existing capability-gated `SystemProbe.openInputPicker()` and closes the overlay only when the native panel launch succeeds; otherwise it keeps the existing Control Center Inputs page. The existing TCL firmware-panel guard remains unchanged. This matches the already-adapted Control Center Inputs button behavior without changing HOME, permissions, or source ordering.

## Per-commit decisions

| Upstream commit | Decision | Reason and boundary |
| --- | --- | --- |
| `c736096` | Selected, bounded | Adds the missing remote input-key fallback to the existing local native-panel capability chain. No new vendor package or automatic source switch is introduced. |
| `4090910` | Skipped | Lite-only clock synchronization; the desktop root does not load `lite/`. |
| `5216c78` | Deferred | Dream no-device Back filtering changes system screensaver lifecycle; requires Android 5/6 and target-device evidence before touching the local Dream path. |
| `f991579` | Deferred | New-process stock-launcher usage takeover changes Guard/HOME timing and can pull the user away from an active app; requires a dedicated safety review and Dangbei F3 Release evidence. |
| `fa39848` | Skipped | README/device report only; not a desktop behavior or local verification fact. |
| `efe171f` | Deferred | Chooser recovery changes HOME resolution semantics; keep the local default-home and original-launcher safety boundary until a dedicated Android 5/6 matrix exists. |
| `46ea8be` | Skipped | Lite-only clock synchronization; outside the desktop `:app` surface. |
| `3fe0372` | Skipped | Handoff and device notes; local work packages and evidence remain authoritative. |
| `b246e5b` | Deferred | keep_adb wake timing changes device settings behavior and must be reviewed with the existing Android 5/6 guard lifecycle; no shared device operation is authorized in this maintenance pass. |
| `4bace84` | Skipped | Upstream product/Lite version metadata; local product remains 0.3.1 / versionCode 106. |
| `b7c3624` | Skipped | Upstream release handoff and owner-device notes; no local source behavior to import. |

## Status

Complete. Only `HomeScreen.kt` changed in product source. No player worktree, shared device, default-home setting, accessibility service, user data, signature, or `local/` file was touched.

## Evidence

- Worktree: clean before the change; desktop branch and file ownership confirmed; no build process was active.
- Upstream fetch: exit 0, `d5a3473` → `b7c3624`, 11 commits reviewed.
- Source diff: one bounded `KEYCODE_TV_INPUT` fallback in `app/src/main/java/app/orchard/tv/ui/home/HomeScreen.kt`.
- `git diff --check`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./tools/check-project-rules.sh`: pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest`: pass (58 actionable tasks; both test suites passed).
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: pass (100 actionable tasks; both lint gates and release packaging passed).
- APK checks: Standard and Guard are `com.dangbei.tvlauncher`, versionName `0.3.1` / `0.3.1-guard`, versionCode `106`, minSdk `21`, targetSdk `34`, with `app.orchard.tv.MainActivity`; Standard retains HOME and Guard has no HOME category; both APKs verify with the same signing certificate; no embedded player classes.
- Device: no Dangbei F3 or shared device operation; HOME, remote, vendor panel, and source behavior remain **未验证**.

## Exact next action

Review the final diff, commit the desktop-only changes, and push the desktop branch. Device behavior remains unverified until a separately coordinated F3 Release run.

## Deferred

The previously open Guard/HOME, Dream, Lite, vendor-source, wallpaper, player, and release-metadata candidates remain open for dedicated work packages. No upstream v2.0 version number is copied into AetherTV.
