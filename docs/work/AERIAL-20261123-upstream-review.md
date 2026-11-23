# AERIAL-20261123-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Spec

Review the 14 upstream commits after `695e4a16` through `c6721912` (Aerial v2.17). Select only
desktop-safe fixes that fit the existing `com.dangbei.tvlauncher` / `app.orchard.tv` boundary,
Java 17, Android 5/6 compatibility line, independent player contract and local update channel.

Acceptance: Control Center refreshes when a TV changes mute without changing stream level; Android
5/6 Guard foreground sampling still stays off the main thread and can recover a future-dated usage
stats bucket without accepting stale future events. Existing HOME, boot-source, glass, layout,
wallpaper, IPTV and user-data behavior remains unchanged.

Owner scope: `app/src/main/java/app/orchard/tv/MainActivity.kt`,
`app/src/main/java/app/orchard/tv/guard/**`, affected tests and desktop review documents. No
player-worktree changes, shared-device operation, HOME/default/accessibility change, version bump,
dependency change or formal Release.

## Plan

1. [x] Confirm the independent worktree is owned by the desktop branch, clean and not building; fetch and inspect all 14 commits.
2. [x] Adapt the mute broadcast and future-dated usage bucket handling with host-side regression tests.
3. [x] Run Java 17 rules, both flavor test suites, dual Release builds and APK assertions.
4. [x] Record evidence, review the diff, commit only the desktop worktree and push its branch.

## Per-commit decisions

| Upstream commit | Decision | Reason and boundary |
| --- | --- | --- |
| `96c3962d` | Deferred | Boot-open persistence is not present locally; do not invent a new takeover state without a separate wake/boot safety package and Android 5/6 evidence. |
| `48617238` | Deferred | The local Guard update/revival lifecycle is structurally different and already protects installer/stock-home paths; defer the full foreground-window chain rather than risk covering playback. |
| `c46dd318` | Skipped | Upstream website command metadata; local update channel and phone controls are independent. |
| `aa0b6731` | Skipped | Upstream Kids mode files are not present in this desktop branch. |
| `9409b69d` | Skipped | Installs/changes vendor HOME properties; outside the desktop safety boundary. |
| `2e373fbf` | Selected, adapted | Add `STREAM_MUTE_CHANGED_ACTION` to the existing exported volume receiver so Control Center reflects firmware mute changes. |
| `78396661` | Skipped | Device documentation only; cannot become local device evidence. |
| `9a02a2a7` | Skipped | Device documentation only; `filterHomeAlways` is not enabled by default here. |
| `b54a01cf` | Deferred | Xiaomi MediaTek `running_package_name` needs vendor capability and Guard takeover regression on real hardware. |
| `f48262ce` | Selected, adapted | Query usage events with an open end to include a future-dated active bucket, discard stale future timestamps, and keep the query off the main thread. |
| `faa3a932` | Deferred | Hisense VIDAA tile artwork is visual/vendor-specific and has no local target-device evidence; do not change generic tile precedence. |
| `d37e205c` | Skipped | Upstream handoff document only. |
| `ccc70f49` | Skipped | Upstream version metadata; local product remains 0.3.1 / 106. |
| `c6721912` | Skipped | Upstream release/site handoff; no formal release or channel change. |

## Status

Implementation and host-side verification are complete. The two selected fixes are applied without
changing player code, settings, versions or device state. Device acceptance remains **未验证**.

## Evidence

- Fetch exit 0, `695e4a16` → `c6721912`, 14 commits and v2.17; all behavior-affecting commits were
  reviewed individually. Other worktrees and shared devices were untouched.
- L0: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:testStandardDebugUnitTest :app:testGuardDebugUnitTest`: exit 0, 152 cases per flavor, zero failures/errors.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh` and `git diff --check`: exit 0.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/black/Library/Android/sdk ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease`: exit 0, 100 actionable tasks (18 executed, 82 up-to-date); lint, R8 and packaging passed.
- APK contract checks: exit 0. Standard and Guard retain `com.dangbei.tvlauncher`, versionCode 106,
  `app.orchard.tv.MainActivity`, the read-only migration provider and no embedded player classes;
  Standard retains HOME and Guard has no HOME category. Standard SHA256
  `48fe6dd14d2ebabd9ca51abbee285b88e9b90dd3bdcaf64c538dc55699bf6cc4`; Guard SHA256
  `36356ae3a789741dbfe46eb7cb591da33d099321cfd2a477f10f1cc5b893afe6`.
- No phone, emulator or TV was contacted. F3 mute/Guard behavior, remote navigation and vendor
  usage-stats behavior remain **未验证**; no formal Release, HOME/default/accessibility change,
  signing-key change or user-data operation occurred.

## Exact next action

Keep the older Guard boot/update candidates deferred until a separately coordinated Android 5/6 and
F3 safety run provides evidence; the next idle pass may review commits after `c6721912`.

## Deferred

Guard boot-source persistence, vendor front-window observers, Hisense tile integration and all
upstream HOME/property, Lite, website, release and device-document changes remain open for separately
scoped review.
