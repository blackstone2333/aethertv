# AERIAL-20261104-legacy-update-diagnostics

Status: review
Owner: desktop conversation (main model)

## Spec

- Extend updater HTTPS trust on Android5/6 with the published ISRG Root X1 while retaining system trust, certificate validation and the default hostname verifier. Apply only to individual updater connections; no global TLS, hostname or cleartext changes.
- Make diagnostic collection tolerate missing cmd and empty optional services, preserve full namespace components under the whitelist package, and mask adjacent IPv4 addresses. Reports remain local and use this repository's issue channel.
- Acceptance: real certificate-chain tests reject unknown/expired server certificates and do not extend client trust; connection tests prove SDK scoping and unchanged hostname/global defaults. Offline ADB stubs prove legacy/modern reports reach completion without mutating device settings or launching an application.
- Desktop Owner scope: app/src/main/java/app/orchard/tv/update/{Updates.kt,LegacyUpdateTls.java}, public root resource, update tests/fixtures, tools/report.sh, tools/tests/test_report.py and desktop documentation. No player workspace or device operations.
- Dependencies: 6bb8f5d desktop checkpoint; upstream fd25188/fa1c201 and 6a32cc2. Java17/min21, com.dangbei.tvlauncher, namespace app.orchard.tv, root :app, own update channel and 0.3.1/106 remain fixed.
- No UI/layout/material changes, HOME/permissions/accessibility changes, deleted user data, new background work or formal Release. Android5/6/F3 runtime remains unverified without a target device.

## Plan

1. Done: verify clean desktop ownership and idle state, fetch upstream and inspect the selected candidates plus the new b09b4ed..916b5d9 range.
2. Done: adapt scoped TLS and read-only diagnostic fixes; add only affected certificate/connection and offline report tests.
3. Done: affected tests, Java17 dual Release, APK/signature/rules checks and all 50 upstream decisions. Git history and the matching remote ref identify the synchronized checkpoint.

## Status

Initial HEAD 6bb8f5d, clean worktree, no desktop build active. Upstream advanced b09b4ed to 916b5d9 (50 commits, tags v1.81/v1.82). Selected code/L0/build checks passed. Status remains review: no device operations or new target-machine evidence.

Root causes: older Android may lack the additional public trust anchor; the diagnostic tool treated missing cmd output as a resolved HOME, stopped on empty optional grep results, used package-relative components despite the separate namespace, and left every second adjacent IPv4 address unmasked. The fix adds a connection-scoped trust manager, validates HOME output and tolerates absent read-only sections, uses full components, removes automatic starts, and rescans shared IP boundaries. Existing server trust and hostname checking, user settings and device state remain intact.

## Evidence

- Java17: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=<local SDK> ./gradlew :app:testStandardReleaseUnitTest :app:testGuardReleaseUnitTest --tests app.orchard.tv.update.LegacyUpdateTlsTest :app:assembleStandardRelease :app:assembleGuardRelease --console=plain`, exit0, 2m11s, 120 tasks (54 executed, 66 up-to-date). Both flavors: 13 affected tests, no failures/errors. Unaffected Android tests were not rerun.
- Certificate-chain tests use public synthetic certificate fixtures, not private keys or live network. Existing system-root and additional-root chains pass; unknown/expired chains and additional-root client authentication fail. Tests verify the published root fingerprint, actual additional-root factory, HTTP no-op, SDK24 no-op, per-connection factory and unchanged global/hostname verifiers (including a pre-existing rejecting verifier).
- Robolectric SDK23/24; SDK21/22 only exercise guard branches by simulation on SDK23. No Android5 TLS runtime or live GitHub/F3 handshake evidence; the custom rejecting-verifier test establishes preservation, not a live hostname-mismatch handshake.
- Offline Python: `PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=tools/tests python3 -m unittest test_report -v`: nine masking tests passed, four integrated cases exposed a Bash3.2 parsing error in the initial fallback string. After removing its apostrophe from parameter expansion, `bash -n tools/report.sh` and only `test_report.ReportCollectionTest` were rerun: four tests passed, exit0 (4.463s). All 13 distinct report tests passed; unchanged masking tests were not repeated.
- Report cases cover legacy no-cmd/absent optional services, modern resolved HOME, unsupported cmd output, missing in-app diagnostics, completion and full namespace listener counting. ADB and clipboard commands are stubs; allowed ADB calls are restricted to connection/read-only queries. The tests reject any application start or device-setting mutation and prove both shell log and app-report adjacent addresses are masked.
- aapt2 36.1.0/apksigner: both com.dangbei.tvlauncher, code106, min21/target34, matching previous signing certificates. Standard 0.3.1 has HOME; guard 0.3.1-guard does not. CONFIG and signature-protected LegacyMediaProvider retained; R8 mapping has the TLS helper and no embedded live classes. Packaged PEM DER fingerprint verified in both APKs.
- APKs: standard 3,076,301 bytes, SHA256 `f03742f5348f720a3038b0340784702832748d41e466b7925cadb4bef940ba8f`; guard 3,077,069 bytes, SHA256 `6028219a8aac8e46e8e9d099262c2de8531abe5723d7d95510b8900491e8ffc1`. These sizes describe APKs, not runtime RAM.
- `tools/check-project-rules.sh` under Java17, shell syntax, whitespace and all-50-commit decision coverage passed. Root remains :app; no player or root-build/contract changes. Local logs/metadata: `local/upstream-review-20261104/`; local test artifacts: `local/releases/aethertv-0.3.1-legacy-update-diagnostics/`. No private device information or signing files enter the commit.
- No phone/emulator/TV operations, installed updates or formal Release. F3/HOME/remote/decoder/performance and real Android5/6 update connectivity remain unverified.

## Exact next action

L0/build/APK work is closed; the commit and remote ref identify synchronization. Next packet can adapt settings focus reversal, focus-color draw-phase reads, or wallpaper-only refresh-rate policy. Preserve user configuration and complete the corresponding target-device checks before claiming compatibility or performance gains.

## Deferred

- Upstream animation/render, refresh-rate, weather-location and manufacturer HOME/input/brightness chains require separate scoped work. No blanket merge of upstream UI, automatic compilation or startup-setting writes.
- Earlier backlog remains open; this packet closes only explicitly adopted TLS and diagnostic behavior.
