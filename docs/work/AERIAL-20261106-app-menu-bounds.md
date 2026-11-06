# AERIAL-20261106-app-menu-bounds

Status: review
Owner: desktop conversation (main model)

## Spec

- Keep app long-press menus inside the current Home canvas, using the measured page height rather than a fixed main-menu estimate or the physical window size. Preserve a 24dp screen margin, tile-relative opening and the existing width/animation/material preferences.
- Bound and scroll the whole menu so all folder, rename and source-device choices remain reachable by D-pad. Reset scroll and first focus on page entry. Retain local soft overflow edges, existing Back/tap dismissal and Pressable's key-up ownership.
- Acceptance: Compose regression tests reproduce placement/long-list failures on the baseline and pass after the fix, including offscreen anchors, bottom-row/last-column anchors, D-pad selection of the last folder and returning to the first row. Java17 dual Release and APK/signature checks pass.
- Desktop Owner scope: ui/home/Overlays.kt, app/build.gradle.kts (test-only Compose dependencies), app/src/test/java/app/orchard/tv/ui/home/AppMenuTest.kt and desktop docs. Dependency: 9751eb1 and upstream 3f343b7. No file ownership conflict or desktop build is active.
- Java17/min21, com.dangbei.tvlauncher, namespace app.orchard.tv, root :app, Media3/Compose compatibility and own update channel stay fixed. Preserve free arrangement, 4–8 columns, aerial choices, Full/Lite glass, control-center half-screen blur and independent media contracts. Version remains 0.3.1/106. No player worktree changes, shared-device operations, permissions/default-HOME changes, deleted user data or formal Release.

## Plan

1. Done: verify desktop ownership/idle/clean state, fetch upstream and review d2a50c1..bf59e7c (16 commits, tag v1.85).
2. Done: add focused Compose tests, confirm both baseline failures and adapt measured bounds/shared scrolling without replacing local material or overflow rendering.
3. Done: affected tests, Java17 dual Release and APK/signature/rules checks; all 16 upstream decisions recorded. Git history and the matching remote ref identify the synchronized desktop checkpoint.

## Status

Initial HEAD 9751eb1. Initial worktree clean, no desktop build active, other worktrees and devices untouched. Both regression tests pass for each flavor under Robolectric SDK23; dual Release and APK checks pass. Status remains review pending separately coordinated real-TV visual/remote acceptance.

Root cause: AppMenu measured with unbounded constraints, estimated menu height as 296dp against LocalWindowInfo, and only clamped the top edge. Folder and rename lists had no scroll container, and the source-device list used a separate fixed-height scroller. This can place taller pages below the canvas or leave final options unreachable. The fix measures against the actual canvas, clamps both axes and shares one bounded, softly faded scroll viewport. Page entry resets scroll before requesting first focus; Back, outside taps and Pressable ownership remain unchanged.

## Evidence

- L0 baseline: Java17 `:app:testStandardReleaseUnitTest --tests app.orchard.tv.ui.home.AppMenuTest`, exit1, 33s, exactly two tests failed against unchanged 9751eb1 product code. The last-column main menu placed the Uninstall label at y=511..528.5dp, beyond the allowed bottom516dp; the folder page had no scroll node. Baseline XML and log are saved locally.
- L0 fixed: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=<local SDK> ./gradlew :app:testStandardReleaseUnitTest :app:testGuardReleaseUnitTest --tests app.orchard.tv.ui.home.AppMenuTest :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`, exit0, 1m37s, 120 tasks (31 executed, 89 up-to-date), including both Release lint gates. Two tests per flavor, zero failures/errors; no unrelated tests rerun.
- Tests are adapted from upstream 3f343b7 and use a real Compose tree under Robolectric SDK23, 960×540dp, 2× density, simulated TV feature and a manually advanced frame clock. They check main-menu labels at Dock/last-column, bottom-row and partially offscreen anchors, the long menu's bounds, D-pad navigation through16 folders with selection of the last ID, Back to Main, reopening the folder page at the first row and selecting New. This is host-side Android6/Compose evidence, not an Android emulator, phone or F3 test.
- Source review: all AppMenu pages use the same measured maximum width/height and scroll viewport, including rename/source-device lists. The material is outside the scroll/fade modifiers, preserving stationary local liquid/frosted glass while softScrollEdges fades overflowing content; no extra background frame capture, decoder, full-screen pass or new animation. No label/name preference or localization changes.
- aapt2 36.1.0/apksigner: both com.dangbei.tvlauncher, code106, min21/target34, signatures matching the existing desktop certificate; standard0.3.1 has HOME, guard0.3.1-guard has none. Launcher/Leanback, full namespace MainActivity, CONFIG and the signature-protected LegacyMediaProvider remain. Root remains :app. R8 mappings contain no player implementation or Compose/JUnit/Android test classes; new dependencies are testImplementation only.
- APKs: standard3,076,172 bytes, SHA256 `8f6556266dce4aa5d5f060a7dc17d952be4cf20c038147f658b5cd033c221a53`; guard3,076,074 bytes, SHA256 `1025f0fdf2346e40d4c0f135c52c00ffc5b1a97de10dd6e34d32d9c9d7c106e7`. APK sizes do not establish runtime RAM savings.
- Java17 project-rules check, whitespace and all16 upstream-decision coverage passed. Logs/XML/metadata/patches: `local/upstream-review-20261106/`; test APK copies and metadata: `local/releases/aethertv-0.3.1-app-menu-bounds/`. Private device/signing files are excluded from the commit.
- No shared-device connection, installation, default-HOME/accessibility/permission changes, deleted user data or formal Release. F3 remote, glass/fade appearance, animation smoothness, Android5 runtime and performance remain unverified; current tests do not measure pixels or frame times.

## Exact next action

L0/build/APK work is closed; do not repeat these unaffected checks. Prioritize a separate usage-stats worker packet for ccf1172 with lifecycle/stale-result safety. When device time is explicitly coordinated, inspect long folder/rename/source-device menus, four/eight-column edge anchors, soft fades and focus enlargement in Full/Lite on F3 Release before marking TV acceptance.

## Deferred

- Manufacturer HOME/boot/filtering/input chains and usage-stat worker changes require separate lifecycle/foreground-safety review.
- Remaining draw-phase color reads, local control-center focus clipping and earlier upstream backlog stay open. No direct ScrollState interceptor for Personalize's LazyListState.
