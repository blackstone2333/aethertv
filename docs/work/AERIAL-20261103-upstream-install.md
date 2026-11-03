# AERIAL-20261103-upstream-install

Status: review
Owner: desktop conversation (main model)

## Spec

- Harden the desktop installation tools without changing the APK, UI, user preferences, player contracts, or product version (0.3.1/106).
- Adapt upstream install-result handling, failed-build propagation, downloaded APK integrity checks, Android6/7 notification-listener registration/restoration, and T-Cast refusal/probe/clock fixes. Preserve this repository's download channel, whitelist applicationId and full namespace component names.
- Acceptance: offline stubs prove failed builds/downloads/install responses never proceed to grants or installation of stale bytes; listener changes preserve unrelated components and fail closed on unreadable settings; T-Cast detects refusal and remains bounded despite wall-clock changes.
- Owner scope: tools/install.sh, tools/restore.sh, tools/tcast-push.py, tools/tests/**, docs/UPSTREAM_SYNC.md, this packet and the project frontier. No player workspace or shared device operations.
- Dependencies: AERIAL-20261102-desktop-independent; upstream e8c251e..b09b4ed plus earlier install-result chain e2f89ac/7ed618c/897c00e. Retain Java17/min21, com.dangbei.tvlauncher, namespace app.orchard.tv, root :app only, stock launchers and all existing accessibility entries.
- No formal Release, new version, automatic installation, HOME change, permission changes on devices, user data removal, or signing changes. ZIP integrity does not establish APK authenticity; local APK/signature validation remains separate.

## Plan

1. Done: confirm desktop worktree ownership and idle/clean state; fetch and record decisions for all 58 new candidates, keeping older deferred work open.
2. Done: adapt only the bounded installation/tool fixes, including symmetric listener removal and decisive offline regression tests.
3. Done: affected tool tests, shell syntax/project rules, Java17 dual Release and APK contract/signature checks. Commit/push reviewed desktop files; synchronization is established by Git history and the remote ref.

## Status

Desktop worktree initially clean at 0da7ad7, with no Gradle build active. Fetch advanced bf64c30 to b09b4ed. The review includes the earlier 41 informationally inspected commits, giving 58 recorded decisions from e8c251e. Tool changes are validated offline; status remains review because the new installer/listener behavior has no device evidence.

The root causes were Bash command substitution suppressing errexit, old ADB protocols reporting zero for installation failures, curl accepting incomplete/non-APK responses, cmd missing on older Android, a platform-specific nc flag, and T-Cast using wall time/ignoring stage4. The fix explicitly propagates build failures, validates a Success line plus exit status, stages and checks downloads, preserves unrelated listener entries, selects the nc flag per host, and uses monotonic time with immediate refusal reporting. No Android product source changed.

## Evidence

- L0 offline Python: `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/tests -v`, 41 tests, exit0 (22.806s). After adding two install-response edge cases, Gradle stdout and remote-shell listener quoting coverage, reran only affected modules with `PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=tools/tests python3 -m unittest test_build test_install test_notification_listeners -v`: 15 tests, exit0 (16.068s). There are 43 distinct passing tests; unchanged protocol/download/probe tests were not repeated.
- Earlier regression run against the original tools: 36 tests with 23 expected failures, including refusal timeout and wall-clock changes. Listener coverage additionally proves null/empty lists, exact matches, prefix/sibling components, sole-component deletion, idempotency, internal-class dollar characters, failed reads and malformed shell output.
- All adb/curl/nc/Gradle installation actions in tests are stubs; unzip validates synthetic archives. Tests cover standard/guard, interrupted/empty/HTML/truncated/CRC-damaged downloads, original-file preservation, no-unzip fail-closed behavior, explicit local APK use, stale build artifacts, CRLF and false Success substrings. The fixture SDK path prevents accidental real aapt2 calls.
- `bash -n tools/install.sh tools/restore.sh` and `JAVA_HOME=/opt/homebrew/opt/openjdk@17 bash tools/check-project-rules.sh`: exit0. Whitespace and upstream-decision coverage checks pass.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=<local Android SDK> ./gradlew :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`: exit0, 8s, 100 tasks (98 up-to-date). No app/contracts/build changes; previously passing Android unit tests were unaffected and not rerun.
- aapt2 36.1.0 and apksigner: both APKs package com.dangbei.tvlauncher, versionCode106, min21/target34; standard 0.3.1 includes HOME, guard 0.3.1-guard excludes HOME. Whitelist CONFIG and signature-protected LegacyMediaProvider remain present. Certificates and SHA256 are identical to the previous desktop checkpoint: standard `ba946b6146b96400517fe7ceb4452dd009e03f831d00cf3bbbed7c789f602012` (3,072,434 bytes), guard `ffb472806c4a9e45c8c02479d076e96ba152278e3f54ff7ceb71aa942a709223` (3,073,109 bytes). No APK repackaging or new product version was needed.
- Local evidence: `local/upstream-review-20261103/{commits.txt,stat.txt,before-tests.log,tests.log,affected-tests.log,build.log,apk-evidence.json}`. No signing secrets, device addresses or user data enter the commit.
- No devices operated; Android5/6 runtime, F3 listener revival, TCL installation and real remote behavior remain unverified. APK identity is build evidence, not runtime-memory or device-validation evidence. No Release published.

## Exact next action

L0/build/APK work is closed; Git history and the remote ref identify the synchronized tools-only checkpoint. Next maintenance packet can address Android5/6 read-only diagnostics, update TLS, or the settings focus-reversal candidate; retain all previous deferred work and require target-device evidence before claiming installation/revival compatibility.

## Deferred

- Children/elders mode, streaming video uploads/posters, settings focus, guard/volume/HOME chains, manufacturer inputs/brightness, weather localization and independent upstream Lite product changes.
- Existing upstream backlog remains open except the exact adopted scope recorded in UPSTREAM_SYNC.md.
- The historical tools/get.sh bootstrap still points to upstream and is outside this checkpoint; it is not an AetherTV installation entry point. A future bootstrap package must use this fork's reviewed scripts and channel.
