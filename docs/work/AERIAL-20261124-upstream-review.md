# AERIAL-20261124-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review the eight upstream commits after `c6721912` through `7af23a46` (Aerial v2.18). Select
desktop-safe fixes that fit the local `:app` root, preserve the liquid-glass/focus geometry and
the independent player boundary, and remain Java 17 / Android 5/6 compatible.

Acceptance: folder navigation keeps the focused row and its enlarged ring visible; daily-photo
refresh retries after a clock correction; large clock digits use a cached software picture only on
GLES2 hardware and return to normal rendering while moving to the screensaver. Existing 4–8 column
layout, user settings, HOME/Guard safety, version 0.3.1/106 and player contracts remain unchanged.

Owner scope: `app/src/main/**`, affected tests and desktop review documents. The root Gradle still
loads only `:app`; no `lite/` source, player worktree, shared device, default HOME, accessibility
or user data is changed.

## Plan

1. [x] Confirm the independent worktree is clean, owned by the desktop branch and not building; fetch and inspect all eight commits.
2. [x] Adapt the folder row visibility, daily-photo clock rollback and GLES2 large-clock fixes within the root `:app` only.
3. [x] Run affected and full flavor tests, Java 17 rules, dual Release builds and APK assertions.
4. [x] Record evidence, review the diff, commit only the desktop worktree and push the branch.

## Per-commit decisions

| Upstream commit | Decision | Reason and boundary |
| --- | --- | --- |
| `5f7877e2` | Selected, adapted | Use the GPU-detected `soft_clock` path in root `:app`; omit upstream `lite/` changes because this desktop root does not load `lite/`. ADB can force true/false or clear to auto detection. |
| `861475e3` | Selected, adapted | Scroll the whole focused folder row with spring motion and ring room; preserve local soft overflow edges, tile sizes and focus restoration. |
| `b9ab51e0` | Selected, partial | Keep only the shared root daily-photo rollback fix (`since < 0`); do not import the independent Lite Home/Wallpaper lifecycle. |
| `6a7bc8c7` | Deferred | Whaley CEC reflection and HDMI polling are vendor-specific and require a separate hardware safety package; no device evidence is available here. |
| `7c63577c` | Skipped | Changhong device documentation only. |
| `e1565ee2` | Skipped | Upstream triage handoff only. |
| `11512abc` | Skipped | Upstream version metadata; local remains 0.3.1 / 106. |
| `7af23a46` | Skipped | Upstream release/site handoff; no formal release or channel change. |

## Status

Implementation and host-side verification are complete. Selected changes are confined to the desktop
`:app` and do not change player code, settings defaults, versions or device state. Device acceptance
remains **未验证**.

## Evidence

- Fetch exit 0, `c6721912` → `7af23a46`, eight commits and v2.18; behavior-affecting commits were
  reviewed individually. Other worktrees and shared devices were untouched.
- L0: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest`: exit 0, 152 cases per flavor, zero failures/errors.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh` and `git diff --check`: exit 0.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: exit 0, 100 actionable tasks (18 executed, 82 up-to-date); lint, R8 and packaging passed.
- APK contract checks: exit 0. Standard and Guard retain `com.dangbei.tvlauncher`, versionCode 106,
  `app.orchard.tv.MainActivity`, the read-only migration provider and no embedded player classes;
  Standard retains HOME and Guard has no HOME category. Standard SHA256
  `c3cf4bc0918b6df4632654b15459241a25ecdbd7bb93e587d17d77cb6d6334fd`; Guard SHA256
  `9587d3f6caec62ce48ae94078e7f8022ce0ba0fb9b3c335ece66dbc51cb2d38d`.
- No phone, emulator or TV was contacted. F3 folder navigation, GLES2 clock rendering and daily
  photo behavior remain **未验证**; no formal Release, HOME/default/accessibility change,
  signing-key change or user-data operation occurred.

## Exact next action

Keep Whaley CEC and Lite candidates deferred until a separately coordinated hardware/package review;
the next idle pass may review commits after `7af23a46`.

## Deferred

Whaley CEC support, Lite product-line changes, Changhong support claims and all upstream version,
release, website, triage and handoff metadata remain outside this package.
