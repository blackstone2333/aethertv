# AetherCinema isolated development

- Work only in the managed AetherCinema worktree, on `codex/aerial/aerial-20261102-nas-independent` or a later explicitly assigned NAS branch. Do not edit, move or clean files in the original shared checkout.
- Allowed changes: `players/nas/**`, `docs/work/AERIAL-20261028-nas-cinema.md`, and `docs/handoffs/AERIAL-20261028-nas-cinema-handoff.md`. Desktop, Live, shared contracts, other documentation and repository-root build files belong to their respective Owners.
- This application has an independent Gradle root. Run `players/nas/tools/build.sh` from the repository root, or `tools/build.sh` from this directory. Do not run the repository-root Gradle build for NAS work.
- The user explicitly authorized **JDK21 for this application only**. Keep Android minSdk23 and JVM17 bytecode. Desktop remains JDK17/minSdk21; its constraints must not be changed to accommodate this app.
- Preserve GPL, copied-source attribution, contributors and bundled third-party notices. Follow the existing five-language and remote-focus conventions for any later authorized UI work.
- Never commit `local/`, credentials, signing keys, runtime JDKs, generated test data, build outputs or screenshots. Only stage explicit owned paths; do not carry FileProvider permission-only changes from the original checkout.
- Desktop integration remains a proposal in `docs/DESKTOP_CONTRACT.md`. Propose shared-interface changes here for Desktop Owner review; do not edit shared consumers. Desktop and NAS build and ship independently.
- The 2026-11-02 assignment covers environment isolation, build verification and a pushed checkpoint only. Do not expand backend/UI behavior, contact real backends, operate devices or publish a formal release as part of this assignment.
- Historical phone/fixture evidence is not new device acceptance. Read `docs/ACCEPTANCE.md` and the NAS handoff before making compatibility claims.
