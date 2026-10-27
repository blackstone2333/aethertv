# AetherTV

A home media launcher for Android TV 5.0+, based on [pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv), with its MIT license and copyright retained. Dangbei F3 is the primary target.

[中文](README.md) · **English**

## 0.3.1 preview

- Aerial home grid, folders, wallpaper, screensavers and QR-based phone management.
- AetherTV branding and five UI languages.
- Auto / Full / Lite rendering, selectable in the phone console under More, with background image cache reclamation.
- Lite retains cached static glass and a transparent, directionally lit video Dock, with smaller video buffers and bounded image/offscreen tile raster caches.
- M3U/XMLTV configuration and foreground HLS/TS playback, channel navigation, EPG and bounded source fallback.
- Independent desktop/menu glass, focus-responsive Dock clearance, 4–8 columns, grid-only names and optional direct system controls.
- Documented work packages for human and Agent collaboration, Java 17 builds and two release flavors.

Foreground playback and desktop settings have Redmi K40S phone evidence. Home previews, NAS posters and network mounts remain [Roadmap](ROADMAP.md) work. See the [0.3.0 work package](docs/work/AERIAL-20261027-liquid-controls.md) for evidence and target-TV limits.

In a single Lite comparison on Redmi K40S / Android 14, median PSS was about 211/251/239 MiB for static Home/aerial Home/settings, roughly 17%/15%/22% below 0.3.0. These phone samples do not establish F3 results. See the [performance work package](docs/work/AERIAL-20261027-lite-performance.md) for conditions and limits.

## Build and install

Use the [0.3.1 prerelease](https://github.com/blackstone2333/aethertv/releases/tag/v0.3.1) and [installation guide](docs/INSTALL.md), or build from source:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleStandardRelease assembleGuardRelease
```

APKs are under `app/build/outputs/apk/<flavor>/release/`. Future in-app updates use this fork’s [Releases](https://github.com/blackstone2333/aethertv/releases). Upstream Aerial APKs are not updates for this fork.

Keep `com.dangbei.tvlauncher` as the application ID and `app.orchard.tv` as the namespace. Never disable or remove the Dangbei stock launcher `com.dangbei.leard.leradlauncher`. Phone testing does not verify F3 HOME takeover, black screens, remote input, HDMI, standby or hardware decoding; those remain unverified.

## Contributing

Start with [AGENTS.md](AGENTS.md), the [project map](docs/PROJECT.md), [workflow](docs/AGENT_WORKFLOW.md) and the relevant [work package](docs/work/) / [handoff](docs/handoffs/). Work unit IDs use `AERIAL-YYYYMMDD-<slug>`.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest
```

Keep real feed addresses, device details, tokens, credentials and signing keys local. Never commit `local/`. Historical device notes describe upstream evidence, not validation of this fork.

[Issues](https://github.com/blackstone2333/aethertv/issues) · [MIT License](LICENSE)
