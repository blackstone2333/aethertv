# AERIAL-20261122-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review all 12 upstream commits after `0c750221` through `695e4a16` (v2.16). Adapt the MiTV backlight Binder slot compatibility and the screensaver large-clock typography fix without importing upstream version numbers, HOME writes, Lite sources or external icon-library APIs.

Acceptance: a MiTV firmware may expose `getAccessoryService("TvSettingService")` at transaction 80 or 81; the selected Binder must identify as `mitv.impl.tv.ITvSettingManager` before backlight reads/writes use it. When the Home clock is configured large, the screensaver clock uses the same `Type.Clock` cut/tracking before scaling; the small-clock path remains unchanged.

Owner scope: `system/MitvPanel.kt`, `ui/home/Screensaver.kt` and desktop review documents. Preserve `com.dangbei.tvlauncher`, `app.orchard.tv`, Java17/min21/Android5–6, root `:app`, own updates, free 4–8 columns, glass/control cards, aerial/category preferences, system tools and independent player launch/read-only migration v1 contracts. No player-worktree or shared-device operation, HOME/default/accessibility change, user-data rewrite, dependency/version change or formal Release.

## Plan

1. [x] Confirm independent desktop ownership, clean tree and no active build; fetch and inspect all 12 commits.
2. [x] Adapt the bounded Binder and screensaver fixes, preserving local capability guards and navigation.
3. [x] Run affected tests, Java17 project rules, dual Release builds and APK assertions.
4. [x] Record evidence and review desktop changes for the work-package commit and GitHub sync.

## Per-commit decisions

| Commit | Decision | Reason and boundary |
| --- | --- | --- |
| `4ade1eff` | Selected, adapted | Local `MitvPanel` already owns the same Binder protocol and Android 5/6 fallback. Try slots 80/81 and require the exact setting-manager descriptor before caching a slot. |
| `6c732000` | Deferred | The desktop has no corresponding battery status control; defer with the earlier battery package rather than add a new status receiver. |
| `500d4a5a` | Deferred | Local Guard already has installer and stock-home handling. A complete update-lifecycle fix needs usage-stat and background-update regression coverage; do not risk pulling a player back to HOME. |
| `f0f43427` | Deferred | Haier YunOS source routing is vendor-specific and lacks local target evidence; preserve existing source priority and fallback. |
| `e72f6ea4` | Selected, adapted | Local Home already exposes `AerialSettings.largeClock` and `Type.Clock`; use those metrics only for the large-clock screensaver path. |
| `40aae7be` | Skipped | README/projector device claims only. |
| `08173acf` | Skipped | Device notes only; no F3 evidence. |
| `1127f5e3` | Skipped | Skyworth device note only. |
| `5765de0d` | Skipped | TCL device/README documentation only. |
| `8469bc0d` | Skipped | Upstream triage handoff, not local evidence. |
| `22f9ad8d` | Skipped | Upstream 2.16/159 and Lite 1.39/41 numbering; local remains 0.3.1/106. |
| `695e4a16` | Skipped | Upstream release/site handoff only. |

## Status

Implementation, L0 verification and desktop diff review complete. Started clean at `199cfd7`, owned by black, with the desktop branch registered to its independent worktree and no active Gradle build. This package closes the bounded host-side integration; device acceptance remains open. Other worktrees and shared devices are untouched.

## Evidence

- Fetch exit 0, `0c750221` → `695e4a16`, 12 commits and v2.16 tag; behavior and dependencies reviewed individually.
- L0: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest`: exit 0, 147 cases per flavor, zero failures/errors/skips. This is host-side verification; no phone, emulator or TV was contacted.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`: exit 0; `git diff --check`: exit 0. No manifest, dependency, media, contract, resource or player-worktree change.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: exit 0, 100 actionable tasks (18 executed, 82 up-to-date); both lint gates, R8 and packaging passed.
- APK checker: exit 0. Both APKs retain `com.dangbei.tvlauncher`, full `app.orchard.tv.MainActivity`, versionCode 106, min21/target34, CONFIG receiver and signature-protected read-only migration provider. Standard has HOME and Guard has none. Signing certificate SHA256 remains `108a0bd59d7b0ce05fc52dbd43be95566db2d86912b499f3ebcf89fcec4f7217`. Root Gradle still loads only `:app`; DEX and R8 mappings contain no embedded player implementations.
- Standard `0.3.1`: 3,086,653 bytes, SHA256 `5ab6e4a2709de0654766d1e7d5fcdbeba0bfe2df45c48d2bb6823694c13c85ac`. Guard `0.3.1-guard`: 3,086,568 bytes, SHA256 `2adcbf7e7181cc30c8a410cf6152e2abc3d66f2eb3cb6396f4aa1cfe72e093a6`. Logs, test summary, manifest dumps and APK assertions are in ignored `local/upstream-review-20261122/`.
- F3/MiTV Binder behavior, screensaver appearance and remote navigation remain **未验证**; no device was contacted. No formal Release, default-HOME/accessibility change, signing-key change or user-data operation.

## Exact next action

On the next idle desktop maintenance run, fetch and review commits after `695e4a16`, retaining older deferred candidates. Coordinate a separate Release device run before claiming F3/MiTV backlight or screensaver appearance has been verified; no automatic shared-device operation is authorized by this package.

## Deferred

Update-after-install foreground protection, battery status, Haier input routing, wake chooser handling, Hisense HOME writes and external icon library need separate scoped integration. Earlier deferred candidates stay open; `695e4a16` is a review boundary, not a wholesale merge marker.
