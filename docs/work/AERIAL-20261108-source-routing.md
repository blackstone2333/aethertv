# AERIAL-20261108-source-routing

Status: review
Owner: desktop conversation (main model)

## Spec

- Adapt upstream 4144acd's MStar input-panel fallback after all existing local panel candidates fail. Resolve the custom action by capability, require an exported/enabled/permitted activity and start that explicit component only on the existing user-triggered input-picker path. Keep the local Sources page, fallback order and failure reporting.
- Adapt 5bfe936's JMGO HDMI route only when the generic and DroidLogic routes do not apply. Parse the positive HDMI port from the input service id, require the actual accessible player activity, and preserve input identity, labels, CEC and other manufacturers' routes. No inferred port inventory or automatic input switching.
- Apply 494dfa2's current Xiangyang name while retaining the former name as an alias and the same coordinates. Preserve that correction when regenerating the table; no network/city-preference changes.
- Acceptance: focused host-side Android6 tests cover input routing, access checks, failed starts and existing panel/route priority; synthetic offline generator tests cover old/current names and alias/dedup preservation. Java17 dual Release and APK/package/signature checks pass. Record all 18 new upstream decisions.
- Desktop Owner scope: system/SystemState.kt, assets/cities-cn.tsv, tools/make-cities.py, focused source/generator tests and desktop docs. Dependencies: b5b3109 and the three upstream commits above. Desktop worktree clean/idle with correct ownership; no competing file/build owner.
- Keep Java17/min21, com.dangbei.tvlauncher, namespace app.orchard.tv, root :app, own update channel and 0.3.1/106. Preserve layout/4–8 columns, aerial preferences, Full/Lite glass, system controls and media contracts. Do not touch player worktrees or shared devices, change default HOME/accessibility, disable factory apps, delete user data or publish a formal Release.

## Plan

1. Done: verify desktop ownership/clean/idle state; fetch upstream 4c2463e..69d5972 (18 commits, v1.87) and inspect candidate patches.
2. Done: adapt the two source capabilities and current city-name correction; add focused host/offline regressions.
3. Done: affected tests, Java17 dual Release, APK/rules checks and all18 upstream decisions. The reviewed desktop commit and matching remote ref identify the synchronized checkpoint.

## Status

Initial HEAD b5b3109; desktop worktree ownership correct, initially clean/idle. Code, host/offline tests, dual Release and APK checks pass. Status remains review pending separately coordinated manufacturer/TV acceptance; no shared device was operated.

Causes: openInputPicker returned false as soon as no TCL service existed, leaving firmware's MStar activity untried; frameworkSources had no route for JMGO's port-name extra when passthrough was unresolved. The fix retains existing candidates' priority, adds the resolved and permission-checked MStar activity after they fail, and adds the accessible JMGO HDMI player only after generic/DroidLogic routes fail. Detection is by installed capability, not shared chipset or model. City-name selection favored the most frequent old GeoNames form; canonicalization now runs before aliases/dedup, keeping the former name searchable and all coordinates unchanged.

## Evidence

- L0: nine InputRoutingTest cases pass for each Release flavor under Robolectric SDK23, zero failures/errors. Real Android6 TvInputInfo objects and a host-side TvInputManager shadow exercise the public sources API; recording contexts prevent any actual launch. Tests cover explicit/new-task MStar launch, absent/unexported/disabled/app-disabled/permission-denied activities, granted permission, failed starts, Hisense/TCL priority and exception/null fallbacks, JMGO inputSrc and source metadata, missing/inaccessible players, invalid ports/non-HDMI input types, generic CEC-child priority and DroidLogic priority. This is not emulator, phone or TV evidence.
- Three offline `python3 -m unittest tools.tests.test_make_cities -v` cases pass (0.197s). Tiny synthetic GeoNames files prove old-name-majority/current-name preference, retained aliases/coordinates, adding a missing current alias, canonicalization before county dedup and unchanged unrelated names. The generator runs in a temporary project-shaped directory; no download or production asset regeneration occurred.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=<local SDK> ./gradlew :app:testStandardReleaseUnitTest --tests app.orchard.tv.system.InputRoutingTest :app:testGuardReleaseUnitTest --tests app.orchard.tv.system.InputRoutingTest :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`, exit0, 1m55s, 120 tasks (34 executed, 86 up-to-date), including both Release lint gates. No unrelated guard/TLS/menu/tool test suites were rerun.
- aapt2/apksigner36.1.0: both com.dangbei.tvlauncher, code106, min21/target34, signature matching the existing desktop certificate. Standard0.3.1 has HOME; guard0.3.1-guard has none. Launcher/Leanback, full namespace MainActivity, CONFIG and signature-protected LegacyMediaProvider remain. Root remains :app; R8 mappings contain no player implementation or test classes. Each APK's city asset matches the reviewed source, with one Xiangyang record, the former-name alias and unchanged coordinates.
- APKs: standard3,077,118 bytes, SHA256 `b208fb0c7dd437917b1f29966cbf371c45ba1fe1c02b61833069a9bf31889efd`; guard3,076,910 bytes, SHA256 `181dec1850844228c605c65540c391654174ee4147d149b1e7fdb396437b8bd8`. No runtime memory/performance benefit is inferred from APK sizes.
- Java17 project-rules/whitespace and all18 upstream-decision coverage checks pass. Source review confirms no new background loop, decoder, renderer, network request, preference migration, UI material or navigation change. Logs/XML/metadata/patches are in `local/upstream-review-20261108/`; verified test copies are in `local/releases/aethertv-0.3.1-source-routing/`. Local evidence and signing/device files are excluded from the commit.
- No device connection/installation, automatic signal switching, default-HOME/accessibility/volume changes, factory-launcher disable, user-data deletion or formal Release. F3/Sharp/JMGO input/CEC/HDMI behavior, Android5 runtime, actual city-selection UI and runtime performance remain unverified; upstream reporter results do not close these items.

## Exact next action

L0/build/APK work is closed; do not repeat unaffected checks. When device time is explicitly coordinated, inspect input-page Back/focus, actual source-panel availability and HDMI/CEC switching on the appropriate Release/firmware before TV acceptance. Next assess mt5873 installation safety, remaining draw-phase color reads and control-center focus clipping in separate scopes.

## Deferred

- Guard revival/recent-task/smart-key and boot-volume changes require separate lifecycle/remote/foreground safety review. Brightness profile chains remain separate.
- mt5873 installation protection, one-shot boot/wake usage query, remaining draw-phase colors and prior upstream backlog remain open; no automatic flavor switch or whole-service merge.
