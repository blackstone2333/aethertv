# AERIAL-20261121-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review all 15 upstream commits after `982f231` through `0c750221` (v2.14/v2.15). Extend the FengOS brightness protection from the previous package to firmware identified solely by `com.tvfengmi.launcher`, without requiring `cn.formovie.fengos.settings`.

Acceptance: either exact package blocks brightness reads and adjustments before resolver/vendor access; unrelated package names retain the existing backlight path. The already-integrated runtime brightness-card filter consumes this same capability check, without changing saved control layouts.

Owner scope: `system/SystemState.kt`, `system/FengOsBrightnessTest.kt` and desktop review documents. Previous package: `AERIAL-20261120-upstream-review`. Preserve applicationId `com.dangbei.tvlauncher`, namespace `app.orchard.tv`, Java17/min21/Android5–6, root `:app`, own updates, free 4–8 columns, glass/control cards, aerial/category preferences, system tools and independent player launch/read-only migration v1 contracts. No player-worktree or shared-device operation, HOME/default/accessibility change, user-data rewrite, dependency/version change or formal Release.

## Plan

1. [x] Confirm independent desktop ownership, clean tree and no active build; fetch and inspect all 15 commits.
2. [x] Extend exact-package detection and cover settings-only, launcher-only and unrelated-package behavior.
3. [x] Run affected Standard/Guard tests, Java17 project rules, dual Release builds and APK assertions.
4. [x] Record evidence and review desktop changes for the work-package commit and GitHub sync.

## Per-commit decisions

| Commit | Decision | Reason and boundary |
| --- | --- | --- |
| `a31d5f81` | Deferred | Fifteen one-second foreground rechecks and a global Back after wake/boot depend on the upstream chooser/Guard lifecycle. Requires separate Android5/6 lifecycle and coordinated target-device recovery evidence; no added polling or automatic dismissal of HOME choosers. |
| `8199f4ec` | Selected, adapted | Extend the existing capability check to the exact Fengmi launcher package. Reuse local early read/write guards and runtime control filtering; no brightness restoration writes, vendor settings routing, Lite product-line import or device-verification claim. |
| `6d639dc0` | Deferred with `1f65677` | Battery status was not imported in the previous package. Keep this plugged-in suppression fix with the future battery/control integration instead of adding a receiver or replacing customized status controls. |
| `d337d47d` | Skipped, local condition differs | Upstream moved About below PIN/Kids and therefore removed their scroll-to-bottom callbacks. Local LazyColumn still ends at PIN, has no Kids/About rows below it, and deliberately reveals its note; retain local navigation and modular About/settings design. |
| `4bb7b9b9` | Deferred | Broadens install-time HOME-property writes to empty Hisense properties and adds the matching restore sentinel. Treat install/restore together in a separately scoped HOME safety review; this heartbeat does not broaden automatic default-desktop changes. |
| `7cb25536` | Skipped | LeTV process-recycling/source-manager device notes only; no local target evidence or product change. |
| `dd2865d8` | Skipped | Xiaomi OS2 boot/PatchWall report only; does not establish F3 behavior or justify a new takeover path. |
| `1946b229` | Skipped | README support claim for a reflashed LeTV C1S using the upstream Lite product line. |
| `f8c75620` | Skipped | Upstream triage/verification handoff, not local evidence. |
| `7332f442` | Skipped | Upstream 2.14/156 and Lite 1.37/39 numbering; local stays 0.3.1/106. |
| `fae44715` | Skipped | Upstream Release/site/link handoff; no local publication or device claim. |
| `0e4e43c0` | Deferred with `c826ad4` | Searchable icon grid/names depend on upstream IconLibrary, its site/index/download APIs and phone endpoints, absent locally. Preserve local uploads, five-language phone UI and cache/network budget. |
| `c9a84b11` | Skipped | Upstream 2.15/157 and Lite 1.38/40 numbering. |
| `0c6d21c5` | Skipped | Upstream Release and device-check handoff only. |
| `0c750221` | Skipped | Upstream site deployment/index-name publication handoff only. |

## Status

Implementation, L0 verification and desktop diff review complete. Started clean at `75fa6c4`, owned by black, with the desktop branch registered to its independent worktree and no active Gradle build. This package closes the bounded host-side integration; device acceptance remains open. Other worktrees and shared devices are untouched.

## Evidence

- Fetch exit 0, `982f231` → `0c750221`, 15 commits and v2.14/v2.15 tags; behavior and dependencies reviewed individually.
- L0: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest --tests app.orchard.tv.system.FengOsBrightnessTest :app:testGuardDebugUnitTest --tests app.orchard.tv.system.FengOsBrightnessTest`: exit 0, three cases per flavor, zero failures/errors/skips. Tests use host-side Robolectric SDK23, not a phone, emulator or TV. Settings-only and launcher-only fixtures reject resolver access before every brightness read/up/down adjustment; unrelated packages retain the Xiaomi backlight path and do not write standard screen brightness.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`: exit 0. Final staged whitespace check covers the production/test changes and new work package. No UI text/resource, build-dependency, manifest, media or contract source changed.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: exit 0, 100 actionable tasks (18 executed, 82 up-to-date); both lint gates, R8 and APK packaging passed.
- APK checker, also with Java17: exit 0. Both APKs retain `com.dangbei.tvlauncher`, full `app.orchard.tv.MainActivity`, versionCode 106, min21/target34, CONFIG receiver and signature-protected read-only migration provider. Standard has HOME and Guard has none. Signing certificate SHA256 remains `108a0bd59d7b0ce05fc52dbd43be95566db2d86912b499f3ebcf89fcec4f7217`. Root Gradle still loads only `:app`; all DEX class definitions and R8 mappings contain no embedded player implementations.
- Standard `0.3.1`: 3,086,289 bytes, SHA256 `99cdcf0bde00e10e79d09d2c6e43d9319c072a1b335c6d903fc40428a8f836d3`. Guard `0.3.1-guard`: 3,086,206 bytes, SHA256 `dfc6c72e218e9fd71db9bc4bcceb707bdb46229fe51e5202d527dd84e989c829`. Logs, test XML/summary, manifest dumps, APK assertions and upstream review inputs are in ignored `local/upstream-review-20261121/`.
- F3/Fengmi/FengOS hardware behavior and runtime control appearance remain **未验证**; no device was contacted. No formal Release, default-HOME/accessibility change, brightness restoration write, signing-key change or user-data operation.

## Exact next action

On the next idle desktop maintenance run, fetch and review commits after `0c750221`, retaining older deferred candidates. Coordinate a separate Release device run before claiming F3/Fengmi brightness or control appearance has been verified; no automatic shared-device operation is authorized by this package.

## Deferred

Guard/HOME chooser recovery, paired Hisense install/restore changes, battery status and the external icon-library dependency need separate integration boundaries. Earlier deferred candidates stay open; `0c750221` is a review boundary, not a wholesale merge marker.
