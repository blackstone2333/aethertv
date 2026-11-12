# AERIAL-20261112-upstream-review

Status: complete
Owner: desktop conversation (main model)

## Spec

- Fetch and assess every commit after the 2026-11-10 ledger boundary through upstream Aerial 31deac6 (v1.91–v1.94).
- Select only changes that fit the desktop worktree, the local TV UI, Java 17, Android 5/6 compatibility, the fixed package/namespace, and the existing free layout, aerial preferences, glass, system tools and independent-player contracts.
- Preserve local behavior and user settings when an upstream patch conflicts. Do not import upstream release numbers, icon assets, Lite/player modules, device claims, HOME/accessibility changes or site documents as a side effect.
- No shared-device operations, no factory-app changes, no user-data changes and no formal Release.

## Plan

1. Done: verify the independent worktree, ownership, clean state and absence of a desktop build.
2. Done: fetch f1a4315..31deac6, inspect all 37 commits and save the raw list/diff under ignored local evidence.
3. Done: record a per-commit decision; no production source change met the current desktop acceptance boundary.
4. Done: run the rules check and whitespace check; reuse the 2026-11-10 Java 17 Release/APK evidence because this pass changed no Android source.

## Status

The worktree is the desktop-owned /Users/black/.codex/worktrees/aethertv-desktop/AetherTV worktree on codex/aerial/aerial-20261102-desktop-independent, initially clean at efb1b20. Upstream fetch succeeded and advanced upstream/main to 31deac6, adding 37 commits and tags v1.91–v1.94. The tree remains clean after review. No source or APK was changed.

The potentially useful wallpaper crash/retriever patches conflict with the local glass/video path and need a separate performance work package. Guard boot/frontmost patches change takeover semantics and require a dedicated Android 5/6 regression matrix. FengOS settings and new input routes need capability tests before changing the local settings/input hierarchy. Icon-library v2 is a large asset and phone-console change that would dilute the local TV UI and increase the APK surface. None was silently merged.

## Evidence

- Independent worktree and file ownership verified; other Live/Cinema worktrees were not touched.
- git fetch upstream --prune exited 0; upstream/main is 31deac69edc56de9d773c208787b939e2c54a76d.
- git diff --check exited 0.
- JAVA_HOME=/opt/homebrew/opt/openjdk@17 tools/check-project-rules.sh exited 0. The root Gradle still includes only :app; applicationId, namespace, minSdk/targetSdk and Java 17 remain fixed.
- No affected source tests or dual Release builds were rerun because this pass changed no production or test source. The prior 2026-11-10 evidence remains authoritative: Java 17 dual Release and APK contract checks passed, and both APK hashes were unchanged from the 2026-11-08 checkpoint.
- No target device was connected or modified. F3, Hisense, FengOS, Skyworth, Baofeng, XGIMI and other vendor behavior remains 未验证.

## Per-commit decisions

| Commit | Decision |
| --- | --- |
| 6a5435e | Defer: Hisense/Vidda ADB instructions are device documentation; keep local device notes and do not infer F3 behavior. |
| 5ef8f47 | Defer: icon-library v2 changes app assets, phone selection and generated data; local icon/category/layout behavior stays authoritative. |
| 3adcef0 | Defer: new icon sources and generation tools add a large asset/licensing/build surface outside the desktop acceptance target. |
| e70cd7a | Skip: upstream version 1.91/129 and Lite numbering cannot change local 0.3.1/106. |
| 5877c26 | Skip: upstream release handoff/site deployment is background only. |
| 18466ae | Defer with icon-library work: immediate phone preview depends on the unselected asset/index contract. |
| e7c6eab | Skip: upstream version 1.92/130 and Lite numbering are not local product changes. |
| 960dd85 | Skip: release handoff/site deployment does not change desktop source behavior. |
| d32c303 | Defer: locked-boot recovery for XGIMI H3S can start Home before unlock and changes guard takeover semantics; no F3 evidence or local regression package. |
| 8e7d38b | Defer with icon-library work: rereading the generated icon index only matters if that asset contract is adopted. |
| e25a05d | Skip: upstream version 1.93/131 and Lite numbering are not local product changes. |
| 1784388 | Skip: release handoff/site deployment is background only. |
| 660f0ee | Defer: persistent PixelCopy crash suppression is valuable for one Hisense MTK9632 report but conflicts with the local LiveWallpaper implementation and needs a targeted crash/recovery test. |
| 3205981 | Defer: retriever refusal falls back to in-app frame decoding, increasing decoder/CPU/memory pressure against the Lite and F3 constraints. |
| b3800e2 | Defer: pre-Android-9 extractor probing is tied to the upstream frame-reader chain; adapt only with local Android 5/6 diagnostic coverage. |
| 1720194e | Defer: filtering Home while AetherTV is the default changes explicit guard semantics; the local guard has no matching opt-in contract or F3 evidence. |
| 54a8a60 | Skip for this tree: the local desktop has no matching Skyworth SendHotKey path; do not add an unverified broadcast route. |
| d34e24c | Skip for this tree: Baofeng's Android 4.4 Lite-only source overlay is outside the root :app desktop scope. |
| 2d75a47 | Skip for this tree: the change is in the upstream Lite module, which the desktop root does not load. |
| d1ca090 | Defer: watching an app left in front at boot must be integrated with the local guard usage worker and explicit foreground safety, not copied in isolation. |
| a78a9e9 | Defer: leaving an unknown foreground app in front after guard revival changes Home ownership and needs Android 5/6 lifecycle tests. |
| 82a497a | Skip as a merge container: assess its underlying locked-boot changes separately. |
| 9ebb603 | Defer with d32c303: one-shot locked-boot start still changes takeover behavior and has no local target-device evidence. |
| 6ac2257 | Defer: FengOS settings/network routing is useful only after a capability test and must preserve the local in-app settings hierarchy and focus behavior. |
| 3f8cfb9 | Defer: leaving a custom video wallpaper on its frame touches the local glass/video lifecycle and needs a memory and transition test before adoption. |
| ea2900c | Defer: compiling behind the screensaver can reduce startup cost but adds a background CPU task on Android 7–11; benchmark it separately for Lite/F3. |
| 1fd3d4d | Skip: DBX5S Pro README/device notes do not constitute local compatibility evidence. |
| 880ecfd | Skip: vendor device notes are not source changes and cannot alter the F3 acceptance record. |
| 5ab21ea | Skip: upstream handoff/architecture documents do not replace local ownership, rules or verification facts. |
| 8831526 | Skip: Hisense monitor README information is background only. |
| ef5da19 | Skip: upstream second-look handoff is background only. |
| c16b16a | Defer with icon-library work: additional credited icons are asset/index changes outside this pass. |
| dc774a3 | Skip as a merge container: its icon/ADB subchanges were assessed separately. |
| e8e2760 | Skip: brand-specific ADB documentation does not change the local installer or prove F3 behavior. |
| 48492eb | Skip: upstream icon handoff is background only. |
| 7032147 | Skip: upstream version 1.94/133 and Lite numbering are not local product changes. |
| 31deac6 | Skip: upstream release handoff/site deployment is background only. |

## Exact next action

When a target device or a dedicated performance test is available, open a new work package for one bounded candidate (wallpaper crash/retriever handling, FengOS settings routing or guard boot semantics). Do not combine those chains or change the desktop version for maintenance.

## Deferred

- No upstream release, site, icon-library, Lite, Java 21, player, HOME, accessibility, factory-app or user-data change was imported.
- No device-specific result is promoted to F3 evidence.
