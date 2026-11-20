# AERIAL-20261120-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review all 25 upstream commits after `29be8ad` through `982f231` (v2.12/v2.13). Adapt UTC daily-photo freshness, long-side upload sampling, FengOS brightness protection, accessible controls, capability-gated Baofeng app discovery, consistent screensaver clock metrics and the five-language phone weather-location hint.

Acceptance: Bing photos expire by their UTC start rather than a mismatched market date; tall uploads are sampled before decoding; detected FengOS settings firmware cannot read/adjust brightness or show a brightness control; accessibility actions expose labels and cannot activate disabled controls; remote key-up still requires a press started on that control. MAIN-only Baofeng entries respect exported/enabled checks and existing launcher priority. Screensaver time uses the same base text metrics as the carried Home clock.

Owner scope: desktop `app/**` affected helpers/components, five string-resource sets, regression tests and these review documents. No player worktree, device operation, data cleanup, HOME/default/accessibility configuration, publication or runtime dependency change. Preserve `com.dangbei.tvlauncher`, `app.orchard.tv`, Java17/min21, root `:app`, own updates, free 4–8 columns, glass/control cards, aerial/category preferences and player launch/read-only migration v1 contracts. Previous package: `AERIAL-20261119-upstream-review`.

## Plan

1. [x] Confirm independent desktop ownership, clean tree and no active build; fetch and inspect all changes.
2. [x] Adapt bounded fixes to local components and cover logic/input/safety boundaries.
3. [x] Run affected Standard/Guard tests, Java17 rules, both Release builds and APK assertions.
4. [x] Record evidence and review only desktop changes for the work-package commit and GitHub sync.

## Per-commit decisions

| Commit | Decision | Reason and boundary |
| --- | --- | --- |
| `db24510` | Deferred | Output diagnostics depend on upstream OutputDevice classification, absent locally. Do not import box/TV volume or brightness policy implicitly; a future read-only report adaptation can separate facts from classification. |
| `8c0346d` | Selected, adapted | Button/label/click semantics, app-name/open/menu semantics, volume/brightness and PIN-delete labels; adapt to local controls, explicitly reject disabled actions, preserve remote press ownership. No accessibility-service settings are changed. |
| `a119447` | Skipped | About-page arrangement is superseded by `57cf4b9` upstream and conflicts with local modular settings/control navigation. |
| `4c7b377` | Deferred | Remote standby and a sleep screensaver depend on Android9+ guard global actions, change device power behavior and require coordinated target-device recovery testing. F3 is Android6; keep its modes and timer intact. |
| `c826ad4` | Deferred | Local desktop has no upstream IconLibrary/site dependency; preserve local icon uploads and cache budget rather than introduce an automatic external library. |
| `1f65677` | Deferred | Battery badge/receiver needs integration with independently configurable status controls and compact layouts; no wholesale status-pill replacement. No battery-powered target evidence in this package. |
| `57cf4b9` | Skipped | Upstream's final About/update relocation would replace local modular settings and return-focus behavior. Preserve own update channel and existing layout. |
| `2e0685b` | Skipped | Upstream 2.12/Lite numbering; local version stays 0.3.1/106. |
| `57b25d0` | Skipped | Release handoff; not local verification. |
| `8932803` | Skipped | Release-link handoff only. |
| `1c4b7a9` | Selected, bounded | Detect the FengOS settings package, block brightness reads/writes before resolver/vendor access and omit the local brightness card without changing saved layout. No upstream vendor settings routing or restoration writes. |
| `f39a5a2` | Selected, adapted | Persist UTC fullstartdate and use a strict 24-hour validity window; ignore malformed/future starts and refetch legacy indices without them. Missing cached image files do not count as current. Preserve date IDs, categories and cache retry policy. |
| `affd296` | Deferred | Dump depends on upstream guard diagnosticState(), still absent locally. Do not import the upstream recovery state solely for this read-only endpoint. |
| `cba1725` | Selected, bounded | Lay out screensaver time at Home's base size and scale at the selected corner; preserve existing saver mode, captions, transition and clock preference. |
| `e468dce` | Selected, bounded | Sample upload decode by the longest side, preserving the existing power-of-two quality threshold; no existing user photos are rewritten. |
| `cbfe660` | Selected, bounded | Add the two exact Baofeng MAIN-only activities to the existing exported/enabled discovery fallback; existing launcher entries win, no pinning or HOME/source switching. |
| `2227656` | Skipped | README device claim only. |
| `434f990` | Skipped | ADB/Hisense device notes do not establish local behavior or authorize device operations. |
| `b16ed76` | Skipped | Upstream triage handoff only. |
| `9e3d11a` | Selected, bounded | Update the existing wx_guessed hint in all five phone text arrays: ISP exits in another city can mislocate weather too. Preserve the local key table and customized phone UI. |
| `90f3f1c` | Deferred | Extending weather display from 3 to 12 hours requires consistent age/expiry on local Home, personalization and phone state, not only upstream's clock. Keep current freshness policy and yesterday's independent retry scheduler. |
| `95cd723` | Skipped | Upstream ADB documentation edit, no desktop behavior. |
| `4d308b0` | Skipped | Upstream 2.13/Lite release numbers. |
| `83d586c` | Deferred with `90f3f1c` | Diagnostic age visibility depends on the unselected 12-hour weather-display policy. |
| `982f231` | Skipped | Release handoff/link publication only. |

## Status

Implementation, L0 verification and desktop diff review complete. Desktop worktree initially clean at `5122a16` and owned by black; no active Gradle process at takeover. Other worktrees and shared devices are untouched. This package closes only the bounded host-side integration; device acceptance remains open.

## Evidence

- Fetch exit0, `29be8ad` → `982f231`; all 25 commits and dependencies reviewed.
- L0: 20 affected cases per flavor passed on StandardDebug and GuardDebug, using Java17. Cases: daily UTC dates (4), long-side photo sampling (3), FengOS brightness access (2), Baofeng discovery (3), Compose accessibility/input (6), existing app-menu input (2). Android/Compose cases use host-side Robolectric SDK23, not a phone, emulator or TV.
- Test evidence combines the initial reports and the targeted successful rerun of `PhotoSamplingTest.narrowTallUploadsAreSampledBeforeDecode` after correcting its power-of-two expectation. Already-passed cases were not repeated. Reports and the consolidated result are in ignored `local/upstream-review-20261120/{initial-standard,initial-guard,targeted-standard,targeted-guard,test-summary.json}`.
- The date tests cover the exact 24-hour boundary, China date-ID mismatch, device time zones, malformed/missing starts and future dates. Sampling covers portrait/landscape symmetry, the existing quality threshold and extreme dimensions. SDK23 fixtures verify brightness is blocked before resolver/vendor access, unrelated packages retain backlight behavior, and only enabled/exported Baofeng entries supplement normal launcher discovery. Compose cases verify merged labels/latest callbacks, disabled semantic actions, unnamed Dock tile open/menu actions and remote press ownership.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh`: exit 0. Five resource sets have the delete label; Node VM checks confirm all five phone text tables retain matching keys and `{place}` placeholders. Final staged whitespace check covers new and tracked files.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: exit 0, 100 actionable tasks; both lint gates, R8 and packaging passed.
- APK assertions: both retain `com.dangbei.tvlauncher`, full `app.orchard.tv.MainActivity`, versionCode 106, min21/target34, the CONFIG receiver and signature-protected read-only migration provider. Standard has HOME; Guard has no HOME. Certificate SHA256 remains `108a0bd59d7b0ce05fc52dbd43be95566db2d86912b499f3ebcf89fcec4f7217`. All DEX class definitions and R8 mappings were checked for embedded player implementations; none were found. Root Gradle still loads only `:app`; media/contract sources and build dependencies are unchanged.
- Standard `0.3.1`: 3,086,275 bytes; APK SHA256 `55e93798650cb185f3258d5af5658f96bab0ae8ad81671992168141421ad3a8e`. Guard `0.3.1-guard`: 3,086,132 bytes; SHA256 `2052b5e090e134191374ea172f1c1cd5bc535a2d1bc9b3c1d017abbd5cad123e`. Manifest dumps, checker and results are in ignored `local/upstream-review-20261120/`.
- F3/FengOS/Baofeng hardware, TalkBack/remote behavior and screensaver appearance remain **未验证**; no device was contacted. No formal Release, HOME/accessibility configuration, signing-key or user-data change.

## Exact next action

On the next idle desktop maintenance run, fetch and review commits after `982f231`, preserving all earlier deferred candidates. A separately coordinated Release device run is needed for F3, FengOS/Baofeng firmware, TalkBack/remote navigation and screensaver appearance; this heartbeat does not authorize shared-device operations.

## Deferred

Battery integration, coherent weather age display, output diagnostics, external icon library, standby and guard state need separate scoped integration. Earlier deferred candidates remain open; this is a review boundary, not a wholesale merge marker.
