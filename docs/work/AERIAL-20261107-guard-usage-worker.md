# AERIAL-20261107-guard-usage-worker

Status: review
Owner: desktop conversation (main model)

## Spec

- Adapt ccf1172's periodic usage-stats query off the main thread, using the existing shared coroutine IO dispatcher instead of adding a permanently owned thread. Retain the existing SDK≤23, permission-gated, one-second polling schedule and stock-home selection rules.
- Keep one unfinished query per service instance; do not enqueue work behind a blocked Binder call. Recheck binding identity, permission, unlock, screen state, visible Home and intentional stock visits before applying a result. Discard results older than two polling intervals and those superseded by window/input/lifecycle changes.
- Cancel delivery and stop periodic callbacks on unbind/destroy; reconnecting the same service cannot overlap its still-unwinding query. A blocked system Binder call may finish later, but must not start an activity from a stale binding.
- Acceptance: deterministic service tests with a blocked UsageStatsManager prove the UI looper remains usable, no query queue accumulates, valid stock events still act once and stale/revoked/unbound results cannot take Home. Relevant existing guard regressions, Java17 dual Release and APK checks pass.
- Desktop Owner scope: guard/HomeGuardService.kt, guard/HomeGuardUsageTest.kt and desktop docs. Dependencies: 07fa055 and upstream ccf1172; no competing desktop changes/build. No blanket guard merge.
- Java17/min21, com.dangbei.tvlauncher, namespace app.orchard.tv, root :app, own update channel and 0.3.1/106 stay fixed. Preserve layout, Full/Lite glass, aerial preferences, system tools and independent media contracts. No player-worktree changes, device operations, default-HOME/accessibility/permission writes, user-data deletion or formal Release.

## Plan

1. Done: confirm desktop worktree ownership/clean/idle state; fetch upstream through 4c2463e (two new version/documentation commits).
2. Done: adapt the periodic worker with bounded concurrency and stale-result safety; add focused service regressions.
3. Done: affected guard tests, Java17 dual Release, APK/signature/rules checks and upstream decisions. The reviewed desktop commit and matching remote ref identify the synchronized checkpoint.

## Status

Initial HEAD 07fa055; desktop worktree clean/idle and ownership correct. Code, host-side tests, dual Release and APK checks pass. Status stays review because F3 HOME/remote/performance acceptance requires separately coordinated device evidence; no shared device was operated.

Root cause of the blocking risk: periodic foregroundCheck called UsageStatsManager.queryEvents synchronously from the main-thread Handler. A slow system Binder could therefore block input/animations and the service; the upstream Android6 ANR is a report, not an F3 reproduction. The fix moves only that query to the existing coroutine IO pool, keeps one unfinished job per service instance and applies results on Main after state/age/generation checks. Cancellation prevents delivery but cannot interrupt the Binder call; a canceled, unfinished job also blocks replacement work on a reconnect. No dedicated permanent thread, new dispatcher dependency, extra monitoring schedule or manufacturer mapping was added.

## Evidence

- L0: 23 distinct affected guard cases passed per flavor: 15 new HomeGuardUsageTest cases, 3 HomeGuardControlCenterTest cases and 5 CecHomeDirectBootTest cases. The new suite uses Robolectric SDK23 with a bounded, blocking UsageStatsManager shadow and real coroutine IO threads; one unlock-gate case uses SDK26. Existing tests also cover SDK26/34. This is host-side evidence, not emulator/phone/TV acceptance.
- A blocked query runs off the main looper while an independent main callback still runs; repeated polls do not queue queries. Valid stock-home events launch the full namespace component once; newer app events and stock subpages stay alone. Query exceptions recover; missing/revoked permissions and visible Home gate work; newer windows, input, resume/pause, screen-off, intentional stock visits and results older than two intervals suppress delivery. Unbind/rebind cannot overlap the canceled Binder call; a destroyed binding cannot deliver even when a replacement service is running. The locked-user test calls the polling helper directly; the production SDK≤23 scheduling restriction is unchanged.
- Minimal rerun: the initial standard run retained 14 passing new cases; after fixing the rebind test's simulated broadcast permission, only that case and the 8 unrun existing regressions were executed for standard. Guard ran all 23. Logs and XML preserve both runs; no unrelated TLS/menu/tool suites were repeated.
- Java17 command: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=<local SDK> ./gradlew :app:testStandardReleaseUnitTest --tests app.orchard.tv.guard.HomeGuardUsageTest.rebindWaitsForTheCancelledBinderCallToUnwind --tests app.orchard.tv.guard.HomeGuardControlCenterTest --tests app.orchard.tv.guard.CecHomeDirectBootTest :app:testGuardReleaseUnitTest --tests app.orchard.tv.guard.HomeGuardUsageTest --tests app.orchard.tv.guard.HomeGuardControlCenterTest --tests app.orchard.tv.guard.CecHomeDirectBootTest :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`, exit0, 1m40s, 120 tasks (23 executed, 97 up-to-date), including both Release lint gates.
- aapt2/apksigner36.1.0: both com.dangbei.tvlauncher, code106, min21/target34, signature matching the existing desktop certificate. Standard0.3.1 has HOME; guard0.3.1-guard has no HOME. Launcher/Leanback, full namespace MainActivity, CONFIG and signature-protected LegacyMediaProvider remain. Root still loads only :app; R8 mappings contain no player implementation or Compose/JUnit/Android test classes.
- APKs: standard3,076,795 bytes, SHA256 `c1aa0a1e50bccc71199a3bf4cf26b0bd1bc7dbb4d2045023caa53a1cf8fe1fb7`; guard3,076,762 bytes, SHA256 `f7372a741c73cc1eae3d71765c475782228e6fe0e1f58153f0ae899b7b19ff64`. APK size is not runtime-RAM or performance evidence.
- Java17 project-rules and whitespace checks passed. Two new upstream commits and the selected old ccf1172 are individually recorded in UPSTREAM_SYNC.md. Logs/XML/metadata/patches: `local/upstream-review-20261107/`; verified test copies: `local/releases/aethertv-0.3.1-guard-usage-worker/`. Local evidence and signing/device files are excluded from the commit.
- No device connection/installation, default-HOME/accessibility/permission changes, factory-launcher disable, user-data deletion or formal Release. Android5 runtime and F3 HOME/remote/HDMI/volume/multitasking/ANR and memory/performance outcomes remain unverified. Other boot/wake queries still have the limitation below.

## Exact next action

L0/build/APK work is closed; do not repeat unaffected checks. When device time is explicitly coordinated, validate Release Home/stock-home transitions, intentional stock visits, standby/wake, remote/HDMI behavior and performance on F3 before TV acceptance. Independently review the one-shot boot/wake query and pending installation-safety/render candidates without merging the whole upstream guard chain.

## Deferred

- One-shot frontByUsage in the separate boot/wake takeover path remains synchronous; this packet only closes the periodic ccf1172 scope. Do not claim all guard Binder/main-thread risks are removed.
- Other manufacturer boot/foreground/filtering chains, mt5873 install protection, control-color draw reads and prior upstream backlog remain separate work.
