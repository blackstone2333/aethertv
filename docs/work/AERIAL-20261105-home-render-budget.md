# AERIAL-20261105-home-render-budget

Status: review
Owner: desktop conversation (main model)

## Spec

- Keep Home's aerial player from requesting a film-driven display refresh rate. Apply the policy before attaching either the Full OES surface or the Lite SurfaceView; leave the shared player factory and dedicated screensaver/player policies unchanged. A screensaver that continues Home's existing player retains that player's policy.
- Observe tile shadow/frame presence through derived threshold states so their springs do not recompose the entire tile on every frame. Preserve the existing thresholds, animations, reduced-motion behavior, viewport residency and byte-limited caches.
- Preserve application-name shadows while dimmed or fading by modulating text alpha without a text-sized offscreen buffer. Keep the current name-display preferences, geometry and materials.
- Acceptance: the selected Media3 API compiles against 1.8.1/min21 in both Release flavors; the raw animated values remain in draw/layer callbacks, with only boolean presence read during composition; APK contracts and signing identity remain intact. Actual refresh rate, focus appearance and performance require separately coordinated device evidence.
- Desktop Owner scope: ui/home/LiveWallpaper.kt, ui/home/AppTile.kt and desktop project/work/upstream documentation. Dependencies: checkpoint 9787220; upstream 7893823, AppTile-only part of ca6e4f2, and 5026c39. No other Owner occupies these files.
- Java17, com.dangbei.tvlauncher, namespace app.orchard.tv, Android5/6 compatibility, root :app, own update channel and version 0.3.1/106 remain fixed. No device operations, HOME/default/accessibility changes, user-data changes, player workspace changes or formal Release.

## Plan

1. Done: confirm desktop worktree ownership, clean state and no active desktop build; fetch and review the 32 new upstream commits through d2a50c1.
2. Done: adapt the three selected fixes without replacing local rendering, layout or cache models.
3. Done: Java17 dual Release, APK/signature/rules checks, focused source review and all 32 upstream decisions. Git history and the matching remote ref identify the synchronized desktop checkpoint.

## Status

Initial HEAD 9787220. Upstream advanced 916b5d9 to d2a50c1 (32 commits, tags v1.83/v1.84). Source/API/build/APK checks passed. Status remains review: display negotiation, appearance and performance need target-device evidence. Shared devices and other worktrees remain untouched.

Causes: Media3's default seamless frame-rate matching may select a higher display rate while Home is drawn over a film; reading spring values directly in composition invalidates whole tiles on each frame; automatic layer compositing at fractional text alpha clips shadows to the text's bounds. The changes limit the refresh-rate request to the Home role, derive only the visibility thresholds, and use modulated alpha for the label. F3 performance and visual results remain unverified.

## Evidence

- Java17: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=<local SDK> ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`, exit0, 1m58s, 100 tasks (18 executed, 82 up-to-date), including both Release lint gates. Compiles the selected API against the unchanged Media3 1.8.1/Compose 1.9 compatibility line.
- L0 source review: Home configures the strategy before either surface attachment and prepare; AerialStream and dedicated Screensaver/VideoSaver are unchanged. AppTile's original thresholds and spring specifications are unchanged; raw lift/ring/ringIn reads are in layer/draw callbacks, with only derived booleans read by compositional presence checks. Viewport residency, cache limits and name preferences are unchanged.
- No Android unit tests run: there is no corresponding existing test for composition invalidation, layer shadow clipping or physical refresh negotiation. This packet changes rendering observation/compositing and a Media3 setting, without new application algorithms or input behavior. Adding setter/threshold replicas would not validate the actual effects; unchanged buffer, TLS, report and tool tests were not repeated. L0 source/API checks do not establish runtime behavior.
- aapt2 36.1.0/apksigner: both com.dangbei.tvlauncher, code106, min21/target34, signatures matching the existing desktop certificate. Standard 0.3.1 has HOME; guard 0.3.1-guard does not. Launcher/Leanback, full namespace MainActivity, CONFIG and the signature-protected LegacyMediaProvider are retained. Root remains :app; R8 mappings contain no embedded player implementation classes.
- APKs: standard 3,076,555 bytes, SHA256 `41ddd0cb16451188a4d61ac1a45bd094d96304443301768c34c40d169c2850cc`; guard 3,077,232 bytes, SHA256 `7e4ee1ca0fc7db2c52eec795d8dc469dc35e3ec4cf25071ad0ec4dc6da53bc93`. These are artifact sizes, not RAM measurements.
- `tools/check-project-rules.sh` under Java17, whitespace and all-32-commit decision coverage passed. Local build/metadata/patch evidence: `local/upstream-review-20261105/`; immutable test APK copies: `local/releases/aethertv-0.3.1-home-render-budget/`. No signing/private device files enter the commit.
- No phone/emulator/TV operations, automatic installation, formal Release, runtime composition measurements, refresh-rate readings or RAM/frame-time claims. F3/HOME/remote/decoder/performance and visual acceptance remain unverified. Android5/6 compatibility here is source/build metadata evidence, not new runtime evidence; Android6 lacks Surface.setFrameRate, so the refresh-rate setting is not a claimed F3 speed fix.

## Exact next action

L0/build/APK work is closed; do not repeat unaffected tests or builds. Next packet can adapt the remaining draw-phase color reads or reproduce local control-center focus clipping. After separately coordinating a device, check Full/Lite focus transitions and label shadows on bright/dark backgrounds at 4 and 8 columns, actual display rate during aerial playback, and F3 Release performance before claiming device gains.

## Deferred

- The remaining ca6e4f2 focus-color changes need adaptation to local independent controls and material caches; no upstream SmoothCorners cache replacement.
- 97ab5cf targets ScrollState, while local Personalize uses LazyColumn/LazyListState. Do not import its interceptor or replace the local card layout without a suitable lazy-list reproduction.
- Manufacturer HOME/boot/input chains, frame extraction, video upload, weather and earlier backlog remain separate work. No unmeasured F3 RAM or frame-time claim.
