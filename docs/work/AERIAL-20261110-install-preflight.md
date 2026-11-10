# AERIAL-20261110-install-preflight

Status: review
Owner: desktop conversation (main model)

## Spec

- Adapt old upstream 54926e3 as an explicit installation preflight for exact model mt5873: reject standard before building/downloading/installing, explain the reported boot risk and require a separate guard invocation. Never silently change flavor or set HOME for it. Do not infer that F3/other MStar models share this restriction.
- For mt5873 guard, inspect the actual selected APK with aapt2 before any installer/push/permission/settings step. Require the whitelist package, guard version suffix, full-namespace MainActivity and no HOME category; missing inspector, malformed APK or inconsistent metadata stops the run. Let Android enforce the existing installed signature via in-place install; no uninstall/data-clear fallback. Model lookup failure/empty/error output also stops before installation.
- Adapt new 39446d4's exact TCL Android5.1 textual PM refusal into the existing guarded local-install/T-Cast fallback, retaining exact Success-line plus exit-code acceptance and signature-mismatch failures.
- Acceptance: offline stubs prove safe/unsafe/unknown models, requested/actual flavor mismatch, inspector failure, installed-standard/guard signatures, no mutations on rejection and legacy TCL refusal on both shell status conventions. Run only affected installer/build/download regressions, Java17 dual Release and APK checks; record every new upstream decision. No device operations.
- Desktop Owner scope: tools/install.sh, tools/tests/test_install.py, test_build.py, test_download.py (model stubs), docs/INSTALL.md and desktop work/project/upstream docs. Dependency:786da25, old54926e3 and new39446d4. Initial desktop worktree clean/idle, ownership correct; no competing file/build owner.
- Keep Java17/min21, com.dangbei.tvlauncher, namespace app.orchard.tv, root:app, own update channel, 0.3.1/106, free layout/4–8 columns, aerial preferences, Full/Lite glass, utilities and independent media contracts. Do not change player worktrees or operate shared devices/default HOME/accessibility/factory apps/user data. No formal Release.

## Plan

1. Done: verify desktop worktree/ownership/clean/idle state; fetch69d5972..f1a4315 and inspect new commits plus the older installation-safety candidate.
2. Done: implement the explicit model/APK preflight and textual refusal adaptation; exercise offline failures and preserved install paths.
3. Done: affected tests, Java17 dual Release/APK/rules checks and all 60 upstream decisions. Review only the eight desktop-owned files for this package and synchronize their enclosing commit; no device or formal Release work.

## Status

Initial HEAD786da25. The installer previously lacked mt5873 protection and classified Android5.1 PM text as an unrelated failure. The preflight now rejects the known-risk standard/model combination before build/download and checks an explicitly requested guard's actual APK before installation. Failed model reads and missing/inconsistent APK metadata stop the run. The TCL text reaches the existing guarded fallback under both exit-status conventions; signature conflict still stops without uninstalling or granting permissions.

Code and offline acceptance passed; status remains review because actual mt5873/TCL/F3 installation and boot behavior is unverified. No Android production source changed, so both Release APKs are identical to the November8 checkpoint. Version stays0.3.1/106; no player, UI, decoder or memory changes.

## Evidence

- L0 only, all adb/curl/network/install calls in tests are local stubs. Initial affected suite ran21 tests: build/download6 passed, installer had3 assertion failures because the new fake aapt2 advertised versionCode106 while the simulated installed update was2. Corrected the fixture to2; no product version change. Reran only the affected installer module:15 tests passed, including known/other/failed models, installed standard/guard, all inspector rejection paths, exact success semantics, signature conflicts and both textual-refusal exit statuses. Logs: `local/upstream-review-20261110/install-tests.log` and `install-tests-rerun.log`.
- Inspection review removed `printf | grep -q` from the new metadata checks: a real large manifest could otherwise cause SIGPIPE under pipefail. Here-strings consume stable captured output instead. Exact package matching escapes dots.
- Actual locally built standard and guard APKs were inspected through the same installer with stub adb: supplying standard while requesting guard on mt5873 stops before any device mutation; actual guard reaches stub install, where a synthetic signature mismatch stops further setup. This proves local preflight/metadata compatibility, not a real installation. Evidence: `real-apk-preflight.json` and corresponding calls logs.
- Java17 `:app:assembleStandardRelease :app:assembleGuardRelease` exit0, BUILD SUCCESSFUL (9s,100 tasks,98 up-to-date). Evidence: `release.log`. No unrelated Android tests rerun because no Android source changed.
- `check_apks.py` exit0: both com.dangbei.tvlauncher, namespace MainActivity, min21/target34, versionCode106, standard0.3.1 with HOME, guard0.3.1-guard without HOME, launcher/leanback, signature-protected read-only media provider and matching prior signatures. Root includes only:app; no embedded player or test classes. `tools/check-project-rules.sh` under Java17 and `git diff --check` exit0. Evidence: `apk-evidence.json`, badging/manifest/signature files and `project-rules.log`.
- Standard3,077,118 bytes, SHA256 `b208fb0c7dd437917b1f29966cbf371c45ba1fe1c02b61833069a9bf31889efd`; guard3,076,910 bytes, SHA256 `181dec1850844228c605c65540c391654174ee4147d149b1e7fdb396437b8bd8`. Both unchanged from the November8 checkpoint; no new APK publication is needed for a tools-only patch.
- Upstream fetch69d5972..f1a4315 succeeded;60 commits plus old54926e3 explicitly assessed in UPSTREAM_SYNC. Raw review inputs and all evidence stay ignored under `local/upstream-review-20261110/`. No private paths/addresses/keys copied into tracked files.
- No shared device operations; actual mt5873/TCL/F3 installation/boot behavior remains **未验证**. Existing phone/F3 evidence from other packages does not validate this patch.

## Exact next action

After the reviewed desktop-only commit is synchronized, keep device acceptance separate: only with an explicitly coordinated target and installation authorization, record actual flavor/signature and model-specific outcomes. Continue other upstream candidates from the new ledger boundary without waiting for player worktrees.

## Deferred

- APK installation does not establish boot/remote/HomeGuard acceptance. Existing guard/standard installer system-setup behavior outside this preflight is unchanged and must only run with separately authorized device setup.
- Other upstream features, boot/guard/remote/brightness/weather/media chains and prior performance backlog remain separate; do not merge whole files or upstream version/release metadata.
