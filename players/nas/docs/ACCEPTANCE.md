# AetherCinema 0.1.0 / 1 — test build acceptance

Date: 2026-10-28. Status: review. This is an installable development Release APK, not a formal product release. Android TV/F3 and household-backend acceptance are separate from fixture results.

## Build and L0

- User-authorized **JDK21**, Temurin21.0.13+9; AGP8.12.0 / Gradle8.14.3. Desktop's build/configuration is untouched.
- `tools/build.sh`: optimized Release, R8, resource shrinking and release lint passed. minSdk23, target34, compile36, applicationId `app.aethertv.cinema`, namespace `app.orchard.tv`, version0.1.0/code1.
- **22 API23 Robolectric tests passed**:20 protocol tests and2 MoonTV tests. The opt-in MoonTV test used the actual authorized deployment for login/search/detail/episode resolution/resume read; it did not write user records. This establishes API23 logic compatibility, not Android6 device UI/codec acceptance.
- Fixtures cover independent Jellyfin/Emby authentication, Emby root and legacy hash, list/detail/resume ticks, direct/remux/transcode selection, external subtitle paths, forced conversion, negotiated-origin rejection, malformed server URLs, error/expired token, response size limit, no credential-following redirects, ordered session payload fields, own encoding stop, opaque art IDs and duration formatting.
- APK signature verification, manifest metadata and permission probe results are recorded below. GPL v2, contributors and102 resolved runtime coordinates/third-party notices are bundled. All47 string keys are present in all five languages.

## Redmi K40S L1 — Android14

The phone is shared. NAS used `local/coordination/nas-device-requested` then an exclusive `nas-device-busy` only after UI/Live released it. All private addresses, account details, snapshots and device logs remain in ignored `players/nas/local/`. No Desktop preferences, media, accessibility, HOME or installed user apps were modified.

Initial Release checks:

- Installation and `cmd package compile -m speed -f app.aethertv.cinema` passed. Xiaomi's USB installation prompt required accepting the observed AetherCinema install.
- Jellyfin protocol fixture: login → poster library → detail → actual AVC/AAC first frame → external subtitle menu/selection → pause/Back → final stop and resumed negotiation at the saved nonzero tick position.
- Emby protocol fixture: independent login → poster library → detail → native first frame, resume and server-converted HLS route. The fixture uses generated media; its HLS route is not a real transcoding server and does not prove actual server-side codec conversion.
- D-pad select/navigation opened details/player, Back returned to source, and focused action/poster feedback was visible.
- A pre-existing user-entered Emby account loaded real views, posters and continue-watching. This only establishes real browsing. Its credentials/data were preserved, and its playback was not used as fixture evidence.
- Initial memory samples before background-art release improvement: login75.3MiB, native playback151.4MiB, browsing157.4MiB, immediate background143.7MiB PSS. These are individual phone samples with warmed caches, not medians or F3 predictions. Final build samples follow separately.

## Final Release checks

- `tools/build.sh -PcinemaProbe :probe:assembleRelease`: exit0, optimized application and optional probe assembled. Final normal test run:21 passed,1 opt-in live test skipped; the earlier explicitly enabled live run passed all22. No application changes followed that successful build.
- Final APK: `app/build/outputs/apk/release/app-release.apk`, **2,231,198 bytes**. SHA256: `c26576568676926c7f6d1ecab0e1f5caa656315e3ea428d1062580880aa6fbf5`. Installed phone package has the same hash. `aapt2` confirms package/version/minSdk23/target34 and launcher entry. `apksigner verify --verbose` exits0 with v1/v2 signatures. This uses a development test signer.
- Actual MoonTV L1: private account login → native search with source labels → episode grid → details → actual HLS first frame. Media3 exposed an in-stream audio track. Pause at26s wrote the selected record with26s progress,10,468s duration and episode1. Force-stop/relaunch loaded its continue card and “从0:26继续”; playback subsequently displayed45s. This verifies a real short playback/record/cold-start resume round trip, not all sources or long playback. The tested stream did not establish external/server subtitle conversion support.
- Same-signer optional probe: snapshot returned2 fixture cards, controlled art opened within64KiB, explicit protected PLAY_ITEM dispatched at10s and native playback advanced to30s. Both an unauthorized shell snapshot query and direct protected Activity launch were denied by `READ_SNAPSHOT`. This verifies the independent app contract; it does not verify the Desktop consumer UI.
- Final individual PSS samples: actual MoonTV native playback **118.3MiB** (121,143KiB), fixture detail/browsing **148.6MiB** (152,134KiB), immediate background **65.5MiB** (67,099KiB). The final process log includes player Release events. These are different states with warmed caches, not a controlled benchmark or proof of a particular improvement percentage. No F3 extrapolation.
- Cleanup targets only recorded own fixture accounts, the temporary probe, reverse18967 and the selected MoonTV test record (baseline absent). Server DELETE returned200 and a read confirmed that record absent. User-entered Emby configuration is preserved. Private evidence stays in ignored `local/acceptance/`.

## Remaining acceptance

- F3 install policy, real TV remote, MStar decoding, long playback, TV audio/subtitle hardware behavior and performance: **未验证**.
- Android6 physical/emulator UI and decoder behavior: **未验证**; API23 test execution is L0 only.
- Actual household Jellyfin playback and full Emby playback/transcoding/version range: **未验证**. Use Accounts to configure real services; fixture success is not proof of compatibility with every server.
- Desktop app-side snapshot/PLAY_ITEM implementation is provided. Desktop rendering and return-focus integration belong to the parent Owner and remain **未验证** until integrated.
- MoonTV source availability is external; some observed media sources returned403/404. No source-management or clearing of existing user records is performed. Repository commit unavailable (supplied repository404); observed deployment fingerprints are in PROVIDERS.md.
- fnOS Video0.8.41: deferred explicitly by user; **未接入**.
