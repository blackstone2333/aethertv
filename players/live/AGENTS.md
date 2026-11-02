# AetherLive independent workspace

- Work only in this managed worktree on `codex/aerial/aerial-20261102-live-independent`.
- Own `players/live/**` and the independent-media work package/handoff. Do not edit the original shared checkout, `app/**`, repository-root Gradle files, desktop docs, or NAS code.
- `contracts/**` is a frozen copy of MediaContract v1. Do not change it; propose protocol changes in the player work package for the desktop Owner to review.
- Run the standalone build from this directory with Java 17 and `./gradlew testReleaseUnitTest assembleRelease`. It loads only AetherLive, not the inherited repository-root `:app` project.
- Keep minSdk 21 and the existing local signing strategy: repository `local/signing.properties` and referenced key if provisioned privately, otherwise the local Android debug key for test Release builds. Do not copy or commit keys, household data, or device evidence.
- The 2026-11-02 isolation task authorizes source checkpoint commit/push only. Do not expand functionality, operate devices, change HOME/accessibility, clear data, or publish a Release for this task.
- Original phone evidence remains in the original checkout's ignored `local/`. F3 installation/static startup was recorded separately; playback, remote behavior, decoding, and GL performance remain unverified.
