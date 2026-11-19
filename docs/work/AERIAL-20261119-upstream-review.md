# AERIAL-20261119-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review all ten commits from `3d4571b` through `29be8ad70ecb71ebae1e9388b23b5030221406cf` (v2.11). Select bounded weather refresh and TCL settings fixes without changing the customized desktop, user preferences, HOME/guard behavior or the independent-player boundary.

Acceptance: visible Home refreshes weather hourly, failures retry after 3/6/12/24/48/60 minutes, concurrent lifecycle/network events do not restart a reading, and paused Home has no refresh timer. Manual location changes replace the request and reschedule its next retry. TCL picture settings carry the firmware's string extras and retain existing priority and failure fallback.

Owner scope: `Weather.kt`, a provider-independent refresh scheduler, `MainActivity.kt` lifecycle calls, `SystemState.kt` TCL request extras, affected tests/test-only dependency, and desktop review documents. Keep applicationId `com.dangbei.tvlauncher`, namespace `app.orchard.tv`, Java17, minSdk21/Android5–6, root Gradle `:app`, own update channel, free 4–8 column layout, aerial preferences, glass, system tools and player v1 launch/read-only migration contracts. No player worktree changes, shared-device operations, formal Release, default HOME/accessibility changes or user-data removal.

Dependencies: previous review `AERIAL-20261118-upstream-review`; no player implementation or new runtime dependency.

## Plan

1. Verify desktop worktree/branch ownership, clean state and no active desktop build.
2. Fetch and inspect every upstream commit's changed files and implementation dependencies.
3. Adapt weather scheduling and TCL extras; cover timing, lifecycle, concurrency and fallback boundaries.
4. Run affected Standard/Guard tests, Java17 project rules, both Release builds and assert APK contracts.
5. Record final evidence, review desktop-only diff, commit and sync GitHub.

## Per-commit decisions

| Upstream commit | Decision | Reason and boundary |
| --- | --- | --- |
| `10eea15` | Deferred | Locked-boot activity-window filtering and unlock-time diagnostic initialization depend on upstream's guard recovery state. Requires a separate Android5/6 locked-boot/F3 safety review; no HOME or guard-chain changes here. |
| `7e31353` | Deferred | Coocaa elder-mode HOME recognition changes which foreground launcher Guard may replace. Requires capability detection and target-device evidence rather than importing upstream HOME rules. |
| `993840e` | Selected, bounded | Add `Type="Settings"` and string `flag="1"` to the existing capability-gated TCL picture-panel service request. Preserve Xiaomi-first priority, local settings layer and activity fallback; omit Lite and unrelated vendor paths. |
| `d8a557c` | Selected, adapted | Refresh while Home is resumed, cap exponential failure backoff at one hour and protect the in-flight request. A provider-independent scheduler re-arms after every result/location change so a failed manual refresh does not retain the old hourly wait. Preserve cache, chosen city, HTTPS timeout fallback and privacy-safe diagnostics; omit Lite. |
| `cb94c6a` | Deferred | The listener dump calls `HomeGuardService.diagnosticState()`, absent locally alongside the upstream recovery state it describes. Do not import that guard chain merely to add a dump; local `DiagLog.dump()` remains available. |
| `3d9dc99` | Skipped | Vidda device/README notes are not local verification evidence or desktop behavior. |
| `392783e` | Deferred | Automatically writes Whaley's power LED through a vendor API once per boot and depends on upstream BootOpen. Hardware behavior requires explicit device scope and evidence; not a generic desktop fix. |
| `f829e0f` | Skipped | Upstream handoff/device/architecture notes do not replace local ownership and evidence. |
| `6a55961` | Skipped | Upstream v2.11/152 and Lite version metadata; local test version stays 0.3.1/106. |
| `29be8ad` | Skipped | Release handoff only; no local behavior or release action. |

## Status

Implementation, L0 verification and desktop review complete. No devices were contacted and no other worktree was modified. The previous package's stale active/commit-next wording is corrected to its already-synced `ad0b710` checkpoint.

## Evidence

- Desktop worktree owned by `black`, correct independent branch, initially clean and without an active Gradle build.
- Upstream fetch exit 0; `3d4571b` → `29be8ad`, ten commits inspected, tag v2.11 observed.
- Cause/fix: weather previously checked staleness only on Home entry, missing retries while Home stayed visible. The resumed-only timer and bounded backoff fix that trigger; every result re-arms the timer and an in-flight request is protected. TCL's bare service request omitted the firmware settings key's string extras; adding them retains Xiaomi-first priority and the existing service-failure activity fallback.
- L0 unit evidence: 23 affected cases per flavor passed across the initial run and targeted correction (8 virtual-time weather tests, 4 TCL/Robolectric SDK23 cases, 11 existing XGIMI settings cases). Weather covers repeated hourly refresh, all retry intervals through the one-hour cap, pause/resume, repeated visibility events, in-flight protection, network recovery, success resetting backoff, manual rescheduling and cancellation.
- Initial command: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest --tests 'app.orchard.tv.weather.WeatherRefreshTest' --tests 'app.orchard.tv.system.TclPictureSettingsTest' --tests 'app.orchard.tv.system.XgimiSettingsTest'`. Standard also ran existing unaffected tests because Gradle applied the filter only to the following Guard task. One new TCL fixture initially assumed an unregistered generic settings activity was resolvable; it actually selected the existing last vendor fallback. The fixture now explicitly registers generic settings; product routing was not changed. Only that failed case was rerun for both flavors, with each task's filter explicit, exit 0. Other passed cases were not repeated. Test-only coroutines virtual time adds no Release dependency.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`: exit 0.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: exit 0; 100 actionable tasks, both lint gates and Release packaging passed. Existing `allNetworks` deprecation warning remains.
- Asserted APK inspection with aapt2/apksigner and all DEX class definitions plus R8 mappings: both `com.dangbei.tvlauncher`, `0.3.1` / `0.3.1-guard`, versionCode106, minSdk21/targetSdk34, complete `app.orchard.tv.MainActivity`; Standard HOME present / Guard HOME absent; CONFIG action and signature-protected read-only migration provider intact. Both use baseline signing certificate SHA256 `108a0bd59d7b0ce05fc52dbd43be95566db2d86912b499f3ebcf89fcec4f7217`; no embedded live/NAS player implementation classes. Root Gradle still loads only `:app`; media/contracts source unchanged.
- APK SHA256: Standard `d90367efc5370e7e97faff3bd6f5b889010e2c9ad026222ff321270b232a5508` (3,081,819 bytes); Guard `325816f07af6651883523a2aaf9cbfb36f3a87dbbd9b79533936a55d24457e5c` (3,081,766 bytes). Local test and assertion evidence is kept under ignored `local/upstream-review-20261119/`; no secrets or artifacts are committed.
- Device evidence: no F3/TCL or shared-device operation; actual panel display, HOME/remote behavior and F3 performance remain **未验证**.

## Exact next action

On the next idle desktop maintenance run, review commits after `29be8ad` and keep earlier deferred candidates open. F3/TCL display, remote and performance acceptance requires a separately coordinated Release device run; no automatic device operation is authorized by this maintenance package.

## Deferred

Locked-boot/Coocaa HOME changes, listener guard-state diagnostics and automatic Whaley LED writes require dedicated device/safety packages. Previously deferred guard, vendor, wallpaper/performance, Lite and player candidates remain open; advancing the review SHA does not close them.
