# Independent development checkpoint

Date: 2026-11-02. Scope: development isolation only; application behavior and version remain unchanged.

## Checkout and ownership

Base commit: `5edcc5bd2b57a9061a4478a682f4903d1369790b`.

NAS branch: `codex/aerial/aerial-20261102-nas-independent`.

The managed worktree contains the base repository plus the public `players/nas` source and this Owner's work/handoff documents. No uncommitted Desktop, Live, shared-contract or repository-root build changes are imported. The original shared checkout and its ignored household data remain in their original location. File contents were copied without FileProvider permission metadata; source text uses0644, with executable modes only for actual command scripts.

Only `players/nas/**` and these two documents may change on this branch:

- `docs/work/AERIAL-20261028-nas-cinema.md`
- `docs/handoffs/AERIAL-20261028-nas-cinema-handoff.md`

Desktop has its own worktree/branch and does not load this Gradle root or wait for NAS commits. The independent app-side contract is documented in `DESKTOP_CONTRACT.md`; Desktop integration remains a separate task. Shared changes require a proposal under `players/nas/docs` for the Desktop Owner.

## Build

Install JDK21 and Android SDK36. Supply `JAVA_HOME` and `ANDROID_HOME`, or use the script's dedicated ignored `.runtime/jdk-21*/Contents/Home` and a newly created ignored SDK-only `local.properties`. Do not copy the original checkout's local configuration or credentials.

From the repository root:

```sh
env -u AETHER_MOON_SERVER_FILE -u AETHER_MOON_AUTH_FILE \
  ANDROID_HOME="$HOME/Library/Android/sdk" players/nas/tools/build.sh --offline
```

`--offline` requires previously cached Gradle distributions, dependencies and Robolectric runtime artifacts. On a new machine, omit it to obtain build dependencies; household-backend tests remain opt-in. The entry changes directory to this independent Gradle root and runs `testReleaseUnitTest assembleRelease`. Optional `-PcinemaProbe :probe:assembleRelease` compiles the standalone test harness without installing it.

Keep applicationId `app.aethertv.cinema`, version0.1.0/code1, minSdk23, target34 and JVM17. A JDK21 build does not require changing Desktop's JDK17/minSdk21. Output is `players/nas/app/build/outputs/apk/release/app-release.apk`, signed with the local development test key; no formal release is created.

## Evidence and next action

Dedicated Temurin21.0.13+9 build with `--offline -PcinemaProbe :probe:assembleRelease` completed successfully in1m47s:21 tests passed,1 live test skipped; optimized Release, lint and optional probe compiled. APK metadata, v1/v2 signatures, JVM17 class version and bundled notices were verified. Only app/probe tasks ran. No repository-root build configuration was changed.

Rebuilt APK:2,231,210bytes, SHA256 `0625ccf1f1caea54f69290c95a09ed482b0eac6c248874f92b3b8d59ca5793e0`. It was not installed. Original implementation, build and license file contents are preserved. Source checkout branch/status and original NAS/work/handoff file hashes were unchanged after isolation.

Staging audit covers60 owned paths and no unrelated or permission-only changes. New development/handoff documentation passes `git diff --check`. Imported source retains its original bytes, including three existing blank lines with trailing spaces in Pressable and the Windows wrapper's CRLF endings; this assignment does not reformat those files.

Historical backend/phone evidence remains dated2026-10-28 in `ACCEPTANCE.md`; it is not rerun or upgraded by moving the source. The inherited `docs/devices/dangbei-f3.md` separately records initial installation/static startup only. This isolation assignment performs no device operation or real-backend connection.

Future work requires a separately scoped assignment. Leave actual Jellyfin/Emby playback/transcoding, TV remote/decoder/performance acceptance, Desktop consumer integration and deferred fnOS work explicitly pending. No build or integration dependency blocks the Desktop Owner.
